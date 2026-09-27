// services/request.js
// Unified wrapper around wx.request.
// - Single baseURL from config
// - Automatic `authentication` header injection
// - Business error (code !== 1) normalization
// - 401 handling: clear token and force redirect to login page

const config = require('../config/index.js');

// Module-level token cache. Auth layer sets this directly after login,
// bypassing any storage/globalData cross-VM issues in the mini program runtime.
let currentToken = '';

function setRequestToken(token) {
  currentToken = token || '';
}

function clearRequestToken() {
  currentToken = '';
}

function request(options) {
  const { url, method = 'GET', data, header = {}, silent, token: explicitToken } = options;
  // Explicit token (passed from 401 retry) takes precedence over stored token.
  // `silent` only controls toast suppression — it must NOT strip the token,
  // otherwise authenticated requests (e.g. cart/category) would always 401.
  const token = explicitToken || getStoredToken() || '';

  const isAbsolute = /^https?:\/\//i.test(url);
  let fullUrl = isAbsolute ? url : config.baseUrl + (url.startsWith('/') ? url : '/' + url);
  // Always also pass token as query parameter (simulator-safe fallback)
  if (token) {
    const sep = fullUrl.includes('?') ? '&' : '?';
    fullUrl = `${fullUrl}${sep}token=${encodeURIComponent(token)}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl,
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...(token ? { authentication: token } : {}),
        ...header,
      },
      success(res) {
        const { statusCode, data: body } = res;

        // Backend returns HTTP 401 with an empty body when JWT is missing/invalid.
        if (statusCode === config.UNAUTHORIZED_STATUS) {
          handleUnauthorized().then(
            () => reject({ type: 'unauthorized' }),
            () => reject({ type: 'unauthorized' })
          );
          return;
        }

        if (statusCode >= 200 && statusCode < 300) {
          if (body && body.code === config.SUCCESS_CODE) {
            resolve(body.data);
          } else {
            // Business failure: code 0 or unexpected shape.
            const message = (body && body.msg) || '请求失败';
            if (!silent) wx.showToast({ title: message, icon: 'none' });
            reject({ type: 'business', message, body });
          }
        } else {
          const message = `网络错误 (${statusCode})`;
          if (!silent) wx.showToast({ title: message, icon: 'none' });
          reject({ type: 'http', statusCode, message });
        }
      },
      fail(err) {
        const message = '网络异常，请检查网络';
        if (!silent) wx.showToast({ title: message, icon: 'none' });
        reject({ type: 'network', message, err });
      },
    });
  });
}

// 401 处理：token 失效，清除登录态并强制跳转登录页，要求重新登录
function handleUnauthorized() {
  return new Promise((resolve) => {
    const auth = require('./auth.js');
    auth.logout(); // 清除本地 token

    wx.showToast({ title: '登录已过期，请重新登录', icon: 'none', duration: 1500 });

    // 跳转登录页。用 redirectTo 而非 reLaunch：
    // - reLaunch 关闭所有页面，冷启动/webview 未就绪时可能触发 "routeDone webviewId not found"
    // - redirectTo 只在当前栈顶跳转，对 webview 状态无依赖
    setTimeout(() => {
      wx.redirectTo({ url: '/pages/login/login' });
    }, 1500);

    resolve();
  });
}

function getStoredToken() {
  // Prefer module-level cache (set by auth.login via setToken) to avoid
  // storage/globalData cross-VM issues in the mini program runtime.
  if (currentToken) return currentToken;
  return wx.getStorageSync('token') || '';
}

// Convenience helpers
const http = {
  get(url, opts = {}) {
    return request({ url, method: 'GET', ...opts });
  },
  post(url, data, opts = {}) {
    return request({ url, method: 'POST', data, ...opts });
  },
  put(url, data, opts = {}) {
    return request({ url, method: 'PUT', data, ...opts });
  },
  delete(url, opts = {}) {
    return request({ url, method: 'DELETE', ...opts });
  },
};

module.exports = { request, setRequestToken, clearRequestToken, ...http };
