const api = require('../../utils/api.js');
const constant = require('../../utils/const.js');
const util = require('../../utils/util.js');

Page({
  data: {
    isLogin: false,
    currentNotice: '',
    PROJECT_TITLE: constant.PROJECT_TITLE,
    PROJECT_SUB_TITLE: constant.PROJECT_SUB_TITLE,
    MINIO_BANNER: constant.MINIO_BANNER,
    banners: null,
    currentArticleIdx: '1',
    articles: null,
    seckills: null,
    activeSeckillIdx: 0,
    MINIO: constant.MINIO_COURSE_COVER
  },

  toLogin() {
    util.page('/pages/index/login-by-account/login-by-account', true);
  },

  topNotice1() {
    const that = this;
    api.get('notice', '/top/1')
      .then(res => {
        let text = '';
        if (Array.isArray(res) && res[0]) text = res[0].content || '';
        else if (res && res.content) text = res.content;
        that.setData({currentNotice: text || '暂无通知'});
      })
      .catch(err => console.error(err));
  },

  topBanner5() {
    const that = this;
    api.get('banner', '/top/5')
      .then(res => that.setData({banners: res}))
      .catch(err => console.error(err));
  },

  topArticle5() {
    const that = this;
    api.get('article', '/top/5')
      .then(res => {
        const list = (Array.isArray(res) ? res : []).map((item, i) => ({
          ...item,
          idx: String(item.id != null ? item.id : i)
        }));
        that.setData({articles: list});
      })
      .catch(err => console.error(err));
  },

  todaySeckill() {
    const that = this;
    api.get('seckill', '/today')
      .then(res => that.setData({seckills: res}))
      .catch(err => console.error(err));
  },

  changeArticle(ev) {
    this.setData({currentArticleIdx: ev.detail});
  },

  changeSeckill(ev) {
    const d = ev.detail;
    const idx = typeof d === 'number' ? d : (d && d.index != null ? d.index : 0);
    this.setData({activeSeckillIdx: idx});
  },

  onLoad() {
    this.setData({isLogin: !wx.getStorageSync('token')});
    this.topNotice1();
    this.topBanner5();
    this.topArticle5();
    this.todaySeckill();
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({activeTab: 0});
    }
  },

  onShow() {
    this.setData({isLogin: !wx.getStorageSync('token')});
  }
});
