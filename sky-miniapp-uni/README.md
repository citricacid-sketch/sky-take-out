# 苍穹外卖 - 用户端 uni-app 小程序

苍穹外卖用户端微信小程序，基于 uni-app + Vue 3 开发。

## 技术栈

- **uni-app**（Vue 3 + uni-app 框架），可编译到微信小程序
- Vue 3 Composition API（`<script setup>`）
- Pinia 状态管理
- uni-ui 组件库（@dcloudio/uni-ui）
- SCSS

## 项目结构

```
sky-miniapp-uni/
├── src/
│   ├── pages/                 # 页面
│   ├── components/            # 公共组件
│   ├── stores/                # Pinia 状态管理
│   ├── utils/                 # 工具函数
│   ├── App.vue
│   ├── main.js
│   ├── manifest.json          # uni-app 配置
│   ├── pages.json             # 页面注册 + tabBar
│   └── uni.scss               # 全局样式变量
├── package.json
└── README.md
```

## 启动方式

### 1. 安装依赖

```bash
cd sky-miniapp-uni
npm install
```

### 2. 配置

编辑 `src/utils/config.js`：

- **BASE_URL**：修改为你的后端服务地址（如 `http://你的IP:8080`）
- **amapKey**：申请高德地图 Web Service API Key 并填入（[申请地址](https://console.amap.com/dev/key/app)）

### 3. 配置微信小程序 AppID

编辑 `src/manifest.json` 中 `mp-weixin.appid` 字段，填入你申请的微信小程序 AppID。

### 4. 运行

```bash
# 微信小程序开发模式
npm run dev:mp-weixin

# H5 模式（调试用）
npm run dev:h5
```

### 5. 构建发布

```bash
# 构建微信小程序
npm run build:mp-weixin
```

## 功能模块

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录 | `/pages/login/index` | 微信一键登录 |
| 首页 | `/pages/index/index` | 店铺状态 + 推荐菜品 |
| 菜单 | `/pages/menu/index` | 分类 + 菜品 + 购物车 |
| 购物车 | `/pages/cart/index` | 购物车管理 |
| 下单确认 | `/pages/order/confirm` | 地址选择 + 提交订单 |
| 订单列表 | `/pages/order/list` | Tab 筛选 + 动态操作按钮 |
| 订单详情 | `/pages/order/detail` | 订单信息 + 操作 |
| 地址簿 | `/pages/address/list` | 地址管理 |
| 地址编辑 | `/pages/address/edit` | 新增/编辑地址 |
| AI 客服 | `/pages/chat/index` | 智能客服聊天 |
| 配送追踪 | `/pages/delivery/index` | 高德地图 + 配送时间线 |
| 个人中心 | `/pages/profile/index` | 用户信息 + 快捷入口 |

## 后端 API

后端服务需运行在 `sky-server` 中，默认端口 8080。

所有 `/user/**` 接口需在请求 header 携带 `authentication: token`。

## 注意事项

- 本项目为前端代码，需配合 `sky-server` 后端使用
- 高德地图 Key 用于配送追踪页面，不配置则显示占位提示
- 微信小程序 AppID 用于真机调试和发布
