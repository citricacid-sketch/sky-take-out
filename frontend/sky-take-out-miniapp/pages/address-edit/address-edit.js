// pages/address-edit/address-edit.js
// Create or edit an address. When `id` is present in query, load & edit; otherwise create.
//
// Backend AddressBook entity fields: consignee, phone, sex, provinceName, cityName,
// districtName, detail, label, isDefault. Province/city/district codes are optional for
// the backend (it only requires the names to display); we send names from a simple picker.

const http = require('../../services/request.js');

// A small static region dataset for the demo (province + cities + districts).
// In production this would come from an API; here it keeps the picker functional offline.
const REGION = [
  {
    name: '北京市', code: '110000',
    children: [
      { name: '北京市', code: '110100', children: [
        { name: '朝阳区', code: '110105' }, { name: '海淀区', code: '110108' },
        { name: '东城区', code: '110101' }, { name: '西城区', code: '110102' },
      ]},
    ],
  },
  {
    name: '上海市', code: '310000',
    children: [
      { name: '上海市', code: '310100', children: [
        { name: '浦东新区', code: '310115' }, { name: '黄浦区', code: '310101' },
        { name: '徐汇区', code: '310104' },
      ]},
    ],
  },
  {
    name: '广东省', code: '440000',
    children: [
      { name: '广州市', code: '440100', children: [
        { name: '天河区', code: '440106' }, { name: '越秀区', code: '440104' },
      ]},
      { name: '深圳市', code: '440300', children: [
        { name: '南山区', code: '440305' }, { name: '福田区', code: '440304' },
      ]},
    ],
  },
];

const LABELS = ['家', '公司', '学校'];

Page({
  data: {
    id: null,
    isEdit: false,
    form: {
      consignee: '',
      phone: '',
      sex: '1',
      provinceName: '',
      cityName: '',
      districtName: '',
      detail: '',
      label: '',
      isDefault: false,
    },
    regionColumns: [[], [], []],
    regionIndexes: [0, 0, 0],
    showRegionPicker: false,
    labels: LABELS,
    saving: false,
  },

  async onLoad(options) {
    // Build the province column.
    const col0 = REGION.map((r) => r.name);
    this.setData({ regionColumns: [col0, [], []] });

    if (options && options.id) {
      this.setData({ id: Number(options.id), isEdit: true });
      wx.setNavigationBarTitle({ title: '编辑地址' });
      await this.loadAddress(this.data.id);
    }
  },

  async loadAddress(id) {
    try {
      const a = await http.get(`/user/addressBook/${id}`, { silent: true });
      if (!a) return;
      const form = {
        consignee: a.consignee || '',
        phone: a.phone || '',
        sex: a.sex || '1',
        provinceName: a.provinceName || '',
        cityName: a.cityName || '',
        districtName: a.districtName || '',
        detail: a.detail || '',
        label: a.label || '',
        isDefault: a.isDefault === 1,
      };
      this.setData({ form });
      this.syncRegionIndexes(form);
    } catch (e) {
      // toast already shown
    }
  },

  // Reflect loaded region names into the picker's selected indexes + cascaded columns.
  syncRegionIndexes(form) {
    const regionColumns = this.data.regionColumns;
    let idx = [0, 0, 0];
    const pIdx = REGION.findIndex((p) => p.name === form.provinceName);
    if (pIdx >= 0) {
      idx[0] = pIdx;
      const cities = (REGION[pIdx].children || []).map((c) => c.name);
      regionColumns[1] = cities;
      const cIdx = cities.indexOf(form.cityName);
      if (cIdx >= 0) {
        idx[1] = cIdx;
        const districts = (REGION[pIdx].children[cIdx].children || []).map((d) => d.name);
        regionColumns[2] = districts;
        const dIdx = districts.indexOf(form.districtName);
        if (dIdx >= 0) idx[2] = dIdx;
      }
    }
    this.setData({ regionColumns, regionIndexes: idx });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onSexChange(e) {
    this.setData({ 'form.sex': e.detail.value });
  },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': e.detail.value });
  },

  onLabelTap(e) {
    const label = e.currentTarget.dataset.label;
    this.setData({ 'form.label': label });
  },

  onRegionTap() {
    this.setData({ showRegionPicker: true });
  },

  onRegionClose() {
    this.setData({ showRegionPicker: false });
  },

  onRegionChange(e) {
    const { columnIndex, index } = e.detail;
    const regionColumns = this.data.regionColumns;
    const idx = this.data.regionIndexes;

    idx[columnIndex] = index;

    // Cascade: when province changes, rebuild city + district columns.
    if (columnIndex === 0) {
      const cities = (REGION[index].children || []).map((c) => c.name);
      regionColumns[1] = cities;
      regionColumns[2] = [];
      idx[1] = 0;
      idx[2] = 0;
    } else if (columnIndex === 1) {
      const pIdx = idx[0];
      const districts = (REGION[pIdx].children[index].children || []).map((d) => d.name);
      regionColumns[2] = districts;
      idx[2] = 0;
    }

    this.setData({ regionColumns, regionIndexes: idx });

    // Write selected names into the form.
    const provinceName = regionColumns[0][idx[0]] || '';
    const cityName = regionColumns[1][idx[1]] || '';
    const districtName = regionColumns[2][idx[2]] || '';
    this.setData({
      'form.provinceName': provinceName,
      'form.cityName': cityName,
      'form.districtName': districtName,
    });
  },

  validate() {
    const f = this.data.form;
    if (!f.consignee.trim()) return '请填写收货人姓名';
    if (!/^1\d{10}$/.test(f.phone)) return '请填写正确的手机号';
    if (!f.provinceName || !f.cityName || !f.districtName) return '请选择所在地区';
    if (!f.detail.trim()) return '请填写详细地址';
    return '';
  },

  async onSave() {
    if (this.data.saving) return;
    const msg = this.validate();
    if (msg) {
      wx.showToast({ title: msg, icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      const f = this.data.form;
      const payload = {
        consignee: f.consignee.trim(),
        phone: f.phone.trim(),
        sex: f.sex,
        provinceName: f.provinceName,
        cityName: f.cityName,
        districtName: f.districtName,
        detail: f.detail.trim(),
        label: f.label,
        isDefault: f.isDefault ? 1 : 0,
      };

      if (this.data.isEdit) {
        payload.id = this.data.id;
        await http.put('/user/addressBook', payload);
      } else {
        await http.post('/user/addressBook', payload);
      }

      wx.showToast({
        title: this.data.isEdit ? '已保存' : '已添加',
        icon: 'success',
      });
      setTimeout(() => wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/profile/profile' }) }), 600);
    } catch (e) {
      // toast already shown
    } finally {
      this.setData({ saving: false });
    }
  },

  onBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.switchTab({ url: '/pages/profile/profile' }) });
  },
});
