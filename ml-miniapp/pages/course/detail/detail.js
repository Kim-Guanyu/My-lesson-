const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    MINIO_COURSE_SUMMARY: constant.MINIO_COURSE_SUMMARY,
    course: null,
    videoSrc: null,
    videoPoster: null,
    videoTitle: null,
    activeTab: 0
  },

  getCourseInfo(courseId) {
    const that = this;
    api.get('course', '/select/' + courseId).then(res => {
      res.created = util.dateFormat(res.created);
      res.updated = util.dateFormat(res.updated);
      if (res.seasons && res.seasons.length > 0) {
        const eps = res.seasons[0].episodes;
        if (eps && eps.length > 0) {
          const firstEpisode = eps[0];
          that.setData({
            videoSrc: constant.MINIO_EPISODE_VIDEO + firstEpisode.video,
            videoPoster: constant.MINIO_EPISODE_VIDEO_COVER + firstEpisode.cover,
            videoTitle: firstEpisode.title
          });
        }
      }
      that.setData({course: res});
    }).catch(err => console.error(err));
  },

  onTabChange(ev) {
    this.setData({activeTab: ev.detail});
  },

  toCart() {
    if (util.isLogin()) {
      util.tab('/pages/cart/cart');
    }
  },

  addToCart() {
    if (!util.isLogin()) return;
    const course = this.data.course;
    if (!course) return;
    const params = {
      fkUserId: wx.getStorageSync('user').id,
      fkCourseId: course.id
    };
    api.post('cart', '/insert', params).then(() => {
      util.success('加购成功');
      setTimeout(() => util.tab('/pages/cart/cart', true), 500);
    }).catch(err => console.error(err));
  },

  chatMe() {
    util.page('/pages/course/detail/chat/chat');
  },

  pay() {
    if (util.isLogin()) {
      util.tip('功能暂未开放');
    }
  },

  onLoad(options) {
    const id = options.courseId;
    if (id) this.getCourseInfo(id);
  }
});
