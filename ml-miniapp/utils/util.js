/**
 * 判断非空值
 *
 * @param value 被判断的值
 * @return boolean 返回 true 表示不为 null 也不为 undefined
 */
function isNotNull(value) {
  return value !== null && value !== undefined;
}

/**
 * 判断空值
 *
 * @param value 被判断的值
 * @return boolean 返回 true 表示为 null 或 undefined
 */
function isNull(value) {
  return !isNotNull(value);
}

/**
 * 判断是否存在空值
 *
 * @param values 被判断的值，不定长列表
 * @return boolean 返回 true 表示包含 null 或 undefined
 */
function hasNull(...values) {
  for (let i in values) {
    if (isNull(values[i])) {
      return true;
    }
  }
  return false;
}

/**
 * 判断空字符串
 *
 * @param value 被判断的值
 * @return boolean 返回 true 表示不为 null 或 undefined 或空字符串
 */
function isNotEmpty(value) {
  return value !== null && value !== undefined && value !== '';
}

/**
 * 判断非空字符串
 *
 * @param value 被判断的值
 * @return boolean 返回 true 表示为 null 或 undefined 或空字符串
 */
function isEmpty(value) {
  return !isNotEmpty(value);
}

/**
 * 判断是否存在空字符串
 *
 * @param values 被判断的值，不定长列表
 * @return boolean 返回 true 表示包含 null 或 undefined 或空字符串
 */
function hasEmpty(...values) {
  for (let i in values) {
    if (isEmpty(values[i])) {
      return true;
    }
  }
  return false;
}

/**
 * 日期字符串处理：1999-01-02T12:12:12 -> 1999年01月02日 12:12
 *
 * @param dateStr 日期字符串
 * @return string 返回格式化后的日期字符串
 */
function datetimeFormat(dateStr) {
  if (isNull(dateStr)) return '';
  // 将日期字符串转为日期格式
  let date = new Date(dateStr);
  // 获取日期中的元素: 年，月，日，时，分
  let toDouble = e => e < 10 ? '0' + e : e;
  let yy = toDouble(date.getFullYear());
  let mm = toDouble(date.getMonth() + 1);
  let dd = toDouble(date.getDate());
  let hh = toDouble(date.getHours());
  let mi = toDouble(date.getMinutes());
  // 返回美化后的日期字符串
  return `${yy}年${mm}月${dd}日 ${hh}:${mi}`;
}

/**
 * 日期字符串处理：1999-01-02T12:12:12 -> 1999年01月02日
 *
 * @param dateStr 日期字符串
 * @return string 返回格式化后的日期字符串
 */
function dateFormat(dateStr) {
  if (isNull(dateStr)) return '';
  // 将日期字符串转为日期格式
  let date = new Date(dateStr);
  // 获取日期中的元素: 年，月，日，时，分
  let toDouble = e => e < 10 ? '0' + e : e;
  let yy = toDouble(date.getFullYear());
  let mm = toDouble(date.getMonth() + 1);
  let dd = toDouble(date.getDate());
  // 返回美化后的日期字符串
  return `${yy}年${mm}月${dd}日`;
}

/**
 * 随机生成一个len位的字符串
 *
 * @param len 随机字符串位数，范围在1 ~ 36之间，默认为18
 * @return string 随机字符串
 * */
function randomStr(len = 18) {
  // 随机一个小数，并将其转为36进制字符并消除所有点符号
  let result = Math.random().toString(36).replaceAll('.', '');
  // 截取后len位字符串并返回: len不小于1且不超过36
  len = Math.max(len, 1);
  len = Math.min(len, 36);
  return result.slice(-len);
}

/**
 * 随机生成一个十六进制的颜色值
 *
 * @returns {string} 十六进制的颜色值如 '#000000'
 */
function randomColor() {
  const rgb = []
  for (let i = 0; i < 3; ++i) {
    let color = Math.floor(Math.random() * 256).toString(16)
    color = color.length === 1 ? '0' + color : color
    rgb.push(color)
  }
  return '#' + rgb.join('')
}

/**
 * 带按钮的确认弹框
 *
 * @param content 弹框内容
 * @param onConfirm 确认时的回调函数
 * @param onCancel 取消时的回调函数
 */
function confirm(content, onConfirm, onCancel) {
  wx.showModal({
    title: '提示',
    content: content,
    success: function (sm) {
      if (sm.confirm && onConfirm) onConfirm();
      if (sm.cancel && onCancel) onCancel();
    }
  });
}

/**
 * 跳转到另一个标签页
 *
 * @param url 目标页面地址
 * @param reload 是否重新加载目标页面，默认true
 */
function tab(url, reload = true) {
  wx.switchTab({
    url: url, success: () => {
      if (reload) {
        let page = getCurrentPages().pop();
        if (page) page.onLoad();
      }
    }
  });
}

/**
 * 跳转到另一个普通页
 *
 * @param url 目标页面地址
 * @param reload 是否重新加载目标页面，默认true
 */
function page(url, reload = true) {
  wx.navigateTo({
    url: url, success: () => {
      if (reload) {
        let page = getCurrentPages().pop();
        if (page) page.onLoad();
      }
    }
  });
}

/**
 * 成功提示框（微信）
 *
 * @param title 提示文字内容，长度限制约 7 个汉字（超出会被截断）。
 */
function success(title) {
  wx.showToast({title: title, icon: 'success', duration: 2000});
}

/**
 * 失败提示框（微信）
 *
 * @param title 提示文字内容，长度限制约 7 个汉字（超出会被截断）。
 */
function error(title) {
  wx.showToast({title: title, icon: 'error', duration: 2000});
}

/**
 * 普通提示框（微信）
 *
 * @param title 提示内容，长度无限制
 */
function tip(title) {
  wx.showToast({title: title, icon: 'none', duration: 2000});
}

/**
 * 模态框（微信）
 *
 * @param title 提示文字
 * @param content 提示内容
 * @param success 用户点击确定后的回调函数
 * @param cancel 用户点击取消后的回调函数
 */
function modal(title, content, success, cancel) {
  wx.showModal({
    title: title,
    content: content,
    success: function (res) {
      if (res.confirm) {
        if (success) success();
      } else {
        if (cancel) cancel();
      }
    }
  });
}

/**
 * 判断用户是否登录
 *
 * @returns {boolean} false未登录，true已登录
 */
function isLogin() {
  if (!wx.getStorageSync('token')) {
    error('请先登录');
    setTimeout(() => {
      page('/pages/index/login-by-account/login-by-account', false);
    }, 500);
    return false;
  }
  return true;
}

// 导出模块
module.exports = {
  isNotNull, isNull, hasNull, isNotEmpty, isEmpty, hasEmpty,
  dateFormat, datetimeFormat, randomStr, randomColor, confirm, tab, page,
  success, error, tip, modal, isLogin
}
