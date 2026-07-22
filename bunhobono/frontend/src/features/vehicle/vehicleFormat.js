export function toVehicleView(item) {
    return {
        ...item,

        // 백엔드가 계산한 만기 구분값을 그대로 사용
        expiryType: item.expiryType ?? null,

        vehicleTypeText: vehicleTypeText(
            item.vehicleType
        ),

        vehicleStatusText: vehicleStatusText(
            item.vehicleStatus,
            item.expiryType
        ),

        approvedAtText: dateText(
            item.approvedAt
        ),

        startDateText: dateText(
            item.startDate
        ),

        // 신청한 등록기간 표시
        periodText: periodText(
            item.startDate,
            item.endDate
        ),

        // 백엔드가 계산한 realEndDate 표시
        endDateText: vehicleEndDateText(item),

        // 백엔드가 계산한 remainingMinutes 표시
        remainingTimeText: vehicleRemainingTimeText(item)
    };
}

// 차량 종류 표시
function vehicleTypeText(vehicleType) {
    if (vehicleType === "normal") {
        return "입주민차량";
    }

    if (vehicleType === "visit") {
        return "방문차량";
    }

    return "-";
}

// 차량 승인 및 만기 상태 표시
function vehicleStatusText(vehicleStatus, expiryType) {
    if (vehicleStatus === "WAITING") {
        return "승인대기";
    }

    if (expiryType === "NO_ENTRY") {
        return "미입차 만기";
    }

    if (expiryType === "OVERSTAY") {
        return "주차시간 만기";
    }

    if (vehicleStatus === "APPROVED") {
        return "승인완료";
    }

    return "-";
}

// 만기일 표시
function vehicleEndDateText(item) {
    if (item.vehicleType === "visit") {

        // 승인된 방문차량이 아직 입차하지 않은 상태
        if (
            !item.inTime
            && item.expiryType !== "NO_ENTRY"
        ) {
            return "입차 X";
        }

        // 미입차 만기 또는 실제 입차 후 만기시간
        return dateText(item.realEndDate);
    }

    // 일반 등록차량은 기존 endDate 사용
    return dateText(item.endDate);
}

// 남은기간 표시
function vehicleRemainingTimeText(item) {

    // 출차 로그가 있으면 남은시간 대신 출차완료
    if (item.outTime) {
        return "출차완료";
    }

    // 승인 후 입차하지 않고 허용시간이 지난 경우
    if (item.expiryType === "NO_ENTRY") {
        return "미입차 만기";
    }

    // 입차 후 실제 만기시간을 초과한 경우
    if (item.expiryType === "OVERSTAY") {
        return overdueMinutesText(
            item.remainingMinutes
        );
    }

    // 방문차량은 입차 전까지 주차시간이 시작되지 않음
    if (
        item.vehicleType === "visit"
        && !item.inTime
    ) {
        return "-";
    }

    return remainingMinutesText(
        item.remainingMinutes
    );
}

// 양수인 남은 분을 화면 문구로 변환
function remainingMinutesText(value) {
    if (
        value === null
        || value === undefined
        || value === ""
    ) {
        return "-";
    }

    const totalMinutes = Number(value);

    if (Number.isNaN(totalMinutes)) {
        return "-";
    }

    // 백엔드 결과가 음수라면 만기 초과
    if (totalMinutes < 0) {
        return overdueMinutesText(totalMinutes);
    }

    const days = Math.floor(
        totalMinutes / (60 * 24)
    );

    const hours = Math.floor(
        (totalMinutes % (60 * 24)) / 60
    );

    const minutes = totalMinutes % 60;

    if (days > 0) {
        if (minutes === 0) {
            return `${days}일 ${hours}시간`;
        }

        return `${days}일 ${hours}시간 ${minutes}분`;
    }

    if (hours > 0) {
        if (minutes === 0) {
            return `${hours}시간`;
        }

        return `${hours}시간 ${minutes}분`;
    }

    return `${minutes}분`;
}

// 음수인 remainingMinutes를 초과 문구로 변환
function overdueMinutesText(value) {
    if (
        value === null
        || value === undefined
        || value === ""
    ) {
        return "만기됨";
    }

    const numberValue = Number(value);

    if (Number.isNaN(numberValue)) {
        return "만기됨";
    }

    const totalMinutes = Math.abs(numberValue);
    const days = Math.floor(
        totalMinutes / (60 * 24)
    );

    const hours = Math.floor(
        (totalMinutes % (60 * 24)) / 60
    );

    const minutes = totalMinutes % 60;

    if (days > 0) {
        return `만기됨(+${days}일 ${hours}시간 ${minutes}분)`;
    }

    if (hours > 0) {
        if (minutes === 0) {
            return `만기됨(+${hours}시간)`;
        }

        return `만기됨(+${hours}시간 ${minutes}분)`;
    }

    return `만기됨(+${minutes}분)`;
}

// startDate와 endDate의 차이를 등록기간 문구로 변환
// 만기 판단이 아니라 화면에 등록기간을 표시하기 위한 변환이다.
function periodText(startDate, endDate) {
    if (!startDate || !endDate) {
        return "-";
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    if (
        Number.isNaN(start.getTime())
        || Number.isNaN(end.getTime())
        || end <= start
    ) {
        return "-";
    }

    const totalMinutes = Math.floor(
        (end - start) / (1000 * 60)
    );

    const totalHours = Math.floor(
        totalMinutes / 60
    );

    const days = Math.floor(
        totalHours / 24
    );

    const hours = totalHours % 24;
    const minutes = totalMinutes % 60;

    if (days >= 30) {
        const months = Math.floor(days / 30);
        const remainDays = days % 30;

        if (remainDays === 0) {
            return `${months}개월`;
        }

        return `${months}개월 ${remainDays}일`;
    }

    if (days > 0) {
        if (hours === 0 && minutes === 0) {
            return `${days}일`;
        }

        return `${days}일 ${hours}시간 ${minutes}분`;
    }

    if (hours > 0) {
        if (minutes === 0) {
            return `${hours}시간`;
        }

        return `${hours}시간 ${minutes}분`;
    }

    return `${minutes}분`;
}

// 날짜를 화면 표시 문자열로 변환
function dateText(value) {
    if (!value) {
        return "-";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    const year = String(
        date.getFullYear()
    ).slice(2);

    const month = pad(
        date.getMonth() + 1
    );

    const day = pad(
        date.getDate()
    );

    const hour = pad(
        date.getHours()
    );

    const minute = pad(
        date.getMinutes()
    );

    const second = pad(
        date.getSeconds()
    );

    return `${year}.${month}.${day} ${hour}:${minute}:${second}`;
}

function pad(value) {
    return String(value).padStart(2, "0");
}