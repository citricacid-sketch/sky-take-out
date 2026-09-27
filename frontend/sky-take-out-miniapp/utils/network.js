// utils/network.js
// 网络状态检测与监听。

let currentNetworkType = 'wifi';
let isListening = false;

/**
 * 获取当前网络类型。
 */
function getNetworkType() {
  return currentNetworkType;
}

/**
 * 检查是否离线。
 */
function isOffline() {
  return currentNetworkType === 'none';
}

/**
 * 初始化网络监听。
 */
function initNetworkListener() {
  if (isListening) return;
  isListening = true;

  if (wx.onNetworkStatusChange) {
    wx.onNetworkStatusChange((res) => {
      currentNetworkType = res.networkType || 'unknown';
      if (!res.isConnected) {
        currentNetworkType = 'none';
        wx.showToast({ title: '网络已断开', icon: 'none', duration: 2000 });
      } else if (currentNetworkType !== 'none') {
        wx.showToast({ title: `已切换到 ${currentNetworkType}`, icon: 'none', duration: 1000 });
      }
    });
  }

  // 主动获取一次
  if (wx.getNetworkType) {
    wx.getNetworkType({
      success(res) {
        currentNetworkType = res.networkType || 'unknown';
      },
    });
  }
}

/**
 * 确保网络可用后再执行回调。
 */
function ensureNetworkConnected(callback) {
  if (isOffline()) {
    wx.showModal({
      title: '无网络连接',
      content: '请检查网络设置后重试',
      showCancel: false,
      confirmText: '确定',
    });
    return false;
  }
  if (callback) callback();
  return true;
}

module.exports = {
  getNetworkType,
  isOffline,
  initNetworkListener,
  ensureNetworkConnected,
};
