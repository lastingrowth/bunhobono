import './assets/css/layout.css'
import './assets/css/variables.css'
import './assets/css/admin-dashboard.css'
import './assets/css/resident-dashboard.css'
import './assets/css/monochrome-theme.css'
import { applyDeviceClass } from './shared/responsive/mobileDevice'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const app = createApp(App)

applyDeviceClass()
window.addEventListener('orientationchange', applyDeviceClass)

app.use(createPinia())
app.use(router)

app.mount('#app')
