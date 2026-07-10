CREATE VIEW notice_view AS
SELECT
    n.notice_no,
    n.parking_no,
    p.parking_name,
    n.car_log_no,

    cd.car_no,
    cd.direction,

    cl.in_time,
    cl.out_time,
    
    n.entry_at,
    n.detect_at,
    n.stay_days,
    n.alert_stat
FROM notice n
JOIN parking p
    ON n.parking_no = p.parking_no
JOIN car_log cl
    ON n.car_log_no = cl.car_log_no
LEFT JOIN camera_data cd
    ON cl.camera_data_no = cd.camera_data_no;