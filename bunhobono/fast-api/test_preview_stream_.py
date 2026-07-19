from pathlib import Path
import os
import time
import shutil
import threading

import cv2
import httpx
import numpy as np

from yolo_detect import PlateDetector
from plate_ocr import PlateOCR


BASE_DIR = Path(__file__).resolve().parent

SPRING_URL = os.getenv(
    "SPRING_URL",
    "http://localhost:80/api/camera-data/ocr"
)

VIDEO_DIR = BASE_DIR / "runtime" / "videos"

STREAM_FRAME_DIR = BASE_DIR / "runtime" / "test_stream_frames"
STREAM_CROP_DIR = BASE_DIR / "runtime" / "test_stream_crops"
STREAM_CANDIDATE_DIR = BASE_DIR / "runtime" / "test_stream_candidates"

STREAM_FRAME_DIR.mkdir(parents=True, exist_ok=True)
STREAM_CROP_DIR.mkdir(parents=True, exist_ok=True)
STREAM_CANDIDATE_DIR.mkdir(parents=True, exist_ok=True)


TEST_STREAMS = [
    {
        "videoName": "cam1",
        "source": str(VIDEO_DIR / "cam1.mp4"),
        "cameras": [
            {"cameraNo": 1, "name": "cam1_as_camera1"},
            {"cameraNo": 2, "name": "cam1_as_camera2"}
        ]
    },
    {
        "videoName": "cam2",
        "source": str(VIDEO_DIR / "cam2.mp4"),
        "cameras": [
            {"cameraNo": 3, "name": "cam2_as_camera3"},
            {"cameraNo": 4, "name": "cam2_as_camera4"}
        ]
    }
]


# =========================
# 설정
# =========================

FRAME_STEP = 60
CROP_COUNT = 2
SEND_COOLDOWN_SECONDS = 10
MIN_OCR_SCORE = 0.5

# GPU 사용이 안 되면 "cpu"로 변경
OCR_DEVICE = "gpu"

PREVIEW_WIDTH = 640
PREVIEW_HEIGHT = 360

SKIP_SECONDS = 5
LONG_SKIP_SECONDS = 30


def draw_text(
    frame,
    text,
    x,
    y,
    color=(0, 255, 255),
    size=0.7,
    thickness=2
):
    cv2.putText(
        frame,
        str(text),
        (x, y),
        cv2.FONT_HERSHEY_SIMPLEX,
        size,
        color,
        thickness,
        cv2.LINE_AA
    )


def make_blank(text):
    frame = np.zeros(
        (PREVIEW_HEIGHT, PREVIEW_WIDTH, 3),
        dtype=np.uint8
    )

    draw_text(
        frame,
        text,
        30,
        PREVIEW_HEIGHT // 2,
        (0, 255, 255),
        0.9
    )

    return frame


class PlaybackControl:
    """
    두 영상 작업자가 공유하는 재생 제어 상태
    """

    def __init__(self):
        self.lock = threading.Lock()

        self.paused = False
        self.stopped = False

        self.seek_version = 0
        self.seek_mode = None
        self.seek_value = 0

    def toggle_pause(self):
        with self.lock:
            self.paused = not self.paused
            return self.paused

    def is_paused(self):
        with self.lock:
            return self.paused

    def stop(self):
        with self.lock:
            self.stopped = True

    def is_stopped(self):
        with self.lock:
            return self.stopped

    def request_relative_seek(self, seconds):
        with self.lock:
            self.seek_version += 1
            self.seek_mode = "relative"
            self.seek_value = seconds

    def request_restart(self):
        with self.lock:
            self.seek_version += 1
            self.seek_mode = "absolute"
            self.seek_value = 0

    def get_seek_command(self, worker_version):
        with self.lock:
            if worker_version == self.seek_version:
                return worker_version, None, 0

            return (
                self.seek_version,
                self.seek_mode,
                self.seek_value
            )


