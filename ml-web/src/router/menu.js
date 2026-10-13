/**
 * 运营后台菜单：只保留公告、轮播图、秒杀三项。
 *
 * 这三项对应小程序首页的三块内容：
 *   公告   -> notice/top/1  顶部通知栏
 *   轮播图 -> banner/top/5  首页 swiper
 *   秒杀   -> seckill/today 秒杀专区
 */
export const menuGroups = [
  {
    label: "运营管理",
    icon: "Goods",
    items: [
      { label: "公告", path: "/notices", icon: "Bell" },
      { label: "轮播图", path: "/banners", icon: "Picture" },
      { label: "秒杀活动", path: "/seckills", icon: "Stopwatch" }
    ]
  }
];

export const menuRouteMeta = {
  "/notices": { apiBase: "/sale-server/api/v1/notice" },
  "/banners": { apiBase: "/sale-server/api/v1/banner" },
  "/seckills": { apiBase: "/sale-server/api/v1/seckill" }
};
