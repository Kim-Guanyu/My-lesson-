const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    user: null,
    nickname: '',
    saving: false
  },

  onLoad(options) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    const nickname = options.nickname || user.nickname || '';
    this.setData({user, nickname});
  },

  onNicknameChange(e) {
    this.setData({nickname: e.detail});
  },

  submit() {
    const {user, nickname, saving} = this.data;
    if (saving) return;
    if (!constant.RULE.NICKNAME[0].pattern.test(nickname)) {
      util.tip(constant.RULE.NICKNAME[0].message);
      return;
    }
    const payload = {
      id: user.id,
      nickname,
      email: user.email,
      gender: user.gender,
      age: user.age,
      zodiac: user.zodiac,
      province: user.province,
      info: user.info
    };
    this.setData({saving: true});
    api.put('user', '/update', payload).then(() => {
      const updatedUser = {...user, nickname};
      wx.setStorageSync('user', updatedUser);
      util.success('修改成功');
      wx.navigateBack();
    }).catch(err => console.error(err)).finally(() => {
      this.setData({saving: false});
    });
  }
});
