export function toCarLogView(log) {
    return {
        ...log,
        parkingStateText: parkingStateText(log.parkingState),
        carKindText: carKindText(log.carKind),
        inTimeText: dateText(log.inTime),
        outTimeText: log.outTime ? dateText(log.outTime) : "주차중",
        parkingTimeText: parkingTimeText(log.inTime, log.outTime),
        inGateText: gateText(log.inGateName, "IN"),
        outGateText: gateText(log.outGateName, "OUT"),
        parkingNameText: parkingNameText(log.parkingName, log.parkingCode),
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
        return "등록";
    }

    if (value === "VISIT") {
        return "방문";
    }

    if (value === "UNKNOWN") {
        return "미등록";
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

function gateText(value, direction) {
    if (!value) {
        return "-";
    }

    const normalized = String(value).trim().toUpperCase();

    // 지상 정문(MAIN)은 구분자가 하이픈/공백이거나 한글명이어도 A로 통일한다.
    if (normalized.includes("MAIN") || normalized.includes("정문")) {
        return `1F-A-${direction}`;
    }

    // 지상 후문(REAR)은 구분자가 하이픈/공백이거나 한글명이어도 B로 통일한다.
    if (normalized.includes("REAR") || normalized.includes("후문")) {
        return `1F-B-${direction}`;
    }

    return normalized;
}

function parkingNameText(name, code) {
    const normalizedCode = String(code ?? "").trim().toUpperCase();
    const normalizedName = String(name ?? "").trim().toUpperCase();

    if (normalizedCode === "SURFACE" || normalizedCode === "1F") {
        return "1F";
    }

    if (normalizedCode === "B1" || normalizedCode === "B2") {
        return normalizedCode;
    }

    if (/지하\s*1\s*층|\bB1\b/.test(normalizedName)) {
        return "B1";
    }

    if (/지하\s*2\s*층|\bB2\b/.test(normalizedName)) {
        return "B2";
    }

    if (/지상|1\s*층/.test(normalizedName)) {
        return "1F";
    }

    return name || "-";
}
