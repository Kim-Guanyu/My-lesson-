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
import { buildMenuGroups } from "../utils/menu";
import { ROLE_GROUPS, hasAnyRole } from "../utils/permissions";

const store = useStore();
const route = useRoute();

const collapsed = computed(() => store.state.sidebarCollapsed);
const activePath = computed(() => route.path);

const roleRules = {
  "用户管理": ROLE_GROUPS.user,
  "课程管理": ROLE_GROUPS.course,
  "营销管理": ROLE_GROUPS.marketing,
  "订单管理": ROLE_GROUPS.order,
  "User Management": ROLE_GROUPS.user,
  "Course Management": ROLE_GROUPS.course,
  "Marketing": ROLE_GROUPS.marketing,
  "Orders": ROLE_GROUPS.order
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

const menuGroups = computed(() => {
  if (store.state.menus && store.state.menus.length) {
    const dynamicGroups = buildMenuGroups(store.state.menus);
    const hasDynamicItems = dynamicGroups.some((group) => group.items?.length);
    return filterGroupsByRole(hasDynamicItems ? dynamicGroups : staticMenuGroups);
  }
  return filterGroupsByRole(staticMenuGroups);
});
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
