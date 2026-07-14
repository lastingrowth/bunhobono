from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

from plate_ocr import PlateOCR


BASE_DIR = Path(__file__).resolve().parent

YOLO_MODEL_PATH = BASE_DIR / "model" / "yolo11s_bono1199" / "best.pt"

CAM1_PATH = BASE_DIR / "runtime" / "videos" / "cam1.mp4"
CAM2_PATH = BASE_DIR / "runtime" / "videos" / "cam2.mp4"

PREVIEW_CROP_DIR = BASE_DIR / "runtime" / "preview_crops"
PREVIEW_CROP_DIR.mkdir(parents=True, exist_ok=True)

CONF = 0.10
IMGSZ = 640

FRAME_STEP = 3
OCR_INTERVAL = 15

PREVIEW_WIDTH = 640
PREVIEW_HEIGHT = 360

SKIP_SECONDS = 5


def save_crop(crop, camera_name, frame_no):
    crop_path = PREVIEW_CROP_DIR / f"{camera_name}_{frame_no}_crop.jpg"

    ok, encoded = cv2.imencode(".jpg", crop)

    if not ok:
        return None

    encoded.tofile(str(crop_path))

    return crop_path


def draw_text(frame, text, x, y, color=(0, 255, 255), size=0.8):
    cv2.putText(
        frame,
        text,
        (x, y),
        cv2.FONT_HERSHEY_SIMPLEX,
        size,
        color,
        2
    )


def detect_and_draw(model, ocr_reader, frame, camera_name, frame_no, last_ocr):
    results = model.predict(
        source=frame,
        conf=CONF,
        imgsz=IMGSZ,
        verbose=False
    )

    result = results[0]
    boxes = result.boxes

    if boxes is None or len(boxes) == 0:
        draw_text(
            frame,
            f"{camera_name} frame:{frame_no} no plate",
            20,
            40,
            (0, 255, 255),
            0.8
        )

        draw_text(
            frame,
            f"Last OCR: {last_ocr['text']} {last_ocr['score'] * 100:.1f}%",
            20,
            80,
            (255, 0, 255),
            0.8
        )

        return frame

    best_box = None
    best_conf = -1

    for box in boxes:
        conf = float(box.conf[0])

        if conf > best_conf:
            best_conf = conf
            best_box = box

    x1, y1, x2, y2 = best_box.xyxy[0].cpu().numpy().astype(int)

    h, w = frame.shape[:2]

    margin_x = max(2, int((x2 - x1) * 0.04))
    margin_y = max(2, int((y2 - y1) * 0.12))

    x1 = max(0, x1 - margin_x)
    y1 = max(0, y1 - margin_y)
    x2 = min(w, x2 + margin_x)
    y2 = min(h, y2 + margin_y)

    cv2.rectangle(
        frame,
        (x1, y1),
        (x2, y2),
        (0, 255, 0),
        2
    )

    draw_text(
        frame,
        f"{camera_name} frame:{frame_no}",
        20,
        40,
        (0, 255, 255),
        0.8
    )

    draw_text(
        frame,
        f"YOLO: {best_conf * 100:.1f}%",
        x1,
        max(30, y1 - 10),
        (0, 255, 0),
        0.8
    )

    if frame_no % OCR_INTERVAL == 0:
        crop = frame[y1:y2, x1:x2]

        if crop.size > 0:
            crop_path = save_crop(crop, camera_name, frame_no)

            if crop_path is not None:
                ocr_result = ocr_reader.read(crop_path)

                last_ocr["text"] = ocr_result["text"] or "-"
                last_ocr["raw_text"] = ocr_result["raw_text"] or "-"
                last_ocr["score"] = ocr_result["score"]
                last_ocr["frame"] = frame_no

                print(
                    f"[{camera_name}] OCR "
                    f"frame={frame_no}, "
                    f"text={last_ocr['text']}, "
                    f"raw={last_ocr['raw_text']}, "
                    f"score={last_ocr['score'] * 100:.1f}%"
                )

    draw_text(
        frame,
        f"OCR: {last_ocr['text']} {last_ocr['score'] * 100:.1f}%",
        x1,
        min(h - 20, y2 + 35),
        (255, 0, 255),
        0.8
    )

    return frame


def read_frame(cap, camera_name):
    ret, frame = cap.read()

    if not ret:
        print(f"[{camera_name}] 영상 끝. 처음부터 다시 재생")
        cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
        ret, frame = cap.read()

    return ret, frame


