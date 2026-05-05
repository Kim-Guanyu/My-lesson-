const api = require('../../utils/api.js');
const util = require('../../utils/util.js');
const constant = require('../../utils/const.js');

Page({
  data: {
    user: null,
    loading: true,
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

  normalizeUser(rawUser) {
    if (!rawUser) {
      return null;
    }
    const user = {...rawUser};
    if (user.avatarUrl) {
      return user;
    }
    if (user.avatar && /^https?:\/\//i.test(user.avatar)) {
      user.avatarUrl = user.avatar;
      return user;
    }
    const cleanAvatar = user.avatar
      ? String(user.avatar).replace(/^\/+/, '').replace(/^avatar\//i, '')
      : '';
    if (cleanAvatar) {
      const legacyAvatarBase = String(constant.MINIO_AVATAR).replace('/my-lesson/', '/mylesson/');
      user.avatarFileName = cleanAvatar;
      user.avatarUrl = `${constant.MINIO_AVATAR}${cleanAvatar}`;
      user.avatarLegacyUrl = `${legacyAvatarBase}${cleanAvatar}`;
      user.avatarProxyUrl = `${constant.USER_AVATAR_URL}${encodeURIComponent(cleanAvatar)}`;
    } else {
      user.avatarUrl = '';
    }
    return user;
  },

  handleAvatarError() {
    const user = this.data.user;
    if (!user || !user.avatarFileName) {
      return;
    }
    const current = user.avatarUrl || '';
    let next = '';
    if (current === user.avatarUrl && user.avatarLegacyUrl) {
      next = user.avatarLegacyUrl;
    } else if (current === user.avatarLegacyUrl && user.avatarProxyUrl) {
      next = user.avatarProxyUrl;
    }
    if (!next || next === current) {
      return;
    }
    this.setData({user: {...user, avatarUrl: next}});
  },

  applyUser(rawUser) {
    const user = this.normalizeUser(rawUser);
    this.setData({user});
  },

  getInfo() {
    const that = this;
    const u = wx.getStorageSync('user');
    if (!u || !u.id) {
      that.setData({loading: false});
      return;
    }
    that.setData({loading: true});
    const url = '/select/' + u.id;
    api.get('user', url).then(res => {
      that.applyUser(res);
      that.setData({loading: false});
    }).catch(err => {
      console.log(err);
      const cachedUser = wx.getStorageSync('user');
      that.applyUser(cachedUser || null);
      that.setData({loading: false});
    });
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
      const cachedUser = wx.getStorageSync('user');
      this.applyUser(cachedUser || null);
      this.getInfo();
    } else {
      this.setData({loading: false});
    }
  },

  onShow() {
    if (wx.getStorageSync('token')) {
      const cachedUser = wx.getStorageSync('user');
      this.applyUser(cachedUser || null);
      this.getInfo();
    } else {
      this.setData({loading: false, user: null});
    }
  }
});
