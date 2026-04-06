<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="login-title">创建账号</div>
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <div class="login-actions">
        <el-button type="primary" :loading="loading" @click="submit">注册</el-button>
        <el-button @click="$router.push('/login')">返回</el-button>
      </div>
      <div class="login-footer">
        <el-text type="info">注册接口可通过 VITE_REGISTER_ENDPOINT 配置。</el-text>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import http from "../api/http";

const form = ref({
  username: "",
  password: "",
  phone: ""
});
const loading = ref(false);

const submit = async () => {
  const endpoint = import.meta.env.VITE_REGISTER_ENDPOINT || "";
  if (!endpoint) {
    ElMessage.warning("未配置注册接口");
    return;
  }
  loading.value = true;
  try {
    await http.post(endpoint, form.value);
    ElMessage.success("注册成功");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e5e7eb 0%, #f8fafc 100%);
}

.login-card {
  width: 420px;
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
  text-align: center;
}

.login-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.login-footer {
  margin-top: 8px;
  text-align: center;
}
</style>
