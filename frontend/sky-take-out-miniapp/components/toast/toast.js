/**
 * 轻提示组件 (Toast)
 *
 * 功能：在屏幕底部/中部显示一条短暂的提示信息。
 * 支持三种语义化类型：success（成功）、error（失败）、loading（加载中）。
 *
 * 使用方式（其他组件通过 selectComponent 获取实例后调用）：
 *   this.selectComponent('#toast').success('操作成功');
 *   this.selectComponent('#toast').error('网络异常');
 *   this.selectComponent('#toast').loading('加载中...');   // 需手动 hide()
 *
 * 设计要点：
 *   - 采用单例计时器 (module 级 timer)，连续调用时自动取消前一个定时器，
 *     避免多个 toast 叠加导致提前消失。
 *   - loading 类型不自动隐藏，由业务方在合适时机调用 hide()。
 *   - onTap 为空函数，用于拦截点击事件，防止用户点击穿透到下层页面。
 */
// 模块级计时器引用，保证全局只有一个活跃的 setTimeout
let timer = null;

Component({
  data: {
    visible: false,   // 是否显示提示框
    icon: 'none',     // 图标：'none' | 'success' | 'error' | 'loading'
    message: '',      // 提示文本
  },

  methods: {
    /**
     * 核心显示方法
     * @param {string} message  提示内容
     * @param {string} icon     图标类型，默认 'none'（无图标）
     * @param {number} duration 自动关闭毫秒数，默认 1500；传 0 则不自动关闭
     */
    show(message, icon = 'none', duration = 1500) {
      // 若存在未执行的定时器，先清除，防止本次显示被提前关闭
      if (timer) clearTimeout(timer);
      this.setData({ visible: true, message, icon });
      // loading 类型需要业务方手动关闭，因此只在非 loading 且 duration > 0 时设置定时器
      if (icon !== 'loading' && duration > 0) {
        timer = setTimeout(() => this.hide(), duration);
      }
    },

    /**
     * 手动隐藏提示框并清理计时器
     * 通常在 loading 结束时由业务方调用
     */
    hide() {
      if (timer) clearTimeout(timer);
      this.setData({ visible: false });
    },

    /** 快捷方法：显示成功提示 */
    success(msg, duration) { this.show(msg, 'success', duration); },

    /** 快捷方法：显示失败提示 */
    error(msg, duration) { this.show(msg, 'error', duration); },

    /**
     * 快捷方法：显示加载中提示
     * duration 固定为 0，表示不自动关闭
     */
    loading(msg) { this.show(msg, 'loading', 0); },

    /**
     * 空点击事件处理器
     * 作用：阻止点击穿透到下层页面元素
     */
    onTap() { /* block clicks */ },
  },
});
