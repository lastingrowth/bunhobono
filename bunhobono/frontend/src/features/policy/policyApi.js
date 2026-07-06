import api from "@/shared/api/apiClient"

export const getPolicyList = () => {
    return api.get("/fee-policies");
};

export const getPolicyDetail = (policyNo) => {
    return api.get(`/fee-policies/${policyNo}`);
};

export const createPolicy = (data) => {
    return api.post("/fee-policies", data);
};

export const updatePolicy = (policyNo, data) => {
    return api.put(`/fee-policies/${policyNo}`, data);
};

export const deletePolicy = (policyNo) => {
    return api.delete(`/fee-policies/${policyNo}`);
};