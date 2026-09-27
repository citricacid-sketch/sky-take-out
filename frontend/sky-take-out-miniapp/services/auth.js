// services/auth.js
// Authentication layer: wx.login -> backend POST /user/user/login -> persist token.
// Token storage key is centralized here ('token'). Do not scatter token keys.

const http = require('./request.js');
const { setRequestToken, clearRequestToken } = require('./request.js');

const TOKEN_KEY = 'token';

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token);
}

function clearToken() {
  wx.setStorageSync(TOKEN_KEY, '');
}

function isLoggedIn() {
  return !!getToken();
}

// Trigger the full login flow.
// `silent` is used internally by request.js to avoid recursive toast spam.
function login(silent = false) {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (!res.code) {
          if (!silent) wx.showToast({ title: '登录失败', icon: 'none' });
          reject(new Error('wx.login no code'));
          return;
        }
        http
          .post('/user/user/login', { code: res.code }, { silent: true })
          .then((data) => {
            // Backend returns UserLoginVO { id, openid, token }
            if (data && data.token) {
              setToken(data.token); // persist to storage (for isLoggedIn checks)
              setRequestToken(data.token); // module-level cache (for API calls)
            }
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
        if (!silent) wx.showToast({ title: '登录失败', icon: 'none' });
        reject(err);
      },
    });
  });
}

function logout() {
  clearToken();
  clearRequestToken();
  const app = getApp();
  if (app && app.globalData) {
    app.globalData.userInfo = null;
  }
}

module.exports = {
  getToken,
  setToken,
  clearToken,
  isLoggedIn,
  login,
  logout,
  TOKEN_KEY,
};
