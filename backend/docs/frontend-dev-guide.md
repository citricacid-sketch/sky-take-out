# 苍穹外卖前端开发参考（全量接口文档）

> 生成时间：2026-09-25 · 基于后端源码全量调研（backend @ commit 42cf2c4f）
> 用途：重建用户端小程序 / 维护管理端 Web 前端时的接口对接手册

---

## 1. 项目全貌

```
sky-take-out/                      ← git 仓库根
├── backend/                       ← Spring Boot 后端（本手册调研对象）
│   ├── sky-common/                ← 常量/工具/统一返回/JWT/OSS/LLM 客户端
│   ├── sky-pojo/                  ← DTO(24) / Entity(14) / VO(22)
│   └── sky-server/                ← 唯一可运行模块：controller/service/mapper/task/websocket
├── frontend/
│   └── sky-admin-web/             ← 管理端 Web（已存在，Vue3 + Element Plus + Pinia + ECharts + Vite）
└── (frontend/sky-miniapp 已删除)  ← 用户端小程序（uni-app），待重建
```

**后端技术栈**：Spring Boot 2.7.3 / JDK 17 / MyBatis 2.2.0 + PageHelper 1.4.6 / Druid / MySQL / Redis（缓存+会话）/ WebSocket（javax.websocket 原生）/ Knife4j 3.0.2 / 阿里云 OSS / 百度地图 API / OpenAI 兼容 LLM

**原小程序页面结构**（重建时可参考，来自被删的 sky-miniapp）：
- TabBar 3 个：首页(index) / 订单(order-list) / 我的(profile)
- 页面：login、dish-detail、cart、checkout、address、address-edit、order-detail、chat（AI客服）、delivery（配送追踪）
- 组件：DishCard、FlavorPicker
- API 层：request.js 封装 + user/goods/cart/order/address/shop/other 分模块

---

## 2. 全局约定（对接必读）

| 项 | 值 |
|---|---|
| Base URL | `http://localhost:8080`（**无 context-path**，接口路径即根路径） |
| 统一响应 | `Result<T>`：`{ "code": 1, "msg": "...", "data": T }`，**code=1 成功，0 失败**；HTTP 状态码恒为 200（除认证失败 401） |
| 分页响应 | `PageResult`：`{ "total": long, "records": List }` |
| **日期格式** | `LocalDateTime` → **`yyyy-MM-dd HH:mm`（不带秒！）**；`LocalDate` → `yyyy-MM-dd`；`LocalTime` → `HH:mm:ss`。反序列化容忍未知字段 |
| 管理端认证头 | **`token`**：JWT（claims 含 `empId`），TTL 2 小时 |
| 用户端认证头 | **`authentication`**：JWT（claims 含 `userId`），TTL 2 小时 |
| 认证失败 | 直接返回 **HTTP 401，无响应体**（前端拦截器按状态码处理，不是 JSON） |
| **CORS** | **后端零 CORS 配置** —— 浏览器直连跨域必被拦截，前端必须走 devServer 代理或 Nginx |
| 参数校验 | 全项目 DTO **无 JSR-303 注解**，"必填"仅业务层强制，错误经 `GlobalExceptionHandler` 返回 `code=0` + 中文 msg |
| 接口文档 | Knife4j：`http://localhost:8080/doc.html`（不受 JWT 拦截） |
| 图片 | 阿里云 OSS 外链 `https://xs-sky-itcast.oss-cn-beijing.aliyuncs.com/{uuid}.{ext}`，前端直接 `<image :src>` |

**管理端 Vite 代理已配置**（sky-admin-web/vite.config.js）：`/api` → `http://localhost:8080`（rewrite 去掉 `/api` 前缀），dev 端口 5173。重建小程序时建议沿用相同思路（H5 模式配代理；小程序真机需直连局域网 IP）。

**请求封装参考**（管理端现状，src/api/request.js）：
```js
const service = axios.create({ baseURL: '/api', timeout: 15000 })
// 请求拦截：headers.token = getToken()        ← 管理端
// 小程序端则为：headers.authentication = getToken()
// 响应拦截：res.code === 1 ? res.data : reject(res.msg)
// HTTP 401 → 清 token 跳登录页
```

---

