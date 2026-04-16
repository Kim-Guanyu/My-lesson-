const api = require('../../utils/api.js');
const util = require('../../utils/util.js');
const constant = require('../../utils/const.js');

Page({
  data: {
    user: null,
    MINIO_AVATAR: constant.MINIO_AVATAR,
    avatarFile: [],
    NICKNAME_UPDATE_URL: '/pages/user/update-nickname/update-nickname?nickname=',
    GENDER_UPDATE_URL: '/pages/user/update-gender/update-gender?gender=',
    AGE_UPDATE_URL: '/pages/user/update-age/update-age?age=',
    ZODIAC_UPDATE_URL: '/pages/user/update-zodiac/update-zodiac?zodiac=',
    PROVINCE_UPDATE_URL: '/pages/user/update-province/update-province?province=',
    EMAIL_UPDATE_URL: '/pages/user/update-email/update-email?email=',
    INFO_UPDATE_URL: '/pages/user/update-info/update-info?info='
  },

  getInfo() {
    const that = this;
    const u = wx.getStorageSync('user');
    if (!u || !u.id) return;
    const url = '/select/' + u.id;
    api.get('user', url).then(res => {
      that.setData({user: res});
    }).catch(err => console.log(err));
  },

  uploadAvatar(ev) {
    const {file} = ev.detail;
    if (file.type !== 'image') {
      util.error('图片格式有误');
      return false;
    }
    if (file.size > 500 * 1024) {
      util.error('图片过大');
      return false;
    }
    const user = wx.getStorageSync('user');
    wx.uploadFile({
      url: constant.UPLOAD_AVATAR_URL + user.id,
      filePath: file.url,
      name: 'avatarFile',
      header: {
        'Content-Type': 'multipart/form-data',
        token: wx.getStorageSync('token')
      },
      success: () => {
        util.success('上传成功');
        this.getInfo();
        util.tab('/pages/user/user');
      },
      fail: err => console.log(err)
    });
  },

  logout() {
    util.confirm('即将退出登录，确认吗？', () => {
      wx.removeStorageSync('token');
      wx.removeStorageSync('user');
      util.page('/pages/index/login-by-account/login-by-account', false);
    });
  },

  onLoad() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({activeTab: 3});
    }
    if (util.isLogin()) {
      this.getInfo();
    }
  },

  onShow() {
    if (wx.getStorageSync('token')) {
      this.getInfo();
    }
  }
});
