const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');
const pay = require('../../../utils/pay.js');

Page({
  data: {
    MINIO_COURSE_SUMMARY: constant.MINIO_COURSE_SUMMARY,
    course: null,
    videoSrc: null,
    videoPoster: null,
    videoTitle: null,
    activeTab: 0,
    danmuList: [],
    activeDanmu: [],
    danmuText: '',
    danmuColor: '#ffffff',
    barrageEpisodeId: null,
    currentTime: 0,
    wsConnected: false,
    courseId: null,
    legacyEpisodeId: null,
    barrageHistoryLoaded: false,
    videoReady: false,
    videoInstanceId: 0,
    payDialogShow: false,
    time: 15 * 60 * 1000,
    timeData: {},
    countDownShow: false,
    qrCodeImage: '',
    sn: ''
  },

  resolveBarrageIds(course, courseId) {
    if (!course) {
      return {barrageEpisodeId: String(courseId || ''), legacyEpisodeId: null};
    }
    const seasons = course.seasons || [];
    const firstSeason = seasons.length ? seasons[0] : null;
    const episodes = firstSeason ? (firstSeason.episodes || []) : [];
    const firstEpisodeId = episodes.length && episodes[0].id ? String(episodes[0].id) : '';
    const courseIdStr = String(courseId || course.id || '');
    const barrageEpisodeId = firstEpisodeId || courseIdStr;
    const legacyEpisodeId = firstEpisodeId && courseIdStr && courseIdStr !== barrageEpisodeId
      ? courseIdStr
      : null;
    return {barrageEpisodeId, legacyEpisodeId};
  },

  getCourseInfo(courseId) {
    const that = this;
    api.get('course', '/select/' + courseId).then(res => {
      res.created = util.dateFormat(res.created);
      res.updated = util.dateFormat(res.updated);
      const videoPoster = null;
      const videoTitle = res.title || '课程视频';
      const ids = that.resolveBarrageIds(res, courseId);
      that._pendingVideoSrc = constant.MINIO_VIDEO + 'lesson.mp4';
      that.setData({
        courseId: String(courseId),
        course: res,
        videoPoster,
        videoTitle,
        barrageEpisodeId: ids.barrageEpisodeId,
        legacyEpisodeId: ids.legacyEpisodeId,
        danmuList: [],
        activeDanmu: [],
        videoReady: false,
        barrageHistoryLoaded: false
      }, () => that.mountVideoWithBarrage());
    }).catch(err => console.error(err));
  },

  onTabChange(ev) {
    this.setData({activeTab: ev.detail});
  },

  danmuKey(item) {
    return `${item.text}|${item.color}|${item.time}`;
  },

  resetDanmuPlayback(fromTime) {
    this._playedDanmuKeys = new Set();
    const list = this.data.danmuList || [];
    list.forEach(item => {
      if (item.time < fromTime - 0.3) {
        this._playedDanmuKeys.add(this.danmuKey(item));
      }
    });
    this.setData({activeDanmu: []});
  },

  buildFlyingDanmu(item) {
    return {
      id: `${Date.now()}_${Math.random()}`,
      text: item.text,
      color: item.color || '#ffffff',
      top: 10 + Math.floor(Math.random() * 55)
    };
  },

  pushActiveDanmu(items) {
    if (!items || !items.length) return;
    const activeDanmu = (this.data.activeDanmu || []).concat(items);
    this.setData({activeDanmu});
    items.forEach(item => {
      setTimeout(() => {
        const next = (this.data.activeDanmu || []).filter(d => d.id !== item.id);
        if (next.length !== (this.data.activeDanmu || []).length) {
          this.setData({activeDanmu: next});
        }
      }, 8000);
    });
  },

  showLiveDanmu(text, color) {
    this.pushActiveDanmu([this.buildFlyingDanmu({text, color: color || '#ffffff', time: 0})]);
  },

  tickDanmu(currentTime) {
    const list = this.data.danmuList || [];
    if (!list.length) return;
    if (!this._playedDanmuKeys) this._playedDanmuKeys = new Set();
    const window = 0.4;
    const newly = [];
    list.forEach(item => {
      const key = this.danmuKey(item);
      if (item.time <= currentTime && item.time > currentTime - window && !this._playedDanmuKeys.has(key)) {
        this._playedDanmuKeys.add(key);
        newly.push(this.buildFlyingDanmu(item));
      }
    });
    this.pushActiveDanmu(newly);
  },

  onVideoTimeUpdate(ev) {
    if (!ev || !ev.detail || typeof ev.detail.currentTime !== 'number') return;
    const currentTime = ev.detail.currentTime;
    this.setData({currentTime});
    this.tickDanmu(currentTime);
  },

  onVideoSeekComplete(ev) {
    const currentTime = (ev && ev.detail && ev.detail.currentTime) || this.data.currentTime || 0;
    this.resetDanmuPlayback(currentTime);
    this.tickDanmu(currentTime);
  },

  onVideoError(ev) {
    console.error('[barrage] video error', ev.detail);
  },

  onDanmuInput(ev) {
    this.setData({danmuText: ev.detail.value || ''});
  },

  normalizeDanmuList(list) {
    const seen = new Set();
    return (list || []).map(item => ({
      text: item.text || '',
      color: item.color || '#ffffff',
      time: Number(item.time) || 0
    })).filter(item => {
      const key = `${item.text}|${item.color}|${item.time}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    }).sort((a, b) => a.time - b.time);
  },

  fetchBarrageList() {
    const courseId = this.data.courseId;
    if (!courseId) return Promise.resolve([]);
    return api.get('episode', '/searchBarrage/course/' + courseId)
      .then(list => this.normalizeDanmuList(list))
      .catch(err => {
        console.error('load barrage history failed', err);
        return [];
      });
  },

  getVideoContextId() {
    return 'courseVideo' + (this.data.videoInstanceId || '');
  },

  initVideoContext(seekTime) {
    this.videoContext = wx.createVideoContext(this.getVideoContextId(), this);
    if (seekTime > 0 && this.videoContext) {
      setTimeout(() => {
        if (this.videoContext) this.videoContext.seek(seekTime);
      }, 300);
    }
    this.connectBarrageWs();
  },

  mountVideoWithBarrage(resumeTime) {
    if (this._mounting) return Promise.resolve();
    this._mounting = true;
    const baseSrc = (this._pendingVideoSrc || constant.MINIO_VIDEO + 'lesson.mp4').split('?')[0];
    this._pendingVideoSrc = baseSrc;
    this._mountSeq = (this._mountSeq || 0) + 1;
    const seq = this._mountSeq;
    const seekTime = typeof resumeTime === 'number' ? resumeTime : 0;

    return this.fetchBarrageList().then(danmuList => {
      if (seq !== this._mountSeq) return;
      console.log('[barrage] mount video with danmu count:', danmuList.length);
      this._playedDanmuKeys = new Set();
      const videoInstanceId = Date.now();
      return new Promise(resolve => {
        this.setData({
          danmuList,
          activeDanmu: [],
          videoInstanceId,
          videoSrc: `${baseSrc}?v=${videoInstanceId}`,
          videoReady: true,
          barrageHistoryLoaded: true
        }, () => {
          this.initVideoContext(seekTime);
          this._mounting = false;
          resolve();
        });
      });
    }).catch(err => {
      this._mounting = false;
      console.error('[barrage] mount failed', err);
    });
  },

  bindBarrageWsHandlers() {
    if (this._wsHandlersBound) return;
    this._wsHandlersBound = true;
    wx.onSocketOpen(() => {
      this._wsReady = true;
      this._wsConnecting = false;
      this.setData({wsConnected: true});
    });
    wx.onSocketError(() => {
      this._wsReady = false;
      this._wsConnecting = false;
      this.setData({wsConnected: false});
    });
    wx.onSocketClose(() => {
      this._wsReady = false;
      this._wsConnecting = false;
      this.setData({wsConnected: false});
    });
    wx.onSocketMessage(msg => {
      try {
        const data = JSON.parse(msg.data);
        const incomingEpisodeId = String(data.episodeId || '');
        const allowedIds = [this.data.barrageEpisodeId, this.data.legacyEpisodeId, this.data.courseId]
          .filter(id => id)
          .map(id => String(id));
        if (incomingEpisodeId && allowedIds.length && allowedIds.indexOf(incomingEpisodeId) === -1) {
          return;
        }
        this.showLiveDanmu(data.text || '', data.color || '#ffffff');
      } catch (e) {
        console.error(e);
      }
    });
  },

  reconnectBarrageWs() {
    if (this._wsReady || this._wsConnecting) {
      wx.closeSocket();
      this._wsReady = false;
      this._wsConnecting = false;
    }
    this.connectBarrageWs();
  },

  connectBarrageWs() {
    if (this._wsReady || this._wsConnecting) return;
    const user = wx.getStorageSync('user');
    if (!user || !user.id) return;
    this._wsConnecting = true;
    wx.connectSocket({
      url: `${constant.SOCKET_SERVER}/api/v1/barrage/${user.id}`,
      fail: () => {
        this._wsConnecting = false;
        this.setData({wsConnected: false});
      }
    });
  },

  sendDanmu() {
    const text = (this.data.danmuText || '').trim();
    if (!text) {
      util.error('请输入弹幕内容');
      return;
    }
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      util.error('请先登录');
      return;
    }
    if (!this._wsReady) {
      util.error('弹幕服务未连接');
      return;
    }
    const payload = {
      episodeId: String(this.data.barrageEpisodeId || this.data.courseId || ''),
      text,
      color: this.data.danmuColor || '#ffffff',
      time: String(this.data.currentTime || 0)
    };
    wx.sendSocketMessage({data: JSON.stringify(payload)});
    this.setData({danmuText: ''});
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
    const course = this.data.course;
    let url = '/pages/course/detail/chat/chat';
    if (course && course.id) {
      url += '?courseId=' + course.id;
      if (course.title) {
        url += '&courseTitle=' + encodeURIComponent(course.title);
      }
    }
    util.page(url, false);
  },

  pay() {
    const that = this;
    if (!util.isLogin()) {
      util.error('请先登录');
      return;
    }
    const course = this.data.course;
    if (!course || !course.id) {
      util.error('课程信息加载中');
      return;
    }
    const price = Number(course.price) || 0;
    const params = {
      fkUserId: wx.getStorageSync('user').id,
      courseIds: [course.id],
      totalAmount: price,
      payAmount: price
    };
    api.post('order', '/prePay', params).then(sn => {
      pay.openPayDialog(that, sn, {
        onSuccess() {
          util.page('/pages/user/order/order');
        }
      });
    }).catch(err => console.error(err));
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

  onLoad(options) {
    this._mounting = false;
    this._wsHandlersBound = false;
    this._wsReady = false;
    this._wsConnecting = false;
    this._playedDanmuKeys = new Set();
    this.bindBarrageWsHandlers();
    const id = options.courseId;
    if (id) {
      this.setData({courseId: String(id)});
      this.getCourseInfo(String(id));
    }
  },

  onShow() {
    if (!this.data.courseId || !this.data.course) return;
    if (!this.data.videoReady) {
      this.reconnectBarrageWs();
      this.mountVideoWithBarrage(this.data.currentTime || 0);
    }
  },

  onHide() {
    this.setData({videoReady: false, activeDanmu: []});
  },

  onUnload() {
    pay.onUnload(this);
    if (this._wsReady || this._wsConnecting) {
      wx.closeSocket();
    }
  }
});