class TestPreviewStreamWorker:
    def __init__(
        self,
        stream,
        detector,
        ocr_reader,
        model_lock,
        playback_control
    ):
        self.video_name = stream["videoName"]
        self.source = stream["source"]
        self.cameras = stream["cameras"]

        self.detector = detector
        self.ocr_reader = ocr_reader
        self.model_lock = model_lock
        self.playback_control = playback_control

        self.camera_index = 0
        self.camera_no = self.cameras[0]["cameraNo"]
        self.camera_name = self.cameras[0]["name"]

        self.last_send_time = {}
        self.candidates = []

        self.preview_lock = threading.Lock()
        self.latest_frame = make_blank(
            f"{self.video_name} loading"
        )
        self.latest_frame_no = 0
        self.latest_status = "loading"

        self.seek_version = 0

    def run(self):
        while not self.playback_control.is_stopped():
            camera = self.cameras[self.camera_index]

            self.camera_no = camera["cameraNo"]
            self.camera_name = camera["name"]

            print("")
            print(f"[{self.video_name}] 시작")
            print(
                f"[{self.video_name}] "
                f"cameraNo={self.camera_no}, "
                f"name={self.camera_name}"
            )
            print(f"[{self.video_name}] source={self.source}")

            self.run_video_once()

            if self.playback_control.is_stopped():
                break

            self.camera_index += 1

            if self.camera_index >= len(self.cameras):
                self.camera_index = 0

        print(f"[{self.video_name}] 작업자 종료")

    def run_video_once(self):
        cap = cv2.VideoCapture(self.source)

        if not cap.isOpened():
            print(
                f"[{self.camera_name}] "
                f"영상 열기 실패: {self.source}"
            )

            self.publish_message("video open failed")
            time.sleep(3)
            return

        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_count = cap.get(cv2.CAP_PROP_FRAME_COUNT)

        if fps <= 0:
            fps = 30

        print(f"[{self.camera_name}] 영상 재생 시작")
        print(
            f"[{self.camera_name}] "
            f"fps={fps}, totalFrame={frame_count}"
        )

        self.candidates = []

        try:
            while not self.playback_control.is_stopped():
                self.apply_seek_command(cap, fps)

                if self.playback_control.is_paused():
                    time.sleep(0.03)
                    continue

                ret, frame = cap.read()

                if not ret:
                    print(f"[{self.camera_name}] 영상 종료")
                    break

                frame_no = int(
                    cap.get(cv2.CAP_PROP_POS_FRAMES)
                )

                if frame_no % FRAME_STEP != 0:
                    self.publish_frame(
                        frame=frame,
                        frame_no=frame_no,
                        status="scanning"
                    )
                    continue

                self.process_frame(
                    frame=frame,
                    frame_no=frame_no
                )

        finally:
            cap.release()

    def apply_seek_command(self, cap, fps):
        version, mode, value = (
            self.playback_control.get_seek_command(
                self.seek_version
            )
        )

        if version == self.seek_version:
            return

        self.seek_version = version
        self.candidates = []

        total_frames = cap.get(
            cv2.CAP_PROP_FRAME_COUNT
        )

        if mode == "absolute":
            target_frame = int(value * fps)

        else:
            current_frame = cap.get(
                cv2.CAP_PROP_POS_FRAMES
            )

            target_frame = int(
                current_frame + (value * fps)
            )

        target_frame = max(0, target_frame)

        if total_frames > 0:
            target_frame = min(
                target_frame,
                int(total_frames - 1)
            )

        cap.set(
            cv2.CAP_PROP_POS_FRAMES,
            target_frame
        )

        # 일시정지 중 이동해도 이동한 화면이 보이도록
        ret, frame = cap.read()

        if ret:
            current_frame = int(
                cap.get(cv2.CAP_PROP_POS_FRAMES)
            )

            self.publish_frame(
                frame=frame,
                frame_no=current_frame,
                status="seek"
            )

        print(
            f"[{self.video_name}] "
            f"이동 frame={target_frame}"
        )

    def process_frame(self, frame, frame_no):
        filename = (
            f"{self.camera_name}_{frame_no}.jpg"
        )

        preview_frame = frame.copy()

        with self.model_lock:
            detect_result = (
                self.detector.detect_and_crop_frame(
                    frame=frame,
                    filename=filename
                )
            )

        if not detect_result["success"]:
            self.draw_preview_header(
                frame=preview_frame,
                frame_no=frame_no,
                status="no plate"
            )

            self.publish_frame(
                frame=preview_frame,
                frame_no=frame_no,
                status="no plate"
            )

            if frame_no % 300 == 0:
                print(
                    f"[{self.camera_name}] "
                    f"번호판 탐지 실패 frame={frame_no}"
                )

            return

        crop_path = Path(
            detect_result["crop_path"]
        )

        with self.model_lock:
            ocr_result = self.ocr_reader.read(
                crop_path
            )

        car_no = ocr_result["text"]
        score = ocr_result["score"]
        detect_score = detect_result["det_conf"]

        self.draw_detection(
            frame=preview_frame,
            detect_result=detect_result,
            car_no=car_no,
            ocr_score=score
        )

        self.draw_preview_header(
            frame=preview_frame,
            frame_no=frame_no,
            status="plate detected"
        )

        self.publish_frame(
            frame=preview_frame,
            frame_no=frame_no,
            status=f"OCR {car_no or '-'}"
        )

        print(
            f"[{self.camera_name}] 후보 "
            f"frame={frame_no}, "
            f"carNo={car_no}, "
            f"ocr={score * 100:.1f}%, "
            f"det={detect_score * 100:.1f}%"
        )

        if not car_no:
            return

        candidate_crop_path = (
            self.copy_candidate_crop(
                crop_path=crop_path,
                frame_no=frame_no,
                car_no=car_no,
                score=score
            )
        )

        self.candidates.append({
            "frameNo": frame_no,
            "frame": frame.copy(),
            "carNo": car_no,
            "score": score,
            "detect": detect_result,
            "ocr": ocr_result,
            "cropPath": candidate_crop_path
        })

        if len(self.candidates) >= CROP_COUNT:
            self.finish_candidates()

    def draw_preview_header(
        self,
        frame,
        frame_no,
        status
    ):
        draw_text(
            frame,
            (
                f"{self.video_name} / "
                f"cameraNo:{self.camera_no} / "
                f"frame:{frame_no}"
            ),
            20,
            35,
            (0, 255, 255),
            0.7
        )

        draw_text(
            frame,
            status,
            20,
            70,
            (255, 255, 0),
            0.7
        )

    def draw_detection(
        self,
        frame,
        detect_result,
        car_no,
        ocr_score
    ):
        box = detect_result.get("box")

        if box is None:
            return

        x1, y1, x2, y2 = box

        cv2.rectangle(
            frame,
            (x1, y1),
            (x2, y2),
            (0, 255, 0),
            2
        )

        detect_score = (
            detect_result["det_conf"] * 100
        )

        label = (
            f"plate det:{detect_score:.1f}% "
            f"ocr:{ocr_score * 100:.1f}%"
        )

        draw_text(
            frame,
            label,
            x1,
            max(30, y1 - 10),
            (0, 255, 0),
            0.6
        )

        # 한글 번호판은 OpenCV 창에서 깨질 수 있음
        if car_no:
            draw_text(
                frame,
                f"car detected",
                x1,
                min(
                    frame.shape[0] - 20,
                    y2 + 30
                ),
                (255, 0, 255),
                0.7
            )

    def publish_frame(
        self,
        frame,
        frame_no,
        status
    ):
        preview = cv2.resize(
            frame,
            (PREVIEW_WIDTH, PREVIEW_HEIGHT)
        )

        with self.preview_lock:
            self.latest_frame = preview
            self.latest_frame_no = frame_no
            self.latest_status = status

    def publish_message(self, message):
        frame = make_blank(
            f"{self.video_name}: {message}"
        )

        with self.preview_lock:
            self.latest_frame = frame
            self.latest_status = message

    def get_preview(self):
        with self.preview_lock:
            return (
                self.latest_frame.copy(),
                self.latest_frame_no,
                self.latest_status,
                self.camera_no,
                self.camera_name
            )

    def finish_candidates(self):
        best = max(
            self.candidates,
            key=lambda item: item["score"]
        )

        car_no = best["carNo"]
        score = best["score"]

        print(
            f"[{self.camera_name}] "
            f"후보 {len(self.candidates)}개 수집 완료"
        )

        print(
            f"[{self.camera_name}] 최종 선택 "
            f"carNo={car_no}, "
            f"score={score * 100:.1f}%, "
            f"frame={best['frameNo']}"
        )

        self.candidates = []

        if score < MIN_OCR_SCORE:
            print(
                f"[{self.camera_name}] "
                f"OCR 신뢰도 낮음. 전송 안 함"
            )
            return

        if self.is_cooldown(car_no):
            print(
                f"[{self.camera_name}] "
                f"중복 전송 방지: {car_no}"
            )
            return

        original_path = self.save_original_frame(
            frame=best["frame"],
            car_no=car_no,
            frame_no=best["frameNo"]
        )

        best_crop_path = self.save_best_crop(
            crop_path=best["cropPath"],
            car_no=car_no,
            frame_no=best["frameNo"]
        )

        print(
            f"[{self.camera_name}] "
            f"원본 저장: {original_path}"
        )

        print(
            f"[{self.camera_name}] "
            f"최종 crop 저장: {best_crop_path}"
        )

        self.send_to_spring(
            car_no=car_no,
            score=score,
            original_path=original_path
        )

        self.last_send_time[
            self.cooldown_key(car_no)
        ] = time.time()

    def copy_candidate_crop(
        self,
        crop_path,
        frame_no,
        car_no,
        score
    ):
        safe_car_no = (
            car_no if car_no else "unknown"
        )

        filename = (
            f"{self.camera_name}_frame{frame_no}_"
            f"{safe_car_no}_"
            f"{score * 100:.1f}_crop.jpg"
        )

        destination = (
            STREAM_CANDIDATE_DIR / filename
        )

        shutil.copyfile(
            crop_path,
            destination
        )

        return destination

    def cooldown_key(self, car_no):
        return f"{self.camera_no}_{car_no}"

    def is_cooldown(self, car_no):
        key = self.cooldown_key(car_no)
        last_time = self.last_send_time.get(key)

        if last_time is None:
            return False

        return (
            time.time() - last_time
            < SEND_COOLDOWN_SECONDS
        )

    def save_original_frame(
        self,
        frame,
        car_no,
        frame_no
    ):
        filename = (
            f"camera{self.camera_no}_"
            f"{self.camera_name}_"
            f"{car_no}_"
            f"frame{frame_no}_"
            f"{int(time.time())}.jpg"
        )

        path = STREAM_FRAME_DIR / filename

        ok, encoded = cv2.imencode(
            ".jpg",
            frame
        )

        if not ok:
            raise ValueError(
                "원본 프레임 저장 실패"
            )

        encoded.tofile(str(path))

        return path

    def save_best_crop(
        self,
        crop_path,
        car_no,
        frame_no
    ):
        source = Path(crop_path)

        filename = (
            f"camera{self.camera_no}_"
            f"{self.camera_name}_"
            f"{car_no}_"
            f"frame{frame_no}_"
            f"best_crop.jpg"
        )

        destination = (
            STREAM_CROP_DIR / filename
        )

        shutil.copyfile(
            source,
            destination
        )

        return destination

    def send_to_spring(
        self,
        car_no,
        score,
        original_path
    ):
        confidence_score = score * 100

        data = {
            "cameraNo": str(self.camera_no),
            "carNo": car_no,
            "confidenceScore": str(
                confidence_score
            )
        }

        with open(original_path, "rb") as file:
            files = {
                "file": (
                    original_path.name,
                    file,
                    "image/jpeg"
                )
            }

            try:
                response = httpx.post(
                    SPRING_URL,
                    data=data,
                    files=files,
                    timeout=10.0
                )

                print(
                    f"[{self.camera_name}] "
                    f"Spring 전송 결과 "
                    f"status={response.status_code}, "
                    f"body={response.text}"
                )

            except Exception as error:
                print(
                    f"[{self.camera_name}] "
                    f"Spring 전송 실패: {error}"
                )