def move_seconds(cap, seconds):
    fps = cap.get(cv2.CAP_PROP_FPS)

    if fps <= 0:
        fps = 30

    current = cap.get(cv2.CAP_PROP_POS_FRAMES)
    total = cap.get(cv2.CAP_PROP_FRAME_COUNT)

    target = current + (seconds * fps)

    if target < 0:
        target = 0

    if total > 0 and target >= total:
        target = total - 1

    cap.set(cv2.CAP_PROP_POS_FRAMES, target)


def make_blank(text):
    img = np.zeros((PREVIEW_HEIGHT, PREVIEW_WIDTH, 3), dtype=np.uint8)

    draw_text(
        img,
        text,
        40,
        PREVIEW_HEIGHT // 2,
        (0, 0, 255),
        1
    )

    return img


def main():
    model = YOLO(str(YOLO_MODEL_PATH))
    ocr_reader = PlateOCR(device="cpu")

    cam1 = cv2.VideoCapture(str(CAM1_PATH))
    cam2 = cv2.VideoCapture(str(CAM2_PATH))

    if not cam1.isOpened():
        print(f"cam1 영상 열기 실패: {CAM1_PATH}")

    if not cam2.isOpened():
        print(f"cam2 영상 열기 실패: {CAM2_PATH}")

    if not cam1.isOpened() and not cam2.isOpened():
        return

    print("미리보기 시작")
    print("키 설명")
    print("b : cam1 + cam2 같이 보기")
    print("1 : cam1만 보기")
    print("2 : cam2만 보기")
    print("space : 일시정지 / 다시재생")
    print("a : 5초 전")
    print("s : 5초 후")
    print("q : 종료")
    print("주의: OpenCV 기본 글자는 한글 표시가 깨질 수 있음. 정확한 OCR은 콘솔 로그 확인.")

    view_mode = "both"
    paused = False

    frame_no1 = 0
    frame_no2 = 0

    last_cam1 = make_blank("cam1 loading")
    last_cam2 = make_blank("cam2 loading")

    last_ocr1 = {
        "text": "-",
        "raw_text": "-",
        "score": 0.0,
        "frame": 0
    }

    last_ocr2 = {
        "text": "-",
        "raw_text": "-",
        "score": 0.0,
        "frame": 0
    }

    while True:
        if not paused:
            if cam1.isOpened():
                ret1, frame1 = read_frame(cam1, "cam1")

                if ret1:
                    frame_no1 += 1

                    if frame_no1 % FRAME_STEP == 0:
                        frame1 = detect_and_draw(
                            model=model,
                            ocr_reader=ocr_reader,
                            frame=frame1,
                            camera_name="cam1",
                            frame_no=frame_no1,
                            last_ocr=last_ocr1
                        )

                        last_cam1 = cv2.resize(
                            frame1,
                            (PREVIEW_WIDTH, PREVIEW_HEIGHT)
                        )

            if cam2.isOpened():
                ret2, frame2 = read_frame(cam2, "cam2")

                if ret2:
                    frame_no2 += 1

                    if frame_no2 % FRAME_STEP == 0:
                        frame2 = detect_and_draw(
                            model=model,
                            ocr_reader=ocr_reader,
                            frame=frame2,
                            camera_name="cam2",
                            frame_no=frame_no2,
                            last_ocr=last_ocr2
                        )

                        last_cam2 = cv2.resize(
                            frame2,
                            (PREVIEW_WIDTH, PREVIEW_HEIGHT)
                        )

        if view_mode == "cam1":
            screen = last_cam1

        elif view_mode == "cam2":
            screen = last_cam2

        else:
            screen = np.hstack((last_cam1, last_cam2))

        if paused:
            draw_text(
                screen,
                "PAUSED",
                20,
                40,
                (0, 0, 255),
                1
            )

        cv2.imshow("camera preview YOLO + OCR", screen)

        key = cv2.waitKey(1) & 0xFF

        if key == ord("q"):
            break

        if key == ord("1"):
            view_mode = "cam1"
            print("cam1만 보기")

        if key == ord("2"):
            view_mode = "cam2"
            print("cam2만 보기")

        if key == ord("b"):
            view_mode = "both"
            print("cam1 + cam2 같이 보기")

        if key == ord(" "):
            paused = not paused

            if paused:
                print("일시정지")
            else:
                print("다시재생")

        if key == ord("a"):
            if cam1.isOpened():
                move_seconds(cam1, -SKIP_SECONDS)

            if cam2.isOpened():
                move_seconds(cam2, -SKIP_SECONDS)

            print("5초 전으로 이동")

        if key == ord("s"):
            if cam1.isOpened():
                move_seconds(cam1, SKIP_SECONDS)

            if cam2.isOpened():
                move_seconds(cam2, SKIP_SECONDS)

            print("5초 후로 이동")

    cam1.release()
    cam2.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
    
    
    #   C:\Users\dkddp\.conda\envs\bono\python.exe camera_preview.py