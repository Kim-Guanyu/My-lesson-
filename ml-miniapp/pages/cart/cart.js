const util = require('../../utils/util.js');
const api = require('../../utils/api.js');
const constant = require('../../utils/const.js');

function arrayBufferToBase64(buffer) {
  if (wx.arrayBufferToBase64) return wx.arrayBufferToBase64(buffer);
  let binary = '';
  const bytes = new Uint8Array(buffer);
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) binary += String.fromCharCode(bytes[i]);
  return wx.base64Encode ? wx.base64Encode(binary) : binary;
}

Page({
  data: {
    couponsCode: '',
    coupons: {},
    totalAmount: 0,
    payAmount: 0,
    couponsShow: false,
    carts: null,
    courseIdAndPrice: {},
    courseIds: [],
    MINIO_COURSE_COVER: constant.MINIO_COURSE_COVER,
    pageInfo: {pageNum: 1, pageSize: 8, totalPage: 0, totalRow: 0},
    payDialogShow: false,
    time: 15 * 60 * 1000,
    timeData: {},
    countDownShow: false,
    qrCodeImage: '',
    sn: ''
  },

  loadCartPage() {
    const that = this;
    let pageNum = that.data.pageInfo.pageNum;
    let pageSize = that.data.pageInfo.pageSize;
    if (util.isNull(pageNum)) pageNum = 1;
    if (util.isNull(pageSize)) pageSize = 8;

    const user = wx.getStorageSync('user');
    if (!user || !user.id) return;

    const params = {pageNum, pageSize, fkUserId: user.id};
    api.get('cart', '/page', params).then(res => {
      that.setData({
        carts: pageNum === 1 ? res.records : that.data.carts.concat(res.records),
        'pageInfo.pageNum': res.pageNumber,
        'pageInfo.pageSize': res.pageSize,
        'pageInfo.totalPage': res.totalPage,
        'pageInfo.totalRow': res.totalRow
      });
      const courseIdAndPrice = {};
      for (let i in that.data.carts) {
        const cart = that.data.carts[i];
        courseIdAndPrice[cart.fkCourseId] = cart.coursePrice;
      }
      that.setData({courseIdAndPrice});
    }).catch(err => console.error(err));
  },

  pageMore() {
    const {pageNum, totalPage} = this.data.pageInfo;
    if (pageNum < totalPage) {
      this.setData({'pageInfo.pageNum': pageNum + 1}, () => this.loadCartPage());
    }
  },

  onCouponsCodeChange(e) {
    const v = e.detail;
    this.setData({couponsCode: typeof v === 'string' ? v : (v && v.value) || ''});
  },

  getCoupons(ev) {
    const that = this;
    let code = typeof ev.detail === 'string' ? ev.detail : (ev.detail && ev.detail.value);
    if (!code) code = that.data.couponsCode;
    if (!code) return;
    if (!constant.RULE.CODE[0].pattern.test(code)) {
      util.tip(constant.RULE.CODE[0].message);
      return;
    }
    const url = '/selectByCode/' + code;
    api.get('coupons', url).then(res => {
      if (util.isNull(res)) {
        util.error('优惠卷无效');
        return;
      }
      res.endTime = util.dateFormat(res.endTime);
      res.startTime = util.dateFormat(res.startTime);
      res.created = util.dateFormat(res.created);
      res.updated = util.dateFormat(res.updated);

      if (res.cpPrice > that.data.totalAmount) {
        that.setData({payAmount: 0});
      } else {
        that.setData({payAmount: that.data.totalAmount - res.cpPrice});
      }

      that.setData({
        couponsCode: '',
        coupons: res,
        couponsShow: true
      });
      util.success('优惠卷已生效');
    }).catch(err => console.log(err));
  },

  openCouponsSheet() {
    this.setData({couponsShow: true});
  },

  closeCouponsSheet() {
    this.setData({couponsShow: false});
  },

  removeCart(ev) {
    const that = this;
    const id = ev.currentTarget.dataset.id;
    util.confirm('课程将被移出购物车，确定吗？', () => {
      api.del('cart', '/delete/' + id).then(() => {
        util.success('移出成功');
        that.setData({carts: null, 'pageInfo.pageNum': 1});
        that.loadCartPage();
        that.getTotalAmount();
      }).catch(err => console.log(err));
    });
  },

  clearCart() {
    const that = this;
    util.confirm('清空购物车中的全部课程，确定吗？', () => {
      const url = '/clearByUserId/' + wx.getStorageSync('user').id;
      api.del('cart', url).then(() => {
        util.success('清空成功');
        that.setData({
          carts: null,
          totalAmount: 0,
          payAmount: 0,
          coupons: {},
          courseIds: []
        });
      }).catch(err => console.log(err));
    });
  },

  openPayDialog() {
    const that = this;
    if (this.data.courseIds.length <= 0) {
      util.error('至少选择一项');
      return;
    }
    const fkCouponsId = (this.data.coupons && this.data.coupons.id) ? this.data.coupons.id : null;
    const params = {
      fkUserId: wx.getStorageSync('user').id,
      courseIds: this.data.courseIds,
      totalAmount: this.data.totalAmount,
      payAmount: this.data.payAmount,
      fkCouponsId
    };
    api.post('order', '/prePay', params).then(sn => {
      wx.request({
        url: constant.GATEWAY_HOST + '/order-server/api/v1/order/getQrCode',
        method: 'POST',
        data: {sn, payAmount: that.data.payAmount},
        header: {token: wx.getStorageSync('token')},
        responseType: 'arraybuffer',
        success(res) {
          if (res.statusCode !== 200) {
            util.error('获取二维码失败');
            return;
          }
          that.setData({
            sn,
            qrCodeImage: 'data:image/png;base64,' + arrayBufferToBase64(res.data),
            payDialogShow: true,
            countDownShow: true
          });
          if (that._payTimer) clearInterval(that._payTimer);
          that._payTimer = setInterval(() => {
            api.get('order', '/checkStatusBySn/' + sn).then(ok => {
              if (ok === true) {
                util.success('支付成功');
                clearInterval(that._payTimer);
                that._payTimer = null;
                that.setData({payDialogShow: false, countDownShow: false});
                util.page('/pages/user/order/order');
              }
            }).catch(() => {});
          }, 2000);
        }
      });
    }).catch(err => console.log(err));
  },

  cancelPay() {
    util.success('取消支付成功');
    if (this._payTimer) {
      clearInterval(this._payTimer);
      this._payTimer = null;
    }
    this.setData({
      payDialogShow: false,
      countDownShow: false
    });
    util.page('/pages/user/order/order');
  },

  countDown(ev) {
    if (this.data.countDownShow) {
      this.setData({timeData: ev.detail});
    }
  },

  onCountDownFinish() {
    util.error('支付超时');
    if (this._payTimer) {
      clearInterval(this._payTimer);
      this._payTimer = null;
    }
    this.setData({
      payDialogShow: false,
      countDownShow: false
    });
  },

  getTotalAmount() {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) return;
    const param = '/totalAmountByUserId/' + user.id;
    api.get('cart', param).then(res => {
      this.setData({
        totalAmount: res,
        payAmount: res
      });
    }).catch(err => {
      console.error(err);
      util.error('总金额查询失败');
    });
  },

  showDetail(ev) {
    const courseId = ev.currentTarget.dataset.courseId;
    util.page('/pages/course/detail/detail?courseId=' + courseId, false);
  },

  changeCourseIds(ev) {
    const courseIds = ev.detail;
    this.setData({courseIds});
    const courseIdAndPrice = this.data.courseIdAndPrice;
    let payAmount = 0;
    let totalAmount = 0;
    for (let i in courseIds) {
      const courseId = courseIds[i];
      const p = courseIdAndPrice[courseId] || 0;
      payAmount += p;
      totalAmount += p;
    }
    this.setData({payAmount, totalAmount});
  },

  onLoad() {
    if (util.isLogin()) {
      this.setData({'pageInfo.pageNum': 1, carts: null});
      this.loadCartPage();
      this.getTotalAmount();
    }
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({activeTab: 2});
    }
  },

  onShow() {
    if (wx.getStorageSync('token')) {
      this.setData({'pageInfo.pageNum': 1, carts: null});
      this.loadCartPage();
      this.getTotalAmount();
    }
  },

  onUnload() {
    if (this._payTimer) clearInterval(this._payTimer);
  }
});
