// pages/dish-detail/dish-detail.js
// Dish detail + spec (flavor) selection + add-to-cart.
// There is no single-dish API, so the dish object arrives via URL query parameter
// (set by product-card when navigating). Falls back to globalData for compatibility.

const auth = require('../../services/auth.js');
const cart = require('../../services/cart.js');
const format = require('../../utils/format.js');

Page({
  data: {
    dish: null,
    // flavorSelections: { [flavorName]: chosenOptionValue }
    flavorSelections: {},
    // parsed flavor groups for rendering: [{ name, options, selected }]
    flavorGroups: [],
    quantity: 1,
    priceText: '0.00',
    adding: false,
  },

  onLoad(options) {
    // Diagnostic: log exactly what we received so we can see where the data breaks.
    console.log('[dish-detail] onLoad options =', JSON.stringify(options));
    // Primary: dish data passed via URL query (survives cross-VM context)
    if (options && options.dish) {
      try {
        // WeChat may or may not pre-decode the query param, so guard against double-decode.
        const raw = options.dish;
        const jsonStr = (raw.charAt(0) === '{') ? raw : decodeURIComponent(raw);
        const dish = JSON.parse(jsonStr);
        console.log('[dish-detail] parsed dish from URL:', dish && dish.name);
        this.initDish(dish);
        return;
      } catch (e) {
        console.warn('[dish-detail] URL dish parse failed:', e.message);
        // corrupted query, fall through
      }
    } else {
      console.warn('[dish-detail] no options.dish in query');
    }
    // Fallback: globalData (same VM context only)
    const app = getApp();
    const dish = (app && app.globalData && app.globalData.currentDish) || null;
    console.log('[dish-detail] globalData.currentDish =', dish && dish.name);
    if (!dish) {
      wx.showToast({ title: '菜品信息缺失', icon: 'none' });
      setTimeout(() => wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/menu/menu' }) }), 600);
      return;
    }
    this.initDish(dish);
  },

  onGoBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/menu/menu' }) });
  },

  initDish(dish) {
    let image = dish.image || '';
    if (image && image.startsWith('http:')) image = 'https:' + image.slice(5);

    // Build selectable flavor groups. Each DishFlavor has name + value (JSON string of options).
    const groups = (dish.flavors || []).map((f) => {
      const options = format.parseFlavorOptions(f.value);
      return {
        name: f.name,
        options,
        // default: first option selected (spec is mandatory)
        selected: options.length ? options[0] : '',
      };
    });

    const selections = {};
    groups.forEach((g) => {
      if (g.selected) selections[g.name] = g.selected;
    });

    this.setData({
      dish: { ...dish, image },
      hasImage: !!image,
      flavorGroups: groups,
      flavorSelections: selections,
      quantity: 1,
      priceText: format.formatPrice(dish.price),
    });
  },

  // Build the stable dishFlavor string the backend uses to de-duplicate cart lines.
  buildDishFlavor() {
    const { flavorGroups, flavorSelections } = this.data;
    // Preserve group order so the string is stable.
    const values = flavorGroups
      .map((g) => flavorSelections[g.name])
      .filter((v) => v !== undefined && v !== '');
    return JSON.stringify(values);
  },

  onFlavorSelect(e) {
    const { group, option } = e.currentTarget.dataset;
    const groups = this.data.flavorGroups.map((g) =>
      g.name === group ? { ...g, selected: option } : g
    );
    const selections = { ...this.data.flavorSelections, [group]: option };
    this.setData({ flavorGroups: groups, flavorSelections: selections });
  },

  onQuantityChange(e) {
    const delta = Number(e.currentTarget.dataset.delta) || 0;
    const next = Math.max(1, (this.data.quantity || 1) + delta);
    this.setData({ quantity: next });
  },

  onGoCart() {
    wx.switchTab({ url: '/pages/cart/cart' });
  },

  async onAddToCart() {
    if (this.data.adding) return;
    const { dish, quantity } = this.data;
    if (!dish) return;

    this.setData({ adding: true });
    try {
      // On-demand login, same pattern as the list pages.
      if (!auth.isLoggedIn()) {
        wx.showLoading({ title: '登录中', mask: true });
        try {
          await auth.login(true);
        } catch (err) {
          wx.hideLoading();
          this.setData({ adding: false });
          return;
        }
        wx.hideLoading();
      }

      const dishFlavor = this.buildDishFlavor();
      // Backend add() increments by one per call → loop for quantity.
      for (let i = 0; i < quantity; i++) {
        // Fire sequentially; each call refreshes the cart cache.
        // eslint-disable-next-line no-await-in-loop
        await cart.add(dish.id, dishFlavor);
      }
      wx.showToast({ title: '已加入购物车', icon: 'success' });
    } catch (e) {
      // request.js already surfaces a toast on failure
    } finally {
      this.setData({ adding: false });
    }
  },
});
