import axios from "axios";
import { ElMessage, ElMessageBox } from "element-plus";
import store from "../store";
import { getToken } from "../utils/auth";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 10000
});

const SUCCESS_CODE = 1000;

const unwrapResponse = (payload) => {
  if (payload && typeof payload === "object" && "code" in payload) {
    if (payload.code === SUCCESS_CODE) {
      return "data" in payload ? payload.data : payload;
    }
    const message = payload.message || "Request failed";
    throw new Error(message);
  }
  return payload;
};

http.interceptors.request.use((config) => {
  const token = store.state.token || getToken();
  if (token) {
    config.headers.token = token;
  }
  return config;
});

const showError = (message, config) => {
  const wantsDialog = config?.meta?.errorDialog;
  if (wantsDialog && /(账号|密码|手机号|验证码)/.test(message)) {
    ElMessageBox.alert(message, "登录失败", {
      type: "error",
      confirmButtonText: "知道了"
    });
    return;
  }
  ElMessage.error(message);
};

http.interceptors.response.use(
  (response) => {
    try {
      return unwrapResponse(response.data);
    } catch (error) {
      const message = error?.message || "Request failed";
      showError(message, response.config);
      return Promise.reject(error);
    }
  },
  (error) => {
    const payload = error?.response?.data;
    if (payload?.code === 6000) {
      store.commit("clearAuth");
      if (window.location.hash !== "#/login") {
        window.location.hash = "#/login";
      }
    }
    const message = payload?.message || error.message || "Request failed";
    showError(message, error?.config);
    return Promise.reject(error);
  }
);

export default http;
