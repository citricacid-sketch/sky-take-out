/**
 * 分区标题组件 (SectionHeader)
 *
 * 功能：在列表或卡片区块顶部渲染一个统一的标题栏，
 * 包含主标题、副标题以及一个可选的"更多/查看全部"操作入口。
 *
 * 典型使用场景：
 *   - 首页"招牌菜"、"人气推荐"等区块标题
 *   - 订单列表"进行中 / 已完成"分组标题
 *
 * 对外事件：
 *   - action: 用户点击右侧操作按钮时触发，由父页面决定跳转逻辑
 *
 * 可配置属性：
 *   - title     主标题文本（必填）
 *   - subtitle  副标题文本（可选，灰色小字）
 *   - actionText 右侧操作文案（如"查看全部"），为空则不渲染操作区
 */
Component({
  properties: {
    /** 主标题，例如 "招牌菜" */
    title: { type: String, value: '' },
    /** 副标题，例如 "店长推荐，不容错过" */
    subtitle: { type: String, value: '' },
    /** 右侧操作文案，例如 "查看全部"；为空字符串时不显示操作区 */
    actionText: { type: String, value: '' },
  },

  methods: {
    /**
     * 用户点击右侧操作按钮时触发自定义事件 'action'，
     * 父页面通过 bind:action 监听并执行跳转等逻辑
     */
    onAction() {
      this.triggerEvent('action');
    },
  },
});
