const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');

Page({
  data: {
    isLogin: false,
    courses: [],
    pageInfo: {pageNum: 1, pageSize: 12, totalPage: 0},
    loading: false,
    MINIO_COURSE_COVER: constant.MINIO_COURSE_COVER
  },

  formatCourse(item) {
    item.boughtText = util.datetimeFormat(item.created);
    return item;
  },

  loadCourses(reset) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      this.setData({isLogin: false, courses: [], loading: false});
      return;
    }
    this.setData({isLogin: true});
    const pageNum = reset ? 1 : this.data.pageInfo.pageNum;
    const pageSize = this.data.pageInfo.pageSize;
    this.setData({loading: true});
    api.get('orderDetail', '/myCourses', {pageNum, pageSize, fkUserId: user.id}).then(res => {
      const records = (res.records || []).map(c => this.formatCourse(c));
      this.setData({
        courses: pageNum === 1 ? records : this.data.courses.concat(records),
        'pageInfo.pageNum': res.pageNum || pageNum,
        'pageInfo.totalPage': res.totalPage || 0,
        loading: false
      });
      if (reset) wx.stopPullDownRefresh();
    }).catch(() => {
      this.setData({loading: false});
      if (reset) wx.stopPullDownRefresh();
    });
  },

  pageMore() {
    const {pageNum, totalPage} = this.data.pageInfo;
    if (this.data.loading || pageNum >= totalPage) return;
    this.setData({'pageInfo.pageNum': pageNum + 1}, () => this.loadCourses(false));
  },

  toLogin() {
    util.page('/pages/index/login-by-account/login-by-account', true);
  },

  showDetail(ev) {
    const courseId = ev.currentTarget.dataset.courseId;
    util.page('/pages/course/detail/detail?courseId=' + courseId, false);
  },

  onLoad() {
    this.loadCourses(true);
  },

  onShow() {
    this.loadCourses(true);
  },

  onPullDownRefresh() {
    this.loadCourses(true);
  }
});
