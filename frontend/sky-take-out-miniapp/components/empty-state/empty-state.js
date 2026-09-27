// components/empty-state/empty-state.js
Component({
  properties: {
    symbolText: { type: String, value: '—' },
    title: { type: String, value: '这里空空如也' },
    subtitle: { type: String, value: '' },
    actionText: { type: String, value: '' },
  },
  methods: {
    onAction() {
      this.triggerEvent('action');
    },
  },
});
