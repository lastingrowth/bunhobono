from pathlib import Path
import os
import shutil
import threading
import time

import cv2
import httpx

from yolo_detect import PlateDetector
from plate_ocr import PlateOCR
from plate_preprocess_standard_v2 import make_candidates
from ocr_selector import select_best_ocr


BASE_DIR = Path(__file__).resolve().parent
VIDEO_DIR = BASE_DIR / "runtime" / "videos"
FRAME_DIR = BASE_DIR / "runtime" / "test_stream_frames"
CROP_DIR = BASE_DIR / "runtime" / "test_stream_crops"
PREPROCESS_DIR = BASE_DIR / "runtime" / "test_stream_preprocess"

FRAME_DIR.mkdir(parents=True, exist_ok=True)
CROP_DIR.mkdir(parents=True, exist_ok=True)
PREPROCESS_DIR.mkdir(parents=True, exist_ok=True)

SPRING_URL = os.getenv(
    "SPRING_URL",
    "http://localhost:80/api/camera-data/ocr",
)

# 영상과 실제 카메라 번호 연결
TEST_STREAMS = [
    {
        "videoName": "cctv1",
        "source": str(VIDEO_DIR / "cctv1.mp4"),
        "cameras": [{"cameraNo": 1, "name": "A-IN"}],
    },
    {
        "videoName": "cctv2",
        "source": str(VIDEO_DIR / "cctv2.mp4"),
        "cameras": [{"cameraNo": 3, "name": "B-IN"}],
    },
    {
        "videoName": "cctv3",
        "source": str(VIDEO_DIR / "cctv3.mp4"),
        "cameras": [{"cameraNo": 5, "name": "A-IN"}],
    },
    {
        "videoName": "cctv4",
        "source": str(VIDEO_DIR / "cctv4.mp4"),
        "cameras": [{"cameraNo": 7, "name": "B-IN"}],
    },
    {
        "videoName": "cctv5",
        "source": str(VIDEO_DIR / "cctv1.mp4"),
        "cameras": [{"cameraNo": 2, "name": "A-OUT"}],
    },
    {
        "videoName": "cctv6",
        "source": str(VIDEO_DIR / "cctv2.mp4"),
        "cameras": [{"cameraNo": 4, "name": "B-OUT"}],
    },
    {
        "videoName": "cctv7",
        "source": str(VIDEO_DIR / "cctv3.mp4"),
        "cameras": [{"cameraNo": 6, "name": "A-OUT"}],
    },
    {
        "videoName": "cctv8",
        "source": str(VIDEO_DIR / "cctv4.mp4"),
        "cameras": [{"cameraNo": 8, "name": "B-OUT"}],
    },
]

FRAME_STEP = 10
CROP_COUNT = 1
NO_DETECTION_RESET_COUNT = 2
PLAYBACK_SPEED = 0.25
CCTV_OCR_DIRECT_ACCEPT_SCORE = 0.95
CCTV_OCR_MAX_CANDIDATES = 3
MIN_OCR_SCORE = 0.5
SEND_COOLDOWN_SECONDS = 10
DETECTION_DISPLAY_SECONDS = 2
DETECTION_ZONE_TOP_RATIO = 1 / 3
OCR_DEVICE = "cpu"
LOW_LIGHT_CAMERA_NOS = {5, 6}
LOW_LIGHT_BRIGHTNESS = 0.40
SUNNY_CAMERA_NOS = {7, 8}
SUNNY_CONTRAST = 1.20
SUNNY_BRIGHTNESS = 18
TOP_PADDED_CAMERA_NOS = {5, 6, 7, 8}
DISPLAY_TOP_CROP_RATIO = 0.08


