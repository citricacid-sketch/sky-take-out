// services/cart.js
// 小程序购物车全局状态管理模块（单一数据源）
// 职责：
// - 作为整个小程序购物车的唯一真实数据源（Single Source of Truth）
// - 提供内存缓存的购物车列表，数据从后端同步
// - 实现发布-订阅模式，页面可订阅购物车变更事件自动刷新 UI
// - 同步更新 TabBar 购物车角标（第 3 项，index=2）
//
// 后端购物车接口约定：
// - 购物车行项通过 (userId, dishId, dishFlavor) 三元组去重
// - dishFlavor 必须是稳定字符串，使用 JSON.stringify(selectedValues)
//   例如：'["热饮","少糖"]'
// - add() 接口每次调用数量 +1，因此添加 N 个商品需调用 N 次
//
// TabBar 结构（角标位置）：
// - index 0: 首页（home）
// - index 1: 菜单（menu）
// - index 2: 购物车（cart）← 角标更新位置
// - index 3: 我的（profile）

const http = require('./request.js');

// ---------------------------------------------------------------------------
// 内部状态
// ---------------------------------------------------------------------------

/** 购物车列表内存缓存（后端 DishVO 数组） */
let cartList = [];

/** 订阅者回调函数列表（发布-订阅模式） */
let listeners = [];

// ---------------------------------------------------------------------------
// 发布-订阅机制
// ---------------------------------------------------------------------------

/**
 * 通知所有订阅者购物车数据已变更
 * 每次购物车数据变化时调用，触发各页面 UI 更新
 * 同时同步更新 TabBar 角标
 */
function emit() {
  // 遍历所有订阅者并执行回调，传入最新购物车列表
  listeners.forEach((fn) => {
    try {
      fn(cartList);
    } catch (e) {
      // 隔离错误：某个订阅者回调异常不应影响其他订阅者执行
      // 防止一个页面报错导致整个购物车通知链断裂
    }
  });
  // 同步更新 TabBar 购物车角标
  updateTabBadge();
}

// ---------------------------------------------------------------------------
// 计算属性（派生状态）
// ---------------------------------------------------------------------------

/**
 * 计算购物车商品总数量（所有行项的 number 之和）
 * @returns {number} 商品总件数
 */
function getCount() {
  return cartList.reduce((sum, item) => sum + (item.number || 0), 0);
}

/**
 * 计算购物车商品总金额（每行 price * qty 之和）
 * @returns {number} 商品总金额（元）
 */
function getTotal() {
  return cartList.reduce((sum, item) => {
    const price = Number(item.amount) || 0; // 单价
    const qty = item.number || 0;           // 数量
    return sum + price * qty;
  }, 0);
}

// ---------------------------------------------------------------------------
// TabBar 角标管理
// ---------------------------------------------------------------------------

/**
 * 更新 TabBar 购物车角标显示
 * - 有商品时显示数量角标
 * - 无商品时移除角标
 *
 * 注意：setTabBarBadge 需要在 TabBar 页面已存在时调用
 * 在小程序启动早期调用可能失败（TabBar 未渲染），通过 try/catch 安全忽略
 */
function updateTabBadge() {
  const count = getCount();
  try {
    if (count > 0) {
      // 显示角标，text 必须为字符串
      wx.setTabBarBadge({ index: 2, text: String(count) }).catch(() => {});
    } else {
      // 移除角标
      wx.removeTabBarBadge({ index: 2 }).catch(() => {});
    }
  } catch (e) {
    // setTabBarBadge 需要 TabBar 已存在；安全忽略早期调用异常
  }
}

// ---------------------------------------------------------------------------
// 后端同步操作
// ---------------------------------------------------------------------------

/**
 * 从后端刷新购物车列表
 * 调用 GET /user/shoppingCart/list 获取最新数据
 * 更新内存缓存后触发发布-订阅通知
 *
 * @returns {Promise<Array>} 最新购物车列表
 */
async function refresh() {
  const list = (await http.get('/user/shoppingCart/list', { silent: true })) || [];
  cartList = list;
  emit(); // 通知所有订阅者
  return cartList;
}

/**
 * 添加商品到购物车（数量 +1）
 * 调用 POST /user/shoppingCart/add
 *
 * @param {number} dishId - 菜品 ID
 * @param {string} [dishFlavor=''] - 口味规格字符串（JSON 格式，如 '["热饮"]'）
 * @returns {Promise<void>}
 */
async function add(dishId, dishFlavor = '') {
  await http.post('/user/shoppingCart/add', { dishId, dishFlavor });
  await refresh(); // 操作后同步最新数据
}

/**
 * 从购物车减少商品（数量 -1）
 * 调用 POST /user/shoppingCart/sub
 *
 * @param {number} dishId - 菜品 ID
 * @param {string} [dishFlavor=''] - 口味规格字符串
 * @returns {Promise<void>}
 */
async function sub(dishId, dishFlavor = '') {
  await http.post('/user/shoppingCart/sub', { dishId, dishFlavor });
  await refresh();
}

/**
 * 清空购物车
 * 调用 DELETE /user/shoppingCart/clean
 * @returns {Promise<void>}
 */
async function clean() {
  await http.delete('/user/shoppingCart/clean');
  await refresh();
}

// ---------------------------------------------------------------------------
// 订阅接口
// ---------------------------------------------------------------------------

/**
 * 订阅购物车变更事件
 * 当购物车数据变化时（增/减/清空/刷新），回调函数会被调用
 *
 * @param {function} fn - 回调函数，接收 cartList 数组作为参数
 *   fn(cartList) => void
 * @returns {function} 取消订阅函数，调用后移除该订阅者
 *
 * @example
 * // 页面 onLoad 中订阅
 * const unsubscribe = cart.subscribe((list) => {
 *   this.setData({ cartCount: list.length });
 * });
 * // 页面 onUnload 中取消订阅，防止内存泄漏
 * unsubscribe();
 */
function subscribe(fn) {
  if (typeof fn !== 'function') return () => {};
  listeners.push(fn);
  // 返回取消订阅函数（闭包引用当前 fn）
  return () => {
    listeners = listeners.filter((l) => l !== fn);
  };
}

// 导出接口
module.exports = {
  refresh,         // 刷新购物车（从后端同步）
  add,             // 添加商品（+1）
  sub,             // 减少商品（-1）
  clean,           // 清空购物车
  subscribe,       // 订阅变更事件
  getCount,        // 获取商品总件数
  getTotal,        // 获取商品总金额
  updateTabBadge,  // 更新 TabBar 角标
  // 原始列表读取——页面通常使用 subscribe，此方法用于一次性读取场景
  getList: () => cartList,
};
