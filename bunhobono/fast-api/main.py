from fastapi import FastAPI, UploadFile, File, Form
import httpx
app = FastAPI(
    title = "Parking API test"
)

SPRING_URL ="http://localhost:80/api/camera-data/ocr"

@app.get("/")
def home():
    return{
        "msg" : "Fast Api"
    }
    
@app.post ("/upload-test")
async def upload_test(file: UploadFile = File(...)):
    contents = await file.read()
    
    return {
        "msg" : "사진 파일 받기 성공",
        "filename" : file.filename,
        "content_type" : file.content_type,
        "file_size": len(contents)
    }

# t  
@app.post("/ocr")
async def ocr(
    file: UploadFile = File(...),
    cameraNo: int = Form(...)
):
    #1. 사진 읽기
    contents = await file.read()
    
    #2. 여기에서 OCR 처리
    # 아직 OCR 모델이 없으니까 임시 번호판 문자열로 테스트 
    carNo = "12가3456"
    
    #3. Spring으로 보낼 multipart/form-data 구성
    files = {
        "file": (
            file.filename,
            contents,
            file.content_type
        )
    }
    
    data = {
        "cameraNo": str(cameraNo),
        "carNo": carNo
    }
    
    #4. Spring API 호출
    async with httpx.AsyncClient() as client:
        response = await client.post(
            SPRING_URL,
            data=data,
            files=files
        )
    
    return {
        "msg": "OCR 처리 후 Spring 전송 완료",
        "cameraNo" : cameraNo,
        "carNo" : carNo,
        "spring_status" : response.status_code,
        "spring_result" : response.text
    }