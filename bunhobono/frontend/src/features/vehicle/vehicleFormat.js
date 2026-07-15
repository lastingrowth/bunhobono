export function toVehicleView(item) {
    return {
        ...item,
        vehicleTypeText: vehicleTypeText(item.vehicleType),
        vehicleStatusText: vehicleStatusText(item.vehicleStatus, item.vehicleType, item.endDate, item.realEndDate),
        approvedAtText: dateText(item.approvedAt),
        startDateText: dateText(item.startDate),

        // 등록기간은 신청/승인된 무료 주차 시간
        // startDate ~ endDate 차이로 계산한다.
        periodText: periodText(item.startDate, item.endDate),

        // 만기일은 차량 종류에 따라 다르게 계산한다.
        // normal: endDate
        // visit + 입차 전: 입차 X
        // visit + 입차 후: realEndDate
        endDateText: vehicleEndDateText(item),

        // 남은기간도 visit 차량은 realEndDate 기준으로 계산한다.
        remainingTimeText: vehicleRemainingTimeText(item),
    };
}

function vehicleTypeText(value) {
    if (value === "normal") {
        return "입주민차량";
    }

    if (value === "visit") {
        return "방문차량";
    }

    return "-";
}

function vehicleStatusText(status, vehicleType, endDate, realEndDate) {
    const targetEndDate = vehicleType === "visit"
        ? realEndDate
        : endDate;

    if (status === "APPROVED" && targetEndDate && isExpired(targetEndDate)) {
        return "승인만료";
    }

    if (status === "WAITING") {
        return "승인대기";
    }

    if (status === "APPROVED") {
        return "승인완료";
    }

    if (status === "EXPIRED") {
        return "승인만료";
    }

    if (status === "UNKNOWN") {
        return "미확인";
    }

    return "-";
}

function vehicleEndDateText(item) {
    if (item.vehicleType === "visit") {
        if (!item.inTime) {
            return "입차 X";
        }

        return dateText(item.realEndDate);
    }

    return dateText(item.endDate);
}

function vehicleRemainingTimeText(item) {
    if (item.vehicleType === "visit") {
        if (!item.inTime) {
            return "-";
        }

        return remainingTimeText(item.realEndDate);
    }

    return remainingTimeText(item.endDate);
}

function dateText(value) {
    if (!value) {
        return "-";
    }

    const date = new Date(value);

    const year = String(date.getFullYear()).slice(2);
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");
    const second = String(date.getSeconds()).padStart(2, "0");

    return `${year}.${month}.${day} ${hour}:${minute}:${second}`;
}

function periodText(startDate, endDate) {
    if (!startDate || !endDate) {
        return "-";
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    const totalHours = Math.floor((end - start) / (1000 * 60 * 60));
    const days = Math.floor(totalHours / 24);
    const hours = totalHours % 24;

    if (days >= 30) {
        const months = Math.floor(days / 30);
        const remainDays = days % 30;

        if (remainDays === 0) {
            return `${months}개월`;
        }

        return `${months}개월 ${remainDays}일`;
    }

    if (days > 0) {
        return `${days}일 ${hours}시간`;
    }

    return `${totalHours}시간`;
}

function remainingTimeText(endDate) {
    if (!endDate) {
        return "-";
    }

    const now = new Date();
    const end = new Date(endDate);

    if (end <= now) {
        const totalMinutes = Math.floor((now - end) / (1000 * 60));
        const hours = Math.floor(totalMinutes / 60);
        let minutes = totalMinutes % 60;

        if (minutes === 0) {
            return `만기됨(+${hours}시간)`;
        }

        return `만기됨(+${hours}시간 ${minutes}분)`;
    }

    const totalMinutes = Math.floor((end - now) / (1000 * 60));
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    let minutes = totalMinutes % 60;

    if (days > 0) {
        return `${days}일 ${hours}시간`;
    }

    if (hours > 0) {
        if (minutes === 0) {
            return `${hours}시간`;
        }

        return `${hours}시간 ${minutes}분`;
    }

    return `${minutes}분`;
}

function isExpired(endDate) {
    if (!endDate) {
        return false;
    }

    return new Date(endDate) <= new Date();
}