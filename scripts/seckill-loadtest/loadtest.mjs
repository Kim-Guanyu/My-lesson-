/**
 * 秒杀接口压测脚本
 * 用法：node loadtest.mjs [config.json]
 */
import { readFileSync } from 'fs';
import { performance } from 'perf_hooks';

const configPath = process.argv[2] || 'config.json';
const cfg = JSON.parse(readFileSync(configPath, 'utf8'));

const GATEWAY = cfg.gateway || 'http://localhost:24101';
const SUCCESS_CODE = 1000;

async function request(method, url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.token = token;
  const start = performance.now();
  const res = await fetch(GATEWAY + url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
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
}

async function login() {
  const { data, status } = await request('POST', '/user-server/api/v1/user/loginByAccount', {
    username: cfg.username,
    password: cfg.password
  });
  if (status !== 200 || !data || data.code !== SUCCESS_CODE || !data.data?.token) {
    throw new Error('登录失败: ' + JSON.stringify(data));
  }
  console.log('登录成功，用户:', data.data.user?.username || data.data.user?.nickname);
  return data.data.token;
}

async function prepareLoadTest(token) {
  const url = `/sale-server/api/v1/seckill/prepareLoadTest?seckillId=${cfg.seckillId}&stock=${cfg.stock ?? 100}`;
  const { data, status } = await request('POST', url, null, token);
  if (status !== 200 || !data || data.code !== SUCCESS_CODE) {
    throw new Error('准备秒杀活动失败: ' + JSON.stringify(data));
  }
  console.log(`秒杀活动 ${cfg.seckillId} 已开启，库存重置为 ${cfg.stock ?? 100}`);
}

async function killOnce(token) {
  const body = {
    fkSeckillId: cfg.seckillId,
    fkCourseId: cfg.courseId
  };
  const { status, data, elapsed } = await request('POST', '/sale-server/api/v1/seckill/kill', body, token);
  const ok = status === 200 && data?.code === SUCCESS_CODE && !!data?.data;
  return { ok, elapsed, code: data?.code, message: data?.message, sn: data?.data };
}

function percentile(arr, p) {
  if (!arr.length) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, idx)];
}

async function runBatch(token, startIdx, count) {
  const tasks = [];
  for (let i = 0; i < count; i++) {
    const userId = cfg.userIdStart + ((startIdx + i) % cfg.totalRequests);
    tasks.push(killOnce(token));
  }
  return Promise.all(tasks);
}

async function main() {
  console.log('=== 秒杀压测 ===');
  console.log('网关:', GATEWAY);
  console.log('并发:', cfg.concurrency, '总请求:', cfg.totalRequests);

  const token = await login();
  if (cfg.prepareBeforeTest !== false) {
    await prepareLoadTest(token);
  }

  const latencies = [];
  let success = 0;
  let fail = 0;
  const errors = {};

  const started = performance.now();
  let sent = 0;
  while (sent < cfg.totalRequests) {
    const batchSize = Math.min(cfg.concurrency, cfg.totalRequests - sent);
    const results = await runBatch(token, sent, batchSize);
    for (const r of results) {
      if (r.ok) {
        success++;
        latencies.push(r.elapsed);
      } else {
        fail++;
        const key = r.message || 'unknown';
        errors[key] = (errors[key] || 0) + 1;
      }
    }
    sent += batchSize;
    process.stdout.write(`\r进度: ${sent}/${cfg.totalRequests}`);
  }
  const totalTime = performance.now() - started;

  console.log('\n\n=== 压测结果 ===');
  console.log('总耗时(ms):', totalTime.toFixed(0));
  console.log('QPS:', (cfg.totalRequests / (totalTime / 1000)).toFixed(2));
  console.log('成功:', success);
  console.log('失败:', fail);
  if (latencies.length) {
    console.log('延迟(ms) avg:', (latencies.reduce((a, b) => a + b, 0) / latencies.length).toFixed(2));
    console.log('延迟(ms) p50:', percentile(latencies, 50).toFixed(2));
    console.log('延迟(ms) p95:', percentile(latencies, 95).toFixed(2));
    console.log('延迟(ms) p99:', percentile(latencies, 99).toFixed(2));
  }
  if (Object.keys(errors).length) {
    console.log('失败原因分布:');
    for (const [k, v] of Object.entries(errors)) {
      console.log(`  ${k}: ${v}`);
    }
  }
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
