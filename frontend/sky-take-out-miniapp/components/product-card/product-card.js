/**
 * 商品卡片组件 (ProductCard)
 *
 * 功能：在商品列表、首页推荐、购物车等场景中统一展示一道菜的信息卡片。
 * 支持三种展示模式，适配不同的页面布局需求。
 *
 * 三种模式说明：
 *   - list     列表模式（默认）：横向排列，左侧图片 + 右侧信息 + 底部加购按钮
 *   - featured 推荐模式：大图卡片，强调视觉冲击力，常用于首页招牌菜
 *   - compact  紧凑模式：用于购物车/订单列表，显示金额 (amount) 而非单价 (price)
 *
 * 交互契约：
 *   - tap : 用户点击卡片主体时触发，跳转菜品详情页
 *           同时将菜品数据写入 app.globalData.currentDish，并通过 URL 参数传递
 *   - add : 用户点击加购按钮时触发
 *           - 有口味规格 (flavors) 的菜品：跳转详情页让用户选择规格（规格必选）
 *           - 无口味规格的菜品：直接触发 add 事件，由父页面执行加购
 *
 * 对外事件：
 *   - tap : 传递 { product }，表示用户希望查看详情
 *   - add : 传递 { product }，表示用户希望加入购物车
 *
 * 注意事项：
 *   - 在购物车/订单场景下，product 对象可能只包含 { name, image, number, dishFlavor, amount }，
 *     因此各字段取值需要做容错处理。
 *   - 图片加载失败时自动回退到占位图状态（hasImage: false）。
 */

const format = require('../../utils/format.js');   // 价格格式化工具

Component({
  properties: {
    /**
     * 商品原始数据对象
     * 列表/推荐模式包含：id, name, price, image, description, flavors
     * 购物车/订单模式包含：name, image, number, dishFlavor, amount
     */
    product: { type: Object, value: {}, observer: 'computePrice' },
    /** 展示模式：'list' | 'featured' | 'compact'，默认 'list' */
    mode: { type: String, value: 'list' },
    /** 销量文案，例如 "月售 999"，仅在列表/推荐模式下展示 */
    salesText: { type: String, value: '' },
  },

  data: {
    priceText: '0.00',   // 格式化后的价格文本
    hasImage: false,     // 是否存在有效图片（用于控制占位图显示）
  },

  lifetimes: {
    /**
     * 组件挂载到页面时执行一次，确保初始价格被正确计算
     * （properties 中的 observer 仅在属性变化时触发，首次渲染不会触发）
     */
    attached() {
      this.computePrice();
    },
  },

  methods: {
    /**
     * 根据当前模式和商品数据计算用于展示的价格文本
     * - compact 模式使用 amount（购物车/订单中的金额）
     * - 其他模式使用 price（菜品单价）
     * 同时更新 hasImage 状态以控制图片/占位图的切换
     */
    computePrice() {
      const { product, mode } = this.data;
      const value = mode === 'compact' ? product.amount : product.price;
      this.setData({
        priceText: format.formatPrice(value),
        hasImage: !!(product && product.image),
      });
    },

    /**
     * 图片加载失败时的回调
     * 将 hasImage 置为 false，触发 WXML 中的占位图显示
     */
    onImageError() {
      this.setData({ hasImage: false });
    },

    /**
     * 用户点击卡片主体区域
     * 1. 将当前菜品数据写入全局，便于详情页读取
     * 2. 将菜品关键信息序列化后拼入 URL（跨虚拟机上下文传递更可靠）
     * 3. 跳转详情页；若跳转失败则降级触发 tap 事件
     */
    onTap() {
      const product = this.data.product;
      const app = getApp();
      if (app) app.globalData.currentDish = product;
      // 通过 URL 拼参传递菜品数据，避免跨虚拟机上下文丢失对象引用
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

    /**
     * 用户点击加购按钮
     * - 有口味规格的菜品：必须让用户先选择规格，因此跳转详情页
     * - 无口味规格的菜品：直接触发 add 事件，由父页面执行加购逻辑
     */
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
      // 无规格菜品直接通知父页面加购
      this.triggerEvent('add', { product });
    },
  },
});
