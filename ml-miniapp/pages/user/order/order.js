const api = require('../../../utils/api.js');
const constant = require('../../../utils/const.js');
const util = require('../../../utils/util.js');
const pay = require('../../../utils/pay.js');

const STATUS_TABS = [
  {name: 'all', title: '全部', status: null},
  {name: 'unpaid', title: '待付款', status: 0},
  {name: 'paid', title: '已付款', status: 1},
  {name: 'cancel', title: '已取消', status: 2}
];

Page({
  data: {
    isLogin: false,
    orders: [],
    statusTabs: STATUS_TABS,
    activeTab: 'all',
    currentStatus: null,
    pageInfo: {pageNum: 1, pageSize: 10, totalPage: 0},
    loading: false,
    MINIO: constant.MINIO_COURSE_COVER,
    payDialogShow: false,
    time: 15 * 60 * 1000,
    timeData: {},
    countDownShow: false,
    qrCodeImage: '',
    sn: ''
  },

  formatOrder(order) {
    order.statusText = pay.ORDER_STATUS[order.status] || '异常';
    order.payTypeText = pay.PAY_TYPE[order.payType] || '异常';
    order.createdText = util.datetimeFormat(order.created);
    order.payAmountText = (order.payAmount != null ? order.payAmount : 0).toFixed(2);
    order.totalAmountText = (order.totalAmount != null ? order.totalAmount : 0).toFixed(2);
    if (order.info && order.info.indexOf('秒杀') >= 0) {
      order.orderType = '秒杀订单';
    } else {
      order.orderType = '购买订单';
    }
    return order;
  },

  loadOrders(reset) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      this.setData({isLogin: false, orders: [], loading: false});
      return;
    }
    this.setData({isLogin: true});
    const pageNum = reset ? 1 : this.data.pageInfo.pageNum;
    const pageSize = this.data.pageInfo.pageSize;
    const params = {pageNum, pageSize, fkUserId: user.id};
    if (this.data.currentStatus != null) {
      params.status = this.data.currentStatus;
    }
    this.setData({loading: true});
    api.get('order', '/myPage', params).then(res => {
      const records = (res.records || []).map(o => this.formatOrder(o));
      this.setData({
        orders: pageNum === 1 ? records : this.data.orders.concat(records),
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

  onTabChange(ev) {
    const name = ev.detail.name;
    const tab = STATUS_TABS.find(t => t.name === name);
    if (!tab) return;
    this.setData({
      activeTab: name,
      currentStatus: tab.status,
      orders: [],
      'pageInfo.pageNum': 1
    }, () => this.loadOrders(true));
  },

  pageMore() {
    const {pageNum, totalPage} = this.data.pageInfo;
    if (this.data.loading || pageNum >= totalPage) return;
    this.setData({'pageInfo.pageNum': pageNum + 1}, () => this.loadOrders(false));
  },

  toLogin() {
    util.page('/pages/index/login-by-account/login-by-account', true);
  },

  continuePay(ev) {
    const sn = ev.currentTarget.dataset.sn;
    pay.openPayDialog(this, sn, {
      onSuccess: () => this.loadOrders(true)
    });
  },

  cancelPay() {
    pay.cancelPay(this);
  },

  countDown(ev) {
    if (this.data.countDownShow) {
      this.setData({timeData: ev.detail});
    }
  },

  onCountDownFinish() {
    pay.onCountDownFinish(this);
  },

  onLoad() {
    this.loadOrders(true);
  },

  onShow() {
    this.loadOrders(true);
  },

  onPullDownRefresh() {
    this.loadOrders(true);
  },

  onUnload() {
    pay.onUnload(this);
  }
});
