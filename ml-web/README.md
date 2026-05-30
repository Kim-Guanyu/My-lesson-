# 绿果付费短剧 · 运营后台

绿果付费短剧平台的 **B 端运营管理后台**，基于 Vite + Vue 3 + Element Plus 构建，用于短剧内容上架、用户管理、营销活动配置与订单处理。

## 功能模块

| 模块 | 功能 |
|------|------|
| 仪表盘 | 数据概览 |
| 用户管理 | 用户、角色、菜单、权限分配 |
| 短剧管理 | 分类、短剧、季、集、评论、举报、关注 |
| 营销管理 | 公告、轮播图、文章、秒杀、优惠券 |
| 订单管理 | 订单、订单明细 |

## 快速开始

```powershell
cd ml-web
npm install
npm run dev
```

## 环境配置

创建 `.env.local`：

```env
VITE_API_BASE_URL=http://localhost:24101
VITE_API_PROXY=http://localhost:24101
VITE_MINIO_BASE_URL=http://192.168.211.132:9000/my-lesson
```

| 变量 | 说明 |
|------|------|
| `VITE_API_BASE_URL` | 网关地址（Axios 请求） |
| `VITE_API_PROXY` | 开发环境 Vite 代理目标 |
| `VITE_MINIO_BASE_URL` | MinIO 资源前缀（封面、图片等） |

## 构建发布

```powershell
npm run build
npm run preview
```

构建产物位于 `dist/`，可部署至 Nginx 等静态服务器，并通过反向代理访问后端网关。

## 技术说明

- 路由与菜单：支持后端动态菜单 + 前端路由映射
- 权限：登录 Token 存储于 localStorage，请求头携带 `token` 字段
- 通用列表页：`EntityList.vue` 等组件复用 CRUD 能力
