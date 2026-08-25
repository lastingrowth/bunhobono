from fastapi import APIRouter, HTTPException

from predictive_maintenance.inference_service import PROJECT_ROOT, PredictiveMaintenanceReplayService


router = APIRouter(prefix="/demo/predictive-maintenance/robot", tags=["로봇 예지보전"])
service = PredictiveMaintenanceReplayService(
    equipment_type="ROBOT", equipment_id_column="robot_id", timestamp_column="timestamp",
    model_path=PROJECT_ROOT / "model" / "robot" / "parking_robot_xgboost.json",
    metadata_path=PROJECT_ROOT / "model" / "robot" / "parking_robot_xgboost_metadata.json",
    test_csv_path=PROJECT_ROOT / "data" / "robot" / "robot_predictive_maintenance_test.csv",
    # DB의 robot_no 1~8과 연결되는 테스트 장비만 재생한다.
    equipment_number_min=1,
    equipment_number_max=8,
)


@router.post("/next-all")
def predict_next_all_robots():
    try:
        return service.predict_next_all()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"로봇 일괄 예측 실패: {error}") from error


@router.post("/next")
def predict_next_robot():
    try:
        return service.predict_next()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"로봇 예측 실패: {error}") from error


@router.post("/{equipment_no}/complete-action")
def complete_robot_action(equipment_no: str):
    try:
        return service.complete_action(equipment_no)
    except KeyError as error:
        raise HTTPException(status_code=404, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"로봇 조치 완료 처리 실패: {error}") from error
