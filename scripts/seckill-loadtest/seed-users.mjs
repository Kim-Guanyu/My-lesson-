/**
 * 批量创建压测用户到 ml_ums.user
 * 用法：node seed-users.mjs [数量，默认100]
 */
import mysql from 'mysql2/promise';
import bcrypt from 'bcryptjs';

const COUNT = Number(process.argv[2] || 100);
const PASSWORD = 'LoadTest123';
const DB = {
  host: '192.168.211.132',
  port: 3306,
  user: 'root',
  password: '123456',
  database: 'ml_ums'
};

function pad(n, width = 3) {
  return String(n).padStart(width, '0');
}

function fakeIdCard(i) {
  return `1101011990${String(10000 + i).slice(-6)}`;
}

async function main() {
  const hash = bcrypt.hashSync(PASSWORD, 10);
  const conn = await mysql.createConnection(DB);

  let inserted = 0;
  let skipped = 0;

  for (let i = 1; i <= COUNT; i++) {
    const username = `loadtest${pad(i)}`;
    const phone = `138${String(10000000 + i).slice(-8)}`;
    const email = `${username}@loadtest.local`;
    const nickname = `压测${pad(i, 2)}`;
    const realname = '压测用户';
    const idcard = fakeIdCard(i);

    const [exists] = await conn.execute(
      'SELECT id FROM user WHERE username = ? OR phone = ? OR email = ? LIMIT 1',
      [username, phone, email]
    );
    if (exists.length) {
      skipped++;
      continue;
    }

    await conn.execute(
      `INSERT INTO user
        (username, password, nickname, email, province, realname, avatar, zodiac,
         phone, idcard, gender, age, info, version, deleted)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)`,
      [
        username,
        hash,
        nickname,
        email,
        '北京',
        realname,
        '',
        '摩羯',
        phone,
        idcard,
        2,
        25,
        '压测专用账号，请勿用于生产'
      ]
    );
    inserted++;
  }

  const [rows] = await conn.execute(
    `SELECT id, username, phone FROM user
     WHERE username LIKE 'loadtest%' AND deleted = 0
     ORDER BY id`
  );

  await conn.end();

  console.log(`完成：新增 ${inserted} 个，跳过 ${skipped} 个（已存在）`);
  console.log(`压测账号前缀 loadtest001 ~ loadtest${pad(COUNT)}，统一密码：${PASSWORD}`);
  console.log(`当前 loadtest 用户总数：${rows.length}`);
  console.log('前 5 条：');
  rows.slice(0, 5).forEach((r) => console.log(`  id=${r.id} username=${r.username} phone=${r.phone}`));
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
