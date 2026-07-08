<template>
    <h2>회원등록</h2>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td>
                    <select v-model="member.role">
                        <option value="" disabled>선택하세요</option>
                        <option value="RESIDENT">입주민</option>
                        <option value="ADMIN">경비실</option>
                        <option value="ADMIN">관리실</option>
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
                <td><input type="text" v-model="member.loginId"><button @click="idCheck">아이디중복확인</button></td>
            </tr>
            <tr>
                <th>비밀번호</th>
                <td><input type="password" v-model="member.loginPwd"></td>
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
import { useMemStore } from "./memStore";

import { useJwtStore } from "../login/jwtStore";

const router = useRouter();
const store = useMemStore();

const jwtStore = useJwtStore();

const member = ref({
    role: "",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    memStatus: "",
});
const idChecked = ref(false);
const checkedLoginId = ref("");

// 아이디 중복확인
const idCheck = async () => {
    if (member.value.loginId === ""){
        alert("아이디를 입력하세요");
        return;
    }

    const exists = await store.idCheck(member.value.loginId);

    if (exists) {
        // true = 이미 존재
        idChecked.value = false;
        checkedLoginId.value = "";
        alert("이미 사용중인 아이디입니다.");
    } else {
        // false = 사용 가능
        idChecked.value = true;
        checkedLoginId.value = member.value.loginId;
        alert("사용 가능한 아이디입니다.");
    }
    
}


const signupGo = async () => {

    // 가입유형 선택 여부
    if (member.value.role === "") {
        alert("가입유형을 선택하세요.");
        return;
    }

    // 상태 선택 여부
    if (member.value.memStatus === "") {
        alert("상태를 선택하세요.");
        return;
    }

    if(!idChecked.value||checkedLoginId.value !== member.value.loginId){
        alert("아이디 중복확인을 해주세요")
        return;
    }

    try {

        // 1. 회원등록
        await store.signup(member.value);

        // 2. 방금 등록한 아이디/비번으로 로그인
        await jwtStore.loginGo(member.value.loginId, member.value.loginPwd);

        alert("회원등록 성공");

        // 권한에 따라 페이지 이동
        if (member.value.role === "ADMIN") {
            router.push("/admin");
        } else if (member.value.role === "RESIDENT") {
            router.push("/resident");
        }

    } catch (e) {
        console.error(e);
        alert("회원등록 실패");
    }
};
</script>