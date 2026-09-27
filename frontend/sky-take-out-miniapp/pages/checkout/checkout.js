// pages/checkout/checkout.js
// Checkout / order submission page.
// - Reads the default address from /user/addressBook/default.
// - Shows the cart items (read-only) from the cart store.
// - Collects a remark, then submits an order via POST /user/order/submit.
// - On success: clears the cart, simulates payment, and returns to the profile.
//
// Payment is simulated (real WeChat Pay needs merchant certificates). We submit the
// order, then immediately mark the flow as complete and route the user to "我的".

const http = require('../../services/request.js');
const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

Page({
  data: {
    // address
    hasAddress: false,
    addressBookId: null,
    addressText: '',
    consignee: '',
    phone: '',

    // order
    items: [],
    itemTotalText: '0.00',
    packAmount: 0,
    deliveryFee: 0,
    orderTotalText: '0.00',

    remark: '',
    submitting: false,
  },

  onLoad() {
    this.unsubscribe = cart.subscribe((list) => this.renderItems(list));
  },

  onShow() {
    cart.refresh();
    this.loadDefaultAddress();
  },

  onUnload() {
    if (this.unsubscribe) this.unsubscribe();
  },

  // ===== Address =====
  async loadDefaultAddress() {
    try {
      const addr = await http.get('/user/addressBook/default', { silent: true });
      if (addr) {
        const fullAddr = this.joinAddress(addr);
        this.setData({
          hasAddress: true,
          addressBookId: addr.id,
          addressText: fullAddr,
          consignee: addr.consignee || '',
          phone: addr.phone || '',
        });
        return;
      }
    } catch (e) {
      // no default address
    }
    this.setData({
      hasAddress: false,
      addressBookId: null,
      addressText: '请添加收货地址',
      consignee: '',
      phone: '',
    });
  },

  joinAddress(addr) {
    const parts = [addr.provinceName, addr.cityName, addr.districtName, addr.detail]
      .filter(Boolean);
    return parts.join('') || addr.detail || '';
  },

  onSelectAddress() {
    wx.navigateTo({ url: '/pages/address/address' });
  },

  // ===== Items / amounts =====
  renderItems(list) {
    const items = (list || []).map((it) => {
      let image = it.image || '';
      if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);
      return {
        ...it,
        image,
        hasImage: !!image,
        flavorText: this.formatFlavor(it.dishFlavor),
        subtotalText: format.formatPrice((Number(it.amount) || 0) * (it.number || 0)),
      };
    });
    const itemTotal = items.reduce(
      (sum, it) => sum + (Number(it.amount) || 0) * (it.number || 0),
      0
    );
    // Simplified extra fees: fixed delivery fee, no per-item packaging.
    const deliveryFee = items.length ? 5 : 0;
    const orderTotal = itemTotal + deliveryFee;
    this.setData({
      items,
      itemTotalText: format.formatPrice(itemTotal),
      deliveryFeeText: format.formatPrice(deliveryFee),
      orderTotalText: format.formatPrice(orderTotal),
      // keep numeric total for submission
      itemTotal,
      deliveryFee,
      orderTotal,
    });
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

  onRemark(e) {
    this.setData({ remark: e.detail.value });
  },

  onGoCart() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/cart/cart' }) });
  },

  // ===== Submit =====
  async onSubmit() {
    if (this.data.submitting) return;
    if (!this.data.items.length) {
      wx.showToast({ title: '购物车是空的', icon: 'none' });
      return;
    }
    if (!this.data.hasAddress) {
      wx.showToast({ title: '请添加收货地址', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const payload = {
        addressBookId: this.data.addressBookId,
        payMethod: 1, // 微信
        remark: this.data.remark || '',
        // Immediate delivery; backend expects a delivery time, use ~30 min out.
        estimatedDeliveryTime: this.formatTime(Date.now() + 30 * 60 * 1000),
        deliveryStatus: 1, // 1 = 立即送出
        tablewareNumber: 0,
        tablewareStatus: 1, // 按餐量提供
        packAmount: this.data.packAmount || 0,
        amount: this.data.orderTotal,
      };

      const result = await http.post('/user/order/submit', payload);

      // Clear the cart so the new order doesn't leave stale items.
      await cart.clean();

      wx.showToast({ title: '下单成功', icon: 'success' });

      // Navigate to the newly created order's detail page.
      // redirectTo (not navigateTo) so the user cannot return to the submitted checkout.
      setTimeout(() => {
        if (result && result.id) {
          wx.redirectTo({
            url: `/pages/order-detail/order-detail?id=${result.id}`,
            fail: () => wx.switchTab({ url: '/pages/profile/profile' }),
          });
        } else {
          wx.switchTab({ url: '/pages/profile/profile' });
        }
      }, 800);
    } catch (e) {
      // request.js already surfaces a toast on failure
    } finally {
      this.setData({ submitting: false });
    }
  },

  // Backend expects 'yyyy-MM-dd HH:mm:ss'.
  formatTime(ts) {
    const d = new Date(ts);
    const p = (n) => String(n).padStart(2, '0');
    return (
      `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
      `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    );
  },
});
