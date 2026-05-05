const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');

Page({
  data: {
    user: null,
    gender: '2',
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const gender = options.gender ?? user.gender ?? 2;
    this.setData({user, gender: String(gender)});
  },

  onGenderChange(e) {
    this.setData({gender: e.detail});
  },

  onGenderTap(e) {
    this.setData({gender: String(e.currentTarget.dataset.value)});
  },

  submit() {
    const {user, gender, saving} = this.data;
    if (saving) return;
    const genderValue = Number(gender);
    if (![0, 1, 2].includes(genderValue)) {
      util.tip('性别代码必须在0~2之间');
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email: user.email,
      gender: genderValue,
      age: user.age,
      zodiac: user.zodiac,
      province: user.province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, gender: genderValue};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
