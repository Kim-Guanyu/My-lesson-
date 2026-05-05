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
    activeTab: 0,
    danmuList: [],
    danmuText: '',
    danmuColor: '#ffffff',
    barrageEpisodeId: null,
    currentTime: 0,
    wsConnected: false,
    courseId: null,
    legacyEpisodeId: null,
    barrageHistoryLoaded: false
  },

  // Use the first episode id when available so history aligns to course progress.
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
      const videoSrc = constant.MINIO_VIDEO + 'lesson.mp4';
      const videoPoster = null;
      const videoTitle = res.title || '课程视频';
      const ids = that.resolveBarrageIds(res, courseId);
      that.setData({
        courseId: String(courseId),
        course: res,
        videoSrc: null,
        videoPoster,
        videoTitle,
        barrageEpisodeId: ids.barrageEpisodeId,
        legacyEpisodeId: ids.legacyEpisodeId,
        danmuList: [],
        barrageHistoryLoaded: false
      });
      const showVideo = () => {
        that.setData({
          videoSrc,
          barrageHistoryLoaded: true
        }, () => {
          that.videoContext = wx.createVideoContext('courseVideo', that);
          that.initBarrageWs();
        });
      };
      that.loadBarrageHistory().then(showVideo).catch(showVideo);
    }).catch(err => console.error(err));
  },

  onTabChange(ev) {
    this.setData({activeTab: ev.detail});
  },

  onVideoTimeUpdate(ev) {
    if (ev && ev.detail && typeof ev.detail.currentTime === 'number') {
      this.setData({currentTime: ev.detail.currentTime});
    }
  },

  onDanmuInput(ev) {
    this.setData({danmuText: ev.detail.value || ''});
  },

  loadBarrageHistory() {
    const episodeId = this.data.barrageEpisodeId;
    const legacyEpisodeId = this.data.legacyEpisodeId;
    const ids = [episodeId, legacyEpisodeId].filter((id, idx, arr) => id && arr.indexOf(id) === idx);
    if (!ids.length) return Promise.resolve([]);
    const fetches = ids.map(id => api.get('episode', '/searchBarrage/' + id).catch(() => []));
    return Promise.all(fetches).then(results => {
      const merged = [].concat(...results);
      const seen = new Set();
      const danmuList = merged.map(item => ({
        text: item.text || '',
        color: item.color || '#ffffff',
        time: Number(item.time) || 0
      })).filter(item => {
        const key = `${item.text}|${item.color}|${item.time}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      }).sort((a, b) => a.time - b.time);
      this.setData({danmuList});
      return danmuList;
    }).catch(err => console.error(err));
  },

  initBarrageWs() {
    if (this._wsReady || this._wsConnecting) return;
    const user = wx.getStorageSync('user');
    if (!user || !user.id) {
      return;
    }
    this._wsConnecting = true;
    const wsUrl = `${constant.SOCKET_SERVER}/api/v1/barrage/${user.id}`;
    wx.connectSocket({url: wsUrl});
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
        const allowedIds = [this.data.barrageEpisodeId, this.data.legacyEpisodeId]
          .filter(id => id)
          .map(id => String(id));
        if (incomingEpisodeId && allowedIds.length && allowedIds.indexOf(incomingEpisodeId) === -1) {
          return;
        }
        const danmu = {
          text: data.text || '',
          color: data.color || '#ffffff',
          time: Number(data.time) || 0
        };
        if (this.videoContext) {
          this.videoContext.sendDanmu(danmu);
        }
      } catch (e) {
        console.error(e);
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
      episodeId: String(this.data.barrageEpisodeId || ''),
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
    util.page('/pages/course/detail/chat/chat');
  },

  pay() {
    if (util.isLogin()) {
      util.tip('功能暂未开放');
    }
  },

  onLoad(options) {
    this.videoContext = wx.createVideoContext('courseVideo', this);
    const id = options.courseId;
    if (id) {
      const courseId = String(id);
      this.setData({courseId});
      this.getCourseInfo(courseId);
    }
  },

  onShow() {
    if (this.data.courseId && this.data.course) {
      const courseId = String(this.data.courseId);
      const ids = this.resolveBarrageIds(this.data.course, courseId);
      if (this.data.barrageEpisodeId !== ids.barrageEpisodeId || this.data.legacyEpisodeId !== ids.legacyEpisodeId) {
        this.setData({
          barrageEpisodeId: ids.barrageEpisodeId,
          legacyEpisodeId: ids.legacyEpisodeId
        });
      }
      this.setData({danmuList: []});
      this.loadBarrageHistory();
      this.initBarrageWs();
    }
  },

  onUnload() {
    if (this._wsReady) {
      wx.closeSocket();
    }
  }
});
