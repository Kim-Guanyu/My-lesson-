<template>
  <el-aside width="240px" class="app-sidebar">
    <div class="sidebar-logo">MyLesson 管理端</div>
    <el-menu
      :collapse="collapsed"
      :default-active="activePath"
      router
      class="sidebar-menu"
    >
      <el-sub-menu v-for="group in menuGroups" :key="group.label" :index="group.label">
        <template #title>
          <el-icon><component :is="group.icon" /></el-icon>
          <span>{{ group.label }}</span>
        </template>
        <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import { menuGroups as staticMenuGroups } from "../router/menu";
import { ROLE_GROUPS, hasAnyRole } from "../utils/permissions";

const store = useStore();
const route = useRoute();

const collapsed = computed(() => store.state.sidebarCollapsed);
const activePath = computed(() => route.path);

const roleRules = {
  "运营管理": ROLE_GROUPS.marketing
};

const filterGroupsByRole = (groups) => {
  const roles = store.state.roles || [];
  if (!roles.length) {
    return groups;
  }
  return groups
    .filter((group) => {
      const requiredRoles = roleRules[group.label] || [];
      return hasAnyRole(roles, requiredRoles);
    })
    .map((group) => ({
      ...group,
      items: group.items || []
    }))
    .filter((group) => group.items.length > 0);
};

// 只用前端静态菜单：后端 menu 表的 url 是 /Notice 这种大写单数形式，
// 与前端路由 /notices 对不上，且 Menu 实体没有 apiBase 字段，走动态菜单会加载不到数据。
const menuGroups = computed(() => filterGroupsByRole(staticMenuGroups));
</script>

<style scoped>
.app-sidebar {
  background: #111827;
  color: #ffffff;
}

.sidebar-logo {
  padding: 16px;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  background: #0f172a;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu) {
  background: transparent;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  color: #e5e7eb;
}

:deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.2);
  color: #ffffff;
}
</style>
