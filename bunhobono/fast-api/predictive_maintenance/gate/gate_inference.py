from fastapi import APIRouter, HTTPException

from predictive_maintenance.inference_service import PROJECT_ROOT, PredictiveMaintenanceReplayService


router = APIRouter(prefix="/demo/predictive-maintenance/gate", tags=["게이트 예지보전"])
service = PredictiveMaintenanceReplayService(
    equipment_type="GATE",
    equipment_id_column="gate_id",
    timestamp_column="collected_at",
    model_path=PROJECT_ROOT / "model" / "gate" / "parking_gate_xgboost.json",
    metadata_path=PROJECT_ROOT / "model" / "gate" / "parking_gate_xgboost_metadata.json",
    test_csv_path=PROJECT_ROOT / "data" / "gate" / "gate_predictive_maintenance_test.csv",
    equipment_number_min=1,
    equipment_number_max=12,
)


@router.post("/next-all")
def predict_next_all_gates():
    try:
        return service.predict_next_all()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"게이트 일괄 예측 실패: {error}") from error


@router.post("/next")
def predict_next_gate():
    try:
        return service.predict_next()
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"게이트 예측 실패: {error}") from error


@router.post("/{equipment_no}/complete-action")
def complete_gate_action(equipment_no: str):
    try:
        return service.complete_action(equipment_no)
    except KeyError as error:
        raise HTTPException(status_code=404, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"게이트 조치 완료 처리 실패: {error}") from error
