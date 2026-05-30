const api = require('../../../../utils/api.js');
const util = require('../../../../utils/util.js');

function nowTime() {
  const d = new Date();
  const pad = n => (n < 10 ? '0' + n : '' + n);
  return pad(d.getHours()) + ':' + pad(d.getMinutes());
}

Page({
  data: {
    messages: [],
    question: '',
    faqList: [],
    sending: false,
    scrollIntoView: '',
    courseId: null,
    courseTitle: ''
  },

  appendMessage(role, content) {
    const messages = this.data.messages.concat({
      id: Date.now() + '_' + Math.random(),
      role,
      content,
      time: nowTime()
    });
    const last = messages[messages.length - 1];
    this.setData({
      messages,
      scrollIntoView: 'msg-' + last.id
    });
  },

  loadFaq() {
    api.get('customerService', '/faq').then(list => {
      this.setData({faqList: list || []});
    }).catch(() => {});
  },

  buildWelcome() {
    let welcome = '您好，我是 ML 课堂智能客服，可为您解答购买、支付、订单、课程学习等问题。';
    if (this.data.courseTitle) {
      welcome += '\n\n您正在咨询课程：《' + this.data.courseTitle + '》';
    }
    welcome += '\n\n点击下方常见问题可快速提问。';
    this.appendMessage('bot', welcome);
  },

  buildAskParams(question) {
    const params = {question};
    const user = wx.getStorageSync('user');
    if (user && user.id) params.fkUserId = user.id;
    if (this.data.courseId) params.courseId = Number(this.data.courseId);
    if (this.data.courseTitle) params.courseTitle = this.data.courseTitle;
    return params;
  },

  sendQuestion(question) {
    const text = (question || '').trim();
    if (!text || this.data.sending) return;
    this.setData({question: ''});
    this.appendMessage('user', text);
    this.setData({sending: true});
    api.post('customerService', '/ask', this.buildAskParams(text)).then(res => {
      this.appendMessage('bot', (res && res.reply) ? res.reply : '客服暂时无法回复，请稍后再试。');
      this.setData({sending: false});
    }).catch(() => {
      this.appendMessage('bot', '网络异常，请检查网络后重试。');
      this.setData({sending: false});
    });
  },

  onInput(e) {
    const v = e.detail;
    this.setData({question: typeof v === 'string' ? v : (v && v.value) || ''});
  },

  onSend() {
    this.sendQuestion(this.data.question);
  },

  tapFaq(e) {
    const question = e.currentTarget.dataset.question;
    this.sendQuestion(question);
  },

  onLoad(options) {
    const courseId = options.courseId || null;
    const courseTitle = options.courseTitle ? decodeURIComponent(options.courseTitle) : '';
    this.setData({courseId, courseTitle});
    this.loadFaq();
    this.buildWelcome();
  }
});
