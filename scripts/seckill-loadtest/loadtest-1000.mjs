/**
 * 1000 人并发秒杀压测 + 超卖验证
 * 用法：node loadtest-1000.mjs [config-1000.json]
 */
import { readFileSync, writeFileSync } from 'fs';
import { performance } from 'perf_hooks';
import mysql from 'mysql2/promise';
import { createClient } from 'redis';

const configPath = process.argv[2] || 'config-1000.json';
const cfg = JSON.parse(readFileSync(configPath, 'utf8'));

const GATEWAY = cfg.gateway || 'http://localhost:24101';
const SUCCESS_CODE = 1000;
const PASSWORD = cfg.password || 'LoadTest123';
const USER_PREFIX = cfg.userPrefix || 'loadtest';
const USER_COUNT = cfg.userCount || cfg.totalRequests || 1000;
const USER_PAD_WIDTH = cfg.userPadWidth ?? 3;

function pad(n, width = USER_PAD_WIDTH) {
  return String(n).padStart(width, '0');
}

function usernameOf(index) {
  return `${USER_PREFIX}${pad(index)}`;
}

async function request(method, url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.token = token;
  const start = performance.now();
  try {
    const res = await fetch(GATEWAY + url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(60_000)
    });
    const elapsed = performance.now() - start;
    let data = null;
    const text = await res.text();
    try {
      data = JSON.parse(text);
    } catch {
      data = { raw: text };
    }
    return { status: res.status, data, elapsed };
  } catch (err) {
    return {
      status: 0,
      data: { message: err.message || String(err) },
      elapsed: performance.now() - start
    };
  }
}

async function loginUser(username) {
  const { data, status } = await request('POST', '/user-server/api/v1/user/loginByAccount', {
    username,
    password: PASSWORD
  });
  if (status !== 200 || data?.code !== SUCCESS_CODE || !data.data?.token) {
    throw new Error(`登录失败 ${username}: ${JSON.stringify(data)}`);
  }
  return {
    username,
    userId: data.data.user.id,
    token: data.data.token
  };
}

async function loginAll() {
  console.log(`批量登录 ${USER_COUNT} 个压测账号...`);
  const users = [];
  const batchSize = cfg.loginConcurrency || 50;
  for (let start = 1; start <= USER_COUNT; start += batchSize) {
    const end = Math.min(start + batchSize - 1, USER_COUNT);
    const tasks = [];
    for (let i = start; i <= end; i++) {
      tasks.push(loginUser(usernameOf(i)));
    }
    const batch = await Promise.all(tasks);
    users.push(...batch);
    process.stdout.write(`\r登录进度: ${users.length}/${USER_COUNT}`);
  }
  console.log('\n登录完成');
  return users;
}

async function prepareLoadTest(token) {
  const stock = cfg.stock ?? 100;
  const url = `/sale-server/api/v1/seckill/prepareLoadTest?seckillId=${cfg.seckillId}&stock=${stock}`;
  const { data, status } = await request('POST', url, null, token);
  if (status !== 200 || data?.code !== SUCCESS_CODE) {
    throw new Error('准备秒杀活动失败: ' + JSON.stringify(data));
  }
  console.log(`秒杀活动 ${cfg.seckillId} 已开启，Redis 库存=${stock}`);
}

async function killOnce(user, index) {
  const body = {
    fkSeckillId: cfg.seckillId,
    fkCourseId: cfg.courseId
  };
  const { status, data, elapsed } = await request(
    'POST',
    '/sale-server/api/v1/seckill/kill',
    body,
    user.token
  );
  const ok = status === 200 && data?.code === SUCCESS_CODE && !!data?.data;
  return {
    ok,
    elapsed,
    code: data?.code,
    message: data?.message,
    sn: data?.data,
    userId: user.userId,
    username: user.username,
    index
  };
}

function percentile(arr, p) {
  if (!arr.length) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, idx)];
}

