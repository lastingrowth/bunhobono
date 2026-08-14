from fastapi import APIRouter, HTTPException

from predictive_maintenance.inference_service import PROJECT_ROOT, PredictiveMaintenanceReplayService


router = APIRouter(prefix="/demo/predictive-maintenance/robot", tags=["로봇 예지보전"])
service = PredictiveMaintenanceReplayService(
    equipment_type="ROBOT", equipment_id_column="robot_id", timestamp_column="timestamp",
    model_path=PROJECT_ROOT / "model" / "robot" / "parking_robot_xgboost.json",
    metadata_path=PROJECT_ROOT / "model" / "robot" / "parking_robot_xgboost_metadata.json",
    test_csv_path=PROJECT_ROOT / "data" / "robot" / "robot_predictive_maintenance_test.csv",
)


@router.post("/next")
def predict_next_robot():
    try:
        return service.predict_next()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"로봇 예측 실패: {error}") from error
