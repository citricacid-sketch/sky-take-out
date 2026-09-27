/**
 * AI 客服聊天组件 (AiChat)
 *
 * 功能：全局悬浮的 AI 客服入口，包含一个可展开的底部抽屉式聊天面板。
 * 集成两种服务模式，覆盖"咨询"与"点餐"两类典型用户需求。
 *
 * 两种模式：
 *   - service（默认）：智能客服模式，回答用户关于订单、配送、菜品等的咨询
 *   - ordering：AI 点餐助手模式，根据用户描述智能推荐菜品组合
 *
 * 核心能力：
 *   1. 消息收发：用户输入 → 调用 /user/chat 接口 → 展示 AI 回复
 *   2. 快捷问题：预设的常见问题按钮，点击即发送
 *   3. AI 点餐：解析用户意图 → 加载候选菜品 → 生成推荐组合 → 一键加入购物车
 *   4. LLM 解释：在推荐结果基础上，异步请求 LLM 生成更友好的自然语言说明
 *
 * 数据流（点餐模式）：
 *   用户描述 → orderPlanner.parseIntent → 结构化意图
 *             → orderPlanner.loadCandidates → 候选菜品池
 *             → orderPlanner.generateRecommendations → 推荐组合
 *             → orderPlanner.askExplanation → LLM 自然语言解释（异步、非阻塞）
 *
 * 对外事件：
 *   本组件通过 properties 接收外部 context（如 orderId），
 *   不对外触发自定义事件，所有交互在组件内部闭环处理。
 */

const http = require('../../services/request.js');              // HTTP 请求封装
const orderPlanner = require('../../services/ai-order-planner.js');  // AI 点餐规划服务

/**
 * 消息 ID 计数器（模块级），保证同一会话内每条消息的 ID 唯一
 */
let msgCounter = 0;

/**
 * 生成唯一消息 ID
 * @param {string} prefix 前缀，用于区分消息类型（如 'user'、'ai'、'welcome'）
 * @returns {string} 格式为 `${prefix}_${时间戳}_${序号}` 的唯一标识
 */
function nextId(prefix) {
  msgCounter += 1;
  return `${prefix}_${Date.now()}_${msgCounter}`;
}

