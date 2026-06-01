/**
 * 将 seckill/today 加入网关白名单，使小程序首页未登录也能展示秒杀活动
 */
const NACOS = 'http://192.168.211.132:8848';

const res = await fetch(
  `${NACOS}/nacos/v1/cs/configs?dataId=ml-gateway-dev.yaml&group=ml-group&tenant=`
);
let content = await res.text();
if (!content.includes('seckill/today')) {
  content = content.replace('seckill/near,', 'seckill/near,\n    seckill/today,');
  const body = new URLSearchParams({
    dataId: 'ml-gateway-dev.yaml',
    group: 'ml-group',
    content,
    type: 'yaml'
  });
  const pub = await fetch(`${NACOS}/nacos/v1/cs/configs`, { method: 'POST', body });
  console.log('Nacos 白名单已更新:', await pub.text());
} else {
  console.log('seckill/today 已在白名单中');
}
