// services/auth.js
// 小程序用户认证与登录态管理模块
// 职责：
// - 封装 wx.login → 后端登录接口 → token 持久化的完整登录流程
// - 统一管理 token 的读取、写入、清除（token 存储键名集中在此定义）
// - 提供登录态判断接口（isLoggedIn）
//
// 后端登录接口约定：
// - 请求：POST /user/user/login  body: { code: wx.login 返回的临时 code }
// - 响应：{ code: 1, data: { id, openid, token } }
//   - id: 用户 ID
//   - openid: 微信 openid
//   - token: JWT 令牌（后续请求通过 authentication 头携带）

const http = require('./request.js');
const { setRequestToken, clearRequestToken } = require('./request.js');

// Token 存储键名（集中定义，避免散落在各处的魔法字符串）
const TOKEN_KEY = 'token';

// ---------------------------------------------------------------------------
// Token 读写操作（本地持久化层）
// ---------------------------------------------------------------------------

/**
 * 从本地 storage 同步读取 token
 * @returns {string} token 字符串，未登录返回空字符串
 */
function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

/**
 * 将 token 写入本地 storage（持久化，用于 isLoggedIn 判断）
 * @param {string} token - JWT 令牌
 */
function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token);
}

/**
 * 清除本地 storage 中的 token
 */
function clearToken() {
  wx.setStorageSync(TOKEN_KEY, '');
}

// ---------------------------------------------------------------------------
// 登录态判断
// ---------------------------------------------------------------------------

/**
 * 判断当前用户是否已登录
 * 仅检查本地是否存在有效 token，不验证 token 是否过期（由后端校验）
 * @returns {boolean}
 */
function isLoggedIn() {
  return !!getToken();
}

// ---------------------------------------------------------------------------
// 登录流程
// ---------------------------------------------------------------------------

/**
 * 触发完整的微信登录流程
 *
 * 流程：
 * 1. 调用 wx.login 获取临时 code
 * 2. 将 code 发送到后端 /user/user/login 接口
 * 3. 后端校验后返回 UserLoginVO（含 id、openid、token）
 * 4. 同时写入本地 storage 和模块级缓存
 * 5. 更新全局 globalData.userInfo
 *
 * @param {boolean} [silent=false] - 静默模式（不弹出 toast 错误提示）
 *   request.js 在 401 重登录时传入 true，避免递归弹出多个 toast
 * @returns {Promise<{id: number, openid: string, token: string}>} 用户登录信息
 */
function login(silent = false) {
  return new Promise((resolve, reject) => {
    // 第一步：调用微信登录 API 获取临时授权码
    wx.login({
      success(res) {
        // wx.login 成功但未返回 code（极端情况，微信 API 异常）
        if (!res.code) {
          if (!silent) wx.showToast({ title: '登录失败', icon: 'none' });
          reject(new Error('wx.login no code'));
          return;
        }
        // 第二步：将 code 发送到后端换取 JWT token
        http
          .post('/user/user/login', { code: res.code }, { silent: true })
          .then((data) => {
            // 后端返回 UserLoginVO { id, openid, token }
            if (data && data.token) {
              setToken(data.token);       // 写入本地 storage（持久化，用于 isLoggedIn 检查）
              setRequestToken(data.token); // 写入模块级缓存（用于 API 请求时读取，避免跨 VM 问题）
            }
            // 更新全局用户信息（供其他页面直接访问，无需每次解析 token）
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.userInfo = {
                id: data.id,
                openid: data.openid,
              };
            }
            resolve(data);
          })
          .catch(reject);
      },
      fail(err) {
        // wx.login 调用失败（用户拒绝、网络异常等）
        if (!silent) wx.showToast({ title: '登录失败', icon: 'none' });
        reject(err);
      },
    });
  });
}

// ---------------------------------------------------------------------------
// 登出流程
// ---------------------------------------------------------------------------

/**
 * 执行登出操作，清除所有登录态
 * 清除范围：
 * - 本地 storage 中的 token
 * - 模块级 token 缓存
 * - globalData.userInfo
 *
 * 注意：此函数不调用后端登出接口（JWT 无状态，清除本地即可）
 * 也不主动跳转页面，跳转逻辑由调用方决定
 */
function logout() {
  clearToken();         // 清除本地持久化 token
  clearRequestToken();  // 清除模块级缓存 token
  const app = getApp();
  if (app && app.globalData) {
    app.globalData.userInfo = null; // 清除全局用户信息
  }
}

// 导出接口
module.exports = {
  getToken,     // 读取本地 token
  setToken,     // 写入本地 token
  clearToken,   // 清除本地 token
  isLoggedIn,   // 判断登录态
  login,        // 执行登录
  logout,       // 执行登出
  TOKEN_KEY,    // token 存储键名常量（供外部引用）
};
