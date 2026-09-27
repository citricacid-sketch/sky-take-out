// pages/order-list/order-list.js
// Order history list with status tabs + pagination.
// status tabs map to Orders status: 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消

const http = require('../../services/request.js');
const format = require('../../utils/format.js');

const PAGE_SIZE = 10;

// Tab value 0 = 全部 (no status filter)
const TABS = [
  { value: 0, label: '全部' },
  { value: 1, label: '待付款' },
  { value: 2, label: '待接单' },
  { value: 5, label: '已完成' },
  { value: 6, label: '已取消' },
];

// Mirror of backend Orders status constants.
const STATUS_TEXT = {
  1: '待付款',
  2: '待接单',
  3: '已接单',
  4: '派送中',
  5: '已完成',
  6: '已取消',
};

Page({
  data: {
    tabs: TABS,
    activeTab: 0,
    orders: [],
    total: 0,
    page: 1,
    pageSize: PAGE_SIZE,
    loading: false,
    finished: false,
  },

  onLoad(options) {
    // Optional deep-link: /pages/order-list/order-list?status=1
    if (options && options.status) {
      const s = Number(options.status);
      if ([0, 1, 2, 5, 6].includes(s)) this.setData({ activeTab: s });
    }
    this.loadFirstPage();
  },

  onShow() {
    // Refresh when returning (e.g. after cancel/repeat in detail page).
    this.refresh();
  },

  onPullDownRefresh() {
    this.loadFirstPage().then(() => wx.stopPullDownRefresh());
  },

  onReachBottom() {
    if (!this.data.finished && !this.data.loading) {
      this.loadMore();
    }
  },

  statusText(status) {
    return STATUS_TEXT[status] || '';
  },

  // Low-saturation semantic class for order status text.
  // pending (待支付/待接单/已接单/派送中) → primary
  // done (已完成) → accent green
  // cancelled (已取消) → neutral gray
  statusClass(status) {
    if (status === 5) return 'is-done';
    if (status === 6) return 'is-cancelled';
    return 'is-pending';
  },

  onTabChange(e) {
    const value = Number(e.currentTarget.dataset.value);
    if (value === this.data.activeTab) return;
    this.setData({ activeTab: value });
    this.loadFirstPage();
  },

  refresh() {
    this.loadFirstPage();
  },

  async loadFirstPage() {
    this.setData({ loading: true, orders: [], page: 1, finished: false });
    try {
      const result = await this.fetchPage(1);
      const records = (result.records || []).map((o) => this.normalize(o));
      this.setData({
        loading: false,
        orders: records,
        total: result.total || 0,
        finished: records.length < this.data.pageSize,
      });
    } catch (e) {
      this.setData({ loading: false, finished: true });
    }
  },

  async loadMore() {
    if (this.data.loading || this.data.finished) return;
    this.setData({ loading: true });
    try {
      const next = this.data.page + 1;
      const result = await this.fetchPage(next);
      const records = (result.records || []).map((o) => this.normalize(o));
      this.setData({
        loading: false,
        orders: this.data.orders.concat(records),
        page: next,
        finished: records.length < this.data.pageSize,
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async fetchPage(page) {
    const params = { page, pageSize: this.data.pageSize };
    if (this.data.activeTab > 0) params.status = this.data.activeTab;
    return http.get('/user/order/historyOrders', { silent: true, data: params });
  },

  normalize(o) {
    let amountText = '0.00';
    const amt = Number(o.amount);
    if (!isNaN(amt)) amountText = format.formatPrice(amt);

    // Prefer the backend-provided dish summary; fall back to detail list.
    let dishSummary = o.orderDishes || '';
    if (!dishSummary && Array.isArray(o.orderDetailList) && o.orderDetailList.length) {
      dishSummary = o.orderDetailList.map((d) => d.name).join('、');
    }

    return {
      id: o.id,
      number: o.number,
      status: o.status,
      statusText: STATUS_TEXT[o.status] || '',
      statusClass: this.statusClass(o.status),
      orderTime: this.formatTime(o.orderTime),
      amountText,
      dishSummary,
    };
  },

  // orderTime comes as a Date from Jackson; format defensively.
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

  onOrderTap(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` });
  },

  onGoMenu() {
    wx.switchTab({ url: '/pages/menu/menu' });
  },
});
