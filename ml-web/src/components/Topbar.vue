<template>
  <el-header class="app-header">
    <div class="header-left">
      <el-button link @click="toggleSidebar">
        <el-icon><Fold /></el-icon>
      </el-button>
      <span class="header-title">{{ title }}</span>
    </div>
    <div class="header-right">
      <el-tag type="info">{{ username }}</el-tag>
      <el-button link @click="logout">退出登录</el-button>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";

const route = useRoute();
const router = useRouter();
const store = useStore();

const title = computed(() => route.meta?.title || "仪表盘");
const username = computed(() => store.state.user?.username || store.state.user?.phone || "管理员");

const toggleSidebar = () => {
  store.commit("toggleSidebar");
};

const logout = () => {
  store.commit("clearAuth");
  router.push("/login");
};
</script>

<style scoped>
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
