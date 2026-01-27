import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import http from './http';
import './style.css';

const app = createApp(App);
app.config.globalProperties.$http = http;
app.use(router);
app.mount('#app');