class TestStreamWorker:
    def __init__(self, stream, detector, ocr_reader, model_lock):
        self.video_name = stream["videoName"]
        self.source = stream["source"]
        self.cameras = stream["cameras"]

        self.detector = detector
        self.ocr_reader = ocr_reader
        self.model_lock = model_lock

        self.camera_index = 0
        self.camera_no = self.cameras[0]["cameraNo"]
        self.camera_name = self.cameras[0]["name"]

        self.running = True
        self.video_finished = False
        self.pause_event = threading.Event()
        self.pause_event.set()

        self.viewer_count = 0
        self.viewer_lock = threading.Lock()
        self.thread_lock = threading.Lock()
        self.thread = None

        self.latest_frame = None
        self.latest_frame_lock = threading.Lock()
        self.latest_detection_box = None
        self.latest_detection_until = 0.0

        self.candidates = []
        self.last_send_time = {}
        self.vehicle_active = False
        self.no_detection_count = 0
        self.auto_paused = False
        self.pause_reason = None
        self.ocr_event_id = 0
        self.last_ocr_car_no = None
        self.pending_camera_data_no = None

    def pause(self):
        self.pause_event.set()
        self.pause_reason = "MANUAL"
        print(f"[{self.camera_name}] 영상 일시정지")

    def resume(self):
        self.pause_event.clear()
        self.auto_paused = False
        self.pause_reason = None
        print(f"[{self.camera_name}] 영상 재생")

    def pause_for_backend(self):
        self.auto_paused = True
        self.pause_reason = "WAITING_FOR_BACKEND"
        self.pause_event.set()
        print(f"[{self.camera_name}] paused: waiting for backend")

    def complete_pending(self):
        self.pending_camera_data_no = None
        self.resume()

    def restart_playback(self):
        previous_thread = self.thread

        if previous_thread is not None and previous_thread.is_alive():
            previous_thread.join(timeout=1.0)

        self.pause_event.clear()
        self.video_finished = False
        self.auto_paused = False
        self.pause_reason = None
        self.pending_camera_data_no = None
        return self.start()

    def stop(self):
        self.running = False
        self.pause_event.clear()

    def start(self):
        with self.thread_lock:
            if self.thread is not None and self.thread.is_alive():
                return self.thread

            self.running = True
            self.video_finished = False
            self.vehicle_active = False
            self.no_detection_count = 0
            self.candidates = []
            self.auto_paused = False
            self.pause_reason = None
            self.thread = threading.Thread(target=self.run, daemon=True)
            self.thread.start()
            return self.thread

    def add_viewer(self):
        should_restart = False

        with self.viewer_lock:
            self.viewer_count += 1
            should_restart = self.video_finished
            if self.viewer_count == 1:
                self.pause_event.clear()
                self.auto_paused = False
                self.pause_reason = None

        if should_restart:
            self.start()

    def remove_viewer(self):
        with self.viewer_lock:
            self.viewer_count = max(0, self.viewer_count - 1)
            if self.viewer_count == 0:
                self.pause_event.set()

    def update_latest_frame(self, frame):
        with self.latest_frame_lock:
            frame_height, frame_width = frame.shape[:2]
            crop_top = 0
            crop_left = 0

            # 테스트 영상은 중앙의 정사각형 영상 양옆에 검은 여백이 포함되어 있다.
            # 탐지에는 원본 프레임을 사용하고, 브라우저로 보낼 화면만 중앙 크롭한다.
            # Remove the source video's top padding from browser output only.
            # Detection and OCR still use the untouched source frame.
            if self.camera_no in TOP_PADDED_CAMERA_NOS:
                crop_top = int(frame_height * DISPLAY_TOP_CROP_RATIO)

            display_height = frame_height - crop_top

            if frame_width > display_height:
                crop_left = (frame_width - display_height) // 2
                display_frame = frame[
                    crop_top:frame_height,
                    crop_left:crop_left + display_height,
                ].copy()
            else:
                display_frame = frame[crop_top:frame_height, :].copy()

            height, width = display_frame.shape[:2]

            cv2.rectangle(
                display_frame,
                (0, int(height * DETECTION_ZONE_TOP_RATIO)),
                (width - 1, height - 1),
                (255, 180, 0),
                2,
            )
            cv2.putText(
                display_frame,
                "DETECTION ZONE",
                (12, int(height * DETECTION_ZONE_TOP_RATIO) + 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (255, 180, 0),
                2,
                cv2.LINE_AA,
            )

            if (
                self.latest_detection_box is not None
                and time.time() < self.latest_detection_until
            ):
                x1, y1, x2, y2 = self.latest_detection_box
                x1 = max(0, x1 - crop_left)
                x2 = min(width - 1, x2 - crop_left)
                y1 = max(0, y1 - crop_top)
                y2 = min(height - 1, y2 - crop_top)

                if x2 <= x1 or y2 <= y1:
                    self.latest_frame = display_frame
                    return

                cv2.rectangle(
                    display_frame,
                    (x1, y1),
                    (x2, y2),
                    (0, 0, 255),
                    3,
                )
                cv2.putText(
                    display_frame,
                    "PLATE DETECTED",
                    (x1, max(30, y1 - 10)),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    (0, 0, 255),
                    2,
                    cv2.LINE_AA,
                )

            self.latest_frame = display_frame

    def apply_low_light(self, frame):
        dark_frame = frame.astype("float32") * LOW_LIGHT_BRIGHTNESS
        return dark_frame.clip(0, 255).astype("uint8")

    def apply_sunny_day(self, frame):
        return cv2.convertScaleAbs(
            frame,
            alpha=SUNNY_CONTRAST,
            beta=SUNNY_BRIGHTNESS,
        )

    def apply_camera_environment(self, frame):
        if self.camera_no in LOW_LIGHT_CAMERA_NOS:
            return self.apply_low_light(frame)

        if self.camera_no in SUNNY_CAMERA_NOS:
            return self.apply_sunny_day(frame)

        return frame

    def show_detection(self, box):
        with self.latest_frame_lock:
            self.latest_detection_box = [int(value) for value in box]
            self.latest_detection_until = (
                time.time() + DETECTION_DISPLAY_SECONDS
            )

    def get_jpeg_frame(self):
        with self.latest_frame_lock:
            if self.latest_frame is None:
                return None
            frame = self.latest_frame.copy()

        success, buffer = cv2.imencode(
            ".jpg",
            frame,
            [cv2.IMWRITE_JPEG_QUALITY, 80],
        )
        return buffer.tobytes() if success else None

    def run(self):
        while self.running:
            camera = self.cameras[self.camera_index]
            self.camera_no = camera["cameraNo"]
            self.camera_name = camera["name"]

            print(
                f"[{self.camera_name}] 영상 시작: {self.source}"
            )
            self.run_video_once()

            if not self.running:
                break

            self.video_finished = True
            self.pause_event.set()
            print(f"[{self.camera_name}] playback finished")
            break

    def run_video_once(self):
        cap = cv2.VideoCapture(self.source)

        if not cap.isOpened():
            print(f"[{self.camera_name}] 영상 열기 실패")
            time.sleep(3)
            return

        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = cap.get(cv2.CAP_PROP_FRAME_COUNT)
        frame_delay = (
            (1 / fps) / PLAYBACK_SPEED
            if fps > 0
            else (1 / 30) / PLAYBACK_SPEED
        )

        print(
            f"[{self.camera_name}] fps={fps}, "
            f"totalFrame={total_frames}"
        )

        frame_no = 0
        self.candidates = []

        # Keep a preview frame ready so the first viewer does not wait on startup.
        success, preview_frame = cap.read()
        if success:
            preview_frame = self.apply_camera_environment(preview_frame)

            frame_no = 1
            self.update_latest_frame(preview_frame)

        try:
            while self.running:
                if self.pause_event.is_set():
                    time.sleep(0.05)
                    continue

                started_at = time.perf_counter()
                success, frame = cap.read()

                if not success:
                    print(f"[{self.camera_name}] 영상 종료 후 반복")
                    break

                frame = self.apply_camera_environment(frame)

                frame_no += 1
                self.update_latest_frame(frame)

                if frame_no % FRAME_STEP == 0:
                    self.process_frame(frame, frame_no)

                elapsed = time.perf_counter() - started_at
                remaining = frame_delay - elapsed
                if remaining > 0:
                    time.sleep(remaining)
        finally:
            cap.release()

    def process_frame(self, frame, frame_no):
        filename = f"{self.camera_name}_{frame_no}.jpg"

        with self.model_lock:
            detect_result = self.detector.detect_and_crop_frame(
                frame=frame,
                filename=filename,
            )

        if not detect_result.get("success"):
            self.candidates = []

            if self.vehicle_active:
                self.no_detection_count += 1
                if self.no_detection_count >= NO_DETECTION_RESET_COUNT:
                    self.vehicle_active = False
                    self.no_detection_count = 0
            return

        self.show_detection(detect_result["box"])
        self.no_detection_count = 0

        if self.vehicle_active:
            return

        self.candidates.append(
            {
                "frameNo": frame_no,
                "frame": frame.copy(),
                "cropPath": Path(detect_result["crop_path"]),
                "detScore": float(detect_result["det_conf"]),
            }
        )

        if len(self.candidates) < CROP_COUNT:
            return

        best = max(self.candidates, key=lambda item: item["detScore"])
        self.candidates = []
        self.vehicle_active = True
        self.pause_for_backend()

        ocr_result = self.read_with_preprocessing(
            best["cropPath"],
            best["frameNo"],
        )

        car_no = ocr_result.get("text", "")
        score = float(ocr_result.get("score", 0))
        
        if not car_no:
            print(
                f"[{self.camera_name}] OCR 미인식 처리 "
                f"rawCarNo={car_no}, score={score * 100:.1f}%"
            )
            car_no = "미인식"

        print(
            f"[{self.camera_name}] OCR selected "
            f"frame={best['frameNo']}, carNo={car_no}, "
            f"det={best['detScore'] * 100:.1f}%, "
            f"ocr={score * 100:.1f}%, "
            f"mode={ocr_result.get('mode', '-')}"
        )

        if not car_no:
            car_no = "미인식"

        if self.is_cooldown(car_no):
            print(f"[{self.camera_name}] duplicate blocked: {car_no}")
            self.resume()
            return

        original_path = self.save_original_frame(
            best["frame"], car_no, best["frameNo"]
        )
        saved_crop_path = self.save_best_crop(
            best["cropPath"], car_no, best["frameNo"]
        )

        spring_result = self.send_to_spring(
            car_no,
            score,
            original_path,
            saved_crop_path,
        )
        if spring_result:
            self.last_send_time[self.cooldown_key(car_no)] = time.time()
            self.ocr_event_id += 1
            self.last_ocr_car_no = car_no
            gate_opened = bool(spring_result.get("gateOpened", False))

            if gate_opened:
                self.pending_camera_data_no = None
                self.resume()
            else:
                self.pending_camera_data_no = spring_result.get(
                    "cameraDataNo"
                )

    def _process_frame_legacy(self, frame, frame_no):
        filename = f"{self.camera_name}_{frame_no}.jpg"

        with self.model_lock:
            detect_result = self.detector.detect_and_crop_frame(
                frame=frame,
                filename=filename,
            )

        if not detect_result.get("success"):
            if frame_no % 300 == 0:
                print(
                    f"[{self.camera_name}] 번호판 탐지 실패 "
                    f"frame={frame_no}"
                )
            return

        crop_path = Path(detect_result["crop_path"])
        self.show_detection(detect_result["box"])

        ocr_result = self.read_with_preprocessing(crop_path, frame_no)

        car_no = ocr_result.get("text", "")
        score = float(ocr_result.get("score", 0))

        print(
            f"[{self.camera_name}] 후보 frame={frame_no}, "
            f"carNo={car_no}, ocr={score * 100:.1f}%"
        )

        if not car_no:
            return

        self.candidates.append(
            {
                "frameNo": frame_no,
                "frame": frame.copy(),
                "carNo": car_no,
                "score": score,
                "cropPath": crop_path,
            }
        )

        if len(self.candidates) >= CROP_COUNT:
            self.finish_candidates()

    def read_with_preprocessing(self, crop_path, frame_no):
        raw_candidates = [{
            "mode": "raw",
            "path": str(crop_path),
        }]

        with self.model_lock:
            ocr_candidates = self.ocr_reader.read_candidates(raw_candidates)

        selected = select_best_ocr(ocr_candidates)

        if (
            selected.get("is_valid_plate", False)
            and float(selected.get("score", 0)) >= CCTV_OCR_DIRECT_ACCEPT_SCORE
        ):
            print(
                f"[{self.camera_name}] OCR 빠른 확정 "
                f"mode={selected.get('mode', '-')}, "
                f"carNo={selected.get('text', '')}"
            )
            return selected

        output_dir = PREPROCESS_DIR / f"camera{self.camera_no}"

        if output_dir.exists():
            shutil.rmtree(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

        preprocess_candidates = make_candidates(
            image_path=crop_path,
            output_dir=output_dir,
            prefix=f"camera{self.camera_no}_frame{frame_no}",
        )

        mode_priority = {
            "standard_soft_h96": 0,
            "gray_h96": 1,
            "illum_norm_h96": 2,
            "shadow_standard_h112": 3,
            "glare_standard_h96": 4,
            "standard_soft_h112": 5,
            "gray_h112": 6,
        }

        additional_candidates = sorted(
            (
                candidate for candidate in preprocess_candidates
                if candidate.get("mode") != "raw"
            ),
            key=lambda candidate: mode_priority.get(
                candidate.get("mode"), 99
            ),
        )[:CCTV_OCR_MAX_CANDIDATES - 1]

        with self.model_lock:
            ocr_candidates.extend(
                self.ocr_reader.read_candidates(additional_candidates)
            )

        selected = select_best_ocr(ocr_candidates)

        print(
            f"[{self.camera_name}] 전처리 OCR "
            f"candidates={len(ocr_candidates)}, "
            f"selected={selected.get('mode', '-')}, "
            f"carNo={selected.get('text', '')}"
        )

        return selected

    def finish_candidates(self):
        best = max(self.candidates, key=lambda item: item["score"])
        self.candidates = []

        raw_car_no = best["carNo"]
        car_no = raw_car_no
        score = best["score"]

        if not car_no:
            car_no = "미인식"

        print(
            f"[{self.camera_name}] 최종 선택 "
            f"carNo={car_no}, rawCarNo={raw_car_no}, score={score * 100:.1f}%"
        )

        if self.is_cooldown(car_no):
            print(f"[{self.camera_name}] 중복 전송 방지: {car_no}")
            self.vehicle_active = False
            self.resume()
            return

        original_path = self.save_original_frame(
            best["frame"], car_no, best["frameNo"]
        )
        saved_crop_path = self.save_crop_image(
            best["cropPath"], car_no, best["frameNo"]
        )

        spring_result = self.send_to_spring(
            car_no,
            score,
            original_path,
            saved_crop_path,
        )

        if spring_result:
            self.last_send_time[self.cooldown_key(car_no)] = time.time()

    def cooldown_key(self, car_no):
        return f"{self.camera_no}_{car_no}"

    def is_cooldown(self, car_no):
        last_time = self.last_send_time.get(self.cooldown_key(car_no))
        return last_time is not None and (
            time.time() - last_time < SEND_COOLDOWN_SECONDS
        )

    def save_original_frame(self, frame, car_no, frame_no):
        path = FRAME_DIR / (
            f"camera{self.camera_no}_{self.camera_name}_"
            f"{car_no}_frame{frame_no}_{int(time.time())}.jpg"
        )
        success, encoded = cv2.imencode(".jpg", frame)
        if not success:
            raise ValueError("원본 프레임 저장 실패")
        encoded.tofile(str(path))
        return path

    def save_best_crop(self, crop_path, car_no, frame_no):
        destination = CROP_DIR / (
            f"camera{self.camera_no}_{self.camera_name}_"
            f"{car_no}_frame{frame_no}_best_crop.jpg"
        )
        shutil.copyfile(crop_path, destination)
        return destination

    def send_to_spring(
        self,
        car_no,
        score,
        original_path,
        crop_path,
    ):
        data = {
            "cameraNo": str(self.camera_no),
            "carNo": car_no,
            "confidenceScore": str(score * 100),
        }

        try:
            with (
                open(original_path, "rb") as image_file,
                open(crop_path, "rb") as crop_file,
            ):
                response = httpx.post(
                    SPRING_URL,
                    data=data,
                    files={
                        "file": (
                            original_path.name,
                            image_file,
                            "image/jpeg",
                        ),
                        "cropFile": (
                            crop_path.name,
                            crop_file,
                            "image/jpeg",
                        ),
                    },
                    timeout=10.0,
                )

            print(
                f"[{self.camera_name}] Spring 전송 "
                f"status={response.status_code}, body={response.text}"
            )
            if not response.is_success:
                return None

            return response.json()
        except Exception as error:
            print(f"[{self.camera_name}] Spring 전송 실패: {error}")
            return None


def main():
    detector = PlateDetector()
    ocr_reader = PlateOCR(device=OCR_DEVICE)
    model_lock = threading.Lock()

    workers = {}
    threads = []

    for stream in TEST_STREAMS:
        worker = TestStreamWorker(
            stream, detector, ocr_reader, model_lock
        )

        for camera in stream["cameras"]:
            workers[camera["cameraNo"]] = worker

        thread = threading.Thread(target=worker.run, daemon=True)
        thread.start()
        threads.append(thread)

    print("등록된 카메라:", list(workers.keys()))

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        for worker in set(workers.values()):
            worker.stop()
        for thread in threads:
            thread.join(timeout=3)


if __name__ == "__main__":
    main()
