/**
 * scroll-to-top 返回顶部组件
 *
 * 功能：当页面滚动超过阈值时，显示一个悬浮按钮，点击后平滑滚动回顶部。
 * 特性：
 *   - 滚动超过 600rpx 时显示（带弹跳进入动画）
 *   - 点击平滑滚动回顶部
 *   - 滚动到底部前隐藏（带淡出动画）
 *
 * 使用方式：
 *   <scroll-to-top bind:scroll="onPageScroll" />
 */

Component({
  properties: {
    /** 滚动阈值（rpx），超过此值显示按钮，默认 600 */
    threshold: { type: Number, value: 600 },
  },
  data: {
    visible: false,
    scrolling: false, // 是否正在滚动中（避免重复触发）
  },
  methods: {
    /** 页面滚动事件处理（由父页面传入） */
    onPageScroll(e) {
      const { scrollTop } = e.detail || e;
      const shouldShow = scrollTop > this.data.threshold;
      if (shouldShow !== this.data.visible) {
        this.setData({ visible: shouldShow });
      }
    },

    /** 点击返回顶部 */
    onTap() {
      if (this.data.scrolling) return;
      this.setData({ scrolling: true });
      wx.pageScrollTo({
        scrollTop: 0,
        duration: 300,
        success: () => {
          setTimeout(() => this.setData({ scrolling: false }), 300);
        },
        fail: () => {
          this.setData({ scrolling: false });
        },
      });
    },
  },
});
