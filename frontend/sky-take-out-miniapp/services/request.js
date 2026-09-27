// services/request.js
// 小程序网络请求统一封装层（核心基础设施模块）
// 基于 wx.request 封装，提供以下能力：
// - 统一 baseURL（从配置文件读取）
// - 自动注入 authentication 请求头（JWT Token）
// - 业务错误标准化处理（code !== 1 视为业务失败）
// - 401 未授权处理：清除本地 token 并强制跳转登录页
// - 网络失败自动重试（指数退避策略，可配置次数）
// - 请求超时与离线检测
//
// 使用方式：
//   const { request, get, post } = require('./request.js');
//   const data = await get('/user/dish/list');
//   const result = await post('/user/shoppingCart/add', { dishId: 1 });

const config = require('../config/index.js');

// ---------------------------------------------------------------------------
// 模块级 Token 缓存
// ---------------------------------------------------------------------------
// 使用模块级变量缓存 token，而非每次从 storage 读取。
// 原因：小程序多页面/组件可能运行在不同 VM 中，storage/globalData 存在跨 VM 同步问题。
// auth.js 登录成功后通过 setRequestToken() 直接写入此缓存，确保请求层始终拿到最新 token。
let currentToken = '';

/**
 * 设置模块级 token 缓存（由 auth.login 调用）
 * @param {string} token - 后端返回的 JWT token
 */
function setRequestToken(token) {
  currentToken = token || '';
}

/**
 * 清除模块级 token 缓存（由 auth.logout 调用）
 */
function clearRequestToken() {
  currentToken = '';
}

/**
 * 获取当前有效 token
 * 优先级：模块级缓存 > 本地 storage > 空字符串
 * @returns {string}
 */
function getStoredToken() {
  // 优先使用模块级缓存（由 auth.login 通过 setRequestToken 设置）
  // 避免小程序运行时中 storage/globalData 跨 VM 读取不一致的问题
  if (currentToken) return currentToken;
  return wx.getStorageSync('token') || '';
}

// ---------------------------------------------------------------------------
// 核心请求函数
// ---------------------------------------------------------------------------
/**
 * 核心请求函数，封装 wx.request 为 Promise 接口
 *
 * @param {object} options - 请求配置项
 * @param {string} options.url - 请求地址（相对路径自动拼接 baseURL，绝对路径原样使用）
 * @param {string} [options.method='GET'] - HTTP 方法：GET / POST / PUT / DELETE
 * @param {object} [options.data] - 请求体数据（JSON 格式）
 * @param {object} [options.header={}] - 自定义请求头（会合并到默认 header 上）
 * @param {boolean} [options.silent] - 为 true 时不弹出 toast 错误提示（用于静默请求场景）
 * @param {string} [options.token] - 显式指定本次请求的 token（不传则自动取缓存）
 * @param {number} [options.retries=1] - 网络失败时的重试次数（不含首次请求）
 * @param {number} [options.timeout=15000] - 超时时间（毫秒），默认 15 秒
 * @returns {Promise<any>} 成功时 resolve 后端返回的 data 字段，失败时 reject 错误对象
 *
 * 错误对象格式：
 * - 业务错误：{ type: 'business', message: string, body: object }
 * - HTTP 错误：{ type: 'http', statusCode: number, message: string }
 * - 网络错误：{ type: 'network', message: string, err: object, attempts: number }
 * - 未授权：  { type: 'unauthorized' }
 */
