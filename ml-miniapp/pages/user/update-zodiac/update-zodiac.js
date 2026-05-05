const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    zodiac: '',
    zodiacOptions: constant.ZODIAC_OPTIONS,
    pickerVisible: false,
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const zodiac = options.zodiac || user.zodiac || '';
    this.setData({user, zodiac});
  },

  openPicker() {
    this.setData({pickerVisible: true});
  },

  closePicker() {
    this.setData({pickerVisible: false});
  },

  onPickerConfirm(e) {
    const zodiac = e.detail.value;
    this.setData({zodiac, pickerVisible: false});
  },

  submit() {
    const {user, zodiac, saving} = this.data;
    if (saving) return;
    if (!zodiac) {
      util.tip('请选择星座');
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email: user.email,
      gender: user.gender,
      age: user.age,
      zodiac,
      province: user.province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, zodiac};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
