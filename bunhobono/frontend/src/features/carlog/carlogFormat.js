export function toCarLogView(log) {
    return {
        ...log,
        parkingStateText: parkingStateText(log.parkingState),
        carKindText: carKindText(log.carKind),
        inTimeText: dateText(log.inTime),
        outTimeText: log.outTime ? dateText(log.outTime) : "주차중",
        parkingTimeText: parkingTimeText(log.inTime, log.outTime),
        inGateText: gateText(log.inGateName),
        outGateText: gateText(log.outGateName),
    };
}

function parkingStateText(value) {
    if (value === "PARKING") {
        return "주차중";
    }

    if (value === "OUT") {
        return "출차완료";
    }

    return "-";
}

function carKindText(value) {
    if (value === "REGISTERED") {
        return "등록차량";
    }

    if (value === "VISIT") {
        return "방문차량";
    }

    if (value === "UNKNOWN") {
        return "미등록차량";
    }

    return "-";
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

function parkingTimeText(inTime, outTime) {
    if (!inTime) {
        return "-";
    }

    const start = new Date(inTime);
    const end = outTime ? new Date(outTime) : new Date();

    const totalMinutes = Math.floor((end - start) / (1000 * 60));
    const days = Math.floor(totalMinutes / (60 * 24));
    const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
    const minutes = totalMinutes % 60;

    if (days > 0) {
        return `${days}일 ${hours}시간 ${minutes}분`;
    }

    if (hours > 0) {
        return `${hours}시간 ${minutes}분`;
    }

    return `${minutes}분`;
}

function gateText(value) {
    if (!value) {
        return "-";
    }

    if (value.endsWith("-IN")) {
        return value.replace("-IN", " GATE");
    }

    if (value.endsWith("-OUT")) {
        return value.replace("-OUT", " GATE");
    }

    return value;
}

