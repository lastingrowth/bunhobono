export const getDataTypeText = (dataType) => {
    const dataTypeText = {
        CAMERA_DATA: "카메라 데이터",
        CAR_LOG: "입출차 기록",
        NOTICE: "알림",
    };

    return dataTypeText[dataType] ?? dataType;
};

export const getDeleteTypeText = (deleteType) => {
    const deleteTypeText = {
        MANUAL: "직접 삭제",
        SCHEDULED: "자동 삭제",
    };

    return deleteTypeText[deleteType] ?? deleteType;
};

export const formatDate = (value) => {
    if (!value) {
        return "-";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString("ko-KR");
};

export const showValue = (value) => {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "-";
    }

    return value;
};

export const getRecognitionText = (value) => {
    if (value === true) {
        return "인식 성공";
    }

    if (value === false) {
        return "인식 실패";
    }

    return "-";
};

export const showConfidence = (value) => {
    if (value === null || value === undefined) {
        return "-";
    }

    return `${value}%`;
};

export const getCarKindText = (value) => {
    const carKindText = {
        REGISTERED: "등록 차량",
        RESIDENT: "등록 차량",
        NORMAL: "등록 차량",
        VISIT: "방문 차량",
        UNKNOWN: "미등록 차량",
        UNREGISTERED: "미등록 차량",
    };

    return carKindText[String(value ?? "").toUpperCase()] ?? showValue(value);
};

export const parseDataJson = (dataJson) => {
    if (!dataJson) {
        return {};
    }

    if (typeof dataJson === "object") {
        return dataJson;
    }

    try {
        return JSON.parse(dataJson);
    } catch {
        return {};
    }
};

export const formatJson = (dataJson) => {
    if (!dataJson) {
        return "-";
    }

    try {
        const parsedData =
            typeof dataJson === "object"
                ? dataJson
                : JSON.parse(dataJson);

        return JSON.stringify(parsedData, null, 2);
    } catch {
        return dataJson;
    }
};