Component({
  properties: {
    /**
     * 可选的额外上下文，会随每次聊天请求一并发送给后端
     * 例如当前订单 ID：{ orderId: 12345 }
     */
    context: { type: Object, value: {} },
  },

  data: {
    // ---- 面板状态 ----
    mode: 'service',        // 当前模式：'service'（客服）| 'ordering'（点餐）
    isOpen: false,          // 聊天面板是否展开
    messages: [],           // 消息列表，每条包含 { id, role, content, status }
    inputValue: '',         // 文本输入框当前内容
    loading: false,         // 是否正在等待 AI 回复（客服模式）
    scrollToView: '',       // 需要滚动到的消息 id（用于自动滚动到底部）

    // ---- 点餐模式状态 ----
    orderingLoading: false,         // 是否正在生成推荐
    orderingIntent: null,           // 用户点餐意图（结构化后的对象）
    orderingRecommendations: [],    // AI 推荐的菜品列表
    orderingExplanation: '',        // LLM 生成的自然语言解释说明
    orderingSubtotal: '0.00',       // 推荐组合的合计金额
  },

  lifetimes: {
    /**
     * 组件挂载时注入一条欢迎语消息
     * 仅在 attached 阶段执行一次，避免重复添加
     */
    attached() {
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
    // ==================== 面板开/关 ====================

    /** 展开聊天面板 */
    onOpen() {
      this.setData({ isOpen: true });
    },

    /** 收起聊天面板 */
    onClose() {
      this.setData({ isOpen: false });
    },

    /** 点击遮罩层关闭面板 */
    onOverlayTap() {
      this.onClose();
    },

    /**
     * 点击面板内部（空白区域）
     * 为空函数，目的是阻止事件冒泡到遮罩层，防止误关闭
     */
    onPanelTap() {
      // Stop propagation: tapping inside the panel should not close it.
    },

    // ==================== 模式切换 ====================

    /**
     * 切换服务模式（客服 / 点餐）
     * 切换时重置点餐模式的状态，避免残留的上一次推荐结果干扰
     * @param {Object} e 事件对象，e.currentTarget.dataset.mode 为目标模式
     */
    switchMode(e) {
      const mode = e.currentTarget.dataset.mode;
      if (mode === this.data.mode) return;   // 模式未变化时直接返回
      this.setData({
        mode,
        // 切换到点餐模式时，清空上一次的推荐状态
        orderingIntent: null,
        orderingRecommendations: [],
        orderingExplanation: '',
        orderingSubtotal: '0.00',
      });
    },

    // ==================== 客服模式：快捷问题 ====================

    /**
     * 用户点击快捷问题按钮
     * 直接将该问题文本作为消息发送
     * @param {Object} e 事件对象，e.currentTarget.dataset.q 为问题文本
     */
    onQuickQuestion(e) {
      if (this.data.loading) return;   // 正在请求中时忽略点击，防止并发
      const q = e.currentTarget.dataset.q;
      this.sendMessage(q);
    },

    // ==================== 客服模式：输入与发送 ====================

    /** 文本输入框内容变化时同步到 data */
    onInput(e) {
      this.setData({ inputValue: e.detail.value });
    },

    /**
     * 用户点击发送按钮
     * 校验输入内容非空后发送消息
     */
    onSend() {
      if (this.data.loading) return;
      const text = (this.data.inputValue || '').trim();
      if (!text) {
        wx.showToast({ title: '请输入您的问题', icon: 'none' });
        return;
      }
      this.sendMessage(text);
    },

    // ==================== 点餐模式：快捷条件 ====================

    /**
     * 用户点击点餐模式的快捷条件标签（如"清淡"、"2人餐"）
     * 将选中的条件追加到意图描述中，逗号分隔
     * @param {Object} e 事件对象，e.currentTarget.dataset.q 为条件文本
     */
    onOrderingQuick(e) {
      if (this.data.orderingLoading) return;
      const val = e.currentTarget.dataset.q;
      const cur = this.data.orderingIntent || { rawText: '' };
      // 已有内容时用逗号拼接，首次则直接赋值
      const next = cur.rawText ? `${cur.rawText}，${val}` : val;
      this.setData({ orderingIntent: { ...cur, rawText: next } });
    },

    /**
     * 点餐模式文本输入框内容变化
     * 直接覆盖意图的 rawText 字段
     */
    onOrderingInput(e) {
      const cur = this.data.orderingIntent || { rawText: '' };
      this.setData({ orderingIntent: { ...cur, rawText: e.detail.value } });
    },

    // ==================== 点餐模式：提交意图 ====================

    /**
     * 提交点餐意图，触发完整的推荐流程
     *
     * 流程：
     *   1. 解析用户原始描述 → 结构化意图
     *   2. 加载候选菜品池
     *   3. 根据意图与候选池生成推荐组合
     *   4. 异步请求 LLM 生成友好说明（不阻塞 UI 展示推荐结果）
     */
    async onOrderingSubmit() {
      if (this.data.orderingLoading) return;
      const intent = this.data.orderingIntent || { rawText: '' };
      if (!intent.rawText || !intent.rawText.trim()) {
        wx.showToast({ title: '请描述你的点餐需求', icon: 'none' });
        return;
      }

      this.setData({ orderingLoading: true, orderingRecommendations: [], orderingExplanation: '' });

      try {
        // 步骤 1：解析用户原始描述，提取结构化意图（人数、口味偏好、预算等）
        const parsed = orderPlanner.parseIntent(intent.rawText);
        this.setData({ orderingIntent: parsed });

        // 步骤 2：加载候选菜品池（从后端获取可用菜品列表）
        const candidates = await orderPlanner.loadCandidates();

        // 步骤 3：基于意图与候选池，生成确定性的推荐组合
        const recs = orderPlanner.generateRecommendations(parsed, candidates);
        const subtotal = orderPlanner.formatPrice(orderPlanner.calcTotal(recs));

        this.setData({
          orderingRecommendations: recs,
          orderingSubtotal: subtotal,
        });

        // 步骤 4：异步请求 LLM 生成自然语言说明（非阻塞，返回后追加展示）
        orderPlanner.askExplanation(parsed, recs).then((explanation) => {
          if (explanation) this.setData({ orderingExplanation: explanation });
        });
      } catch (e) {
        wx.showToast({ title: '推荐失败，请重试', icon: 'none' });
      } finally {
        this.setData({ orderingLoading: false });
      }
    },

    // ==================== 点餐模式：加入购物车 ====================

    /**
     * 将 AI 推荐的全部菜品一键加入购物车
     * 根据返回结果分类（成功 / 需选规格 / 失败）给出差异化的用户反馈
     */
    async onAddToCart() {
      if (this.data.orderingRecommendations.length === 0) return;
      wx.showLoading({ title: '加入中...' });
      try {
        const { success, failed, needsFlavor } = await orderPlanner.addToCart(this.data.orderingRecommendations);
        wx.hideLoading();

        // 拼装用户友好的反馈文案
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

    /**
     * 点餐模式下，将单个无规格菜品直接加入购物车
     * @param {Object} e 事件对象，e.currentTarget.dataset.dish 为菜品数据
     */
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

    /**
     * 点餐模式下，查看某道推荐菜品的详情
     * 跳转详情页并将菜品数据存入全局
     */
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

    // ==================== 核心发送流程（客服模式） ====================

    /**
     * 发送聊天消息并等待 AI 回复
     *
     * 流程：
     *   1. 将用户消息和一条"加载中"占位消息追加到列表
     *   2. 调用后端 /user/chat 接口（携带 context 上下文）
     *   3. 成功：将占位消息替换为 AI 回复
     *   4. 失败：将占位消息替换为错误提示，并保留用户输入便于重试
     *
     * @param {string} content 用户发送的消息文本
     */
    sendMessage(content) {
      if (this.data.loading) return;   // 守卫：防止并发请求

      // 构造用户消息
      const userMsg = {
        id: nextId('user'),
        role: 'user',
        content,
        status: 'success',
      };
      // 构造"正在发送中"的占位消息，用于展示加载动画
      const loadingMsg = {
        id: nextId('ai'),
        role: 'assistant',
        content: '',
        status: 'sending',
      };

      const messages = this.data.messages.concat([userMsg, loadingMsg]);
      this.setData({
        messages,
        inputValue: '',                    // 清空输入框
        loading: true,                     // 标记为加载中
        scrollToView: loadingMsg.id,       // 滚动到新消息位置
      });

      // 构造请求载荷：消息内容 + 可选的上下文信息
      const payload = { message: content, ...this.data.context };

      http
        .post('/user/chat', payload)
        .then((reply) => {
          // 规范化回复内容：确保为非空字符串
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
          // 请求失败：替换占位消息为错误提示，并保留用户输入内容便于重试
          const updated = this.data.messages.map((m) =>
            m.id === loadingMsg.id
              ? { ...m, content: '暂时无法联系智能客服，请稍后再试。', status: 'error' }
              : m
          );
          this.setData({
            messages: updated,
            loading: false,
            inputValue: content,           // 失败时保留输入，方便用户修改后重发
            scrollToView: loadingMsg.id,
          });
        });
    },
  },
});
