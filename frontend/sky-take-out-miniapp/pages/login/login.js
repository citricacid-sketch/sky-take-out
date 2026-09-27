// pages/login/login.js
const auth = require('../../services/auth.js');

Page({
  data: {
    loading: false,
    isLoggedIn: false,
  },

  onShow() {
    this.setData({ isLoggedIn: auth.isLoggedIn() });
  },

  onWxLogin() {
    if (this.data.loading || this.data.isLoggedIn) return;
    this.setData({ loading: true });
    auth.login(false).then(() => {
      this.setData({ loading: false, isLoggedIn: true });
      wx.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/profile/profile' }) });
      }, 800);
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  onLogout() {
    auth.logout();
    this.setData({ isLoggedIn: false });
    wx.showToast({ title: '已退出登录', icon: 'success' });
  },

  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/index/index' }) });
  },
});
