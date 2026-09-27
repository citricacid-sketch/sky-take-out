// pages/profile/profile.js
// Profile page. V1: local-only nickname/avatar (no backend persistence).

const auth = require('../../services/auth.js');

Page({
  data: {
    isLoggedIn: false,
    nickname: '点击登录',
    avatarText: '登',
    loginHint: '登录后享受完整服务',
  },

  onShow() {
    this.refresh();
  },

  refresh() {
    const loggedIn = auth.isLoggedIn();
    this.setData({
      isLoggedIn: loggedIn,
      nickname: loggedIn ? '微信用户' : '点击登录',
      avatarText: loggedIn ? '微' : '登',
      loginHint: loggedIn ? '已登录' : '登录后享受完整服务',
    });
  },

  onLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  onOrders() {
    wx.navigateTo({ url: '/pages/order-list/order-list' });
  },

  onOrderStatus(e) {
    const status = e.currentTarget.dataset.status;
    wx.navigateTo({ url: `/pages/order-list/order-list?status=${status}` });
  },

  onAddress() {
    wx.navigateTo({ url: '/pages/address/address' });
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmColor: '#DC2626',
      success: (res) => {
        if (res.confirm) {
          auth.logout();
          this.refresh();
          wx.showToast({ title: '已退出登录', icon: 'success' });
        }
      },
    });
  },

  onAbout() {
    wx.showToast({ title: '鲜味小厨 demo', icon: 'none' });
  },
});
