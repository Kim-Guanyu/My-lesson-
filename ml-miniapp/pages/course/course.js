const util = require('../../utils/util.js');
const api = require('../../utils/api.js');
const constant = require('../../utils/const.js');

Page({
  data: {
    MINIO_COURSE_COVER: constant.MINIO_COURSE_COVER,
    pageInfo: {pageNum: 1, pageSize: 12, totalPage: 0, totalRow: 0},
    courses: null,
    keyword: ''
  },

  loadCourses() {
    const that = this;
    let keyword = this.data.keyword;
    let pageNum = this.data.pageInfo.pageNum;
    let pageSize = this.data.pageInfo.pageSize;
    if (util.isNull(pageNum)) pageNum = 1;
    if (util.isNull(pageSize)) pageSize = 12;

    if (keyword.length > 42) {
      util.error('搜索关键字过长');
      return;
    }

    keyword = keyword.trim();
    const params = {pageNum, pageSize, keyword};
    const url = util.isEmpty(keyword) ? '/page' : '/search';
    api.get('course', url, params).then(res => {
      const records = res.records || [];
      const merged = pageNum === 1 ? records : (that.data.courses || []).concat(records);
      that.setData({
        courses: merged,
        'pageInfo.pageNum': res.pageNum,
        'pageInfo.pageSize': res.pageSize,
        'pageInfo.totalPage': res.totalPage,
        'pageInfo.totalRow': res.totalRow
      });
    }).catch(err => console.error(err));
  },

  onListEnd() {
    const {pageNum, totalPage} = this.data.pageInfo;
    if (pageNum < totalPage) {
      const next = pageNum + 1;
      this.setData({'pageInfo.pageNum': next}, () => this.loadCourses());
    }
  },

  searchByKeyword(ev) {
    const kw = typeof ev.detail === 'string' ? ev.detail : (ev.detail && ev.detail.value != null ? ev.detail.value : '');
    this.setData({
      keyword: kw,
      'pageInfo.pageNum': 1
    });
    this.loadCourses();
  },

  cancelSearch() {
    this.setData({
      keyword: '',
      'pageInfo.pageNum': 1
    });
    this.loadCourses();
  },

  showDetail(ev) {
    const courseId = ev.currentTarget.dataset.courseId;
    util.page('/pages/course/detail/detail?courseId=' + courseId, false);
  },

  onLoad() {
    this.loadCourses();
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({activeTab: 1});
    }
  }
});