def add_control_bar(
    screen,
    paused,
    view_mode
):
    bar_height = 70

    control_bar = np.zeros(
        (
            bar_height,
            screen.shape[1],
            3
        ),
        dtype=np.uint8
    )

    state = "PAUSED" if paused else "PLAYING"

    draw_text(
        control_bar,
        (
            f"{state} | view:{view_mode} | "
            f"SPACE pause/play | "
            f"A -5s | D +5s | "
            f"F +30s | R restart | Q stop"
        ),
        15,
        42,
        (255, 255, 255),
        0.55,
        1
    )

    return np.vstack(
        (screen, control_bar)
    )


def run_preview(
    workers,
    playback_control
):
    print("")
    print("================================")
    print("영상 제어 키")
    print("================================")
    print("1     : cam1만 보기")
    print("2     : cam2만 보기")
    print("b     : cam1 + cam2 같이 보기")
    print("space : 일시정지 / 다시 재생")
    print("a     : 5초 전")
    print("d     : 5초 후")
    print("s     : 5초 후")
    print("f     : 30초 후")
    print("r     : 영상 처음으로")
    print("q     : 완전히 종료")
    print("================================")
    print("")

    view_mode = "both"

    while not playback_control.is_stopped():
        previews = []

        for worker in workers:
            frame, _, _, _, _ = (
                worker.get_preview()
            )
            previews.append(frame)

        if len(previews) < 2:
            previews.append(
                make_blank("cam2 not available")
            )

        if view_mode == "cam1":
            screen = previews[0]

        elif view_mode == "cam2":
            screen = previews[1]

        else:
            screen = np.hstack(
                (previews[0], previews[1])
            )

        paused = playback_control.is_paused()

        if paused:
            draw_text(
                screen,
                "PAUSED",
                20,
                110,
                (0, 0, 255),
                1.2,
                3
            )

        screen = add_control_bar(
            screen=screen,
            paused=paused,
            view_mode=view_mode
        )

        cv2.imshow(
            "Test Preview Stream - YOLO OCR Spring",
            screen
        )

        key = cv2.waitKey(30) & 0xFF

        if key in (ord("q"), 27):
            print("영상 및 OCR 작업 종료")
            playback_control.stop()
            break

        elif key == ord("1"):
            view_mode = "cam1"
            print("cam1만 보기")

        elif key == ord("2"):
            view_mode = "cam2"
            print("cam2만 보기")

        elif key == ord("b"):
            view_mode = "both"
            print("cam1 + cam2 같이 보기")

        elif key == ord(" "):
            paused = (
                playback_control.toggle_pause()
            )

            if paused:
                print("일시정지: 영상 처리와 전송도 멈춤")
            else:
                print("다시 재생")

        elif key == ord("a"):
            playback_control.request_relative_seek(
                -SKIP_SECONDS
            )
            print("5초 전으로 이동")

        elif key in (ord("d"), ord("s")):
            playback_control.request_relative_seek(
                SKIP_SECONDS
            )
            print("5초 후로 이동")

        elif key == ord("f"):
            playback_control.request_relative_seek(
                LONG_SKIP_SECONDS
            )
            print("30초 후로 이동")

        elif key == ord("r"):
            playback_control.request_restart()
            print("영상 처음으로 이동")

    cv2.destroyAllWindows()


