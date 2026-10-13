import { createRouter, createWebHashHistory } from "vue-router";
import AdminLayout from "../layouts/AdminLayout.vue";
import NotFound from "../views/NotFound.vue";
import Login from "../views/Login.vue";
import Forbidden from "../views/Forbidden.vue";
import store from "../store";
import { getToken } from "../utils/auth";
import { menuGroups } from "./menu";
import { buildDynamicRoutes } from "./dynamic";
import { hasAnyRole } from "../utils/permissions";
import { ElMessageBox } from "element-plus";
import { fetchRoleTitlesByUserId } from "../api/roles";

const AUTH_WHITELIST = ["/login", "/403"];
let dynamicRoutesReady = false;
let permissionPrompting = false;

const routes = [
  {
    path: "/login",
    component: Login,
    meta: { title: "Login" }
  },
  {
    path: "/403",
    component: Forbidden,
    meta: { title: "Forbidden" }
  },
  {
    path: "/",
    name: "admin",
    component: AdminLayout,
    redirect: "/dashboard",
    children: []
  },
  {
    path: "/:pathMatch(.*)*",
    component: NotFound
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

const syncDynamicRoutes = () => {
  if (dynamicRoutesReady) {
    return false;
  }
  buildDynamicRoutes().forEach((route) => {
    router.addRoute("admin", route);
  });
  dynamicRoutesReady = true;
  return true;
};

// 访问控制只按前端静态菜单判断。
// 后端 menu 表存的是 /Notice 这类路径，与本后台的 /notices 对不上，
// 用它做校验会把三个页面全部拦成 403。角色校验仍然生效（见 route.meta.roles）。
const ALLOWED_PATHS = menuGroups.flatMap((group) => (group.items || []).map((item) => item.path));

const hasMenuAccess = (path) => ALLOWED_PATHS.includes(path) || path.startsWith("/dashboard");

const ensureRolesLoaded = async () => {
  const userId = store.state.user?.id;
  if (!userId || (store.state.roles && store.state.roles.length)) {
    return;
  }
  try {
    const roles = await fetchRoleTitlesByUserId(userId);
    if (roles.length) {
      store.commit("setRoles", roles);
    }
  } catch (error) {
    // ignore, fallback to existing roles
  }
};

const showNoAccess = async () => {
  if (permissionPrompting) {
    return;
  }
  permissionPrompting = true;
  try {
    await ElMessageBox.alert("当前账号没有权限访问该页面。", "无权限", {
      type: "warning",
      confirmButtonText: "知道了"
    });
  } finally {
    permissionPrompting = false;
  }
};

router.beforeEach(async (to, from, next) => {
  const token = store.state.token || getToken();
  if (AUTH_WHITELIST.includes(to.path)) {
    if (to.path === "/login" && token) {
      next("/dashboard");
      return;
    }
    next();
    return;
  }
  if (!token) {
    next("/login");
    return;
  }
  const didSync = syncDynamicRoutes();
  if (didSync && to.matched.length === 0) {
    next({ ...to, replace: true });
    return;
  }

  await ensureRolesLoaded();

  if (!hasAnyRole(store.state.roles || [], to.meta?.roles || [])) {
    await showNoAccess();
    next("/403");
    return;
  }
  if (!hasMenuAccess(to.path)) {
    await showNoAccess();
    next("/403");
    return;
  }
  next();
});

router.afterEach((to) => {
  const title = to.meta?.title;
  document.title = title ? `MyLesson Admin - ${title}` : "MyLesson Admin";
});

export default router;
