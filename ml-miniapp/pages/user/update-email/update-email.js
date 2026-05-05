const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    email: '',
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const email = options.email || user.email || '';
    this.setData({user, email});
  },

  onEmailChange(e) {
    this.setData({email: e.detail});
  },

  submit() {
    const {user, email, saving} = this.data;
    if (saving) return;
    if (!constant.RULE.EMAIL[0].pattern.test(email)) {
      util.tip(constant.RULE.EMAIL[0].message);
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email,
      gender: user.gender,
      age: user.age,
      zodiac: user.zodiac,
      province: user.province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, email};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
