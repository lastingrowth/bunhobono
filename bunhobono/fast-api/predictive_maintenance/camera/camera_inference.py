from fastapi import APIRouter, HTTPException

from predictive_maintenance.inference_service import PROJECT_ROOT, PredictiveMaintenanceReplayService


router = APIRouter(prefix="/demo/predictive-maintenance/camera", tags=["카메라 예지보전"])
service = PredictiveMaintenanceReplayService(
    equipment_type="CAMERA", equipment_id_column="equipment_id", timestamp_column="collected_at",
    model_path=PROJECT_ROOT / "model" / "camera" / "parking_camera_xgboost.json",
    metadata_path=PROJECT_ROOT / "model" / "camera" / "parking_camera_xgboost_metadata.json",
    test_csv_path=PROJECT_ROOT / "data" / "camera" / "camera_predictive_maintenance_test.csv",
    # DB의 camera_no 1~12와 연결할 수 있는 테스트 장비만 재생한다.
    equipment_number_min=1,
    equipment_number_max=12,
)


@router.post("/next")
def predict_next_camera():
    try:
        return service.predict_next()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"카메라 예측 실패: {error}") from error