## 3. 用户端（小程序）API —— 30 个接口

**鉴权规则**：拦截 `/user/**`，仅 2 个公开接口：`POST /user/user/login`、`GET /user/shop/status`。其余全部需 `authentication` 头。

### 3.1 速查总表

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/user/user/login` | 公开 | 微信登录 |
| 2 | GET | `/user/shop/status` | 公开 | 店铺营业状态（1营业/0打烊，Redis 异常兜底返回 1） |
| 3 | GET | `/user/category/list?type=` | 登录 | 分类列表（type: 1菜品/2套餐，可选） |
| 4 | GET | `/user/dish/list?categoryId=` | 登录 | 菜品列表（含口味 flavors；categoryId 可选=全部起售） |
| 5 | GET | `/user/setmeal/list?categoryId=` | 登录 | 套餐列表 |
| 6 | GET | `/user/setmeal/dish/{id}` | 登录 | 套餐内菜品明细 |
| 7 | POST | `/user/shoppingCart/add` | 登录 | 加入购物车（JSON body） |
| 8 | GET | `/user/shoppingCart/list` | 登录 | 购物车列表 |
| 9 | POST | `/user/shoppingCart/sub` | 登录 | 减购一件（**query/form 参数，非 JSON**） |
| 10 | DELETE | `/user/shoppingCart/clean` | 登录 | 清空购物车 |
| 11 | GET | `/user/addressBook/list` | 登录 | 地址列表 |
| 12 | POST | `/user/addressBook` | 登录 | 新增地址 |
| 13 | GET | `/user/addressBook/{id}` | 登录 | 地址详情 |
| 14 | PUT | `/user/addressBook` | 登录 | 修改地址 |
| 15 | PUT | `/user/addressBook/default` | 登录 | 设为默认地址（body 只需 `id`） |
| 16 | DELETE | `/user/addressBook?id=` | 登录 | 删除地址（**id 是 query 参数**） |
| 17 | GET | `/user/addressBook/default` | 登录 | 查默认地址（无默认时 code=0 "没有查询到默认地址"） |
| 18 | POST | `/user/order/submit` | 登录 | 提交订单 |
| 19 | PUT | `/user/order/payment` | 登录 | 支付（当前为**模拟支付**，直接成功） |
| 20 | GET | `/user/order/historyOrders` | 登录 | 历史订单分页 |
| 21 | GET | `/user/order/orderDetail/{id}` | 登录 | 订单详情 |
| 22 | PUT | `/user/order/cancel/{id}` | 登录 | 取消订单 |
| 23 | POST | `/user/order/repetition/{id}` | 登录 | 再来一单（明细复制回购物车） |
| 24 | GET | `/user/order/reminder/{id}` | 登录 | 催单（WebSocket type=2 推商家） |
| 25 | GET | `/user/order/{id}/actions` | 登录 | 订单可执行动作（新） |
| 26 | GET | `/user/recommend` | 登录 | 猜你喜欢（新，返回 DishVO 列表） |
| 27 | POST | `/user/chat` | 登录 | AI 智能客服（新） |
| 28 | GET | `/user/delivery/{orderId}` | 登录 | 配送追踪（新，无配送单时 data=null） |
| 29 | 任意 | `/notify/paySuccess` | 公开 | 微信支付回调（服务端间调用，前端不用管） |
| 30 | WS | `/ws/{sid}` | 公开 | WebSocket 推送（见第 5 节） |

### 3.2 请求体字段

**UserLoginDTO**（登录）：`code` String —— 微信授权码（wx.login 获取）

**ShoppingCartDTO**（加购 JSON / 减购 query）：
| 字段 | 类型 | 说明 |
|---|---|---|
| `dishId` | Long | 菜品 id（与 setmealId 二选一） |
| `setmealId` | Long | 套餐 id |
| `dishFlavor` | String | 口味，如 `"[\"不要辣\",\"少冰\"]"` |

**OrdersSubmitDTO**（下单）：
| 字段 | 类型 | 说明 |
|---|---|---|
| `addressBookId` | Long | **业务必填**（不存在报"地址簿为空"） |
| `payMethod` | int | 1微信 2支付宝 |
| `remark` | String | 备注 |
| `estimatedDeliveryTime` | LocalDateTime | 格式 `yyyy-MM-dd HH:mm:ss`（该字段单独注解覆盖全局格式） |
| `deliveryStatus` | Integer | 1立即送出 0选择具体时间 |
| `tablewareNumber` | Integer | 餐具数量 |
| `tablewareStatus` | Integer | 1按餐量提供 0具体数量 |
| `packAmount` | Integer | 打包费（服务端直接采用） |
| `amount` | BigDecimal | **传了也被忽略，服务端按购物车重算** |

**OrdersPaymentDTO**（支付）：`orderNumber` String（必填）、`payMethod` Integer（可选）

**AddressBook**（地址簿直接用实体做请求体）：
`id`(修改/设默认必传)、`consignee`、`phone`、`sex`(0女1男)、`provinceCode/provinceName`、`cityCode/cityName`、`districtCode/districtName`、`detail`(详细地址)、`label`(如"家"/"公司")、`isDefault`(0/1)

**AI 客服**：`POST /user/chat` body 为 `{"message": "..."}`（无 DTO 类，Map 取键）

### 3.3 响应 VO 字段

**UserLoginVO**：`id` Long、`openid` String、`token` String（后续放 `authentication` 头）

**DishVO**：`id`、`name`、`categoryId`、`price`(BigDecimal)、`image`、`description`、`status`(0停售1起售)、`updateTime`、`categoryName`、`flavors`: List&lt;DishFlavor&gt;
- DishFlavor：`id`、`dishId`、`name`（口味名如"温度"）、`value`（JSON 字符串如 `"[\"热饮\",\"冷饮\"]"`，**前端需 JSON.parse**）

**Setmeal**（实体直接返回）：`id`、`categoryId`、`name`、`price`、`status`、`description`、`image`、`createTime`、`updateTime`

**DishItemVO**（套餐明细）：`name`、`copies`、`image`、`description`

**Category**：`id`、`type`(1菜品2套餐)、`name`、`sort`、`status`

**ShoppingCart**：`id`、`name`、`userId`、`dishId`、`setmealId`、`dishFlavor`、`number`、`amount`、`image`、`createTime`

**OrderSubmitVO**：`id`、`orderNumber`、`orderAmount`(BigDecimal)、`orderTime`

**OrderPaymentVO**：`nonceStr`、`paySign`、`timeStamp`、`signType`、`packageStr`（模拟支付下基本为空，前端只需视为"支付成功"）

**OrderVO**（extends Orders，订单详情/历史订单）：
- 继承 Orders：`id`、`number`(订单号)、`status`(见第 6 节)、`userId`、`addressBookId`、`orderTime`、`checkoutTime`、`payMethod`(1微信2支付宝)、`payStatus`(0未支付1已支付2退款)、`amount`、`remark`、`userName`、`phone`、`address`、`consignee`、`cancelReason`、`rejectionReason`、`cancelTime`、`estimatedDeliveryTime`、`deliveryStatus`(1立即送出0具体时间)、`deliveryTime`、`packAmount`、`tablewareNumber`、`tablewareStatus`
- 新增：`orderDishes`（菜品信息串，仅管理端填充）、`orderDetailList`: List&lt;OrderDetail&gt;
- OrderDetail：`id`、`name`、`orderId`、`dishId`、`setmealId`、`dishFlavor`、`number`、`amount`、`image`

**ActionDetailVO**（订单动作）：`action`(PAY/CANCEL)、`label`(中文标签)、`needReason`(Boolean)

**DeliveryVO**（配送追踪）：`id`、`orderId`、`riderId`、`status`(0待分配1已分配2取餐中3配送中4已完成5异常)、`assignTime`、`pickupTime`、`finishTime`、`remark`、`riderName`、`riderPhone`、`riderLongitude`、`riderLatitude`

**historyOrders 查询参数**：`page`、`pageSize`（必传 int）+ 可选 `number`/`phone`/`status`/`beginTime`/`endTime`（`yyyy-MM-dd HH:mm:ss`）；`userId` 服务端强制覆盖

### 3.4 关键业务规则（前端需感知）

- **下单校验**：地址簿必须存在；百度地图算配送距离 **>5000 米报"超出配送范围，无法下单"**；购物车空报错
- **下单流程**：`submit` 成功（清空购物车，返回订单号）→ 跳收银台 → `payment`（模拟支付直接成功，订单变"待接单"，WebSocket 推 type=1）
- **取消订单**：走状态机校验；待接单(2)状态取消会置 payStatus=2 退款；cancelReason 固定"用户取消"
- **超时**：待付款 15 分钟未支付被定时任务自动取消（"超时未支付"）；每天 1 点派送中超 1 小时自动完成
- **推荐接口**：基于历史订单频次 + 热销兜底，Redis 缓存 1 小时，最多 10 个菜品

---

## 4. 管理端 API —— 13 个 Controller / 53 个接口

**鉴权规则**：拦截 `/admin/**`，仅 `POST /admin/employee/login` 公开，其余需 `token` 头。

### 4.1 员工 `/admin/employee`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/employee/login` | 登录（body: `username`/`password`）→ `EmployeeLoginVO{id, userName, name, token}`（注意是 `userName` 驼峰） |
| POST | `/admin/employee` | 新增员工（EmployeeDTO） |
| GET | `/admin/employee/page?name=&page=&pageSize=` | 分页（records 为 Employee 列表） |
| POST | `/admin/employee/status/{status}?id=` | 启用/禁用（status 0/1） |
| GET | `/admin/employee/{id}` | 详情 |
| PUT | `/admin/employee` | 修改 |
| PUT | `/admin/employee/editPassword` | 改密码（body: `oldPassword`/`newPassword`；empId 服务端从 JWT 取） |
| POST | `/admin/employee/logout` | 退出 |

EmployeeDTO：`id`、`username`、`name`、`phone`、`sex`、`idNumber`

### 4.2 分类 `/admin/category`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/category` | 新增（CategoryDTO：`id`/`type`/`name`/`sort`） |
| GET | `/admin/category/page?page=&pageSize=&name=&type=` | 分页 |
| DELETE | `/admin/category?id=` | 删除 |
| PUT | `/admin/category` | 修改 |
| POST | `/admin/category/status/{status}?id=` | 启用/禁用 |
| GET | `/admin/category/list?type=` | 列表（下拉选择用） |

### 4.3 菜品 `/admin/dish`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/dish` | 新增（DishDTO + flavors） |
| GET | `/admin/dish/page?page=&pageSize=&name=&categoryId=&status=` | 分页（records 为 DishVO） |
| DELETE | `/admin/dish?ids=1,2,3` | **批量删除** |
| POST | `/admin/dish/status/{status}?status=&id=` | 起售/停售（**status 实际取自 query 参数**，路径变量形同虚设，两个都传保持一致即可） |
| GET | `/admin/dish/{id}` | 详情（DishVO 含 flavors） |
| PUT | `/admin/dish` | 修改 |
| GET | `/admin/dish/list?categoryId=` | 按分类查（套餐编辑用） |

DishDTO：`id`、`name`、`categoryId`、`price`、`image`、`description`、`status`、`flavors`: List&lt;DishFlavor&gt;（`name` 口味名 + `value` JSON 数组字符串）
DishVO 额外含：`categoryName`、`updateTime`
副作用：增删改/起售停售会清 Redis `dish_*` 缓存

### 4.4 套餐 `/admin/setmeal`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/setmeal` | 新增（SetmealDTO） |
| GET | `/admin/setmeal/page?page=&pageSize=&name=` | 分页（**裸参数，非 DTO**） |
| DELETE | `/admin/setmeal?ids=` | 批量删除 |
| GET | `/admin/setmeal/{id}` | 详情（SetmealVO） |
| PUT | `/admin/setmeal` | 修改 |
| POST | `/admin/setmeal/status/{status}?id=` | 起售/停售 |

SetmealDTO：`id`、`categoryId`、`name`、`price`、`status`、`description`、`image`、`setmealDishes`: List&lt;SetmealDish&gt;（`dishId`、`name`、`price`、`copies`）

### 4.5 店铺 / 文件上传

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/admin/shop/{status}` | 设置营业状态（1/0） |
| GET | `/admin/shop/status` | 查询营业状态 |
| POST | `/admin/common/upload` | 上传图片（form-data 字段名 `file`）→ 返回 OSS URL 字符串 |

### 4.6 订单 `/admin/order`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/order/conditionSearch` | 条件搜索分页（OrdersPageQueryDTO：page/pageSize/number/phone/status/beginTime/endTime，日期格式 `yyyy-MM-dd HH:mm:ss`） |
| GET | `/admin/order/details/{id}` | 订单详情（OrderVO） |
| GET | `/admin/order/statistics` | 各状态订单数量统计 |
| PUT | `/admin/order/confirm` | 接单（body: `id`） |
| PUT | `/admin/order/rejection` | 拒单（body: `id` + `rejectionReason`，**必填原因**） |
| PUT | `/admin/order/delivery/{id}` | 派送 |
| PUT | `/admin/order/complete/{id}` | 完成 |
| PUT | `/admin/order/cancel` | 商家取消（body: `id` + `cancelReason`） |
| GET | `/admin/order/{id}/actions` | **可执行动作列表（新，状态机驱动）** |

OrderStatisticsVO：`toBeConfirmed`(待接单)、`confirmed`(待派送)、`deliveryInProgress`(派送中)

### 4.7 数据统计 `/admin/report`（日期参数格式均为 `yyyy-MM-dd`，query：begin/end）

| 方法 | 路径 | 返回 |
|---|---|---|
| GET | `/admin/report/turnoverStatistics?begin=&end=` | TurnoverReportVO：`dateList`/`turnoverList`（**逗号分隔字符串**，如 `"2026-09-01,2026-09-02"`） |
| GET | `/admin/report/userStatistics` | UserReportVO：`dateList`/`totalUserList`/`newUserList`（同上逗号串） |
| GET | `/admin/report/ordersStatistics` | OrderReportVO：`dateList`/`orderCountList`/`validOrderCountList`/`totalOrderCount`/`validOrderCount`/`orderCompletionRate`（百分数） |
| GET | `/admin/report/top10` | SalesTop10ReportVO：`nameList`/`numberList`（逗号串） |
| GET | `/admin/report/export` | **Excel 二进制流**（固定近 30 天，无 Result 包装，前端用 blob 接收） |

### 4.8 工作台 `/admin/workspace`

| 方法 | 路径 | 返回 |
|---|---|---|
| GET | `/admin/workspace/businessData` | BusinessDataVO：`turnover`/`validOrderCount`/`orderCompletionRate`/`unitPrice`/`newUsers`（今日实时） |
| GET | `/admin/workspace/overviewOrders` | OrderOverViewVO：`waitingOrders`/`deliveredOrders`/`completedOrders`/`cancelledOrders`/`allOrders` |
| GET | `/admin/workspace/overviewDishes` | DishOverViewVO：`sold`/`discontinued` |
| GET | `/admin/workspace/overviewSetmeals` | SetmealOverViewVO：`sold`/`discontinued` |

### 4.9 骑手管理 `/admin/rider`（新）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/rider` | 新增（RiderDTO：`name`/`phone`/`status`） |
| PUT | `/admin/rider` | 修改 |
| GET | `/admin/rider/{id}` | 详情 |
| GET | `/admin/rider/page?status=&page=1&pageSize=10` | 分页 |
| POST | `/admin/rider/status/{status}?id=` | 改状态（0离线 1空闲 2配送中） |
| POST | `/admin/rider/location?riderId=&lng=&lat=` | 上报骑手位置（模拟配送用） |

### 4.10 配送单 `/admin/delivery`（新）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/delivery/page` | 分页（DeliveryQueryDTO：`orderId`/`riderId`/`status`/`page`/`pageSize`） |
| GET | `/admin/delivery/{id}` | 详情 |
| POST | `/admin/delivery/{id}/assign?riderId=` | 手动分配骑手 |
| POST | `/admin/delivery/{id}/status/{status}` | 更新配送状态（0待分配1已分配2取餐中3配送中4已完成5异常） |

注意：配送单**没有"创建"接口**，`createDelivery`/`autoAssignRider` 尚未接入下单主流程（见第 7.4 节）。

### 4.11 AI 数据助手 `/admin/ai`（新）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/ai/ask` | body `{"question": "..."}` → 返回 String（自然语言数据分析结论） |

---

## 5. WebSocket 协议

- **URL**：`ws://localhost:8080/ws/{sid}`（sid 自定义标识，如管理员 id；公开无鉴权）
- **方向**：仅服务端 → 客户端**群发**（不区分 sid，所有连接都会收到）；客户端发的消息服务端只打日志不处理
- **消息格式**（JSON 文本）：

```json
{ "type": 1, "orderId": 123, "content": "订单号：1761139200000" }
```

| type | 含义 | 触发时机 |
|---|---|---|
| `1` | 来单提醒 | 用户支付成功（模拟支付/微信回调） |
| `2` | 催单提醒 | 用户点催单 |
| `3` | 配送通知 | 骑手接单（content 如"骑手【王五】已接单，正在为您配送"） |

**主要消费方是管理端**（来单提醒/催单弹窗+语音播报）。小程序端如需也可连同一个 endpoint 接收。**配送实时位置不走 WebSocket**，用 `GET /user/delivery/{orderId}` HTTP 轮询。

---

## 6. 订单状态机（前端按钮渲染依据）

### 状态（Orders.status）

| 值 | 含义 |
|---|---|
| 1 | 待付款（PENDING_PAYMENT） |
| 2 | 待接单（TO_BE_CONFIRMED） |
| 3 | 已接单（CONFIRMED） |
| 4 | 派送中（DELIVERY_IN_PROGRESS） |
| 5 | 已完成（COMPLETED，终态） |
| 6 | 已取消（CANCELLED，终态） |

支付状态：`0` 未支付 / `1` 已支付 / `2` 退款

### 转移规则

```
1 待付款 ──PAY──→ 2 待接单 ──CONFIRM──→ 3 已接单 ──DELIVER──→ 4 派送中 ──COMPLETE──→ 5 已完成
   │                │──REJECT──→ 6 已取消                                │
   │                └──CANCEL───→ 6 已取消                     (超时自动→5)
   ├──CANCEL──→ 6 已取消
   └──(15分钟超时自动→6)
```

### 动作接口（动态渲染按钮，推荐用法）

- 用户端：`GET /user/order/{id}/actions` → 只会返回 `PAY`（支付）/`CANCEL`（取消订单，needReason=true）
- 管理端：`GET /admin/order/{id}/actions` → 只会返回 `CONFIRM`/`REJECT`（needReason=true）/`DELIVER`/`COMPLETE`
- 返回 `ActionDetailVO{action, label, needReason}`，前端按 label 显示按钮、needReason=true 时弹原因输入框

---

## 7. 智能功能对接要点

### 7.1 猜你喜欢 `GET /user/recommend`
返回 `List<DishVO>`（含口味，已过滤停售），无推荐时空数组。首页"猜你喜欢"区块直接渲染。

### 7.2 AI 客服 `POST /user/chat`
- body：`{"message": "..."}`；响应：`Result<String>` 纯文本
- 会话历史存 Redis（30 分钟 TTL，最多 20 条），前端聊天页刷新后可继续上下文
- 后端自动注入最近 3 笔订单 + 店铺状态作为上下文
- **依赖 `LLM_API_KEY` 环境变量**（OpenAI 兼容协议，默认模型 gpt-4o-mini），未配置时调用会失败
- 同步阻塞调用（LLM 超时 30s），**前端 axios/uni.request 超时要设 >30s**，或加 loading 动画

### 7.3 数据助手（管理端）`POST /admin/ai/ask`
body `{"question": "..."}` → String 分析结论。LLM 生成只读 SQL（表白名单 7 张）→ 执行 → LLM 总结。同样依赖 LLM_API_KEY。

### 7.4 配送追踪 `GET /user/delivery/{orderId}`
- 返回 DeliveryVO（含骑手姓名/电话/实时经纬度），**HTTP 轮询**获取位置（建议 3~5s 间隔）
- 无配送单时 `data=null`，前端显示"暂无配送信息"
- ⚠️ 已知现状：`createDelivery`/`autoAssignRider` **未接入下单主流程**，配送单需手工造数（`schema_delivery.sql` 的 rider/delivery 表）或后续补逻辑。前端做配送页时注意这个数据可能为空

---

## 8. 环境与启动

| 依赖 | 配置 |
|---|---|
| MySQL | `localhost:3306/sky_take_out`（root / XS971818） |
| Redis | `localhost:6379`，密码 XS971818，database 0 |
| LLM（可选） | 环境变量 `LLM_API_KEY`（必需）、`LLM_BASE_URL`（默认 https://api.openai.com/v1/chat/completions）、`LLM_MODEL`（默认 gpt-4o-mini） |
| 后端启动 | IDE 运行 `sky-server` 的 `SkyApplication`；或 `mvn spring-boot:run -pl sky-server`。构建需 `-Dlombok=1.18.46`（JDK 高版本兼容） |
| 管理端启动 | `cd frontend/sky-admin-web && npm i && npm run dev`（端口 5173，已配 /api 代理） |

数据库表：employee、category、dish、dish_flavor、setmeal、setmeal_dish、user、address_book、shopping_cart、orders、order_detail、rider、delivery（后两张见 `sky-server/src/main/resources/schema_delivery.sql`，**无全量建库 SQL 文件**）

---

## 9. 已知坑位清单（前端对接时注意）

1. **日期不带秒**：全局 `yyyy-MM-dd HH:mm`。下单传 `estimatedDeliveryTime` 例外，要 `yyyy-MM-dd HH:mm:ss`
2. **减购接口是 query 参数**：`POST /user/shoppingCart/sub?dishId=&dishFlavor=`，不是 JSON body
3. **删除地址 id 是 query**：`DELETE /user/addressBook?id=`
4. **菜品起售停售**：`POST /admin/dish/status/{status}?status=&id=`，status 从 query 取，路径变量是摆设
5. **报表接口返回逗号分隔字符串**：`dateList: "2026-09-01,2026-09-02"`，前端要 `split(',')` 再画图
6. **401 无响应体**：拦截器只能按 HTTP 状态码判断，读不到 JSON
7. **无 CORS**：H5 开发必须走代理；小程序真机需把 baseURL 换成局域网 IP
8. **口味 value 是 JSON 字符串**：`"[\"热饮\",\"冷饮\"]"`，需 `JSON.parse` 再渲染 FlavorPicker
9. **模拟支付**：`PUT /user/order/payment` 直接成功，无需真实调起微信支付；OrderPaymentVO 字段为空是正常的
10. **金额服务端重算**：下单 DTO 的 `amount` 传了也被忽略
11. **OSS 上传可能失败**：`application-dev.yml` 中阿里云 key 键名写作 `access-key- id`（多了空格，疑似笔误导致 accessKeyId 绑定为 null）——上传报错时先查这里
12. **EmployeeLoginVO 字段是 `userName`**（不是 username）
13. **配送数据可能为空**：配送单创建未接入主流程（见 7.4）
14. **敏感信息明文**：数据库/Redis 密码、OSS AccessKey、微信 secret、百度 AK 均明文在 `application-dev.yml`，注意不要外泄/公网暴露仓库

---

## 10. 重建小程序建议（页面 → 接口映射）

| 页面 | 调用接口 |
|---|---|
| login | `POST /user/user/login`（wx.login 拿 code） |
| index 首页 | `GET /user/shop/status`、`/user/category/list`、`/user/dish/list`、`/user/setmeal/list`、`/user/recommend`（猜你喜欢区块） |
| dish-detail | `GET /user/dish/list` 内的 DishVO（flavors 渲染 FlavorPicker）→ `POST /user/shoppingCart/add` |
| cart | `GET /user/shoppingCart/list`、`POST .../sub`、`DELETE .../clean` |
| checkout | `GET /user/addressBook/default`、`POST /user/order/submit` → `PUT /user/order/payment` |
| address / address-edit | 地址簿 7 个接口 |
| order-list / order-detail | `GET /user/order/historyOrders`（status 筛选）、`/user/order/orderDetail/{id}`、`/user/order/{id}/actions`（渲染按钮）、`PUT .../cancel/{id}`、`POST .../repetition/{id}`、`GET .../reminder/{id}` |
| delivery | `GET /user/delivery/{orderId}`（轮询） |
| chat | `POST /user/chat` |
| profile | 本地存储的 UserLoginVO + 清 token |
