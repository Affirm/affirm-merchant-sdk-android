// Headless Chrome checks for affirm_promo.html. Set CHROME_PATH if Chrome is not in the default place.
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const puppeteer = require('puppeteer-core');

const RAW = path.resolve(__dirname, '../../main/res/raw');
const TEMPLATE = fs.readFileSync(path.join(RAW, 'affirm_promo.html'), 'utf8');
const SANITIZER = fs.readFileSync(path.join(RAW, 'dompurify.js'), 'utf8');
const BADGE_CSS = fs.readFileSync(path.join(RAW, 'badge_styles.css'), 'utf8');
const CHROME_PATH = process.env.CHROME_PATH
    || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const PAGE_URL = 'https://promo.test/';
const PNG_1X1 = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
    'base64');

let browser;

before(async () => {
    browser = await puppeteer.launch({ executablePath: CHROME_PATH, headless: true });
});

after(async () => {
    await browser.close();
});

const encode = (s) => Buffer.from(s, 'utf8').toString('base64');

/** Mirrors PromotionWebView.initialHtml. */
function fillTemplate({ html, font, sanitizer, remoteCss, extraCss }) {
    const values = {
        SANITIZER_JS: sanitizer, AFFIRM_FONT: encode(font), API_KEY: 'test_key', JAVASCRIPT: '',
        LOCALE: 'en_US', COUNTRY_CODE: 'USA', HTML_FRAGMENT: encode(html),
        REMOTE_CSS_URL: encode(remoteCss), BUNDLED_CSS: BADGE_CSS + extraCss,
    };
    return Object.entries(values)
        .reduce((page, [key, value]) => page.split(`{{${key}}}`).join(value), TEMPLATE);
}

/**
 * @param resources https URL -> { type, body } served to the page; any other https URL fails.
 */
