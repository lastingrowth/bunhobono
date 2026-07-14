from pathlib import Path
import os
import time
import shutil
import threading

import cv2
import httpx

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

# GPU / CPU 설정 변경 하는곳# GPU / CPU 설정 변경 하는곳# GPU / CPU 설정 변경 하는곳
FRAME_STEP = 3
CROP_COUNT = 2
SEND_COOLDOWN_SECONDS = 10
MIN_OCR_SCORE = 0.5
OCR_DEVICE = "gpu"


class TestStreamWorker:
    def __init__(self, stream, detector, ocr_reader, model_lock):
        self.video_name = stream["videoName"]
        self.source = stream["source"]
        self.cameras = stream["cameras"]

        self.detector = detector
        self.ocr_reader = ocr_reader
        self.model_lock = model_lock

        self.camera_index = 0
        self.last_send_time = {}
        self.candidates = []

    # cam1 -> camera1, cam1 -> camera2 식으로 계속 반복
    def run(self):
        while True:
            camera = self.cameras[self.camera_index]

            self.camera_no = camera["cameraNo"]
            self.camera_name = camera["name"]

            print("")
            print(f"[{self.video_name}] 시작")
            print(f"[{self.video_name}] 현재 cameraNo={self.camera_no}, name={self.camera_name}")
            print(f"[{self.video_name}] source={self.source}")

            self.run_video_once()

            self.camera_index += 1

            if self.camera_index >= len(self.cameras):
                self.camera_index = 0

    # 영상 하나를 처음부터 끝까지 한 번만 처리
    def run_video_once(self):
        cap = cv2.VideoCapture(self.source)

        if not cap.isOpened():
            print(f"[{self.camera_name}] 영상 열기 실패: {self.source}")
            time.sleep(3)
            return

        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_count = cap.get(cv2.CAP_PROP_FRAME_COUNT)

        print(f"[{self.camera_name}] 영상 재생 시작")
        print(f"[{self.camera_name}] fps={fps}, totalFrame={frame_count}")

        frame_no = 0
        self.candidates = []

        while True:
            ret, frame = cap.read()

            if not ret:
                print(f"[{self.camera_name}] 영상 종료")
                break

            frame_no += 1

            if frame_no % FRAME_STEP != 0:
                continue

            self.process_frame(frame, frame_no)

        cap.release()

    # 프레임 1장에서 번호판 crop 후보 1개 생성 후 OCR 처리
    def process_frame(self, frame, frame_no):
        filename = f"{self.camera_name}_{frame_no}.jpg"

        with self.model_lock:
            detect_result = self.detector.detect_and_crop_frame(
                frame=frame,
                filename=filename
            )

        if not detect_result["success"]:
            if frame_no % 300 == 0:
                print(f"[{self.camera_name}] 번호판 탐지 실패 frame={frame_no}")
            return

        crop_path = Path(detect_result["crop_path"])

        with self.model_lock:
            ocr_result = self.ocr_reader.read(crop_path)

        car_no = ocr_result["text"]
        score = ocr_result["score"]

        print(
            f"[{self.camera_name}] 후보 "
            f"frame={frame_no}, carNo={car_no}, "
            f"ocr={score * 100:.1f}%, "
            f"det={detect_result['det_conf'] * 100:.1f}%"
        )

        if not car_no:
            return

        candidate_crop_path = self.copy_candidate_crop(
            crop_path=crop_path,
            frame_no=frame_no,
            car_no=car_no,
            score=score
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

    # 후보 5개 중 OCR 신뢰도 가장 높은 1개 선택
    def finish_candidates(self):
        best = max(self.candidates, key=lambda item: item["score"])

        car_no = best["carNo"]
        score = best["score"]

        print(f"[{self.camera_name}] 후보 {len(self.candidates)}개 수집 완료")
        print(
            f"[{self.camera_name}] 최종 선택 "
            f"carNo={car_no}, score={score * 100:.1f}%, frame={best['frameNo']}"
        )

        self.candidates = []

        if score < MIN_OCR_SCORE:
            print(f"[{self.camera_name}] OCR 신뢰도 낮음. 전송 안 함")
            return

        if self.is_cooldown(car_no):
            print(f"[{self.camera_name}] 중복 전송 방지: {car_no}")
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

        print(f"[{self.camera_name}] 원본 저장: {original_path}")
        print(f"[{self.camera_name}] 최종 crop 저장: {best_crop_path}")

        self.send_to_spring(
            car_no=car_no,
            score=score,
            original_path=original_path
        )

        self.last_send_time[self.cooldown_key(car_no)] = time.time()

    # 후보 crop 저장
    def copy_candidate_crop(self, crop_path, frame_no, car_no, score):
        safe_car_no = car_no if car_no else "unknown"

        filename = (
            f"{self.camera_name}_frame{frame_no}_"
            f"{safe_car_no}_{score * 100:.1f}_crop.jpg"
        )

        dst = STREAM_CANDIDATE_DIR / filename

        shutil.copyfile(crop_path, dst)

        return dst

    # cameraNo + carNo 기준으로 중복 전송 방지
    def cooldown_key(self, car_no):
        return f"{self.camera_no}_{car_no}"

    def is_cooldown(self, car_no):
        key = self.cooldown_key(car_no)
        last_time = self.last_send_time.get(key)

        if last_time is None:
            return False

        return time.time() - last_time < SEND_COOLDOWN_SECONDS

    # 최종 선택된 원본 프레임 저장
    def save_original_frame(self, frame, car_no, frame_no):
        filename = (
            f"camera{self.camera_no}_{self.camera_name}_"
            f"{car_no}_frame{frame_no}_{int(time.time())}.jpg"
        )

        path = STREAM_FRAME_DIR / filename

        ok, encoded = cv2.imencode(".jpg", frame)

        if not ok:
            raise ValueError("원본 프레임 저장 실패")

        encoded.tofile(str(path))

        return path

    # 최종 선택된 crop 1장 저장
    def save_best_crop(self, crop_path, car_no, frame_no):
        src = Path(crop_path)

        filename = (
            f"camera{self.camera_no}_{self.camera_name}_"
            f"{car_no}_frame{frame_no}_best_crop.jpg"
        )

        dst = STREAM_CROP_DIR / filename

        shutil.copyfile(src, dst)

        return dst

    # Spring Boot로 원본 이미지 + OCR 결과 전송
    def send_to_spring(self, car_no, score, original_path):
        confidence_score = score * 100

        data = {
            "cameraNo": str(self.camera_no),
            "carNo": car_no,
            "confidenceScore": str(confidence_score)
        }

        with open(original_path, "rb") as f:
            files = {
                "file": (
                    original_path.name,
                    f,
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
                    f"[{self.camera_name}] Spring 전송 결과 "
                    f"status={response.status_code}, body={response.text}"
                )

            except Exception as e:
                print(f"[{self.camera_name}] Spring 전송 실패: {e}")


def main():
    print("test_stream 시작")
    print(f"Spring URL: {SPRING_URL}")
    print(f"FRAME_STEP: {FRAME_STEP}")
    print(f"CROP_COUNT: {CROP_COUNT}")

    detector = PlateDetector()
    ocr_reader = PlateOCR(device=OCR_DEVICE)

    model_lock = threading.Lock()

    threads = []

    for stream in TEST_STREAMS:
        worker = TestStreamWorker(
            stream=stream,
            detector=detector,
            ocr_reader=ocr_reader,
            model_lock=model_lock
        )

        thread = threading.Thread(
            target=worker.run,
            daemon=True
        )

        thread.start()
        threads.append(thread)

    while True:
        time.sleep(1)


if __name__ == "__main__":
    main()
    
    
#        cd C:\kwon\mbc_a_java21\java_17\fast-api
#        C:\Users\dkddp\.conda\envs\bono\python.exe test_stream.py