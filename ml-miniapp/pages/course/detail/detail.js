const api = require('../../../utils/api.js');
const util = require('../../../utils/util.js');
const constant = require('../../../utils/const.js');
const pay = require('../../../utils/pay.js');

// 与后端 ML.Episode.DEFAULT_VIDEO 及历史库内数据保持一致的占位视频名
const DEFAULT_VIDEO_NAMES = ['default-video.mp4', 'default-episode-video.mp4'];

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
    currentEpisodeId: null,
    currentTime: 0,
    wsConnected: false,
    courseId: null,
    legacyEpisodeId: null,
    barrageHistoryLoaded: false,
    videoReady: false,
    videoInstanceId: 0,
    payDialogShow: false,
    // 是否已购买本课程：未购买只能试看一节，其余章节点击后提示购买
    purchased: false,
    // 未购买用户可试看的那一节（集次ID）与它的展示名，如「第二章第一节」
    freeEpisodeId: null,
    freeEpisodeLabel: '',
    // 打开小精灵那一刻的播放进度快照（秒）
    spriteCurrentTime: 0,
    time: 15 * 60 * 1000,
    timeData: {},
    countDownShow: false,
    qrCodeImage: '',
    sn: ''
  },

  /**
   * 定位起播集次的弹幕作用域。
   *
   * @param startEpisode 实际起播的集次（未购买时是试看那一节，而非首集）
   */
  resolveBarrageIds(course, courseId, startEpisode) {
    if (!course) {
      return {barrageEpisodeId: String(courseId || ''), legacyEpisodeId: null};
    }
    const startEpisodeId = startEpisode && startEpisode.id ? String(startEpisode.id) : '';
    const courseIdStr = String(courseId || course.id || '');
    const barrageEpisodeId = startEpisodeId || courseIdStr;
    // 历史弹幕曾以 courseId 落库，那批数据属于首集；起播的不是首集时不能并进来，
    // 否则会把第一章的弹幕放到别的章节上（playEpisode 切集时同样置空）。
    const firstEpisode = this.firstEpisodeOf(course);
    const isFirstEpisode = !!firstEpisode && !!startEpisodeId
      && String(firstEpisode.id) === startEpisodeId;
    const legacyEpisodeId = isFirstEpisode && courseIdStr && courseIdStr !== barrageEpisodeId
      ? courseIdStr
      : null;
    return {barrageEpisodeId, legacyEpisodeId};
  },

  /** 取课程下第一个有集次的章节的首集 */
  firstEpisodeOf(course) {
    const seasons = (course && course.seasons) || [];
    for (let i = 0; i < seasons.length; i++) {
      const episodes = seasons[i].episodes || [];
      if (episodes.length) return episodes[0];
    }
    return null;
  },

  /**
   * 未购买用户唯一能看的那一节：第二章第一节。
   *
   * 按数组下标取（seasons[1].episodes[0]）而不是按 idx 字段排序，是为了和目录里
   * 「第{{seasonIndex + 1}}章」的编号严格对齐——否则会出现界面标着「第二章」、
   * 放开的却是别的一章。课程不足两章或第二章没有集次时，回退到首个有集次章节的首节，
   * 保证任何课程都至少有一节可试看。
   */
  freeEpisodeOf(course) {
    const seasons = (course && course.seasons) || [];
    const second = seasons[1];
    if (second && second.episodes && second.episodes.length) {
      return second.episodes[0];
    }
    return this.firstEpisodeOf(course);
  },

  /**
   * 给目录里每一节打上 locked / free 标记，并算出试看节的展示名。
   *
   * WXML 里表达不了「是不是那一节」这种判断，所以在 setData 之前一次性算好。
   * 已购买用户全部解锁，标记也就都是 false。
   *
   * @return {{freeEpisodeId: string|null, freeEpisodeLabel: string}}
   */
  decorateCatalog(course, purchased) {
    const seasons = (course && course.seasons) || [];
    const free = purchased ? null : this.freeEpisodeOf(course);
    const freeId = free && free.id ? String(free.id) : null;
    let freeEpisodeLabel = '';

    seasons.forEach((season, seasonIndex) => {
      (season.episodes || []).forEach((ep, epIndex) => {
        const isFree = !!freeId && String(ep.id) === freeId;
        ep.locked = !purchased && !isFree;
        ep.free = !purchased && isFree;
        if (isFree) {
          freeEpisodeLabel = `第${seasonIndex + 1}章第${epIndex + 1}节`;
        }
      });
    });
    return {freeEpisodeId: freeId, freeEpisodeLabel};
  },

  /**
   * 查询当前用户是否已购买本课程。
   *
   * 未登录直接视为未购买；接口异常也降级为未购买（只影响试看范围，不该白屏）。
   */
  checkPurchased(courseId) {
    const user = wx.getStorageSync('user');
    if (!user || !user.id) return Promise.resolve(false);
    return api.get('orderDetail', '/isPurchased', {fkUserId: user.id, fkCourseId: courseId})
      .then(res => res === true || res === 'true')
      .catch(err => {
        console.error('check purchased failed', err);
        return false;
      });
  },

  /** 点了锁住的章节：提示购买，确认后直接进支付流程 */
  promptPurchase() {
    const that = this;
    const label = this.data.freeEpisodeLabel;
    const scope = label ? `本课程仅${label}可免费试看，` : '本课程需购买后观看，';
    util.modal('请购买课程', scope + '购买后即可解锁全部章节。', () => that.pay());
  },

  /**
   * 拼集次视频地址。
   * episode.video 仍是占位默认值（尚未上传真实视频）时回退到 video/lesson.mp4，
   * 否则会指向 MinIO 里不存在的对象导致黑屏。
   */
  resolveEpisodeVideoSrc(episode) {
    const video = episode && episode.video;
    if (!video || DEFAULT_VIDEO_NAMES.indexOf(video) !== -1) {
      return constant.MINIO_VIDEO + 'lesson.mp4';
    }
    return constant.MINIO_EPISODE_VIDEO + video;
  },

  resolveEpisodePoster(episode) {
    const cover = episode && episode.cover;
    return cover ? constant.MINIO_EPISODE_VIDEO_COVER + cover : null;
  },

  onEpisodeTap(ev) {
    const dataset = (ev.currentTarget && ev.currentTarget.dataset) || {};
    // locked 以 1/0 传递：dataset 里拿到字符串 "false" 会被判为真
    if (Number(dataset.locked) === 1) {
      this.promptPurchase();
      return;
    }
    const episodeId = String(dataset.id || '');
    if (!episodeId || episodeId === this.data.currentEpisodeId) return;
    this.playEpisode({
      id: episodeId,
      video: dataset.video,
      cover: dataset.cover,
      title: dataset.title
    });
  },

  /** 切集：换视频源与封面，并把弹幕作用域切到该集 */
  playEpisode(episode) {
    this._pendingVideoSrc = this.resolveEpisodeVideoSrc(episode);
    // 主动切集需要越过 mountVideoWithBarrage 的重入保护，内部 _mountSeq 会丢弃过期结果
    this._mounting = false;
    this._playedDanmuKeys = new Set();
    this.setData({
      currentEpisodeId: String(episode.id),
      barrageEpisodeId: String(episode.id),
      legacyEpisodeId: null,
      videoPoster: this.resolveEpisodePoster(episode),
      videoTitle: episode.title || '课程视频',
      videoReady: false,
      currentTime: 0,
      danmuList: [],
      activeDanmu: [],
      barrageHistoryLoaded: false
    }, () => this.mountVideoWithBarrage(0));
  },

  getCourseInfo(courseId) {
    const that = this;
    // 课程详情与购买状态并行拉取：目录的锁定标记依赖两者
    Promise.all([
      api.get('course', '/select/' + courseId),
      that.checkPurchased(courseId)
    ]).then(([res, purchased]) => {
      res.created = util.dateFormat(res.created);
      res.updated = util.dateFormat(res.updated);
      const {freeEpisodeId, freeEpisodeLabel} = that.decorateCatalog(res, purchased);
      // 已购买默认播首集；未购买只能从试看那一节起播，否则一进页面就越过了门禁
      const startEpisode = purchased ? that.firstEpisodeOf(res) : that.freeEpisodeOf(res);
      const videoPoster = that.resolveEpisodePoster(startEpisode);
      const videoTitle = (startEpisode && startEpisode.title) || res.title || '课程视频';
      const ids = that.resolveBarrageIds(res, courseId, startEpisode);
      that._pendingVideoSrc = startEpisode
        ? that.resolveEpisodeVideoSrc(startEpisode)
        : constant.MINIO_VIDEO + 'lesson.mp4';
      that.setData({
        courseId: String(courseId),
        course: res,
        purchased,
        freeEpisodeId,
        freeEpisodeLabel,
        currentEpisodeId: startEpisode && startEpisode.id ? String(startEpisode.id) : null,
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

  /**
   * 打开小精灵：暂停视频，并把提问那一刻的播放进度交给它。
   * 播放进度是小精灵理解「刚才那段」的唯一依据，所以必须在这里快照，
   * 而不是让组件长期订阅 currentTime（那样每 250ms 就要 setData 一次）。
   */
  onSpriteOpen() {
    if (this.videoContext) this.videoContext.pause();
    this.setData({spriteCurrentTime: this.data.currentTime || 0});
  },

  /** 关闭小精灵：继续播放，学员接着看 */
  onSpriteClose() {
    if (this.videoContext) this.videoContext.play();
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

  /**
   * 按集拉弹幕，切集后互不串台。
   * legacyEpisodeId 只在首集存在：历史弹幕曾以 courseId 落库，一并合并进来。
   */
  fetchBarrageList() {
    const ids = [this.data.barrageEpisodeId, this.data.legacyEpisodeId]
      .filter(id => id)
      .map(id => String(id));
    if (!ids.length) return Promise.resolve([]);
    return Promise.all(ids.map(id => api.get('episode', '/searchBarrage/' + id).catch(err => {
      console.error('load barrage history failed', id, err);
      return [];
    }))).then(lists => this.normalizeDanmuList([].concat.apply([], lists)));
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
    if (this.data.purchased) {
      util.tip('您已购买该课程');
      return;
    }
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

  pay() {
    const that = this;
    if (!util.isLogin()) {
      util.error('请先登录');
      return;
    }
    if (this.data.purchased) {
      util.tip('您已购买该课程');
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

  /**
   * 支付完成后会先跳订单页再返回，回来时购买状态已经变了，需要重新解锁目录。
   * 只在尚未购买时查询，已购买的页面不必再问一次。
   */
  refreshPurchasedState() {
    const that = this;
    const {courseId, course, purchased} = this.data;
    if (purchased || !courseId || !course) return;
    this.checkPurchased(courseId).then(nowPurchased => {
      if (!nowPurchased) return;
      const {freeEpisodeId, freeEpisodeLabel} = that.decorateCatalog(course, true);
      that.setData({purchased: true, course, freeEpisodeId, freeEpisodeLabel});
    });
  },

  onShow() {
    if (!this.data.courseId || !this.data.course) return;
    this.refreshPurchasedState();
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
