const util = require('./util.js');
const constant = require('./const.js');

/**
 * （私有）API前缀处理：根据模块名称返回API前缀
 */
function apiPrefixFormat(module) {
  const serviceModuleMap = {
    'user-server': ['menu', 'role', 'user'],
    'course-server': ['category', 'comment', 'course', 'episode', 'report', 'season'],
    'sale-server': ['article', 'banner', 'coupons', 'notice', 'seckill', 'seckillDetail'],
    'order-server': ['cart', 'order', 'orderDetail']
  };
  const microServiceName = Object.keys(serviceModuleMap).find(key => serviceModuleMap[key].includes(module));
  return `/${microServiceName}/api/v1/${module}`;
}

function sendRequest(config) {
  let module = config['module'];
  let url = config['url'];
    if (util.hasNull(module, url)) {
      return Promise.reject(new Error('缺少 module 或 url'));
    }

  let method = config['method'];
  let params = config['params'];
  let header = config['header'];
  if (util.isEmpty(method)) method = 'GET';
  if (util.isNull(header)) {
    let token = wx.getStorageSync('token') || null;
    header = {'Content-Type': 'application/json', 'token': token};
  }

  url = constant.GATEWAY_HOST + apiPrefixFormat(module) + url;

  return new Promise((resolve, reject) => {
    wx.request({
      url: url,
      method: method,
      data: params,
      header: header,
      success(res) {
        if (util.isNull(res)) {
          util.error('服务器无响应');
          reject(new Error('服务器无响应'));
          return;
        }
        const data = res.data;
        if (data && typeof data === 'object' && 'code' in data) {
          if (data.code === constant.STATUS.SUCCESS) {
            resolve(util.isNotNull(data.data) ? data.data : true);
          } else {
            util.error(data.message || '请求失败');
            console.error(data.coderMessage);
            reject(new Error(data.message || '请求失败'));
          }
          return;
        }
        resolve(data);
      },
      fail(err) {
        reject('请求异常: ' + err);
      }
    });
  });
}

function get(module, url, params = null) {
  return sendRequest({module, url, params});
}

function post(module, url, params = null) {
  return sendRequest({module, url, params, method: 'POST'});
}

function put(module, url, params = null) {
  return sendRequest({module, url, params, method: 'PUT'});
}

function del(module, url, params = null) {
  return sendRequest({module, url, params, method: 'DELETE'});
}

module.exports = {get, post, put, del};
