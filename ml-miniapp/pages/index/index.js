const api = require('../../utils/api.js');
const constant = require('../../utils/const.js');
const util = require('../../utils/util.js');
const pay = require('../../utils/pay.js');

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
    MINIO: constant.MINIO_COURSE_COVER,
    payDialogShow: false,
    time: 15 * 60 * 1000,
    timeData: {},
    countDownShow: false,
    qrCodeImage: '',
    sn: '',
    killing: false,
    // 本地节流：距离下一次允许发起秒杀请求的时间戳，冷却期内直接拦截，不请求后端
    killCooldownUntil: 0
  },

  toLogin() {
    util.page('/pages/index/login-by-account/login-by-account', true);
  },

  tapBanner(ev) {
    const courseId = ev.currentTarget.dataset.courseId;
    if (!courseId) {
      util.tip('暂无对应课程');
      return;
    }
    util.page('/pages/course/detail/detail?courseId=' + courseId, false);
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
      .then(res => {
        const banners = (Array.isArray(res) ? res : []).map(item => ({
          ...item,
          courseId: item.id
        }));
        that.setData({banners});
      })
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

  // 标记指定秒杀商品为"已抢光/已结束"，之后本页面不再为它发起请求
  markSeckillDetailDone(seckillId, courseId, label) {
    const that = this;
    const seckills = that.data.seckills || [];
    const si = seckills.findIndex(s => s.id === seckillId);
    if (si === -1) return;
    const details = seckills[si].seckillDetails || [];
    const di = details.findIndex(d => d.fkCourseId === courseId);
    if (di === -1) return;
    that.setData({
      [`seckills[${si}].seckillDetails[${di}].soldOut`]: true,
      [`seckills[${si}].seckillDetails[${di}].soldOutLabel`]: label
    });
  },

  doSeckill(ev) {
    const that = this;
    if (!util.isLogin()) return;

    const dataset = ev.currentTarget.dataset;
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      setTimeout(() => util.page('/pages/index/login-by-account/login-by-account', false), 500);
      return;
    }
    if (!dataset.seckillId || !dataset.courseId) {
      util.tip('秒杀参数异常');
      return;
    }
    const seckillId = Number(dataset.seckillId);
    const courseId = Number(dataset.courseId);

    // 该商品在本地已确认售罄/结束，直接拦截，不再打到后端
    if (dataset.soldOut) {
      util.tip('手慢了，该商品已被抢光');
      return;
    }
    // 正在请求中，忽略重复点击
    if (that.data.killing) return;
    // 冷却期内直接拦截，避免用户连续快速点击造成大量无效请求
    const now = Date.now();
    if (now < that.data.killCooldownUntil) {
      util.tip('手速太快啦，请稍后再试');
      return;
    }

    const params = {fkSeckillId: seckillId, fkCourseId: courseId};

    that.setData({
      killing: true,
      killCooldownUntil: now + constant.SECKILL_THROTTLE.COOLDOWN_MS
    });
    wx.showLoading({title: '排队中...', mask: true});

    // 随机错峰延迟：把同一时刻大量客户端的点击打散到一个小窗口内再真正发出请求，
    // 削弱瞬时并发峰值，而不是让所有请求在同一毫秒集中冲击后端
    const jitter = Math.floor(Math.random() * constant.SECKILL_THROTTLE.JITTER_MAX_MS);
    setTimeout(() => {
      wx.showLoading({title: '秒杀中...', mask: true});
      api.post('seckill', '/kill', params).then(sn => {
        wx.hideLoading();
        that.setData({killing: false});
        util.success('秒杀成功');
        pay.openPayDialog(that, sn, {
          onSuccess() {
            wx.navigateTo({url: '/pages/user/order/order'});
          }
        });
      }).catch(err => {
        wx.hideLoading();
        const update = {killing: false};
        if (err && err.code === constant.SECKILL_CODE.STOCK_OUT) {
          that.markSeckillDetailDone(seckillId, courseId, '已抢光');
        } else if (err && err.code === constant.SECKILL_CODE.END) {
          that.markSeckillDetailDone(seckillId, courseId, '已结束');
        } else if (err && err.code === constant.SECKILL_CODE.TOO_FAST) {
          // 后端也判定过快，进一步拉长本地冷却时间，减少无效重试
          update.killCooldownUntil = Date.now() + constant.SECKILL_THROTTLE.TOO_FAST_PENALTY_MS;
        }
        that.setData(update);
        console.error(err);
      });
    }, jitter);
  },

  cancelPay() {
    pay.cancelPay(this, {
      onCancel() {
        util.page('/pages/user/order/order');
      }
    });
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
    this.todaySeckill();
  },

  onUnload() {
    pay.onUnload(this);
  }
});
