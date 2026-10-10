/**
 * Visual/behavioural verification of the built site (dist/) via headless Chrome.
 * Checks per page: structure (nav/footer/skip-link/h1), JS boot, console errors,
 * horizontal overflow, monochrome theme (no purple computed styles), heading
 * order, and the ai.html React app mount.
 */
const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');

const ROOT = path.resolve(__dirname, '..');
const BASE = 'http://localhost:8912';
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const PAGES = [
  'index.html', '404.html', 'about.html', 'blog.html', 'contact.html',
  'faq.html', 'help-us-grow.html', 'integrations.html', 'local-visibility.html',
  'marketplace.html', 'mobile-apps.html', 'portfolio.html', 'services.html',
  'synapse.html', 'vista.html', 'ai.html',
];

const results = [];
function check(page, name, ok, detail) {
  results.push({ page, name, ok, detail });
  if (!ok) console.log('  FAIL [' + page + '] ' + name + (detail ? ' — ' + detail : ''));
}

(async () => {
  const browser = await puppeteer.launch({ executablePath: CHROME, headless: 'new', args: ['--no-sandbox'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  for (const p of PAGES) {
    const errors = [];
    page.removeAllListeners('console'); page.removeAllListeners('pageerror');
    page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });
    page.on('pageerror', e => errors.push('PAGEERROR: ' + e.message));

    let navOk = true;
    await page.goto(BASE + '/' + p, { waitUntil: 'networkidle0', timeout: 30000 }).catch(e => {
      navOk = false; check(p, 'loads', false, e.message);
    });
    if (!navOk) continue;
    await new Promise(r => setTimeout(r, 700));

    const data = await page.evaluate(() => {
      const q = s => document.querySelector(s);
      const all = s => [...document.querySelectorAll(s)];
      // purple detection in computed styles
      const purple = [];
      const els = document.querySelectorAll('body *');
      for (let i = 0; i < Math.min(els.length, 3000); i++) {
        const cs = getComputedStyle(els[i]);
        for (const prop of ['backgroundColor', 'color', 'borderTopColor', 'borderBottomColor', 'backgroundImage', 'boxShadow']) {
          const v = cs[prop] || '';
          if (/124,\s*92,\s*255|107,\s*78,\s*255|155,\s*142,\s*255|124, 92/i.test(v) && v !== 'none') {
            purple.push(els[i].tagName + '.' + (els[i].className + '').toString().slice(0, 40) + ' {' + prop + ':' + v.slice(0, 60) + '}');
            if (purple.length > 4) break;
          }
        }
        if (purple.length > 4) break;
      }
      const h1s = all('h1').filter(h => h.offsetParent !== null || h.getClientRects().length);
      const headings = all('h1,h2,h3').map(h => h.tagName);
      const firstH1 = headings.indexOf('H1');
      const firstH2 = headings.indexOf('H2');
      const accent = getComputedStyle(document.documentElement).getPropertyValue('--saas-accent').trim();
      const skip = q('.skip-link');
      const skipInfo = skip ? { href: skip.getAttribute('href'), targetExists: !!q('#' + (skip.getAttribute('href') || '').slice(1)) } : null;
      const tickerLinks = all('.ticker-links__track .ticker-link').length;
      const tickerRows = all('.tb-row').length;
      return {
        nav: !!q('.velora-nav'),
        burger: !!q('.velora-burger'),
        footer: !!q('.glass-footer'),
        newsForm: !!q('#glassNews'),
        backdrop: !!q('.velora-backdrop'),
        main: !!q('#main'),
        h1Count: document.querySelectorAll('h1').length,
        h1BeforeH2: firstH1 !== -1 && (firstH2 === -1 || firstH1 < firstH2),
        headingSeq: headings.slice(0, 8).join('>'),
        accent,
        purple,
        skipInfo,
        tickerLinks, tickerRows,
        dotGrid: !!q('.dot-grid-bg'),
        chat: !!q('.chat-widget__trigger'),
        overflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      };
    });

    check(p, 'no console errors', errors.length === 0, errors.slice(0, 2).join(' | '));
    check(p, 'nav present', data.nav || p === 'ai.html');
    check(p, 'footer present', data.footer || p === 'ai.html');
    check(p, 'main landmark', data.main);
    check(p, 'exactly one h1', data.h1Count === 1, 'found ' + data.h1Count);
    check(p, 'h1 before h2', data.h1BeforeH2, data.headingSeq);
    check(p, 'skip-link → #main', !data.skipInfo || (data.skipInfo.href === '#main' && data.skipInfo.targetExists), JSON.stringify(data.skipInfo));
    check(p, 'monochrome (--saas-accent)', data.accent.toLowerCase() === '#fdfdfd' || data.accent.toLowerCase() === '#FDFDFD', data.accent);
    check(p, 'no purple computed styles', data.purple.length === 0, data.purple.join(' ; '));
    check(p, 'no horizontal overflow', data.overflowX <= 2, 'overflow ' + data.overflowX + 'px');
    check(p, 'nav behaviour wired', data.backdrop || p === 'ai.html');
    if (['index.html', 'portfolio.html', 'services.html'].includes(p)) {
      check(p, 'ticker populated', (data.tickerLinks + data.tickerRows) > 0, 'links=' + data.tickerLinks + ' rows=' + data.tickerRows);
    }
    if (p === 'index.html') {
      check(p, 'dot-grid + chat', data.dotGrid && data.chat, 'grid=' + data.dotGrid + ' chat=' + data.chat);
      check(p, 'newsletter form', data.newsForm);
    }
    if (p === 'ai.html') {
      const ai = await page.evaluate(() => {
        const root = document.querySelector('#root');
        const app = root && root.firstElementChild;
        const bar = document.querySelector('.app-bar');
        const cs = app ? getComputedStyle(app) : null;
        return {
          mounted: !!(root && root.childElementCount),
          appBar: !!bar,
          h1: (document.querySelector('h1') || {}).textContent,
          appHeight: app ? app.getBoundingClientRect().height : 0,
          viewport: window.innerHeight,
          bg: cs ? cs.backgroundColor : '',
        };
      });
      check(p, 'React app mounted', ai.mounted);
      check(p, 'app-bar + h1 label', ai.appBar && /AI Platform/.test(ai.h1 || ''), ai.h1);
      check(p, 'app fits below header (100dvh-64)', ai.appHeight <= ai.viewport - 60 && ai.appHeight > 400, 'h=' + Math.round(ai.appHeight) + ' vp=' + ai.viewport);
      check(p, 'app bg not purple/white', !/253, 253, 253|124, 92, 255/.test(ai.bg), ai.bg);
    }

    // screenshot for the record
    const dir = path.join(require('os').tmpdir(), 'lx-shots');
    fs.mkdirSync(dir, { recursive: true });
    await page.screenshot({ path: path.join(dir, p.replace('.html', '') + '-desktop.png') }).catch(() => {});
  }

  // ── Mobile spot checks ─────────────────────────────────────────────
  await page.setViewport({ width: 390, height: 844, isMobile: true, hasTouch: true });
  for (const p of ['index.html', 'ai.html', '404.html', 'marketplace.html', 'services.html']) {
    await page.goto(BASE + '/' + p, { waitUntil: 'networkidle0', timeout: 30000 });
    await new Promise(r => setTimeout(r, 500));
    const m = await page.evaluate(() => {
      const burger = document.querySelector('.velora-burger');
      const vis = burger ? getComputedStyle(burger).display !== 'none' : false;
      return {
        overflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
        burgerVisible: vis,
        nav: !!document.querySelector('.velora-nav'),
        footer: !!document.querySelector('.glass-footer'),
      };
    });
    check(p, '[mobile] no h-overflow', m.overflowX <= 2, 'overflow ' + m.overflowX + 'px');
    if (p !== 'ai.html') check(p, '[mobile] burger shown', m.burgerVisible && m.nav);
    const dir = path.join(require('os').tmpdir(), 'lx-shots');
    await page.screenshot({ path: path.join(dir, p.replace('.html', '') + '-mobile.png') }).catch(() => {});
  }

  await browser.close();

  const fails = results.filter(r => !r.ok);
  const byPage = {};
  for (const r of results) byPage[r.page] = byPage[r.page] || { pass: 0, fail: 0 };
  for (const r of results) r.ok ? byPage[r.page].pass++ : byPage[r.page].fail++;
  console.log('\n=== SUMMARY ===');
  for (const [pg, s] of Object.entries(byPage)) console.log(`${s.fail ? 'FAIL' : 'ok  '} ${pg}: ${s.pass} pass / ${s.fail} fail`);
  console.log(`\nTOTAL: ${results.length - fails.length} pass, ${fails.length} fail`);
  process.exit(fails.length ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
