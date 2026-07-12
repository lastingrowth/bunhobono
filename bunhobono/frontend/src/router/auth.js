import AuthLayout from "@/layouts/AuthLayout.vue";
import LoginView from "@/features/login/LoginView.vue";
import MemSignup from "@/features/member/MemSignup.vue";

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
        path : '/signup',
        component : AuthLayout,

        children : [
            {
                path : '',
                name : 'Signup',
                component : MemSignup,
            }
        ]
    }
]