async function render({
    html, font = '', sanitizer = SANITIZER, remoteCss = '', extraCss = '',
    resources = {}, hang = [], width = 360, waitMs = 3000,
}) {
    const page = await browser.newPage();
    await page.setViewport({ width, height: 640 });
    await page.setRequestInterception(true);
    const filled = fillTemplate({ html, font, sanitizer, remoteCss, extraCss });
    page.on('request', (request) => {
        const url = request.url();
        if (url === PAGE_URL) {
            request.respond({ contentType: 'text/html', body: filled });
        } else if (hang.includes(url)) {
            return;
        } else if (resources[url]) {
            request.respond({ contentType: resources[url].type, body: resources[url].body });
        } else if (url.startsWith('data:')) {
            request.continue();
        } else {
            request.abort();
        }
    });
    await page.evaluateOnNewDocument(() => {
        window.hostCalls = [];
        window.sanitized = null;
        window.AffirmPromoHost = {
            onSanitized: (usable) => { window.sanitized = usable; },
            onReady: (height) => window.hostCalls.push(['ready', height]),
            onFailure: (reason) => window.hostCalls.push(['failure', reason]),
            onHeightChanged: (height) => window.hostCalls.push(['height', height]),
        };
    });
    await page.goto(PAGE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForFunction(() => window.hostCalls.length > 0, { timeout: waitMs });
    const calls = await page.evaluate(() => window.hostCalls);
    const rootHtml = await page.$eval('#affirm-promo-root', (root) => root.innerHTML);
    const sanitized = await page.evaluate(() => window.sanitized);
    return { page, calls, rootHtml, sanitized };
}

test('script breakout, unsafe URLs, handlers and clobbering are removed', async () => {
    const { page, calls, rootHtml, sanitized } = await render({
        html: '<span class="affirm-ala-price">$50/mo</span>'
            + '<img src="x" onerror="window.pwned=1"><script>window.pwned=2</script>'
            + '<a href="javascript:alert(1)">js</a><a href="http://evil.test">http</a>'
            + '<a href="https://ok.test">ok</a>'
            + '<form id="AffirmPromoHost"><input name="onReady"></form>'
            + '<span style="color:red" id="clobber" name="n" aria-label="y">s</span>'
            + '<iframe src="https://frame.test"></iframe><img src="data:image/png;base64,AA">'
            + '</p></script><script>window.pwned=3</script>',
    });
    assert.equal(await page.evaluate(() => window.pwned), undefined);
    assert.equal(await page.evaluate(() => typeof window.AffirmPromoHost.onReady), 'function');
    assert.equal(await page.$$eval('#affirm-promo-root', (roots) => roots.length), 1);
    assert.match(rootHtml, /class="affirm-ala-price"/);
    assert.match(rootHtml, /href="https:\/\/ok\.test"/);
    assert.doesNotMatch(rootHtml, /javascript:|http:\/\/evil|<script|<iframe|<form|<input/);
    assert.doesNotMatch(rootHtml, /style=|id=|name=|aria-|on\w+=/);
    assert.doesNotMatch(rootHtml, /src="x"|data:image/);
    assert.deepEqual(calls[0][0], 'ready');
    assert.equal(sanitized, true);
    await page.close();
});

test('font declaration keeps only style and https stylesheet links', async () => {
    const { page, calls } = await render({
        html: '<span>ok</span>',
        font: '<link rel="stylesheet" href="https://fonts.test/a.css">'
            + '<link rel="stylesheet" href="http://fonts.test/b.css">'
            + '<style>.x{font-family:A}</style><script>window.pwned=4</script>'
            + '<img src="x" onerror="window.pwned=5">',
        resources: { 'https://fonts.test/a.css': { type: 'text/css', body: '.y{}' } },
    });
    assert.equal(await page.evaluate(() => window.pwned), undefined);
    assert.ok(await page.$('head link[href="https://fonts.test/a.css"]'));
    assert.equal(await page.$('head link[href^="http://"]'), null);
    assert.ok(await page.$$eval('head style',
        (styles) => styles.some((style) => style.textContent.includes('font-family:A'))));
    assert.equal(calls[0][0], 'ready');
    await page.close();
});

test('badge classes survive and merchant CSS of equal specificity overrides them', async () => {
    const badge = '<span class="__apr_badge __apr_badge_indigo">0% APR</span>';
    const badgeColor = (page) =>
        page.$eval('.__apr_badge', (el) => getComputedStyle(el).color);

    const bundledOnly = await render({ html: badge });
    assert.equal(bundledOnly.calls[0][0], 'ready');
    assert.equal(await badgeColor(bundledOnly.page), 'rgb(66, 66, 207)');
    await bundledOnly.page.close();

    const withMerchant = await render({
        html: badge,
        remoteCss: 'https://merchant.test/promo.css',
        resources: {
            'https://merchant.test/promo.css': {
                type: 'text/css', body: '.__apr_badge.__apr_badge_indigo{color:rgb(1, 2, 3)}',
            },
        },
    });
    assert.equal(withMerchant.calls[0][0], 'ready');
    assert.equal(await badgeColor(withMerchant.page), 'rgb(1, 2, 3)');
    await withMerchant.page.close();
});

test('remote CSS URL with a quote cannot inject attributes', async () => {
    const { page, calls } = await render({
        html: '<span>ok</span>',
        remoteCss: 'https://merchant.test/a.css" onload="window.pwned=7',
    });
    assert.deepEqual(calls, [['failure', 'resource_failed']]);
    assert.equal(await page.evaluate(() => window.pwned), undefined);
    await page.close();
});

test('sanitizer missing or unsupported fails closed', async () => {
    const { page, calls, rootHtml, sanitized } = await render({ html: '<span>ok</span>', sanitizer: '' });
    assert.deepEqual(calls, [['failure', 'sanitizer_unavailable']]);
    assert.equal(rootHtml, '');
    assert.equal(sanitized, null);
    await page.close();
});

test('content that is empty after sanitizing fails', async () => {
    const { page, calls, sanitized } = await render({ html: '<script>window.pwned=6</script>' });
    assert.deepEqual(calls, [['failure', 'html_empty']]);
    assert.equal(sanitized, false);
    await page.close();

    const strippedImg = await render({ html: '<img src="javascript:alert(1)" onerror="window.pwned=8">' });
    assert.deepEqual(strippedImg.calls, [['failure', 'html_empty']]);
    assert.equal(strippedImg.sanitized, false);
    await strippedImg.page.close();
});

test('remote CSS, font CSS and image load errors fail', async () => {
    const css = await render({ html: '<span>ok</span>', remoteCss: 'https://merchant.test/missing.css' });
    assert.deepEqual(css.calls, [['failure', 'resource_failed']]);
    await css.page.close();

    const fontCss = await render({
        html: '<span>ok</span>',
        font: '<link rel="stylesheet" href="https://fonts.test/missing.css">',
    });
    assert.deepEqual(fontCss.calls, [['failure', 'resource_failed']]);
    await fontCss.page.close();

    const image = await render({ html: '<img src="https://cdn.test/missing.png">' });
    assert.deepEqual(image.calls, [['failure', 'image_failed']]);
    await image.page.close();

    const decodedButBroken = await render({
        html: '<span>ok</span><img src="https://cdn.test/not-an-image.png">',
        resources: {
            'https://cdn.test/not-an-image.png': { type: 'image/png', body: 'not-a-png' },
        },
    });
    assert.deepEqual(decodedButBroken.calls, [['failure', 'image_failed']]);
    await decodedButBroken.page.close();
});

test('required resources that never settle fail with html_timeout', { timeout: 10000 }, async () => {
    const { page, calls } = await render({
        html: '<span>ok</span>',
        remoteCss: 'https://merchant.test/hang.css',
        hang: ['https://merchant.test/hang.css'],
        waitMs: 7000,
    });
    assert.deepEqual(calls, [['failure', 'html_timeout']]);
    await page.close();
});

test('ready waits for images and reports the content height', async () => {
    const { page, calls } = await render({
        html: '<span>ok</span><img src="https://cdn.test/logo.png" width="10" height="40">',
        resources: { 'https://cdn.test/logo.png': { type: 'image/png', body: PNG_1X1 } },
    });
    assert.equal(calls[0][0], 'ready');
    assert.ok(calls[0][1] >= 40 && calls[0][1] < 200,
        `height ${calls[0][1]} should follow the promo root, not the viewport`);
    await page.close();
});

test('long and localized text wraps inside the view and reports height changes', async () => {
    const longText = 'Paga en cuotas mensuales con Affirm 月々のお支払い '.repeat(12)
        + 'Supercalifragilisticexpialidocious'.repeat(4);
    const { page, calls } = await render({
        html: `<span class="affirm-ala-price">${longText}</span>`,
        extraCss: '#affirm-promo-root{word-wrap:break-word;overflow-wrap:break-word;}',
        width: 240,
    });
    assert.equal(calls[0][0], 'ready');
    const widths = await page.evaluate(() => ({
        content: document.documentElement.scrollWidth,
        viewport: document.documentElement.clientWidth,
    }));
    assert.ok(widths.content <= widths.viewport, JSON.stringify(widths));
    const before = calls[0][1];
    await page.$eval('#affirm-promo-root', (root) => { root.style.paddingBottom = '200px'; });
    await page.waitForFunction(
        (initial) => window.hostCalls.some(([kind, h]) => kind === 'height' && h > initial),
        { timeout: 3000 }, before);
    await page.close();
});
