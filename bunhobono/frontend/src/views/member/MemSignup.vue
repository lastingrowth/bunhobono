<template>
    <h2>회원등록</h2>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>
                    <select v-model="member.memRole">
                        <option value="" disabled>선택하세요</option>
                        <option value="resident">입주민</option>
                        <option value="guard">경비실</option>
                        <option value="office">관리실</option>
                    </select>
                </td>
            </tr>
            <tr>
                <th>이름</th>
                <td><input type="text" v-model="member.memName"></td>
            </tr>
            <tr>
                <th>동</th>
                <td><input type="text" v-model="member.memDong"></td>
            </tr>
            <tr>
                <th>호수</th>
                <td><input type="text" v-model="member.memHo"></td>
            </tr>
            <tr>
                <th>연락처</th>
                <td><input type="text" v-model="member.memPhone"></td>
            </tr>
            <tr>
                <th>아이디</th>
                <td><input type="text" v-model="member.memLoginId"></td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td><input type="password" v-model="member.memLoginPwd"></td>
            </tr>
            <tr>
                <th>상태</th>
                <td>
                    <select v-model="member.memStatus">
                        <option value="" disabled>선택하세요</option>
                        <option value="거주">거주</option>
                        <option value="전출">전출</option>
                        <option value="근무">근무</option>
                        <option value="휴직">휴직</option>
                        <option value="퇴사">퇴사</option>
                    </select>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                <button @click="signupGo">등록</button>
                </td>
            </tr>
        </tbody>
    </table>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const member = ref({
    memRole: "",
    memStatus: ""
});

const signupGo = async () => {
    const res = await fetch("http://localhost/api/members", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(member.value)
    });

    if (res.ok) {
        alert("회원등록 성공");
        router.push("/members");
    } else {
        alert("회원등록 실패");
    }
};
</script>