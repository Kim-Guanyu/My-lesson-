const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    age: '',
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const age = options.age || user.age || '';
    this.setData({user, age});
  },

  onAgeChange(e) {
    this.setData({age: e.detail});
  },

  submit() {
    const {user, age, saving} = this.data;
    if (saving) return;
    const ageValue = Number(age);
    if (!Number.isInteger(ageValue) || ageValue < 16 || ageValue > 60) {
      util.tip('年龄必须在16~60之间');
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email: user.email,
      gender: user.gender,
      age: ageValue,
      zodiac: user.zodiac,
      province: user.province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, age: ageValue};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