async function runKillTest(users) {
  const total = Math.min(cfg.totalRequests || USER_COUNT, users.length);
  const concurrency = cfg.concurrency || total;
  console.log(`\n开始压测: 总请求=${total}, 并发=${concurrency}, burst=${!!cfg.burst}`);

  const targets = users.slice(0, total);
  const started = performance.now();
  let results;

  if (cfg.burst) {
    const waveSize = cfg.burstWaveSize || concurrency;
    results = [];
    for (let i = 0; i < total; i += waveSize) {
      const wave = targets.slice(i, i + waveSize);
      const waveResults = await Promise.all(wave.map((u, j) => killOnce(u, i + j)));
      results.push(...waveResults);
      if (i + waveSize < total) {
        process.stdout.write(`\r进度: ${results.length}/${total}`);
      }
    }
    if (total > waveSize) console.log('');
  } else {
    results = [];
    let sent = 0;
    while (sent < total) {
      const batchSize = Math.min(concurrency, total - sent);
      const batch = await Promise.all(
        targets.slice(sent, sent + batchSize).map((u, i) => killOnce(u, sent + i))
      );
      results.push(...batch);
      sent += batchSize;
      process.stdout.write(`\r进度: ${sent}/${total}`);
    }
    console.log('');
  }

  const totalTime = performance.now() - started;
  return { results, totalTime, total };
}

async function verifyResults(apiResults, testStartedAt) {
  const stock = cfg.stock ?? 100;
  const courseId = cfg.courseId;
  const successes = apiResults.filter(r => r.ok);
  const sns = successes.map(r => r.sn).filter(Boolean);
  const uniqueSn = new Set(sns);

  console.log('\n=== 超卖 / 数据校验 ===');

  let redisStock = null;
  if (cfg.redis) {
    const client = createClient({
      socket: { host: cfg.redis.host, port: cfg.redis.port },
      password: cfg.redis.password || undefined
    });
    await client.connect();
    redisStock = await client.get(`seckill:stock:${cfg.seckillId}:${courseId}`);
    await client.quit();
    console.log('Redis 剩余库存:', redisStock ?? 'null');
  }

  let dbOrderCount = null;
  let dbPaidCount = null;
  if (cfg.mysql) {
    const conn = await mysql.createConnection({
      host: cfg.mysql.host,
      port: cfg.mysql.port,
      user: cfg.mysql.user,
      password: cfg.mysql.password
    });
    const since = new Date(testStartedAt - 60000).toISOString().slice(0, 19).replace('T', ' ');
    const [rows] = await conn.execute(
      `SELECT COUNT(DISTINCT o.id) AS cnt
       FROM ml_oms.\`order\` o
       JOIN ml_oms.order_detail od ON o.id = od.fk_order_id AND od.deleted = 0
       WHERE od.fk_course_id = ?
         AND o.info LIKE '%秒杀%'
         AND o.created >= ?
         AND o.deleted = 0`,
      [courseId, since]
    );
    dbOrderCount = rows[0]?.cnt ?? 0;

    const [paidRows] = await conn.execute(
      `SELECT COUNT(DISTINCT o.id) AS cnt
       FROM ml_oms.\`order\` o
       JOIN ml_oms.order_detail od ON o.id = od.fk_order_id AND od.deleted = 0
       WHERE od.fk_course_id = ?
         AND o.info LIKE '%秒杀%'
         AND o.status = 1
         AND o.created >= ?
         AND o.deleted = 0`,
      [courseId, since]
    );
    dbPaidCount = paidRows[0]?.cnt ?? 0;
    await conn.end();
    console.log('DB 秒杀订单数(压测时段):', dbOrderCount);
    console.log('DB 已付款订单数:', dbPaidCount);
  }

  const apiSuccess = successes.length;
  const expectedRemain = stock - apiSuccess;

  console.log('\n--- 结论 ---');
  console.log('初始库存:', stock);
  console.log('API 成功数:', apiSuccess);
  console.log('唯一订单号数:', uniqueSn.size);
  console.log('重复订单号:', apiSuccess - uniqueSn.size);

  let oversell = false;
  if (apiSuccess > stock) {
    console.log('⚠ API 层超卖: 成功数 > 库存');
    oversell = true;
  }
  if (redisStock !== null && Number(redisStock) < 0) {
    console.log('⚠ Redis 库存为负');
    oversell = true;
  }
  if (redisStock !== null && Number(redisStock) !== expectedRemain) {
    console.log(`ℹ Redis 库存 ${redisStock}，按 API 成功推算应为 ${expectedRemain}（MQ 异步可能有延迟）`);
  }
  if (dbOrderCount !== null && dbOrderCount > stock) {
    console.log('⚠ DB 订单数 > 库存，存在超卖风险');
    oversell = true;
  } else if (dbOrderCount !== null && dbOrderCount <= stock) {
    console.log('✓ DB 订单数未超过库存');
  }
  if (!oversell && apiSuccess <= stock && uniqueSn.size === apiSuccess) {
    console.log('✓ 未发现超卖（API 成功数 ≤ 库存，订单号无重复）');
  }

  return { apiSuccess, uniqueSn: uniqueSn.size, redisStock, dbOrderCount, oversell };
}

