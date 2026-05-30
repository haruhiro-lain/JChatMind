import { createApp } from "vue";
import Antd from "ant-design-vue";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import App from "./App.vue";
import router from "./router";
import "./index.css";

const app = createApp(App);
// @ts-expect-error ant-design-vue 4 locale typing issue
app.use(Antd, { locale: zhCN });
app.use(router);
app.mount("#root");
