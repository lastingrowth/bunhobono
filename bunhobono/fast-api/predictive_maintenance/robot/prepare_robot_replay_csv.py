from __future__ import annotations

from pathlib import Path

import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
DEFAULT_CSV_PATH = (
    PROJECT_ROOT / "data" / "robot" / "robot_predictive_maintenance_test.csv"
)

SOURCE_ROBOT_IDS = [f"ROBOT_{number:02d}" for number in range(1, 5)]
ROBOT_ID_COPIES = {
    "ROBOT_01": ("ROBOT_01", "ROBOT_05"),
    "ROBOT_02": ("ROBOT_02", "ROBOT_06"),
    "ROBOT_03": ("ROBOT_03", "ROBOT_07"),
    "ROBOT_04": ("ROBOT_04", "ROBOT_08"),
}


def expand_to_eight_robots(source: pd.DataFrame) -> pd.DataFrame:
    """모델 입력값은 유지하고 배포 로봇 식별자를 1~8로 확장한다."""
    missing = sorted(set(SOURCE_ROBOT_IDS) - set(source["robot_id"].unique()))
    if missing:
        raise ValueError(f"복제 원본 로봇 데이터가 없습니다: {missing}")

    expanded_parts = []
    for source_id, target_ids in ROBOT_ID_COPIES.items():
        source_rows = source.loc[source["robot_id"] == source_id].copy()
        for target_id in target_ids:
            target_rows = source_rows.copy()
            target_rows["robot_id"] = target_id
            expanded_parts.append(target_rows)

    return (
        pd.concat(expanded_parts, ignore_index=True)
        .sort_values(["robot_id", "timestamp"])
        .reset_index(drop=True)
    )


def main() -> None:
    data = pd.read_csv(DEFAULT_CSV_PATH, encoding="utf-8-sig")
    expanded = expand_to_eight_robots(data)
    expanded.to_csv(DEFAULT_CSV_PATH, index=False, encoding="utf-8-sig")

    counts = expanded["robot_id"].value_counts().sort_index()
    print(f"로봇 예지보전 테스트 CSV 저장: {DEFAULT_CSV_PATH}")
    print(counts.to_string())


if __name__ == "__main__":
    main()
