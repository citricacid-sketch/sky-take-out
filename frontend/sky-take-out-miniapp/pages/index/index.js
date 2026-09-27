// pages/index/index.js
// Homepage: product decision page. Loads shop status, categories, dishes, recommendation.
// Phase 1: add-to-cart is a placeholder (toast only); full cart logic lands in a later phase.

const http = require('../../services/request.js');
const auth = require('../../services/auth.js');
const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

const MAX_FEATURED = 6;
const MAX_DISCOVERY = 4;

Page({
  data: {
    loading: true,
    shopOpen: true,
    categories: [],
    featured: [],
    discovery: [],
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    // Refresh shop status each time we return to the homepage.
    this.refreshShopStatus();
    // Keep the cart TabBar badge fresh (cart may have changed elsewhere).
    cart.updateTabBadge();
  },

  onPullDownRefresh() {
    this.loadData().then(() => wx.stopPullDownRefresh());
  },

  refreshShopStatus() {
    http
      .get('/user/shop/status', { silent: true })
      .then((status) => this.setData({ shopOpen: status === 1 }))
      .catch(() => this.setData({ shopOpen: true }));
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [categories, dishes, recommend] = await Promise.all([
        http.get('/user/category/list', { silent: true }).catch(() => []),
        http.get('/user/dish/list', { silent: true }).catch(() => []),
        http.get('/user/recommend', { silent: true }).catch(() => []),
      ]);

      const normalizedDishes = this.normalizeDishes(dishes);
      const featured = this.normalizeDishes(recommend).slice(0, MAX_FEATURED);

      this.setData({
        loading: false,
        categories: categories || [],
        featured,
        // Discovery is a small slice; if recommendation is empty, fall back to first dishes.
        discovery: (featured.length ? normalizedDishes.filter((d) => !featured.find((f) => f.id === d.id)) : normalizedDishes).slice(0, MAX_DISCOVERY),
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  // Backend may return flavors[].value as a JSON string; normalize for display.
  normalizeDishes(list) {
    if (!Array.isArray(list)) return [];
    return list.map((d) => {
      let flavorText = '';
      if (Array.isArray(d.flavors) && d.flavors.length) {
        const opts = d.flavors.flatMap((f) => format.parseFlavorOptions(f.value));
        if (opts.length) flavorText = opts.join(' / ');
      }
      let image = d.image || '';
      // Backend returns OSS URLs; ensure https for wx image component.
      if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);
      return {
        ...d,
        image,
        hasImage: !!image,
        priceText: format.formatPrice(d.price),
        flavorText,
        // No real sales field from backend yet; leave tag empty rather than fabricate.
        salesTag: d.sales > 0 ? `月售${format.formatSales(d.sales)}` : '',
      };
    });
  },

  // ===== Interactions =====
  onStartOrder() {
    wx.switchTab({ url: '/pages/menu/menu' });
  },

  onMoreFeatured() {
    wx.switchTab({ url: '/pages/menu/menu' });
  },

  onViewAll() {
    wx.switchTab({ url: '/pages/menu/menu' });
  },

  onCategoryTap(e) {
    const { id } = e.currentTarget.dataset;
    wx.switchTab({ url: '/pages/menu/menu' });
    // In a later phase we will pass the category to the menu page via globalData/event.
    const app = getApp();
    if (app) app.globalData.pendingCategoryId = id;
  },

  // product-card routes flavored dishes to detail itself; tap is a graceful fallback.
  // Dish data lives in e.currentTarget.dataset.product (set by data-product in wxml).
  onProductTap(e) {
    const product = e.currentTarget.dataset.product;
    if (!product) return;
    const app = getApp();
    if (app) app.globalData.currentDish = product;
    // Pass dish data via URL to survive cross-VM context in mini program runtime
    const dishQuery = encodeURIComponent(JSON.stringify({
      id: product.id,
      name: product.name,
      price: product.price,
      image: product.image,
      description: product.description,
      flavors: product.flavors || [],
    }));
    wx.navigateTo({
      url: `/pages/dish-detail/dish-detail?dish=${dishQuery}`,
      fail: () => wx.showToast({ title: '无法打开菜品详情', icon: 'none' }),
    });
  },

  // Only fires for flavorless dishes (flavored ones route to detail via the card).
  async onAddProduct(e) {
    const product = e.currentTarget.dataset.product;
    if (!product) return;
    // On-demand login.
    if (!auth.isLoggedIn()) {
      wx.showLoading({ title: '登录中', mask: true });
      try {
        await auth.login(true);
      } catch (err) {
        wx.hideLoading();
        return;
      }
      wx.hideLoading();
    }
    try {
      await cart.add(product.id, '');
      wx.showToast({ title: `已加入：${product.name}`, icon: 'success' });
    } catch (_e) {
      // request.js already surfaces a toast on failure
    }
  },

  /** 页面滚动事件：传递给 scroll-to-top 组件 */
  onPageScroll(e) {
    this.setData({ scrollTop: e.scrollTop });
  },
});
