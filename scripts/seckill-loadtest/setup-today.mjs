/**
 * 一键准备今日秒杀活动（更新日期 + 开启上午场 + 重置 Redis 库存）
 * 用法：node setup-today.mjs [seckillId] [stock]
 */
import mysql from 'mysql2/promise';

const GATEWAY = process.env.GATEWAY || 'http://localhost:24101';
const seckillId = Number(process.argv[2] || 1);
const stock = Number(process.argv[3] || 100);

const db = await mysql.createConnection({
  host: process.env.MYSQL_HOST || '192.168.211.132',
  port: Number(process.env.MYSQL_PORT || 3306),
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '123456'
});

await db.execute(`
  UPDATE ml_sms.seckill
  SET start_time = CONCAT(CURDATE(), ' 08:00:00'),
      end_time = CONCAT(CURDATE(), ' 12:00:00'),
      status = 0, updated = NOW()
  WHERE id = ?
`, [seckillId]);

await db.execute(`
  UPDATE ml_sms.seckill
  SET start_time = CONCAT(CURDATE(), ' 12:00:00'),
      end_time = CONCAT(CURDATE(), ' 14:00:00'),
      status = 0, updated = NOW()
  WHERE id = 2
`);
await db.execute(`
  UPDATE ml_sms.seckill
  SET start_time = CONCAT(CURDATE(), ' 14:00:00'),
      end_time = CONCAT(CURDATE(), ' 18:00:00'),
      status = 0, updated = NOW()
  WHERE id = 3
`);
await db.execute(`
  UPDATE ml_sms.seckill_detail SET sk_count = ?, updated = NOW()
  WHERE fk_seckill_id = ? AND deleted = 0
`, [stock, seckillId]);

console.log(`数据库：秒杀活动 1~3 已设为今日，活动 ${seckillId} 库存=${stock}`);

const loginRes = await fetch(`${GATEWAY}/user-server/api/v1/user/loginByAccount`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'loadtest001', password: 'LoadTest123' })
}).then(r => r.json());
if (loginRes.code !== 1000) throw new Error('登录失败: ' + JSON.stringify(loginRes));
const token = loginRes.data.token;

const prep = await fetch(
  `${GATEWAY}/sale-server/api/v1/seckill/prepareLoadTest?seckillId=${seckillId}&stock=${stock}`,
  { method: 'POST', headers: { token } }
).then(r => r.json());
if (prep.code !== 1000) throw new Error('开启活动失败: ' + JSON.stringify(prep));

const today = await fetch(`${GATEWAY}/sale-server/api/v1/seckill/today`, { headers: { token } })
  .then(r => r.json());
const active = today.data?.find(s => s.id === seckillId);
console.log(`活动 ${seckillId} 已开启，状态=${active?.status}，商品数=${active?.seckillDetails?.length}`);
console.log('小程序首页刷新即可看到「上午场」秒杀活动');

await db.end();
