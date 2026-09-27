// pages/menu/menu.js
// Menu page: left category sidebar + right product list.
// Phase 1: category filtering + add placeholder. Full cart logic later.

const http = require('../../services/request.js');
const auth = require('../../services/auth.js');
const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

Page({
  data: {
    loading: false,
    categories: [],
    products: [],
    activeCategory: null,
    activeCategoryName: '全部菜品',
  },

  onLoad() {
    // Page-level dish cache: categoryId -> normalized dishes.
    // Survives across category switches; cleared when the page unloads.
    this.dishCache = {};
    // Per-category in-flight guard: prevents duplicate requests for the same category.
    this._loadingCategories = {};
    // Monotonic request sequence: only the latest request may update the UI.
    this._requestId = 0;
    this.loadCategories();
  },

  onShow() {
    // If homepage asked us to jump to a specific category, honor it.
    const app = getApp();
    if (app && app.globalData && app.globalData.pendingCategoryId) {
      const id = app.globalData.pendingCategoryId;
      app.globalData.pendingCategoryId = null;
      this.selectCategory(id);
    }
  },

  async loadCategories() {
    try {
      const categories = (await http.get('/user/category/list', { silent: true })) || [];
      this.setData({ categories });
      if (categories.length) {
        this.selectCategory(categories[0].id, categories[0].name);
      }
    } catch (e) {
      // ignore
    }
  },

  async selectCategory(id, name) {
    // Always update active state immediately — never block the sidebar.
    this.setData({
      activeCategory: id,
      activeCategoryName: name || '全部菜品',
    });

    // Cache hit: render instantly, no network.
    if (this.dishCache[id]) {
      this.setData({ loading: false, products: this.dishCache[id] });
      return;
    }

    // Skip if this exact category is already being fetched (dedup).
    if (this._loadingCategories[id]) return;

    const requestId = ++this._requestId;
    this._loadingCategories[id] = true;
    this.setData({ loading: true });
    const query = id ? { categoryId: id } : {};
    try {
      const dishes = (await http.get('/user/dish/list', { silent: true, data: query })) || [];
      delete this._loadingCategories[id];
      // Discard if a newer request has since been made.
      if (requestId !== this._requestId) return;
      const normalized = this.normalizeDishes(dishes);
      this.dishCache[id] = normalized;
      this.setData({ loading: false, products: normalized });
    } catch (e) {
      delete this._loadingCategories[id];
      if (requestId !== this._requestId) return;
      this.setData({ loading: false, products: [] });
    }
  },

  normalizeDishes(list) {
    if (!Array.isArray(list)) return [];
    return list.map((d) => {
      let image = d.image || '';
      if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);
      return {
        ...d,
        image,
        hasImage: !!image,
        priceText: format.formatPrice(d.price),
        salesTag: d.sales > 0 ? `月售${format.formatSales(d.sales)}` : '',
      };
    });
  },

  onCategorySelect(e) {
    const { id } = e.currentTarget.dataset;
    const item = this.data.categories.find((c) => c.id === id);
    this.selectCategory(id, item ? item.name : '全部菜品');
  },

  onBrowseAll() {
    if (this.data.categories.length) {
      const first = this.data.categories[0];
      this.selectCategory(first.id, first.name);
    }
  },

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
});
