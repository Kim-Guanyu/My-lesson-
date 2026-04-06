export const menuGroups = [
  {
    label: "仪表盘",
    icon: "DataAnalysis",
    items: [{ label: "概览", path: "/dashboard", icon: "DataLine" }]
  },
  {
    label: "用户管理",
    icon: "User",
    items: [
      { label: "用户", path: "/users", icon: "User" },
      { label: "角色", path: "/roles", icon: "Avatar" },
      { label: "菜单", path: "/menus", icon: "Menu" },
      { label: "用户角色", path: "/user-roles", icon: "UserFilled" },
      { label: "角色菜单", path: "/role-menus", icon: "List" }
    ]
  },
  {
    label: "课程管理",
    icon: "Notebook",
    items: [
      { label: "分类", path: "/categories", icon: "Management" },
      { label: "课程", path: "/courses", icon: "Notebook" },
      { label: "季次", path: "/seasons", icon: "Collection" },
      { label: "集次", path: "/episodes", icon: "Tickets" },
      { label: "评论", path: "/comments", icon: "ChatLineRound" },
      { label: "举报", path: "/reports", icon: "Warning" },
      { label: "关注", path: "/follows", icon: "Star" }
    ]
  },
  {
    label: "营销管理",
    icon: "Goods",
    items: [
      { label: "公告", path: "/notices", icon: "Bell" },
      { label: "轮播图", path: "/banners", icon: "Picture" },
      { label: "文章", path: "/articles", icon: "Document" },
      { label: "秒杀", path: "/seckills", icon: "Stopwatch" },
      { label: "秒杀详情", path: "/seckill-details", icon: "Document" },
      { label: "优惠券", path: "/coupons", icon: "Present" }
    ]
  },
  {
    label: "订单管理",
    icon: "Files",
    items: [
      { label: "订单", path: "/orders", icon: "Goods" },
      { label: "订单明细", path: "/order-details", icon: "Document" }
    ]
  }
];

export const menuRouteMeta = {
  "/users": { apiBase: "/user-server/api/v1/user" },
  "/roles": { apiBase: "/user-server/api/v1/role" },
  "/menus": { apiBase: "/user-server/api/v1/menu" },
  "/user-roles": { apiBase: "/user-server/api/v1/userRole" },
  "/role-menus": { apiBase: "/user-server/api/v1/roleMenu" },
  "/categories": { apiBase: "/course-server/api/v1/category" },
  "/courses": { apiBase: "/course-server/api/v1/course" },
  "/seasons": { apiBase: "/course-server/api/v1/season" },
  "/episodes": { apiBase: "/course-server/api/v1/episode" },
  "/comments": { apiBase: "/course-server/api/v1/comment" },
  "/reports": { apiBase: "/course-server/api/v1/report" },
  "/follows": { apiBase: "/course-server/api/v1/follow" },
  "/notices": { apiBase: "/sale-server/api/v1/notice" },
  "/banners": { apiBase: "/sale-server/api/v1/banner" },
  "/articles": { apiBase: "/sale-server/api/v1/article" },
  "/seckills": { apiBase: "/sale-server/api/v1/seckill" },
  "/seckill-details": { apiBase: "/sale-server/api/v1/seckillDetail" },
  "/coupons": { apiBase: "/sale-server/api/v1/coupons" },
  "/orders": { apiBase: "/order-server/api/v1/order" },
  "/order-details": { apiBase: "/order-server/api/v1/orderDetail" }
};
