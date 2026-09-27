// components/toast/toast.js
let timer = null;

Component({
  data: {
    visible: false,
    icon: 'none',
    message: '',
  },
  methods: {
    show(message, icon = 'none', duration = 1500) {
      if (timer) clearTimeout(timer);
      this.setData({ visible: true, message, icon });
      if (icon !== 'loading' && duration > 0) {
        timer = setTimeout(() => this.hide(), duration);
      }
    },
    hide() {
      if (timer) clearTimeout(timer);
      this.setData({ visible: false });
    },
    success(msg, duration) { this.show(msg, 'success', duration); },
    error(msg, duration) { this.show(msg, 'error', duration); },
    loading(msg) { this.show(msg, 'loading', 0); },
    onTap() { /* block clicks */ },
  },
});
