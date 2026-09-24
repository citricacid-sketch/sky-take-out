# 苍穹外卖 - 管理端 Web 前端

苍穹外卖项目的管理后台前端，基于 Vue 3 + Element Plus 构建。

## 技术栈

- **Vue 3** — Composition API + `<script setup>`
- **Element Plus** — UI 组件库
- **Vite** — 构建工具
- **Vue Router 4** — 路由管理
- **Pinia** — 状态管理
- **Axios** — HTTP 请求
- **ECharts** — 数据可视化
- **SCSS** — 样式预处理

## 功能模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 登录 | `/login` | 员工登录（MD5 加密） |
| 工作台 | `/dashboard` | 今日数据概览 + 图表 |
| 员工管理 | `/employee` | 员工 CRUD、启停、改密 |
| 分类管理 | `/category` | 菜品/套餐分类管理 |
| 菜品管理 | `/dish` | 菜品 CRUD、口味、图片上传 |
| 套餐管理 | `/setmeal` | 套餐 CRUD、关联菜品 |
| 订单管理 | `/order` | 条件搜索、动态操作按钮 |
| 店铺设置 | `/shop` | 营业/打烊状态切换 |
| 数据统计 | `/report` | ECharts 图表 + Excel 导出 |
| AI 数据助手 | `/ai-assistant` | 对话式数据查询 |
| 骑手管理 | `/rider` | 骑手 CRUD、状态、位置 |
| 配送管理 | `/delivery` | 配送单分配、状态跟踪 |

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装与启动

```bash
# 进入项目目录
cd sky-admin-web

# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 构建生产版本
npm run build
```

### 后端服务

确保后端服务已启动并运行在 `http://localhost:8080`，Vite 代理会自动将 `/api` 请求转发到后端。

### 默认账号

使用后端已配置的员工账号登录（用户名 + 密码），密码在前端经 MD5 加密后传输。

## 项目结构

```
sky-admin-web/
├── src/
│   ├── api/            # 接口封装（axios + 各业务模块）
│   ├── views/          # 页面组件
│   ├── router/         # 路由配置 + 守卫
│   ├── stores/         # Pinia 状态管理
│   ├── components/     # 公共组件
│   ├── utils/          # 工具函数
│   ├── styles/         # 全局样式
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

## 主题色

主色调 `#FFC300`（苍穹黄），侧边栏 `#304156`（深蓝灰）。
