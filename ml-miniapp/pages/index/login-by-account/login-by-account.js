const util = require('../../../utils/util.js');
const api = require('../../../utils/api.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    username: '',
    password: '',
    showPassword: false
  },

  togglePasswordVisible() {
    this.setData({showPassword: !this.data.showPassword});
  },

  loginByAccount() {
    const username = this.data.username;
    const password = this.data.password;

    if (util.hasEmpty(username, password)) {
      util.tip('账号或密码不能为空');
      return;
    }
    if (!constant.RULE.USERNAME[0].pattern.test(username)) {
      util.tip(constant.RULE.USERNAME[0].message);
      return;
    }
    if (!constant.RULE.PASSWORD[0].pattern.test(password)) {
      util.tip(constant.RULE.PASSWORD[0].message);
      return;
    }

    api.post('user', '/loginByAccount', {username, password}).then(res => {
      wx.setStorageSync('token', res.token);
      wx.setStorageSync('user', res.user);
      util.success('登录成功');
      setTimeout(() => util.tab('/pages/index/index'), 500);
    }).catch(err => console.error(err));
  },

  toRegister() {
    util.page('/pages/index/register/register', false);
  },

  toLoginByPhone() {
    util.page('/pages/index/login-by-phone/login-by-phone', false);
  },

  toIndex() {
    util.tab('/pages/index/index');
  },

  onLoad() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('user');
  }
});
