// pages/cart/cart.js
// Real shopping cart page. Subscribes to the cart store so it re-renders on any change
// (add/sub/clean from any page). +/- per line, clear all, total, checkout CTA.

const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

Page({
  data: {
    items: [],
    totalText: '0.00',
    totalAmount: 0,
    count: 0,
    checkoutEnabled: false,
  },

  onLoad() {
    this.unsubscribe = cart.subscribe((list) => this.render(list));
    cart.refresh();
  },

  onShow() {
    // Ensure fresh data when returning to the cart tab.
    cart.refresh();
  },

  onUnload() {
    if (this.unsubscribe) this.unsubscribe();
  },

  render(list) {
    const items = (list || []).map((it) => {
      let image = it.image || '';
      if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);
      return {
        ...it,
        image,
        hasImage: !!image,
        // Compact card shows dishFlavor (a JSON array string) as flavor text.
        flavorText: this.formatFlavor(it.dishFlavor),
        subtotalText: format.formatPrice((Number(it.amount) || 0) * (it.number || 0)),
      };
    });
    const total = items.reduce(
      (sum, it) => sum + (Number(it.amount) || 0) * (it.number || 0),
      0
    );
    const count = items.reduce((sum, it) => sum + (it.number || 0), 0);
    this.setData({
      items,
      totalText: format.formatPrice(total),
      totalAmount: total,
      count,
      checkoutEnabled: items.length > 0,
    });
  },

  // dishFlavor is stored as a JSON string of selected values, e.g. '["热饮","少糖"]'.
  formatFlavor(dishFlavor) {
    if (!dishFlavor) return '';
    try {
      const parsed = JSON.parse(dishFlavor);
      if (Array.isArray(parsed)) return parsed.join('、');
      return String(parsed);
    } catch (e) {
      return String(dishFlavor);
    }
  },

  onIncrease(e) {
    const { dishId, dishFlavor } = e.currentTarget.dataset;
    cart.add(dishId, dishFlavor || '');
  },

  onDecrease(e) {
    const { dishId, dishFlavor } = e.currentTarget.dataset;
    cart.sub(dishId, dishFlavor || '');
  },

  onClean() {
    if (!this.data.items.length) return;
    wx.showModal({
      title: '清空购物车',
      content: '确定要清空全部商品吗？',
      confirmColor: '#D97706',
      success: (res) => {
        if (res.confirm) cart.clean();
      },
    });
  },

  onCheckout() {
    if (!this.data.checkoutEnabled) return;
    wx.navigateTo({ url: '/pages/checkout/checkout' });
  },

  onGoMenu() {
    wx.switchTab({ url: '/pages/menu/menu' });
  },
});
