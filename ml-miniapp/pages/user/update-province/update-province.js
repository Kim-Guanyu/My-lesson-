const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    province: '',
    provinceColumns: constant.PROVINCE_OPTIONS,
    pickerVisible: false,
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const province = options.province || user.province || '';
    this.setData({user, province});
  },

  openPicker() {
    this.setData({pickerVisible: true});
  },

  closePicker() {
    this.setData({pickerVisible: false});
  },

  onPickerConfirm(e) {
    const value = e.detail.value;
    const province = Array.isArray(value) ? (value[1] || value[0]) : value;
    this.setData({province, pickerVisible: false});
  },

  submit() {
    const {user, province, saving} = this.data;
    if (saving) return;
    if (!province) {
      util.tip('请选择省份');
      return;
    }
    if (!constant.RULE.PROVINCE[0].pattern.test(province)) {
      util.tip(constant.RULE.PROVINCE[0].message);
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email: user.email,
      gender: user.gender,
      age: user.age,
      zodiac: user.zodiac,
      province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, province};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
