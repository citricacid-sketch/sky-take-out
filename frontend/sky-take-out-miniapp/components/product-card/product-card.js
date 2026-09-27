// components/product-card/product-card.js
// Product card in three modes: list | featured | compact.
//
// Interaction contract (list/featured):
//   - `tap`  : open the dish detail page (sets globalData.currentDish, navigates).
//   - `add`  : quick-add. For dishes WITH flavors this routes to detail (spec is mandatory);
//              for dishes WITHOUT flavors it fires `add` so the page can add directly.

const format = require('../../utils/format.js');

Component({
  properties: {
    // raw product object. For cart/order use we pass { name, image, number, dishFlavor, amount }
    product: { type: Object, value: {}, observer: 'computePrice' },
    // 'list' | 'featured' | 'compact'
    mode: { type: String, value: 'list' },
    salesText: { type: String, value: '' },
  },
  data: {
    priceText: '0.00',
    hasImage: false,
  },
  lifetimes: {
    attached() {
      this.computePrice();
    },
  },
  methods: {
    computePrice() {
      const { product, mode } = this.data;
      const value = mode === 'compact' ? product.amount : product.price;
      this.setData({
        priceText: format.formatPrice(value),
        hasImage: !!(product && product.image),
      });
    },

    // Called when an image fails to load → fall back to the placeholder state.
    onImageError() {
      this.setData({ hasImage: false });
    },

    onTap() {
      const product = this.data.product;
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
        fail: () => this.triggerEvent('tap', { product }),
      });
    },

    onAdd() {
      const product = this.data.product;
      const hasFlavors = Array.isArray(product.flavors) && product.flavors.length > 0;
      if (hasFlavors) {
        const app = getApp();
        if (app) app.globalData.currentDish = product;
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
          fail: () => this.triggerEvent('add', { product }),
        });
        return;
      }
      this.triggerEvent('add', { product });
    },
  },
});
