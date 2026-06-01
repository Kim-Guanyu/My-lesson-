/**
 * 对比两次压测报告
 * 用法：node compare-reports.mjs report-1000.json report-10000.json
 */
import { readFileSync, writeFileSync } from 'fs';

const [fileA, fileB] = process.argv.slice(2);
if (!fileA || !fileB) {
  console.error('用法: node compare-reports.mjs <报告A> <报告B>');
  process.exit(1);
}

function load(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function pct(n, d) {
  return d ? ((n / d) * 100).toFixed(2) + '%' : '-';
}

function latenciesFromReport(r) {
  if (r.successLatencies?.length) return r.successLatencies;
  const results = r.results || [];
  return results.filter(x => x.ok).map(x => x.elapsed);
}

function percentile(arr, p) {
  if (!arr.length) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, idx)];
}

function summarize(name, r) {
  const cfg = r.config || {};
  const s = r.summary || {};
  const v = r.verify || {};
  const total = r.total || cfg.totalRequests || 0;
  const totalTime = r.totalTimeMs || 0;
  const lats = latenciesFromReport(r);
  return {
    name,
    seckillId: cfg.seckillId,
    courseId: cfg.courseId,
    stock: cfg.stock,
    totalRequests: total,
    concurrency: cfg.concurrency,
    burstWaveSize: cfg.burstWaveSize,
    totalTimeMs: totalTime,
    qps: totalTime ? (total / (totalTime / 1000)).toFixed(2) : '-',
    success: s.success ?? 0,
    fail: s.fail ?? 0,
    successRate: pct(s.success ?? 0, total),
    latencyMin: lats.length ? Math.min(...lats).toFixed(2) : '-',
    latencyAvg: lats.length ? (lats.reduce((a, b) => a + b, 0) / lats.length).toFixed(2) : '-',
    latencyP50: lats.length ? percentile(lats, 50).toFixed(2) : '-',
    latencyP95: lats.length ? percentile(lats, 95).toFixed(2) : '-',
    latencyP99: lats.length ? percentile(lats, 99).toFixed(2) : '-',
    latencyMax: lats.length ? Math.max(...lats).toFixed(2) : '-',
    redisStock: v.redisStock ?? '-',
    dbOrderCount: v.dbOrderCount ?? '-',
    apiSuccess: v.apiSuccess ?? s.success ?? '-',
    uniqueSn: v.uniqueSn ?? '-',
    oversell: v.oversell ? '是' : '否',
    errors: s.errors || {}
  };
}

function delta(a, b, key, fmt = (x) => x) {
  const va = Number(a[key]);
  const vb = Number(b[key]);
  if (Number.isNaN(va) || Number.isNaN(vb)) return '-';
  const d = vb - va;
  const sign = d > 0 ? '+' : '';
  if (key.includes('Rate') || key.includes('qps')) return `${sign}${d.toFixed(2)}`;
  return `${sign}${fmt(d)}`;
}

const rA = load(fileA);
const rB = load(fileB);
const a = summarize('1000并发', rA);
const b = summarize('10000并发', rB);

const rows = [
  ['指标', a.name, b.name, '变化(万-千)'],
  ['秒杀活动ID', a.seckillId, b.seckillId, '-'],
  ['课程ID', a.courseId, b.courseId, '-'],
  ['初始库存', a.stock, b.stock, '-'],
  ['总请求数', a.totalRequests, b.totalRequests, delta(a, b, 'totalRequests')],
  ['客户端并发', a.concurrency, b.concurrency, delta(a, b, 'concurrency')],
  ['突发波大小', a.burstWaveSize ?? '-', b.burstWaveSize ?? '-', '-'],
  ['总耗时(ms)', a.totalTimeMs.toFixed(0), b.totalTimeMs.toFixed(0), delta(a, b, 'totalTimeMs', (x) => x.toFixed(0))],
  ['QPS', a.qps, b.qps, delta(a, b, 'qps')],
  ['API成功', a.success, b.success, delta(a, b, 'success')],
  ['API失败', a.fail, b.fail, delta(a, b, 'fail')],
  ['成功率', a.successRate, b.successRate, '-'],
  ['延迟min(ms)', a.latencyMin, b.latencyMin, '-'],
  ['延迟avg(ms)', a.latencyAvg, b.latencyAvg, delta(a, b, 'latencyAvg')],
  ['延迟p50(ms)', a.latencyP50, b.latencyP50, delta(a, b, 'latencyP50')],
  ['延迟p95(ms)', a.latencyP95, b.latencyP95, delta(a, b, 'latencyP95')],
  ['延迟p99(ms)', a.latencyP99, b.latencyP99, delta(a, b, 'latencyP99')],
  ['延迟max(ms)', a.latencyMax, b.latencyMax, delta(a, b, 'latencyMax')],
  ['Redis剩余库存', a.redisStock, b.redisStock, '-'],
  ['DB订单数', a.dbOrderCount, b.dbOrderCount, '-'],
  ['是否超卖', a.oversell, b.oversell, '-']
];

console.log('\n========== 秒杀压测对比 ==========\n');
const colW = [22, 14, 14, 14];
for (const row of rows) {
  console.log(row.map((c, i) => String(c).padEnd(colW[i])).join(''));
}
console.log('\n--- 失败原因 ---');
console.log(`${a.name}:`, a.errors);
console.log(`${b.name}:`, b.errors);

const out = `compare-${Date.now()}.json`;
writeFileSync(out, JSON.stringify({ baseline: a, target: b, files: [fileA, fileB] }, null, 2));
console.log('\n对比 JSON 已写入:', out);
