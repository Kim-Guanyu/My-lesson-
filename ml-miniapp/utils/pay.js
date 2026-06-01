const api = require('./api.js');
const constant = require('./const.js');
const util = require('./util.js');

function isJpegBuffer(buffer) {
  if (!buffer || !buffer.byteLength) return false;
  const bytes = new Uint8Array(buffer);
  return bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF;
}

function saveBufferAsTempImage(buffer) {
  return new Promise((resolve, reject) => {
    const fs = wx.getFileSystemManager();
    const filePath = `${wx.env.USER_DATA_PATH}/pay_qr_${Date.now()}.jpg`;
    fs.writeFile({
      filePath,
      data: buffer,
      encoding: 'binary',
      success: () => resolve(filePath),
      fail: reject
    });
  });
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
          // 秒杀订单由 MQ 异步创建，需等到返回真实 JPEG 而非 JSON 错误
          if (res.statusCode === 200 && isJpegBuffer(res.data)) {
            saveBufferAsTempImage(res.data).then(resolve).catch(reject);
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
  wx.showLoading({title: '生成二维码...', mask: true});
  fetchQrCode(sn).then(qrCodeImage => {
    wx.hideLoading();
    page.setData({
      sn,
      qrCodeImage,
      payDialogShow: true,
      countDownShow: true,
      time: 15 * 60 * 1000
    });
    if (page._payTimer) clearInterval(page._payTimer);
    page._payTimer = startPayPolling(page, sn, options);
  }).catch(() => {
    wx.hideLoading();
    util.error('获取二维码失败');
  });
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
