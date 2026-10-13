import Dashboard from "../views/Dashboard.vue";
import NoticeManage from "../views/NoticeManage.vue";
import BannerManage from "../views/BannerManage.vue";
import SeckillManage from "../views/SeckillManage.vue";
import { ROLE_GROUPS } from "../utils/permissions";

/**
 * 运营后台只有三个功能页，路由直接写死，不再依赖后端 menu 表。
 *
 * 原先走后端动态菜单，但 menu 表的 url 是 /Notice 这类大写单数形式，
 * 与前端路由 /notices 对不上，且 Menu 实体没有 apiBase 字段，
 * 导致页面渲染出来却拉不到数据。
 */
const routes = [
  {
    path: "/dashboard",
    name: "Dashboard",
    component: Dashboard,
    meta: {
      title: "仪表盘",
      icon: "DataAnalysis",
      roles: ROLE_GROUPS.marketing
    }
  },
  {
    path: "/notices",
    name: "NoticeManage",
    component: NoticeManage,
    meta: {
      title: "公告",
      icon: "Bell",
      roles: ROLE_GROUPS.marketing
    }
  },
  {
    path: "/banners",
    name: "BannerManage",
    component: BannerManage,
    meta: {
      title: "轮播图",
      icon: "Picture",
      roles: ROLE_GROUPS.marketing
    }
  },
  {
    path: "/seckills",
    name: "SeckillManage",
    component: SeckillManage,
    meta: {
      title: "秒杀活动",
      icon: "Stopwatch",
      roles: ROLE_GROUPS.marketing
    }
  }
];

export const buildDynamicRoutes = () => routes;

export default routes;
