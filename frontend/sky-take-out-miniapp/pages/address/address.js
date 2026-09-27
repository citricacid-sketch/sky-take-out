// pages/address/address.js
// Address book list: load current user's addresses, set default, edit, add, delete.

const http = require('../../services/request.js');

Page({
  data: {
    addresses: [],
    loading: false,
  },

  onShow() {
    this.loadList();
  },

  async loadList() {
    this.setData({ loading: true });
    try {
      const list = (await http.get('/user/addressBook/list', { silent: true })) || [];
      const addresses = list.map((a) => this.normalize(a));
      this.setData({ addresses, loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  normalize(a) {
    const fullAddr = [a.provinceName, a.cityName, a.districtName, a.detail]
      .filter(Boolean)
      .join('');
    return {
      id: a.id,
      consignee: a.consignee || '',
      phone: a.phone || '',
      sex: a.sex || '1',
      detail: a.detail || '',
      label: a.label || '',
      isDefault: a.isDefault === 1,
      fullAddr,
    };
  },

  onAdd() {
    wx.navigateTo({ url: '/pages/address-edit/address-edit' });
  },

  onEdit(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/address-edit/address-edit?id=${id}` });
  },

  async onSetDefault(e) {
    const { id } = e.currentTarget.dataset;
    try {
      await http.put('/user/addressBook/default', { id });
      wx.showToast({ title: '已设为默认', icon: 'success' });
      this.loadList();
    } catch (e) {
      // toast already shown
    }
  },

  onDelete(e) {
    const { id } = e.currentTarget.dataset;
    wx.showModal({
      title: '删除地址',
      content: '确定删除该收货地址吗？',
      confirmColor: '#DC2626',
      success: async (res) => {
        if (res.confirm) {
          try {
            await http.delete(`/user/addressBook?id=${id}`);
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadList();
          } catch (err) {
            // toast already shown
          }
        }
      },
    });
  },
});
