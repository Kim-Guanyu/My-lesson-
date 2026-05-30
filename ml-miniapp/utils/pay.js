const api = require('./api.js');
const constant = require('./const.js');
const util = require('./util.js');

function arrayBufferToBase64(buffer) {
  if (wx.arrayBufferToBase64) return wx.arrayBufferToBase64(buffer);
  let binary = '';
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
  return wx.base64Encode ? wx.base64Encode(binary) : binary;
}

function isPaidResult(ok) {
  return ok === true || ok === 'true';
}

/**
 * 支付成功：关闭弹窗、提示用户、跳转订单页
 */
function handlePaySuccess(page, options) {
  if (page._payHandled) return;
  page._payHandled = true;
  if (page._payTimer) {
    clearInterval(page._payTimer);
    page._payTimer = null;
  }
  page.setData({payDialogShow: false, countDownShow: false});
  wx.showModal({
    title: '支付成功',
    content: '您的订单已支付成功，点击确定查看订单详情',
    showCancel: false,
    confirmText: '查看订单',
    success() {
      const goOrder = () => {
        if (options && options.onSuccess) {
          options.onSuccess();
        } else {
          wx.navigateTo({url: '/pages/user/order/order'});
        }
      };
      goOrder();
    }
  });
}

/**
 * 获取支付宝扫码支付二维码（支持异步建单重试）
 */
function fetchQrCode(sn, retries = 15, interval = 1000) {
  return new Promise((resolve, reject) => {
    const attempt = (left) => {
      wx.request({
        url: constant.GATEWAY_HOST + '/order-server/api/v1/order/getQrCode',
        method: 'POST',
        data: {sn},
        header: {token: wx.getStorageSync('token')},
        responseType: 'arraybuffer',
        success(res) {
          if (res.statusCode === 200) {
            resolve('data:image/jpeg;base64,' + arrayBufferToBase64(res.data));
            return;
          }
          if (left <= 0) {
            reject(new Error('获取二维码失败'));
            return;
          }
          setTimeout(() => attempt(left - 1), interval);
        },
        fail(err) {
          if (left <= 0) {
            reject(err);
            return;
          }
          setTimeout(() => attempt(left - 1), interval);
        }
      });
    };
    attempt(retries);
  });
}

/**
 * 轮询订单支付状态（立即查一次，之后每 2 秒查一次）
 */
function startPayPolling(page, sn, options) {
  const check = () => {
    api.get('order', '/checkStatusBySn/' + sn).then(ok => {
      if (isPaidResult(ok)) {
        handlePaySuccess(page, options);
      }
    }).catch(() => {});
  };
  check();
  return setInterval(check, 2000);
}

/**
 * 打开支付弹窗：拉取二维码并开始轮询
 */
function openPayDialog(page, sn, options) {
  page._payHandled = false;
  fetchQrCode(sn).then(qrCodeImage => {
    page.setData({
      sn,
      qrCodeImage,
      payDialogShow: true,
      countDownShow: true,
      time: 15 * 60 * 1000
    });
    if (page._payTimer) clearInterval(page._payTimer);
    page._payTimer = startPayPolling(page, sn, options);
  }).catch(() => util.error('获取二维码失败'));
}

function cancelPay(page, options) {
  if (page._payHandled) return;
  if (page._payTimer) {
    clearInterval(page._payTimer);
    page._payTimer = null;
  }
  page.setData({payDialogShow: false, countDownShow: false});
  if (options && options.onCancel) options.onCancel();
}

function onCountDownFinish(page) {
  if (page._payHandled) return;
  util.error('支付超时');
  cancelPay(page);
}

function onUnload(page) {
  if (page._payTimer) clearInterval(page._payTimer);
}

const ORDER_STATUS = {
  0: '未付款',
  1: '已付款',
  2: '已取消',
  3: '其他'
};

const PAY_TYPE = {
  0: '未支付',
  1: '微信',
  2: '支付宝',
  3: '其他'
};

module.exports = {
  fetchQrCode,
  startPayPolling,
  openPayDialog,
  cancelPay,
  onCountDownFinish,
  onUnload,
  handlePaySuccess,
  ORDER_STATUS,
  PAY_TYPE
};
