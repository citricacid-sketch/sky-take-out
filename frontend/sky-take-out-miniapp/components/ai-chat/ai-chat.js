// components/ai-chat/ai-chat.js
// Global AI customer service: floating button + bottom-sheet chat panel.
// Supports two modes: 'service' (default) and 'ordering' (AI ordering assistant).

const http = require('../../services/request.js');
const orderPlanner = require('../../services/ai-order-planner.js');

let msgCounter = 0;
function nextId(prefix) {
  msgCounter += 1;
  return `${prefix}_${Date.now()}_${msgCounter}`;
}

Component({
  properties: {
    // Optional: extra context passed with every message (e.g. orderId)
    context: { type: Object, value: {} },
  },

  data: {
    // Mode: 'service' | 'ordering'
    mode: 'service',
    isOpen: false,
    messages: [],
    inputValue: '',
    loading: false,
    scrollToView: '',
    // Ordering mode state
    orderingLoading: false,
    orderingIntent: null,
    orderingRecommendations: [],
    orderingExplanation: '',
    orderingSubtotal: '0.00',
  },

  lifetimes: {
    attached() {
      // Seed the welcome message once when the component mounts.
      this.setData({
        messages: [
          {
            id: nextId('welcome'),
            role: 'assistant',
            content: '您好，我可以帮您查询订单、了解配送情况，也可以推荐适合您的菜品。',
            status: 'success',
          },
        ],
      });
    },
  },

  methods: {
    // ---- Panel open/close ----
    onOpen() {
      this.setData({ isOpen: true });
    },
    onClose() {
      this.setData({ isOpen: false });
    },
    onOverlayTap() {
      this.onClose();
    },
    onPanelTap() {
      // Stop propagation: tapping inside the panel should not close it.
    },

    // ---- Mode switching ----
    switchMode(e) {
      const mode = e.currentTarget.dataset.mode;
      if (mode === this.data.mode) return;
      this.setData({
        mode,
        // Reset ordering state when switching to ordering
        orderingIntent: null,
        orderingRecommendations: [],
        orderingExplanation: '',
        orderingSubtotal: '0.00',
      });
    },

    // ---- Quick questions (service mode) ----
    onQuickQuestion(e) {
      if (this.data.loading) return;
      const q = e.currentTarget.dataset.q;
      this.sendMessage(q);
    },

    // ---- Input (service mode) ----
    onInput(e) {
      this.setData({ inputValue: e.detail.value });
    },
    onSend() {
      if (this.data.loading) return;
      const text = (this.data.inputValue || '').trim();
      if (!text) {
        wx.showToast({ title: '请输入您的问题', icon: 'none' });
        return;
      }
      this.sendMessage(text);
    },

    // ---- Ordering mode: quick conditions ----
    onOrderingQuick(e) {
      if (this.data.orderingLoading) return;
      const val = e.currentTarget.dataset.q;
      const cur = this.data.orderingIntent || { rawText: '' };
      const next = cur.rawText ? `${cur.rawText}，${val}` : val;
      this.setData({ orderingIntent: { ...cur, rawText: next } });
    },

    // ---- Ordering mode: input ----
    onOrderingInput(e) {
      const cur = this.data.orderingIntent || { rawText: '' };
      this.setData({ orderingIntent: { ...cur, rawText: e.detail.value } });
    },

    // ---- Ordering mode: submit intent ----
    async onOrderingSubmit() {
      if (this.data.orderingLoading) return;
      const intent = this.data.orderingIntent || { rawText: '' };
      if (!intent.rawText || !intent.rawText.trim()) {
        wx.showToast({ title: '请描述你的点餐需求', icon: 'none' });
        return;
      }

      this.setData({ orderingLoading: true, orderingRecommendations: [], orderingExplanation: '' });

      try {
        // 1. Parse intent
        const parsed = orderPlanner.parseIntent(intent.rawText);
        this.setData({ orderingIntent: parsed });

        // 2. Load candidates
        const candidates = await orderPlanner.loadCandidates();

        // 3. Generate deterministic recommendations
        const recs = orderPlanner.generateRecommendations(parsed, candidates);
        const subtotal = orderPlanner.formatPrice(orderPlanner.calcTotal(recs));

        this.setData({
          orderingRecommendations: recs,
          orderingSubtotal: subtotal,
        });

        // 4. Ask LLM for friendly explanation (non-blocking for UI)
        orderPlanner.askExplanation(parsed, recs).then((explanation) => {
          if (explanation) this.setData({ orderingExplanation: explanation });
        });
      } catch (e) {
        wx.showToast({ title: '推荐失败，请重试', icon: 'none' });
      } finally {
        this.setData({ orderingLoading: false });
      }
    },

    // ---- Ordering mode: add all to cart ----
    async onAddToCart() {
      if (this.data.orderingRecommendations.length === 0) return;
      wx.showLoading({ title: '加入中...' });
      try {
        const { success, failed, needsFlavor } = await orderPlanner.addToCart(this.data.orderingRecommendations);
        wx.hideLoading();

        // Build user-friendly feedback
        const parts = [];
        if (success.length > 0) parts.push(`已加入 ${success.length} 项`);
        if (needsFlavor.length > 0) parts.push(`${needsFlavor.length} 项需选择规格`);
        if (failed.length > 0) parts.push(`${failed.length} 项失败`);

        if (success.length > 0 && needsFlavor.length === 0 && failed.length === 0) {
          wx.showToast({ title: '已加入购物车', icon: 'success' });
        } else if (success.length > 0) {
          wx.showToast({ title: parts.join('，'), icon: 'none' });
        } else if (needsFlavor.length > 0 && success.length === 0) {
          wx.showToast({ title: '这些菜品需要选择规格', icon: 'none' });
        } else {
          wx.showToast({ title: '加购失败，请重试', icon: 'none' });
        }
      } catch (e) {
        wx.hideLoading();
        wx.showToast({ title: '加购失败', icon: 'none' });
      }
    },

    // ---- Ordering mode: add a single dish without flavor to cart ----
    async onAddSingle(e) {
      if (this.data.orderingLoading) return;
      const dish = e.currentTarget.dataset.dish;
      if (!dish) return;
      try {
        await http.post('/user/shoppingCart/add', { dishId: dish.id, dishFlavor: '' });
        wx.showToast({ title: `已加入：${dish.name}`, icon: 'success' });
      } catch (err) {
        wx.showToast({ title: '加购失败', icon: 'none' });
      }
    },
    // ---- Ordering mode: view dish detail ----
    onViewDish(e) {
      const dish = e.currentTarget.dataset.dish;
      if (!dish) return;
      const app = getApp();
      if (app && app.globalData) app.globalData.currentDish = dish;
      const dishQuery = encodeURIComponent(JSON.stringify({
        id: dish.id,
        name: dish.name,
        price: dish.price,
        image: dish.image,
        description: dish.description,
        flavors: dish.flavors || [],
      }));
      wx.navigateTo({
        url: `/pages/dish-detail/dish-detail?dish=${dishQuery}`,
        fail: () => wx.showToast({ title: '无法打开菜品详情', icon: 'none' }),
      });
    },

    // ---- Core send flow (service mode) ----
    sendMessage(content) {
      if (this.data.loading) return; // Guard: prevent concurrent requests

      const userMsg = {
        id: nextId('user'),
        role: 'user',
        content,
        status: 'success',
      };
      const loadingMsg = {
        id: nextId('ai'),
        role: 'assistant',
        content: '',
        status: 'sending',
      };

      const messages = this.data.messages.concat([userMsg, loadingMsg]);
      this.setData({
        messages,
        inputValue: '',
        loading: true,
        scrollToView: loadingMsg.id,
      });

      // Build payload: message + optional context (e.g. orderId)
      const payload = { message: content, ...this.data.context };

      http
        .post('/user/chat', payload)
        .then((reply) => {
          // Normalize reply: ensure non-empty string
          const text = (typeof reply === 'string' && reply.trim()) ? reply : '抱歉，我暂时无法回答。';
          const updated = this.data.messages.map((m) =>
            m.id === loadingMsg.id
              ? { ...m, content: text, status: 'success' }
              : m
          );
          this.setData({
            messages: updated,
            loading: false,
            scrollToView: loadingMsg.id,
          });
        })
        .catch(() => {
          const updated = this.data.messages.map((m) =>
            m.id === loadingMsg.id
              ? { ...m, content: '暂时无法联系智能客服，请稍后再试。', status: 'error' }
              : m
          );
          this.setData({
            messages: updated,
            loading: false,
            inputValue: content, // Preserve input on failure for retry
            scrollToView: loadingMsg.id,
          });
        });
    },
  },
});
