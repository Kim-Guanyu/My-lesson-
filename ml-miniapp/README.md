# 绿果付费短剧 · 微信小程序

绿果付费短剧平台的 **C 端用户小程序**，提供短剧浏览、视频播放、弹幕互动、购物车与个人中心等功能。

## 主要页面

| 路径 | 说明 |
|------|------|
| `pages/index/index` | 首页（轮播、推荐） |
| `pages/course/course` | 短剧列表 / 搜索 |
| `pages/course/detail/detail` | 短剧详情、视频播放、弹幕 |
| `pages/cart/cart` | 购物车 |
| `pages/user/user` | 个人中心 |
| `pages/user/order/order` | 我的订单 |

## 快速开始

1. 安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 导入本项目目录 `ml-miniapp`
3. 修改 `utils/const.js` 中的服务地址：

```javascript
const HOST = 'localhost';              // 网关地址
const LINUX_HOST = '192.168.211.132';  // MinIO 地址
```

4. 确保后端网关（24101）、弹幕服务（24106）已启动
5. 编译运行

## 依赖说明

- UI 组件：Vant Weapp
- 网络请求：`utils/api.js` 统一封装，经网关访问各微服务
- 弹幕：WebSocket 连接 `ml-barrage`，历史弹幕 HTTP 拉取 + 自定义弹幕层渲染

## 开发注意

- 开发阶段可在开发者工具中关闭「校验合法域名」
- 视频、图片资源托管于 MinIO，需保证网络可达
- 发送弹幕需先登录
