// pages/order-detail/order-detail.js
// Order detail + dynamic action buttons.
// - Loads the order from /user/order/orderDetail/{id}.
// - Loads allowed actions from /user/order/{id}/actions and renders a button per action.
// - Actions: PAY / CANCEL / CONFIRM / REJECT / DELIVER / COMPLETE.
//   We wire the user-facing ones: PAY (simulated), CANCEL, and REPETITION (再来一单).

const http = require('../../services/request.js');
const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

const STATUS_TEXT = {
  1: '待付款', 2: '待接单', 3: '已接单', 4: '派送中', 5: '已完成', 6: '已取消',
};

Page({
  data: {
    id: null,
    order: null,
    statusText: '',
    items: [],
    actions: [], // [{ action, label, needReason }]
    processing: false,
  },

  onLoad(options) {
    const id = options && options.id ? Number(options.id) : null;
    if (!id) {
      wx.showToast({ title: '订单不存在', icon: 'none' });
      setTimeout(() => wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/profile/profile' }) }), 600);
      return;
    }
    this.setData({ id });
  },

  onShow() {
    if (this.data.id) this.loadAll();
  },

  async loadAll() {
    await Promise.all([this.loadOrder(), this.loadActions()]);
  },

  async loadOrder() {
    try {
      const o = await http.get(`/user/order/orderDetail/${this.data.id}`, { silent: true });
      const items = (o.orderDetailList || []).map((d) => {
        let image = d.image || '';
        if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);
        return {
          ...d,
          image,
          hasImage: !!image,
          flavorText: this.formatFlavor(d.dishFlavor),
          subtotalText: format.formatPrice((Number(d.amount) || 0) * (d.number || 0)),
        };
      });
      const order = {
        id: o.id,
        number: o.number,
        status: o.status,
        statusText: STATUS_TEXT[o.status] || '',
        orderTime: this.formatTime(o.orderTime),
        amountText: format.formatPrice(o.amount),
        remark: o.remark || '',
        address: o.address || '',
        phone: o.phone || '',
        payMethod: o.payMethod,
      };
      this.setData({ order, items });
    } catch (e) {
      // toast already shown by request layer
    }
  },

  async loadActions() {
    try {
      const actions = (await http.get(`/user/order/${this.data.id}/actions`, { silent: true })) || [];
      this.setData({ actions });
    } catch (e) {
      this.setData({ actions: [] });
    }
  },

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

  formatTime(t) {
    if (!t) return '';
    const d = new Date(t);
    if (isNaN(d.getTime())) return String(t);
    const p = (n) => String(n).padStart(2, '0');
    return (
      `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
      `${p(d.getHours())}:${p(d.getMinutes())}`
    );
  },

  // ===== Action handlers =====
  onPay() {
    // Real WeChat Pay needs merchant certs; simulate success.
    wx.showToast({ title: '支付成功（演示）', icon: 'success' });
    setTimeout(() => this.loadAll(), 600);
  },

  async onCancel() {
    if (this.data.processing) return;
    this.setData({ processing: true });
    try {
      await http.put(`/user/order/cancel/${this.data.id}`);
      wx.showToast({ title: '订单已取消', icon: 'success' });
      setTimeout(() => this.loadAll(), 600);
    } catch (e) {
      // toast already shown
    } finally {
      this.setData({ processing: false });
    }
  },

  // 再来一单: backend rebuilds the cart from the order → jump to cart.
  async onRepetition() {
    if (this.data.processing) return;
    this.setData({ processing: true });
    try {
      await http.post(`/user/order/repetition/${this.data.id}`);
      wx.showToast({ title: '已加入购物车', icon: 'success' });
      setTimeout(() => wx.switchTab({ url: '/pages/cart/cart' }), 600);
    } catch (e) {
      // toast already shown
    } finally {
      this.setData({ processing: false });
    }
  },

  // 催单
  async onReminder() {
    try {
      await http.get(`/user/order/reminder/${this.data.id}`, { silent: true });
      wx.showToast({ title: '已提醒商家', icon: 'success' });
    } catch (e) {
      // toast already shown
    }
  },

  // Dynamic action button dispatch (PAY / CANCEL). Other merchant-side actions
  // (CONFIRM / REJECT / DELIVER / COMPLETE) are not wired for the user client.
  onAction(e) {
    const code = e.currentTarget.dataset.code;
    if (code === 'PAY') this.onPay();
    else if (code === 'CANCEL') this.onCancel();
  },

  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/profile/profile' }) });
  },
});
