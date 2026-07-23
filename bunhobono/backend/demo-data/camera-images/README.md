# Demo camera images

이 폴더는 `db/demo_dummy_full.sql`의 `demo_plate` 목록과 함께 Git에 포함한다.

- `img_*.jpeg`: `C:/Users/503-30/Desktop/val/val.txt`의 이미지명과 차량번호를 대조한 원본
- `required_*`: 필수 차량 9대의 스트림 전체 이미지
- `required_*_crop.jpg`: 필수 차량 9대의 번호판 크롭 이미지

DB의 `camera_data.image_path`와 `crop_image_path`에는 이 폴더를 기준으로 한
파일명만 저장된다. 기본 폴더는 `application-path.yaml`의
`demo-data/camera-images`이며, 필요하면 `BONO_CAMERA_DATA` 환경변수로 바꾼다.

`81라7385` 차량의 원본 OCR 번호는 이미지에 보이는 `817라7385`이다.
SQL에서는 `ocr_car_no=817라7385`, 관리자 보정 차량번호 `car_no=81라7385`로
저장하여 원본 인식값과 등록 차량번호를 모두 보존한다.
