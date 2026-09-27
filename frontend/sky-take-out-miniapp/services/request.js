// services/request.js
// Unified wrapper around wx.request.
// - Single baseURL from config
// - Automatic `authentication` header injection
// - Business error (code !== 1) normalization
// - 401 handling: clear token and force redirect to login page
// - Retry on network failure (configurable)
// - Timeout & offline detection

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

function getStoredToken() {
  // Prefer module-level cache (set by auth.login via setToken) to avoid
  // storage/globalData cross-VM issues in the mini program runtime.
  if (currentToken) return currentToken;
  return wx.getStorageSync('token') || '';
}

/**
 * 核心请求函数。
 * @param {object} options
 * @param {number} options.retries 网络失败重试次数（默认 1）
 * @param {number} options.timeout 超时毫秒（默认 15000）
 * @param {boolean} options.silent 是否抑制 toast
 */
function request(options) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    silent,
    token: explicitToken,
    retries = 1,
    timeout = 15000,
  } = options;

  const token = explicitToken || getStoredToken() || '';

  const isAbsolute = /^https?:\/\//i.test(url);
  let fullUrl = isAbsolute ? url : config.baseUrl + (url.startsWith('/') ? url : '/' + url);
  // Always also pass token as query parameter (simulator-safe fallback)
  if (token) {
    const sep = fullUrl.includes('?') ? '&' : '?';
    fullUrl = `${fullUrl}${sep}token=${encodeURIComponent(token)}`;
  }

  return new Promise((resolve, reject) => {
    let attempts = 0;

    function doRequest() {
      attempts += 1;

      const requestTask = wx.request({
        url: fullUrl,
        method,
        data,
        header: {
          'content-type': 'application/json',
          ...(token ? { authentication: token } : {}),
          ...header,
        },
        timeout,
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
              if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
              reject({ type: 'business', message, body });
            }
          } else {
            const message = `服务繁忙 (${statusCode})`;
            if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
            reject({ type: 'http', statusCode, message });
          }
        },
        fail(err) {
          const isNetworkError = !err.errMsg || err.errMsg.includes('timeout') || err.errMsg.includes('connect');
          const canRetry = isNetworkError && attempts <= retries;

          if (canRetry) {
            // 指数退避重试：500ms, 1000ms, 2000ms...
            const delay = Math.min(500 * Math.pow(2, attempts - 1), 3000);
            if (!silent) wx.showLoading({ title: `重试中 (${attempts}/${retries + 1})...`, mask: false });
            setTimeout(() => {
              wx.hideLoading();
              doRequest();
            }, delay);
            return;
          }

          // 最终失败
          const message = isNetworkError ? '网络不稳定，请检查网络后重试' : '请求失败';
          if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
          reject({ type: 'network', message, err, attempts });
        },
      });

      // 超时处理（wx.request timeout 在某些基础库版本不生效，做兜底）
      if (timeout > 0) {
        setTimeout(() => {
          if (requestTask && requestTask.abort) {
            requestTask.abort();
          }
        }, timeout + 2000);
      }
    }

    doRequest();
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
