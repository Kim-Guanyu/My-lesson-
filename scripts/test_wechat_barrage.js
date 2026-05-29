const automator = require('miniprogram-automator');

const PORT = process.env.WECHAT_PORT || 12837;
const wsEndpoint = `ws://127.0.0.1:${PORT}`;

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function readPageData(mp, label) {
  const page = await mp.currentPage();
  const data = await page.data();
  console.log(`\n=== ${label} ===`);
  console.log('path:', page.path);
  console.log('courseId:', data.courseId);
  console.log('videoReady:', data.videoReady);
  console.log('videoSrc:', data.videoSrc ? 'set' : 'null');
  console.log('danmuList count:', (data.danmuList || []).length);
  console.log('barrageHistoryLoaded:', data.barrageHistoryLoaded);
  if ((data.danmuList || []).length) {
    console.log('danmu sample:', data.danmuList.slice(0, 3));
  }
  return data;
}

async function main() {
  console.log('Connecting to', wsEndpoint);
  const mp = await automator.connect({ wsEndpoint });
  console.log('Connected');

  mp.on('console', msg => {
    const text = typeof msg.text === 'string' ? msg.text : JSON.stringify(msg.text);
    if (text.includes('barrage') || text.includes('error') || text.includes('Error')) {
      console.log('[console]', msg.level, text);
    }
  });

  // login token mock if needed
  await mp.evaluate(() => {
    if (!wx.getStorageSync('token')) {
      // will rely on existing devtools session login state
    }
  });

  let page = await mp.reLaunch('/pages/course/detail/detail?courseId=1');
  await sleep(5000);
  const first = await readPageData(mp, 'First enter course 1');

  // simulate leave: navigate to course list
  page = await mp.reLaunch('/pages/course/course');
  await sleep(2000);

  // re-enter detail
  page = await mp.navigateTo({ url: '/pages/course/detail/detail?courseId=1' });
  await sleep(5000);
  const second = await readPageData(mp, 'Re-enter course 1');

  const ok = second.videoReady && (second.danmuList || []).length > 0;
  console.log('\n=== RESULT ===');
  console.log(ok ? 'PASS: re-enter has danmuList and videoReady' : 'FAIL: re-enter missing danmu or video not ready');
  console.log(`first danmu=${first.danmuList?.length || 0}, second danmu=${second.danmuList?.length || 0}`);

  await mp.disconnect();
  process.exit(ok ? 0 : 1);
}

main().catch(err => {
  console.error('TEST FAILED:', err.message);
  process.exit(1);
});
