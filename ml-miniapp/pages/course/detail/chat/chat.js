const util = require('../../../../utils/util.js');

Page({
  data: {question: ''},
  onQ(e) {
    const v = e.detail;
    this.setData({question: typeof v === 'string' ? v : (v && v.value) || ''});
  },
  ask() {
    util.tip('请按教程对接智能客服接口');
  }
});
