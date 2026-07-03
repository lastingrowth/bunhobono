<template>
    <div>
        <table border="">    
            <tr ><td colspan="2"><HeaderView/></td></tr>      
            <tr>
                <td><NavView/></td>
                <td><router-view/></td>
            </tr>
            
            <tr><td colspan="2"><FooterView/></td></tr>       
        </table>
    </div>
</template>
<script setup>
import FooterView from './FooterView.vue';
import HeaderView from './HeaderView.vue';
import NavView from './NavView.vue';




</script>