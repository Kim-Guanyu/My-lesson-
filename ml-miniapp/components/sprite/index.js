const api = require('../../utils/api.js');

/** 工具名 -> 给用户看的中文标签。让回答的来源可见，答案更可信 */
const TOOL_LABELS = {
  search_knowledge: '查了知识库',
  get_course_outline: '查了课程目录',
  get_episode_info: '查了本节信息',
  search_courses: '搜了课程',
  search_episode_questions: '看了同学弹幕',
  get_my_purchased_courses: '查了我的课程',
  get_my_orders: '查了我的订单'
};

/** 引导气泡显示时长 */
const HINT_MS = 5000;

function seq() {
  return Date.now() + '_' + Math.random().toString(36).slice(2, 7);
}

/** 秒 -> mm:ss，用于头部展示当前播放位置 */
function formatTime(seconds) {
  const total = Math.max(0, Math.floor(Number(seconds) || 0));
  const m = Math.floor(total / 60);
  const s = total % 60;
  return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
}

Component({
  properties: {
    courseId: {type: null, value: null},
    courseTitle: {type: String, value: ''},
    episodeId: {type: null, value: null},
    episodeTitle: {type: String, value: ''},
    // 由父页面在打开抽屉时写入：用户提问那一刻的播放进度（秒）
    currentTime: {type: Number, value: 0}
  },

  data: {
    visible: false,
    showHint: false,
    messages: [],
    question: '',
    sending: false,
    scrollIntoView: '',
    contextLabel: '',
    quickAsks: [],
    // 键盘高度（px）。抽屉贴底，键盘弹起会盖住输入栏，
    // 用它给输入栏加 padding-bottom 把输入框顶上去
    keyboardHeight: 0
  },

  lifetimes: {
    attached() {
      // 属性 observer 一般会在初始化时触发，这里再兜一次，
      // 保证头部标签和快捷提问在首帧就有内容
      this.refreshContextLabel();
      this.refreshQuickAsks();

      // 进页面几秒后飘出引导气泡，然后自动收起，不打扰用户
      this._hintTimer = setTimeout(() => {
        if (this.data.visible) return;
        this.setData({showHint: true});
        this._hideHintTimer = setTimeout(
          () => this.setData({showHint: false}),
          HINT_MS
        );
      }, 1200);
    },

    detached() {
      clearTimeout(this._hintTimer);
      clearTimeout(this._hideHintTimer);
    }
  },

  observers: {
    // 课程/课时变了就刷新头部标签和快捷提问
    'courseTitle, episodeTitle': function () {
      this.refreshContextLabel();
      this.refreshQuickAsks();
    }
  },

  methods: {
    noop() {},

    /** 头部副标题：让用户看到小精灵知道他停在哪 */
    refreshContextLabel() {
      const {episodeTitle, courseTitle} = this.data;
      let label = '课程学习答疑';
      if (episodeTitle) {
        label = '正在看：' + episodeTitle;
      } else if (courseTitle) {
        label = '《' + courseTitle + '》';
      }
      this.setData({contextLabel: label});
    },

    refreshQuickAsks() {
      const asks = [];
      if (this.data.episodeTitle) {
        asks.push('这节课讲了什么？');
        asks.push('刚才这段没听懂');
      }
      if (this.data.courseTitle) {
        asks.push('这门课的目录');
      }
      asks.push('我买过哪些课？');
      asks.push('课程支持退款吗？');
      this.setData({quickAsks: asks});
    },

    appendMessage(role, content, tools) {
      const toolLabels = (tools || [])
        .map(name => TOOL_LABELS[name])
        .filter(Boolean);
      // 同一工具可能被调多次，去重后展示
      const uniqueLabels = [...new Set(toolLabels)];

      const id = seq();
      const messages = this.data.messages.concat({
        id,
        role,
        content,
        toolLabels: uniqueLabels
      });
      this.setData({messages, scrollIntoView: 'msg-' + id});
    },

    scrollToTyping() {
      this.setData({scrollIntoView: 'msg-typing'});
    },

    /** 首次打开时的欢迎语，结合播放位置说话 */
    buildWelcome() {
      let text = '嗨，我是你的学习小精灵 ✨\n看视频遇到不懂的地方，随时问我。';
      if (this.data.episodeTitle) {
        text += '\n\n你正在看「' + this.data.episodeTitle + '」';
        // 进度为 0 时不提（刚开播，报 00:00 没有意义），
        // 也顺带避开父页面 setData 还没传到时的空值
        if (this.data.currentTime > 0) {
          text += '，进度 ' + formatTime(this.data.currentTime);
        }
        text += '。直接说「刚才那段没懂」我也能接上。';
      }
      this.appendMessage('bot', text);
    },

    onOpen() {
      if (this.data.visible) return;
      clearTimeout(this._hideHintTimer);
      this.setData({visible: true, showHint: false});

      // 通知父页面：暂停视频并把当前播放进度写进来
      this.triggerEvent('open');

      if (this.data.messages.length === 0) {
        // 等父页面把 currentTime setData 进来后再拼欢迎语，
        // 否则欢迎语里的进度会是 0
        setTimeout(() => this.buildWelcome(), 60);
      }
    },

    onClose() {
      if (!this.data.visible) return;
      this.setData({visible: false});
      this.triggerEvent('close');
    },

    onInput(e) {
      const v = e.detail;
      this.setData({
        question: typeof v === 'string' ? v : (v && v.value) || ''
      });
    },

    onInputFocus(e) {
      // adjust-position 关掉了，自己把输入栏顶到键盘上方
      const height = (e.detail && e.detail.height) || 0;
      if (!height) return;
      this.setData({keyboardHeight: height});
      // 消息区被压缩了，把最新一条重新滚进可视范围
      const messages = this.data.messages;
      if (messages.length) {
        this.setData({scrollIntoView: 'msg-' + messages[messages.length - 1].id});
      }
    },

    onInputBlur() {
      this.setData({keyboardHeight: 0});
    },

    onQuickAsk(e) {
      this.send(e.currentTarget.dataset.q);
    },

    onSend() {
      this.send(this.data.question);
    },

    send(text) {
      const question = (text || '').trim();
      if (!question || this.data.sending) return;

      this.setData({question: '', sending: true});
      this.appendMessage('user', question);
      this.scrollToTyping();

      api
        .post('sprite', '/ask', this.buildParams(question))
        .then(res => {
          const reply =
            (res && res.reply) || '我这边没拿到回复，要不再问一次？';
          this.setData({sending: false});
          this.appendMessage('bot', reply, res && res.tools);
        })
        .catch(() => {
          // api.js 已经弹过错误提示，这里只补一条气泡保持对话连贯
          this.setData({sending: false});
          this.appendMessage(
            'bot',
            '网络好像不太稳定，稍后再问我一次吧。'
          );
        });
    },

    buildParams(question) {
      const params = {question};
      // 注意：不传 fkUserId。用户身份由后端从登录令牌解析，
      // 前端传 userId 会带来越权查询风险。
      if (this.data.courseId) params.courseId = Number(this.data.courseId);
      if (this.data.courseTitle) params.courseTitle = this.data.courseTitle;
      if (this.data.episodeId) params.episodeId = Number(this.data.episodeId);
      if (this.data.episodeTitle) params.episodeTitle = this.data.episodeTitle;
      if (this.data.currentTime) {
        params.currentTime = Number(this.data.currentTime);
      }
      return params;
    }
  }
});
