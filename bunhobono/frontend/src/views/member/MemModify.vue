<template>
    <h2>회원 수정</h2>

    <div>
        <router-link to="/members">목록</router-link>
    </div>

    <table border="">
        <tbody>
            <tr>
                <th>가입유형</th>
                <td><input type="text" v-model="member.memRole"></td>
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
                <td><input type="text" v-model="member.memStatus"></td>
            </tr>
        </tbody>
    </table>

    <br>

    <button @click="modify">수정</button>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

const member = ref({});

onMounted(async () => {
    const memberNo = route.params.memberNo;

    const res = await fetch(`http://localhost/api/members/detail/${memberNo}`);

    member.value = await res.json();
});

async function modify() {
    const memberNo = route.params.memberNo;

    const res = await fetch(`http://localhost/api/members/detail/${memberNo}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(member.value)
    });

    if (res.ok) {
        alert("수정 완료");
        router.push(`/members/detail/${memberNo}`);
    } else {
        alert("수정 실패");
    }
}
</script>