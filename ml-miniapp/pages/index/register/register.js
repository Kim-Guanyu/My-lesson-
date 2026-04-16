const util = require('../../../utils/util.js');
const api = require('../../../utils/api.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    username: '',
    password: '',
    rePassword: '',
    realname: '',
    phone: '',
    idcard: '',
    email: ''
  },

  register() {
    const {username, password, rePassword, realname, phone, idcard, email} = this.data;
    if (password !== rePassword) {
      util.tip('两次密码不一致');
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
    if (!constant.RULE.REALNAME[0].pattern.test(realname)) {
      util.tip(constant.RULE.REALNAME[0].message);
      return;
    }
    if (!constant.RULE.PHONE[0].pattern.test(phone)) {
      util.tip(constant.RULE.PHONE[0].message);
      return;
    }
    if (!constant.RULE.IDCARD[0].pattern.test(idcard)) {
      util.tip(constant.RULE.IDCARD[0].message);
      return;
    }
    if (!constant.RULE.EMAIL[0].pattern.test(email)) {
      util.tip(constant.RULE.EMAIL[0].message);
      return;
    }
    api.post('user', '/insert', {username, password, realname, phone, idcard, email}).then(() => {
      util.success('注册成功');
      setTimeout(() => {
        util.page('/pages/index/login-by-account/login-by-account', false);
      }, 500);
    }).catch(err => console.error(err));
  },

  toLoginByAccount() {
    util.page('/pages/index/login-by-account/login-by-account', false);
  },

  toLoginByPhone() {
    util.page('/pages/index/login-by-phone/login-by-phone', false);
  },

  toIndex() {
    util.tab('/pages/index/index');
  },

  onLoad() {}
});