function request(options) {
  // 解构请求参数并设置默认值
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

  // 解析本次请求使用的 token：显式传入 > 模块级缓存 > storage
  const token = explicitToken || getStoredToken() || '';

  // 判断是否为绝对 URL（http:// 或 https://）
  const isAbsolute = /^https?:\/\//i.test(url);
  // 拼接完整 URL：相对路径前面补 baseURL，确保以 / 开头
  let fullUrl = isAbsolute ? url : config.baseUrl + (url.startsWith('/') ? url : '/' + url);

  // 兼容方案：同时通过 query 参数传递 token（模拟器/部分旧版本 webview 的 header 可能被拦截）
  if (token) {
    const sep = fullUrl.includes('?') ? '&' : '?';
    fullUrl = `${fullUrl}${sep}token=${encodeURIComponent(token)}`;
  }

  return new Promise((resolve, reject) => {
    let attempts = 0; // 当前尝试次数计数器

    /**
         * 执行单次请求（内部函数，支持递归重试）
         */
    function doRequest() {
      attempts += 1;

      // 发起 wx.request 请求
      const requestTask = wx.request({
        url: fullUrl,
        method,
        data,
        header: {
          'content-type': 'application/json', // 默认 JSON 格式
          // 有 token 时注入 authentication 请求头（后端 JWT 校验字段）
          ...(token ? { authentication: token } : {}),
          // 自定义 header 覆盖默认值
          ...header,
        },
        timeout,
        /**
                 * 请求成功的回调（HTTP 层面成功，业务层面需进一步判断）
                 * @param {object} res - wx.request 返回的响应对象
                 * @param {number} res.statusCode - HTTP 状态码
                 * @param {object} res.data - 响应体（后端统一返回格式）
                 */
        success(res) {
          const { statusCode, data: body } = res;

          // 后端约定：token 缺失或过期时返回 HTTP 401 + 空 body
          if (statusCode === config.UNAUTHORIZED_STATUS) {
            // 触发 401 处理流程（清除登录态 + 跳转登录页）
            handleUnauthorized().then(
              () => reject({ type: 'unauthorized' }),
              () => reject({ type: 'unauthorized' })
            );
            return;
          }

          // HTTP 2xx 成功状态码
          if (statusCode >= 200 && statusCode < 300) {
            // 后端业务约定：code === 1 表示业务成功
            if (body && body.code === config.SUCCESS_CODE) {
              resolve(body.data); // 只返回 data 字段，调用方无需关心外层包装
            } else {
              // 业务失败：code 为 0 或返回结构异常
              const message = (body && body.msg) || '请求失败';
              // 非静默模式下弹出 toast 提示
              if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
              reject({ type: 'business', message, body });
            }
          } else {
            // HTTP 非 2xx 错误（500 / 502 / 503 等服务器错误）
            const message = `服务繁忙 (${statusCode})`;
            if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
            reject({ type: 'http', statusCode, message });
          }
        },
        /**
                 * 请求失败的回调（网络层面失败：断网、超时、DNS 解析失败等）
                 * @param {object} err - 错误对象
                 * @param {string} err.errMsg - 微信返回的错误信息
                 */
        fail(err) {
          // 判断是否为可重试的网络错误（超时或连接失败）
          const isNetworkError = !err.errMsg || err.errMsg.includes('timeout') || err.errMsg.includes('connect');
          // 满足重试条件：是网络错误 且 当前尝试次数未超过重试上限
          const canRetry = isNetworkError && attempts <= retries;

          if (canRetry) {
            // 指数退避策略：第1次 500ms，第2次 1000ms，第3次 2000ms，最大 3000ms
            const delay = Math.min(500 * Math.pow(2, attempts - 1), 3000);
            // 显示重试 loading 提示
            if (!silent) wx.showLoading({ title: `重试中 (${attempts}/${retries + 1})...`, mask: false });
            setTimeout(() => {
              wx.hideLoading();
              doRequest(); // 递归重试
            }, delay);
            return;
          }

          // 最终失败：所有重试耗尽或非网络错误，弹出 toast 提示
          const message = isNetworkError ? '网络不稳定，请检查网络后重试' : '请求失败';
          if (!silent) wx.showToast({ title: message, icon: 'none', duration: 2000 });
          reject({ type: 'network', message, err, attempts });
        },
      });

      // 超时兜底处理：wx.request 的 timeout 参数在某些基础库版本不生效
      // 额外设置一个定时器，超时后强制中断请求（abort）
      if (timeout > 0) {
        setTimeout(() => {
          if (requestTask && requestTask.abort) {
            requestTask.abort();
          }
        }, timeout + 2000); // 比正常超时多 2 秒，避免误杀
      }
    }

    // 首次发起请求
    doRequest();
  });
}

// ---------------------------------------------------------------------------
// 401 未授权处理
// ---------------------------------------------------------------------------
/**
 * 处理 401 未授权场景：token 失效或未登录
 * 流程：清除本地登录态 → 弹出提示 → 1.5 秒后跳转登录页
 *
 * 路由策略说明：
 * - 使用 redirectTo 而非 reLaunch：
 *   - reLaunch 会关闭所有页面再打开目标页，在冷启动/webview 未就绪时
 *     可能触发 "routeDone webviewId not found" 错误
 *   - redirectTo 只在当前页面栈顶替换，对 webview 状态无依赖，更稳定
 *
 * @returns {Promise<void>}
 */
function handleUnauthorized() {
  return new Promise((resolve) => {
    const auth = require('./auth.js');
    auth.logout(); // 清除本地 token 和模块级缓存

    wx.showToast({ title: '登录已过期，请重新登录', icon: 'none', duration: 1500 });

    // 延迟跳转登录页，等 toast 显示完毕
    setTimeout(() => {
      wx.redirectTo({ url: '/pages/login/login' });
    }, 1500);

    resolve();
  });
}

// ---------------------------------------------------------------------------
// 便捷 HTTP 方法封装
// ---------------------------------------------------------------------------
/**
 * 提供语义化的 HTTP 快捷方法
 * 内部统一调用 request()，无需手动指定 method
 *
 * @example
 * http.get('/user/dish/list');
 * http.post('/user/shoppingCart/add', { dishId: 1 });
 * http.put('/user/order', { id: 1, status: 2 });
 * http.delete('/user/shoppingCart/clean');
 */
const http = {
  /**
   * GET 请求
   * @param {string} url - 请求地址
   * @param {object} [opts] - 其他配置项（同 request 的 options）
   * @returns {Promise<any>}
   */
  get(url, opts = {}) {
    return request({ url, method: 'GET', ...opts });
  },
  /**
   * POST 请求
   * @param {string} url - 请求地址
   * @param {object} [data] - 请求体
   * @param {object} [opts] - 其他配置项
   * @returns {Promise<any>}
   */
  post(url, data, opts = {}) {
    return request({ url, method: 'POST', data, ...opts });
  },
  /**
   * PUT 请求
   * @param {string} url - 请求地址
   * @param {object} [data] - 请求体
   * @param {object} [opts] - 其他配置项
   * @returns {Promise<any>}
   */
  put(url, data, opts = {}) {
    return request({ url, method: 'PUT', data, ...opts });
  },
  /**
   * DELETE 请求
   * @param {string} url - 请求地址
   * @param {object} [opts] - 其他配置项
   * @returns {Promise<any>}
   */
  delete(url, opts = {}) {
    return request({ url, method: 'DELETE', ...opts });
  },
};

// 导出接口
module.exports = {
  request,          // 核心请求函数（完整配置）
  setRequestToken,  // 设置模块级 token 缓存
  clearRequestToken,// 清除模块级 token 缓存
  ...http           // get / post / put / delete 便捷方法
};
