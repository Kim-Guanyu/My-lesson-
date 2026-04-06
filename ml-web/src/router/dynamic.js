import Dashboard from "../views/Dashboard.vue";
import EntityList from "../views/EntityList.vue";
import { normalizePath, buildMenuTree } from "../utils/menu";
import { ROLE_GROUPS } from "../utils/permissions";
import { menuGroups, menuRouteMeta } from "./menu";

const routes = [
  {
    path: "/dashboard",
    name: "Dashboard",
    component: Dashboard,
    meta: {
      title: "仪表盘",
      icon: "DataAnalysis",
      roles: [ROLE_GROUPS.user, ROLE_GROUPS.course, ROLE_GROUPS.marketing, ROLE_GROUPS.order].flat()
    }
  }
  // ...other static routes can be added here
];

const mapMenuToRoute = (menu) => {
  const path = normalizePath(menu.url || menu.path || menu.menuUrl || "");
  if (!path) {
    return null;
  }
  const isDashboard = path === "/dashboard" || path === "/";
  return {
    path: isDashboard ? "/dashboard" : path,
    name: menu.name || menu.title || `menu-${menu.id}`,
    component: isDashboard ? Dashboard : EntityList,
    meta: {
      title: menu.title || menu.name || "菜单",
      icon: menu.icon || "Menu",
      roles: menu.roles || menu.roleNames || [],
      apiBase: menu.apiBase || menu.api || menu.baseUrl || "",
      listPath: menu.listPath || "page",
      createPath: menu.createPath || "insert",
      updatePath: menu.updatePath || "update",
      deletePath: menu.deletePath || "delete",
      deleteBatchPath: menu.deleteBatchPath || ""
    }
  };
};

const flattenMenus = (menus = []) => {
  const tree = buildMenuTree(menus);
  const result = [];
  const visit = (items) => {
    items.forEach((item) => {
      const route = mapMenuToRoute(item);
      if (route) {
        result.push(route);
      }
      if (item.children?.length) {
        visit(item.children);
      }
    });
  };
  visit(tree);
  return result;
};

const buildStaticRoutes = () => {
  const routes = [];
  menuGroups.forEach((group) => {
    (group.items || []).forEach((item) => {
      routes.push({
        path: item.path,
        name: `static-${item.path.replace("/", "")}`,
        component: EntityList,
        meta: {
          title: item.label,
          icon: item.icon || "Menu",
          apiBase: menuRouteMeta[item.path]?.apiBase || "",
          listPath: menuRouteMeta[item.path]?.listPath || "page"
        }
      });
    });
  });
  return routes;
};

export const buildDynamicRoutes = (menus = []) => {
  const dynamic = menus.length ? flattenMenus(menus) : [];
  const staticRoutes = dynamic.length ? [] : buildStaticRoutes();
  return [...routes, ...dynamic, ...staticRoutes];
};

export default routes;
