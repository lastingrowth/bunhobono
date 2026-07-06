    <template>
        <div>
            <table border="">    
                <tr ><td colspan="2"><HeaderView/></td></tr>      
                <tr>
                    <td>
                        <AdminNav v-if="jwtStore.role === 'ADMIN'"/> 
                        <ResidentNav v-else/>
                    </td>
                    <td><router-view/></td>
                </tr>
                
                <tr><td colspan="2"><FooterView/></td></tr>       
            </table>
        </div>
    </template>


    <script setup>
    import { useJwtStore } from '@/features/login/jwtStore.js';
    import HeaderView from './HeaderView.vue';
    import FooterView from './FooterView.vue';
    import AdminNav from '../admin/AdminNav.vue';
    import ResidentNav from '../resident/ResidentNav.vue';
    import { useRoute } from 'vue-router';

    const jwtStore = useJwtStore()
    const route = useRoute()
    </script>