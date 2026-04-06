import { createApp } from "vue";
import ElementPlus from "element-plus";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import "element-plus/dist/index.css";
import "./assets/styles/main.css";

import App from "./App.vue";
import router from "./router";
import store from "./store";

const app = createApp(App);

app.use(router);
app.use(store);
app.use(ElementPlus);

Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component);
});

app.mount("#app");

