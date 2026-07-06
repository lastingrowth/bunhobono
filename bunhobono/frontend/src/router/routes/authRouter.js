import LoginView from "@/features/login/LoginView.vue";
import MemSignup from "@/features/member/MemSignup.vue";

const authRouter = [

    {
        path : '/login',
        component : LoginView,
    },

    {
        path : '/signup',
        component : MemSignup,
    },
   

] 

export default authRouter