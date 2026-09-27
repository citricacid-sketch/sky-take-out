/**
 * 空状态组件 (EmptyState)
 *
 * 功能：当某个列表、容器或搜索结果为空时，展示一个友好的占位提示，
 * 告知用户"当前没有内容"，并可提供引导性操作按钮引导用户下一步动作。
 *
 * 典型使用场景：
 *   - 购物车为空时引导去点餐
 *   - 订单列表为空时展示安慰性文案
 *   - 搜索无结果时提示换个关键词
 *
 * 对外事件：
 *   - action: 用户点击操作按钮时触发，由父页面决定跳转或刷新逻辑
 *
 * 可配置属性：
 *   - symbolText  顶部大号符号，默认 "—"，可传入 emoji 或图标字符
 *   - title       主提示文案，默认 "这里空空如也"
 *   - subtitle    副提示文案（可选）
 *   - actionText  操作按钮文案（如"去逛逛"），为空则不渲染按钮
 */
Component({
  properties: {
    /** 顶部占位符号，用于视觉装饰，默认 "—" */
    symbolText: { type: String, value: '—' },
    /** 主提示文案 */
    title: { type: String, value: '这里空空如也' },
    /** 副提示文案，灰色小字，可选 */
    subtitle: { type: String, value: '' },
    /** 操作按钮文案；为空字符串时不显示按钮 */
    actionText: { type: String, value: '' },
  },

  methods: {
    /**
     * 用户点击操作按钮时触发自定义事件 'action'，
     * 父页面通过 bind:action 监听并执行跳转等后续逻辑
     */
    onAction() {
      this.triggerEvent('action');
    },
  },
});