function printReport(results, totalTime, total) {
  const latencies = [];
  let success = 0;
  let fail = 0;
  const errors = {};

  for (const r of results) {
    if (r.ok) {
      success++;
      latencies.push(r.elapsed);
    } else {
      fail++;
      const key = r.message || `code:${r.code}` || 'unknown';
      errors[key] = (errors[key] || 0) + 1;
    }
  }

  console.log('\n=== 压测结果 ===');
  console.log('总请求:', total);
  console.log('总耗时(ms):', totalTime.toFixed(0));
  console.log('QPS:', (total / (totalTime / 1000)).toFixed(2));
  console.log('成功:', success);
  console.log('失败:', fail);
  console.log('成功率:', ((success / total) * 100).toFixed(2) + '%');
  if (latencies.length) {
    console.log('延迟(ms) min:', Math.min(...latencies).toFixed(2));
    console.log('延迟(ms) avg:', (latencies.reduce((a, b) => a + b, 0) / latencies.length).toFixed(2));
    console.log('延迟(ms) p50:', percentile(latencies, 50).toFixed(2));
    console.log('延迟(ms) p95:', percentile(latencies, 95).toFixed(2));
    console.log('延迟(ms) p99:', percentile(latencies, 99).toFixed(2));
    console.log('延迟(ms) max:', Math.max(...latencies).toFixed(2));
  }
  if (Object.keys(errors).length) {
    console.log('\n失败原因分布:');
    for (const [k, v] of Object.entries(errors).sort((a, b) => b[1] - a[1])) {
      console.log(`  ${k}: ${v}`);
    }
  }

  return { success, fail, latencies: latencies.length, errors };
}

async function main() {
  console.log('=== 1000 人秒杀压测 ===');
  console.log('网关:', GATEWAY);
  console.log('活动ID:', cfg.seckillId, '课程ID:', cfg.courseId, '库存:', cfg.stock);

  const users = await loginAll();
  if (cfg.prepareBeforeTest !== false) {
    await prepareLoadTest(users[0].token);
  }

  const testStartedAt = Date.now();
  const { results, totalTime, total } = await runKillTest(users);
  const summary = printReport(results, totalTime, total);

  if (cfg.waitOrdersMs > 0) {
    console.log(`\n等待 ${cfg.waitOrdersMs}ms，让 MQ 异步建单...`);
    await new Promise(r => setTimeout(r, cfg.waitOrdersMs));
  }

  let verify = null;
  if (cfg.verifyAfterTest !== false) {
    verify = await verifyResults(results, testStartedAt);
  }

  const reportPath = `report-${Date.now()}.json`;
  writeFileSync(
    reportPath,
    JSON.stringify(
      {
        config: { ...cfg, password: '***' },
        total,
        totalTimeMs: totalTime,
        summary,
        verify,
        successLatencies: results.filter(r => r.ok).map(r => r.elapsed),
        sampleErrors: results.filter(r => !r.ok).slice(0, 20)
      },
      null,
      2
    )
  );
  console.log('\n详细报告已写入:', reportPath);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
