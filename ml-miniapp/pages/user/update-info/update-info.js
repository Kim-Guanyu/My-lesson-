const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    info: '',
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const info = options.info || user.info || '';
    this.setData({user, info});
  },

  onInfoChange(e) {
    this.setData({info: e.detail});
  },

  submit() {
    const {user, info, saving} = this.data;
    if (saving) return;
    if (!constant.RULE.INFO[0].pattern.test(info)) {
      util.tip(constant.RULE.INFO[0].message);
      return;
    }
    const payload = {
      id: user.id,
      nickname: user.nickname,
      email: user.email,
      gender: user.gender,
      age: user.age,
      zodiac: user.zodiac,
      province: user.province,
      info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, info};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
