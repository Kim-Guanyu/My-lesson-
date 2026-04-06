<template>
  <div class="page-card">
    <el-result icon="warning" title="页面不存在">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回仪表盘</el-button>
      </template>
    </el-result>
  </div>
</template>
