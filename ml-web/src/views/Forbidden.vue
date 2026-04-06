<template>
  <div class="page-card">
    <el-result icon="error" title="无权限" sub-title="当前账号没有权限访问该页面。">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回仪表盘</el-button>
      </template>
    </el-result>
  </div>
</template>
