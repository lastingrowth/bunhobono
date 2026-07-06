import { defineStore } from "pinia";
import { createPolicy, deletePolicy, getPolicyDetail, getPolicyList, updatePolicy } from "./policyApi";
import { ref } from "vue";

export const usePolicyStore = defineStore("policy", () => {
    const policyList = ref([]);
    const policy = ref({});

    const loadPolicyList = async () => {
        const res = await getPolicyList();
        policyList.value = res.data;
    };

    const loadPolicy = async (policyNo) => {
        const res = await getPolicyDetail(policyNo);
        policy.value = res.data;
    };

    const addPolicy = async (data) => {
        const res = await createPolicy(data);
        return res.data;
    };

    const editPolicy = async (policyNo, data) => {
        await updatePolicy(policyNo, data);
    };

    const removePolicy = async (policyNo) => {
        await deletePolicy(policyNo);
    };

    return {
        policyList,
        policy,

        loadPolicyList,
        loadPolicy,
        addPolicy,
        editPolicy,
        removePolicy,
    };
});