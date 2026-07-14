import cv2
import time

video_path = "runtime/videos/cam1.mp4"

cap = cv2.VideoCapture(video_path)

if not cap.isOpened():
    print("영상 열기 실패")
    exit()

fps = cap.get(cv2.CAP_PROP_FPS)

if fps == 0:
    fps = 30

delay = 1 / fps

while True:
    ret, frame = cap.read()

    if not ret:
        print("영상 끝")
        break

    cv2.imshow("cam1", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

    time.sleep(delay)

cap.release()
cv2.destroyAllWindows()