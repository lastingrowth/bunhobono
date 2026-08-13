BEGIN;

ALTER TABLE robot_task
ADD COLUMN IF NOT EXISTS phase_updated_at TIMESTAMPTZ;

UPDATE robot_task
SET phase_updated_at = COALESCE(
    completed_at,
    started_at,
    requested_at,
    CURRENT_TIMESTAMP
)
WHERE phase_updated_at IS NULL;

ALTER TABLE robot_task
ALTER COLUMN phase_updated_at
SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE robot_task
ALTER COLUMN phase_updated_at
SET NOT NULL;

CREATE OR REPLACE VIEW robot_task_detail AS
SELECT
    task.task_no,
    task.car_log_no,
    task.pickup_space_no,
    pickup.space_code AS pickup_space_code,
    task.dropoff_space_no,
    dropoff.space_code AS dropoff_space_code,
    task.set_no,
    task.task_type,
    task.task_phase,
    task.task_status,
    task.priority,
    task.requested_at,
    task.started_at,
    task.completed_at,
    task.failure_reason,
    log.car_no,
    log.snapshot_car_kind AS car_kind,
    log.parking_code,
    log.parking_name,
    task.phase_updated_at
FROM robot_task task
JOIN car_log_detail log
    ON task.car_log_no = log.car_log_no
JOIN parking_space pickup
    ON task.pickup_space_no = pickup.space_no
JOIN parking_space dropoff
    ON task.dropoff_space_no = dropoff.space_no;

COMMIT;