def main():
    print("test_previe_stream 시작")
    print(f"Spring URL: {SPRING_URL}")
    print(f"VIDEO DIR: {VIDEO_DIR}")
    print(f"FRAME STEP: {FRAME_STEP}")
    print(f"CROP COUNT: {CROP_COUNT}")
    print(f"OCR DEVICE: {OCR_DEVICE}")

    detector = PlateDetector()
    ocr_reader = PlateOCR(
        device=OCR_DEVICE
    )

    model_lock = threading.Lock()
    playback_control = PlaybackControl()

    workers = []
    threads = []

    for stream in TEST_STREAMS:
        worker = TestPreviewStreamWorker(
            stream=stream,
            detector=detector,
            ocr_reader=ocr_reader,
            model_lock=model_lock,
            playback_control=playback_control
        )

        thread = threading.Thread(
            target=worker.run,
            daemon=True
        )

        thread.start()

        workers.append(worker)
        threads.append(thread)

    try:
        run_preview(
            workers=workers,
            playback_control=playback_control
        )

    except KeyboardInterrupt:
        print("Ctrl+C 종료")
        playback_control.stop()

    finally:
        playback_control.stop()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
    
    
    
#       cd C:\Users\dkddp\Documents\bunhobono\bunhobono\fast-api
#       C:\Users\dkddp\.conda\envs\bono\python.exe test_preview_stream_.py