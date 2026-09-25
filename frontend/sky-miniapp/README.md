苍穹外卖 - 微信小程序端
=======================

基于 uni-app (Vue 3 + Vite) 开发的苍穹外卖微信小程序，全新开发，未复用旧版代码。

## 技术栈

- **uni-app** — 小程序开发框架
- **Vue 3** — Composition API + `<script setup>`
- **Vite** — 构建工具
- **Pinia** — 状态管理
- **SCSS** — 样式预处理

## 设计系统：「暖街食光」

- 主色 `#FFC300` 苍穹黄渐变，年轻活力风格
- 暖白底 `#FFFDF7`、深炭灰文字 `#1A1A2E`
- 橙红价格强调 `#FF4757`
- 大圆角、柔阴影、弹力动效

## 功能模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 首页菜单 | `pages/index` | 左侧分类+右侧菜品、猜你喜欢、购物车悬浮条 |
| 购物车 | `pages/cart` | 增减商品、清空、结算 |
| 确认订单 | `pages/checkout` | 地址、配送设置、备注、提交 |
| 订单列表 | `pages/order-list` | 状态筛选、分页、操作按钮 |
| 订单详情 | `pages/order-detail` | 状态横幅、骑手、商品、金额 |
| 收货地址 | `pages/address` | 地址列表、选择模式 |
| 地址编辑 | `pages/address-edit` | 省市区、标签、默认地址 |
| 配送追踪 | `pages/delivery` | 进度步骤、骑手信息 |
| 智能客服 | `pages/chat` | 对话式气泡、打字动画 |
| 个人中心 | `pages/profile` | 用户信息、订单入口、功能列表 |
| 登录 | `pages/login` | 微信一键登录 + 演示模式 |
| 菜品详情 | `pages/dish-detail` | 大图、口味选择 |

## 快速开始

### 环境要求

- HBuilder X 5.26+
- Node.js >= 18.0.0

### 安装与运行

```bash
# 安装依赖
cd sky-miniapp
npm install

# HBuilder X 中运行到微信开发者工具
# 1. 用 HBuilder X 打开本项目目录
# 2. 运行 -> 运行到小程序模拟器 -> 微信开发者工具
# 3. 确保微信开发者工具已开启服务端口

# 或命令行运行 H5 版
npm run dev:h5

# 运行微信小程序开发版
npm run dev:mp-weixin
```

### 后端配置

确保后端服务运行在 `http://localhost:8080`。

- H5 模式：通过 Vite 代理将 `/user` 请求转发到后端
- 微信小程序模式：在 `src/api/request.js` 修改 `BASE_URL` 为实际后端地址，并在小程序后台配置请求域名

## 配置说明

### 修改 API 地址

编辑 `src/api/request.js`：

```js
const BASE_URL = 'https://your-api-domain.com'  // 生产环境后端地址
```

### 微信小程序 AppID

编辑 `src/manifest.json` 中 `mp-weixin.appid` 字段，填入你的小程序 AppID。

### TabBar 图标

`static/tabbar/` 目录需要放置以下图标（建议 81x81 PNG）：
- `home.png` / `home-active.png`
- `order.png` / `order-active.png`
- `profile.png` / `profile-active.png`

## 项目结构

```
sky-miniapp/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.js          # 入口
│   ├── App.vue
│   ├── manifest.json    # 小程序配置
│   ├── pages.json       # 页面/路由/tabBar
│   ├── api/             # 接口封装
│   │   ├── request.js   # 请求封装
│   │   ├── user.js
│   │   ├── shop.js
│   │   ├── goods.js     # 分类/菜品/套餐
│   │   ├── cart.js
│   │   ├── order.js
│   │   ├── address.js
│   │   └── other.js     # 配送/客服
│   ├── components/      # 公共组件
│   │   ├── DishCard.vue
│   │   └── FlavorPicker.vue
│   ├── pages/           # 页面
│   ├── stores/          # Pinia
│   │   ├── user.js
│   │   └── cart.js
│   ├── utils/           # 工具函数
│   └── styles/          # 全局样式
└── static/              # 静态资源
```

## 接口契约

- 后端统一返回：`{ code: 1, data, msg }`，`code=1` 为成功
- 鉴权：请求头携带 `authentication: <token>`
- 登录：`POST /user/user/login` 传 `{code}` → 返回 `{id, openid, token}`
