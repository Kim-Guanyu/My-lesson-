<template>
  <el-container class="app-layout">
    <Sidebar />
    <el-container>
      <Topbar />
      <el-main class="app-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from "../components/Sidebar.vue";
import Topbar from "../components/Topbar.vue";
</script>

