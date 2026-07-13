<template>
    <h2>입주민 회원가입</h2>

    <table border="">
        <tbody>
            <tr>
                <th>이름</th>
                <td><input type="text" v-model="member.memName"></td>
            </tr>
            <tr>
                <th>동</th>
                <td>
                    <!-- 입주민 회원가입에서 101~108동 셀렉트로 선택. -->
                    <select v-model.number="member.memDong">
                        <option disabled value="">동을 선택하세요</option>
                        <option v-for="dong in dongOptions" :key="dong" :value="dong">{{ dong }}</option>
                    </select>
                </td>
            </tr>
            <tr>
                <th>호수</th>
                <td>
                    <input
                        type="text"
                        inputmode="numeric"
                        :value="member.memHo"
                        placeholder="숫자만 입력하세요"
                        @input="handleHoInput"
                    >
                </td>
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

const router = useRouter();
const store = useMemStore();
const dongOptions = [101, 102, 103, 104, 105, 106, 107, 108];

const member = ref({
    // 공개 회원가입은 입주민 전용이므로 권한을 선택받지 않고 RESIDENT로 고정.
    role: "RESIDENT",
    memName: "",
    memDong: "",
    memHo: "",
    memPhone: "",
    loginId: "",
    loginPwd: "",
    // 공개 입주민 회원가입의 초기 상태는 항상 거주로 고정.
    memStatus: "거주",
});
const idChecked = ref(false);
const checkedLoginId = ref("");

// 호수에 문자가 입력되면 제거하고 숫자 입력 안내를 표시.
const handleHoInput = (event) => {
    const inputValue = event.target.value;
    const numericValue = inputValue.replace(/\D/g, "");
    if (inputValue !== numericValue) {
        alert("숫자만 입력하세요.");
    }
    event.target.value = numericValue;
    member.value.memHo = numericValue === "" ? "" : Number(numericValue);
};

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

    if(!idChecked.value||checkedLoginId.value !== member.value.loginId){
        alert("아이디 중복확인을 해주세요")
        return;
    }

    try {
        await store.signup(member.value);

        alert("회원등록 성공");

        router.push("/login");
    } catch (e) {
        console.error(e);
        alert("회원등록 실패");
    }
};
</script>
