import AuthLayout from "@/layouts/AuthLayout.vue";
import LoginView from "@/features/login/LoginView.vue";
import MemSignup from "@/features/member/MemSignup.vue";
import FindAccount from "@/features/login/FindAccount.vue";

export const authRoutes = [
    {
        path : '/login',
        component : AuthLayout,
        children : [
            {
                path : '',
                name : 'Login',
                component : LoginView
            }
        ]
    },
    {
        path : '/resident/signup',
        component : AuthLayout,

        children : [
            {
                path : '',
                name : 'Signup',
                component : MemSignup,
            }
        ]
    },
    {
        path : '/resident/find-account',
        component : AuthLayout,
        children : [
            {
                path : '',
                name : 'FindAccount',
                component : FindAccount,
            }
        ]
    }
]
