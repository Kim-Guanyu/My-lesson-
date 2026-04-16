const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    phone: '17766541438',
    vcode: ''
  },

  getVcode() {
    const that = this;
    const phone = this.data.phone;
    if (util.isEmpty(phone)) {
      util.tip('手机号码不能为空');
      return;
    }
    if (!constant.RULE.PHONE[0].pattern.test(phone)) {
      util.tip(constant.RULE.PHONE[0].message);
      return;
    }
    api.get('user', '/getVcode/' + phone).then(res => {
      util.success('验证码获取成功');
      that.setData({vcode: res});
    }).catch(err => console.error(err));
  },

  loginByPhone() {
    const phone = this.data.phone;
    const vcode = this.data.vcode;
    if (util.hasEmpty(phone, vcode)) {
      util.tip('手机号码或验证码不能为空');
      return;
    }
    if (!constant.RULE.PHONE[0].pattern.test(phone)) {
      util.tip(constant.RULE.PHONE[0].message);
      return;
    }
    if (!constant.RULE.VCODE[0].pattern.test(vcode)) {
      util.tip(constant.RULE.VCODE[0].message);
      return;
    }
    api.post('user', '/loginByPhone', {phone, vcode}).then(res => {
      wx.setStorageSync('token', res.token);
      wx.setStorageSync('user', res.user);
      util.success('登录成功');
      setTimeout(() => util.tab('/pages/index/index'), 500);
    }).catch(err => console.error(err));
  },

  toRegister() {
    util.page('/pages/index/register/register', false);
  },

  toLoginByAccount() {
    util.page('/pages/index/login-by-account/login-by-account', false);
  },

  toIndex() {
    util.tab('/pages/index/index');
  },

  onLoad() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('user');
  }
});
