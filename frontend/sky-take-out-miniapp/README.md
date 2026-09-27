# 苍穹外卖 - 用户端小程序

原生微信小程序（非 uni-app），苍穹外卖系统的用户端。

## 启动

微信开发者工具 → 导入项目 → 选择本目录

AppID：使用自己的小程序 AppID 或测试号

## 配置

编辑 `config/index.js`：

```javascript
const ENV = {
  development: {
    baseUrl: 'http://127.0.0.1:8000',  // 改为你的后端地址
  },
  production: {
    baseUrl: 'https://your-domain.com',  // 生产环境必须 HTTPS
  },
};
```

## 功能

| 页面 | 路径 | 说明 |
|---|---|---|
| 首页 | pages/index | 推荐、分类、热销菜品 |
| 菜单 | pages/menu | 左侧分类 + 右侧菜品 |
| 菜品详情 | pages/dish-detail | 规格选择 + 加购 |
| 购物车 | pages/cart | 商品列表 + 结算 |
| 下单 | pages/checkout | 地址 + 支付 |
| 订单列表 | pages/order-list | 历史订单 |
| 订单详情 | pages/order-detail | 订单状态 + 配送 |
| 地址管理 | pages/address | 增改删地址 |
| 登录 | pages/login | 微信登录 |
| 个人中心 | pages/profile | 用户信息 |

## AI 功能

- **智能客服**：首页右下角浮动按钮，AI 对话助手
- **AI 点餐**：客服内切换"点餐助手"模式，智能推荐菜品

## 目录结构

```
sky-take-out-miniapp/
├── app.js                 # 入口
├── app.json               # 全局配置（tabBar、页面注册）
├── app.wxss               # 全局样式（design system）
├── components/            # 公共组件
│   ├── ai-chat/           # AI 客服悬浮按钮 + 聊天面板
│   ├── empty-state/       # 空状态占位
│   ├── loading-skeleton/  # 加载骨架屏
│   ├── product-card/      # 菜品卡片（3 种模式）
│   ├── section-header/    # 区块标题
│   └── toast/             # 轻提示
├── config/                # 环境配置
├── pages/                 # 页面
├── services/              # 数据层
│   ├── auth.js            # 认证（JWT）
│   ├── cart.js            # 购物车
│   ├── request.js         # HTTP 封装（重试、超时、错误处理）
│   └── ai-order-planner.js  # AI 点餐（本地规则引擎）
├── utils/                 # 工具
│   ├── format.js          # 格式化（金额、销量）
│   └── network.js         # 网络状态监听
└── assets/                # 静态资源
    ├── icons/             # 图标
    └── tabbar/            # tabBar 图标
```

## Design System

全局 CSS 变量定义在 `app.wxss`：

- 主色：`#D97706`（暖橙）
- 间距：8/12/16/24/32/40/48/64 rpx
- 圆角：8/12/16/20/999(胶囊) rpx
- 阴影：sm/md/lg/primary/float

页面和组件应消费变量，不要硬编码。

## 网络请求

```javascript
const http = require('./services/request.js');

// GET
http.get('/user/category/list');

// POST
http.post('/user/chat', { message: '你好' }, { silent: true });

// 带重试
http.get('/user/shop/status', { retries: 3, timeout: 10000 });
```

特性：
- 自动注入 JWT（header + query parameter）
- 网络失败自动重试（指数退避，最多 3 次）
- 401 自动跳转登录
- 业务错误自动 toast

## 认证流程

1. 用户点击登录 → `wx.login` 获取 code
2. 调 `POST /user/user/login` 获取 JWT
3. 存 storage + 模块级缓存
4. 后续请求自动带 `authentication` header

## 注意事项

- 真机调试时，后端域名必须 HTTPS + 在小程序后台配置白名单
- 模拟器可用 HTTP（已关闭合法域名校验）
- 图片 URL 必须是 HTTPS

---

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
