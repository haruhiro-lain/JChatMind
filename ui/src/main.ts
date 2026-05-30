import { createApp } from "vue";
import Antd from "ant-design-vue";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import App from "./App.vue";
import router from "./router";
import "./index.css";

const app = createApp(App);
app.use(Antd, { locale: zhCN as any });
app.use(router);
app.mount("#root");
