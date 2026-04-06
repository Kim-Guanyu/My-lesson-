<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="login-title">MyLesson 管理端登录</div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="账号" name="account">
          <el-form :model="accountForm" label-width="90px">
            <el-form-item label="用户名">
              <el-input v-model="accountForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="accountForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="手机" name="phone">
          <el-form :model="phoneForm" label-width="90px">
            <el-form-item label="手机号">
              <el-input v-model="phoneForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="phoneForm.vcode" placeholder="请输入验证码">
                <template #append>
                  <el-button :disabled="vcodeCountdown > 0" @click="sendVcode">
                    {{ vcodeCountdown > 0 ? `${vcodeCountdown}s` : "发送" }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="login-actions">
        <el-button type="primary" :loading="loading" @click="submitLogin">登录</el-button>
      </div>
      <div class="login-footer">
        <el-text type="info">没有账号？请联系管理员开通。</el-text>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import http from "../api/http";
import { useStore } from "vuex";
import { fetchRoleTitlesByUserId } from "../api/roles";

const router = useRouter();
const store = useStore();

const activeTab = ref("account");
const loading = ref(false);
const accountForm = ref({
  username: "",
  password: ""
});
const phoneForm = ref({
  phone: "",
  vcode: ""
});

const vcodeCountdown = ref(0);
let vcodeTimer = null;

const startCountdown = () => {
  vcodeCountdown.value = 60;
  vcodeTimer = setInterval(() => {
    vcodeCountdown.value -= 1;
    if (vcodeCountdown.value <= 0) {
      clearInterval(vcodeTimer);
      vcodeTimer = null;
    }
  }, 1000);
};

const sendVcode = async () => {
  if (!phoneForm.value.phone) {
    ElMessage.warning("请输入手机号");
    return;
  }
  await http.get(`/user-server/api/v1/user/getVcode/${phoneForm.value.phone}`);
  ElMessage.success("验证码已发送");
  startCountdown();
};

const submitLogin = async () => {
  if (activeTab.value === "account") {
    if (!accountForm.value.username || !accountForm.value.password) {
      ElMessage.warning("请输入账号和密码");
      return;
    }
  } else if (!phoneForm.value.phone || !phoneForm.value.vcode) {
    ElMessage.warning("请输入手机号和验证码");
    return;
  }
  loading.value = true;
  try {
    const payload =
      activeTab.value === "account"
        ? await http.post("/user-server/api/v1/user/loginByAccount", accountForm.value, {
            meta: { errorDialog: true }
          })
        : await http.post("/user-server/api/v1/user/loginByPhone", phoneForm.value, {
            meta: { errorDialog: true }
          });

    store.commit("setAuth", {
      token: payload.token,
      user: payload.user,
      roles: payload.roleTitles,
      menus: payload.menus
    });

    if (payload.user?.id) {
      const roles = await fetchRoleTitlesByUserId(payload.user.id);
      if (roles.length) {
        store.commit("setRoles", roles);
      }
    }

    ElMessage.success("登录成功");
    router.push("/dashboard");
  } finally {
    loading.value = false;
  }
};

onBeforeUnmount(() => {
  if (vcodeTimer) {
    clearInterval(vcodeTimer);
  }
});
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
  margin-top: 12px;
}

.login-footer {
  margin-top: 8px;
  text-align: center;
}
</style>
