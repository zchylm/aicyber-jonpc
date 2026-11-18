import { test, expect, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';

const manifest = JSON.parse(readFileSync(new URL('../dist/storefront-manifest.json', import.meta.url), 'utf8'));
const productPath = manifest.find((entry: { page: { kind: string } }) => entry.page.kind === 'product').path;
const productDocument = readFileSync(new URL(`../dist${productPath}index.html`, import.meta.url), 'utf8');
const catalogue = JSON.parse(productDocument.match(/<script id="storefront-data"[^>]*>([\s\S]*?)<\/script>/)![1]).systems;

const modules = [
  ['Gaming PCs', '#gaming-pcs'],
  ['Mini PCs', '#compact-ai-systems'],
  ['Custom PCs', '#build'],
  ['AI Solutions', '#ai-infrastructure'],
  ['Support', '#support'],
];

async function home(page: Page) {
  await page.goto('/');
  await expect(page.locator('.first-batch-card')).toHaveCount(8);
}

async function moduleInView(page: Page, target: string) {
  await expect(page).toHaveURL(new RegExp(`${target}$`));
  await expect.poll(async () => {
    const box = await page.locator(target).boundingBox();
    return box !== null && box.y > -100 && box.y < page.viewportSize()!.height / 2;
  }, { message: `${target} must actually scroll into the viewport` }).toBe(true);
}

test.beforeEach(async ({ page }) => {
  // This suite reads catalogues and exercises guest UI; it must never create an order or send a message.
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    expect(['GET', 'OPTIONS']).toContain(route.request().method());
    if (new URL(route.request().url()).pathname === '/api/systems/preview') {
      await route.fulfill({ json: catalogue, headers: { 'access-control-allow-origin': '*' } });
      return;
    }
    await route.continue();
  });
  page.on('pageerror', error => { throw error; });
});

for (const width of [1440, 390]) {
  test(`homepage navigation and Hero categories preserve in-page scrolling at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    await home(page);
    const primary = page.getByRole('navigation', { name: 'Primary navigation', exact: true });
    for (const [label, target] of modules) {
      const link = primary.getByRole('link', { name: label, exact: true });
      await expect(link).toHaveAttribute('href', target);
      await link.click();
      expect(new URL(page.url()).pathname).toBe('/');
      await moduleInView(page, target);
    }
    for (const [label, target] of modules.slice(0, 3)) {
      const link = page.locator('.hero-categories').getByRole('link').filter({ hasText: label });
      await expect(link).toHaveAttribute('href', target);
      await link.click();
      await moduleInView(page, target);
    }
  });
}

test('Mini PC cards use a single details link for their fixed configurations', async ({ page }) => {
  await home(page);
  for (let index = 0; index < 4; index++) {
    const card = page.locator('.mini-system-card').nth(index);
    const name = await card.locator('h3').innerText();
    const href = await card.locator('h3 a').getAttribute('href');
    const link = card.getByRole('link', { name: 'View details', exact: true });
    await expect(link).toHaveCount(1);
    await expect(link).toHaveAttribute('href', href!);
    await expect(card.getByRole('button')).toHaveCount(0);
    await expect(card.locator('.first-batch-card-bottom')).not.toContainText('From');
    await link.click();
    await expect(page.locator('#product-title')).toHaveText(`${name}.`);
    await expect(page.getByRole('heading', { name: 'Specifications' })).toBeVisible();
    await expect(page.getByRole('group', { name: 'System RAM' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Add to cart' })).toBeVisible();
    await page.goBack();
    await expect(page.locator('.mini-system-card')).toHaveCount(4);
  }
});

for (const width of [1440, 390]) {
  test(`all eight product images link to their matching product pages at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    await home(page);
    for (let index = 0; index < 8; index++) {
      const card = page.locator('.first-batch-card').nth(index);
      const name = await card.locator('h3').innerText();
      const href = await card.locator('h3 a').getAttribute('href');
      const imageLink = card.getByRole('link').filter({ has: page.getByRole('img') });
      await expect(imageLink).toHaveAttribute('href', href!);
      if (width === 1440 && (index === 0 || index === 4)) {
        await imageLink.focus();
        await page.keyboard.press('Enter');
      } else {
        await imageLink.getByRole('img').click();
      }
      await expect(page).toHaveURL(new RegExp(`${href}$`));
      await expect(page.locator('#product-title')).toHaveText(`${name}.`);
      await page.goBack();
      await expect(page.locator('.first-batch-card')).toHaveCount(8);
    }
  });
}

test('comparisons and the product-page guest cart are reachable from the homepage', async ({ page }) => {
  await home(page);
  for (const [button, title] of [['Compare gaming PCs', 'Compare gaming PCs.'], ['Compare mini PCs', 'Choose your edge.']]) {
    await page.getByRole('button', { name: button, exact: false }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Close', exact: true }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeHidden();
  }
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: catalogue.map((system: { sku: string }) => ({ ...system, available: true, saleEnabled: true })), headers: { 'access-control-allow-origin': '*' } }));
  const strike = page.locator('.first-batch-card').first();
  await strike.getByRole('link', { name: 'Choose configuration' }).click();
  await page.getByRole('button', { name: 'Add to cart' }).click();
  await expect(page.getByText('Sign in to add items and view your shopping bag.', { exact: true })).toBeVisible();
});

test('separate product pages open explicitly, refresh correctly and never fall back to the homepage', async ({ page }) => {
  await home(page);
  await page.locator('.first-batch-card').first().getByRole('link', { name: 'Choose configuration' }).click();
  await expect(page.locator('#product-title')).toHaveText('Strike 5060.');
  await expect(page.locator('.hero')).toHaveCount(0);
  await page.reload();
  await expect(page.locator('#product-title')).toHaveText('Strike 5060.');
  const response = await page.goto('/products/edge-mini/');
  expect(response!.status()).toBe(200);
  await expect(page.locator('#product-title')).toHaveText('Edge Mini.');
  await expect(page.locator('.hero')).toHaveCount(0);
});

test('navigation from a separate page returns to the original homepage modules', async ({ page }) => {
  for (const [label, target] of modules) {
    await page.goto('/products/strike-5060/');
    const link = page.getByRole('navigation', { name: 'Primary navigation', exact: true }).getByRole('link', { name: label, exact: true });
    await expect(link).toHaveAttribute('href', `/${target}`);
    await link.click();
    await expect(page.locator('.hero')).toBeVisible();
    await moduleInView(page, target);
  }
});

test('an early navigation click still reaches its module after the catalogue finishes loading', async ({ page }) => {
  let releaseCatalogue: (() => void) | undefined;
  await page.route(url => url.pathname === '/api/systems/preview', async route => {
    await new Promise<void>(resolve => { releaseCatalogue = resolve; });
    await route.fulfill({ json: catalogue, headers: { 'access-control-allow-origin': '*' } });
  });
  for (const [label, target] of modules.slice(0, 3)) {
    releaseCatalogue = undefined;
    await page.goto('/');
    await expect(page.locator('#systems')).toHaveAttribute('aria-busy', 'true');
    await expect.poll(() => Boolean(releaseCatalogue)).toBe(true);
    await page.getByRole('navigation', { name: 'Primary navigation', exact: true }).getByRole('link', { name: label, exact: true }).click();
    releaseCatalogue!();
    await expect(page.locator('.first-batch-card')).toHaveCount(8);
    await moduleInView(page, target);
  }
});

for (const width of [1440, 390]) {
  test(`ordinary homepage refresh consistently returns to Hero at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 });
    await home(page);
    await page.evaluate(() => {
      localStorage.setItem('jonpc:shopping-bag:v2:refresh-test', '[{"id":"preserved"}]');
      sessionStorage.setItem('jonpc:stripe-request:refresh-test', 'preserved');
    });
    for (const [label, target] of modules) {
      await page.getByRole('navigation', { name: 'Primary navigation', exact: true }).getByRole('link', { name: label, exact: true }).click();
      await moduleInView(page, target);
      await page.reload();
      await expect(page.locator('.first-batch-card')).toHaveCount(8);
      expect(new URL(page.url()).hash).toBe('');
      await expect.poll(() => page.evaluate(() => scrollY)).toBe(0);
      await expect(page.locator('.hero')).toBeInViewport();
    }
    await page.evaluate(() => window.scrollTo({ top: 2500, behavior: 'instant' }));
    await page.reload();
    await expect(page.locator('.first-batch-card')).toHaveCount(8);
    await expect.poll(() => page.evaluate(() => scrollY)).toBe(0);
    expect(await page.evaluate(() => localStorage.getItem('jonpc:shopping-bag:v2:refresh-test'))).toBe('[{"id":"preserved"}]');
    expect(await page.evaluate(() => sessionStorage.getItem('jonpc:stripe-request:refresh-test'))).toBe('preserved');
  });
}

test('incoming module links and explicit order/payment links keep their intended targets', async ({ page }) => {
  await page.goto('/#compact-ai-systems');
  await moduleInView(page, '#compact-ai-systems');
  await page.goto('/?open=orders&order=QA-REFRESH#support');
  await moduleInView(page, '#support');
  await page.reload();
  await moduleInView(page, '#support');
  expect(new URL(page.url()).searchParams.get('order')).toBe('QA-REFRESH');
  // The guest return cannot request a payment without an authenticated account.
  await page.goto('/?stripe_result=success&stripe_payment=QA-REFRESH#support');
  await page.reload();
  expect(new URL(page.url()).hash).toBe('#support');
  expect(new URL(page.url()).searchParams.get('stripe_payment')).toBe('QA-REFRESH');
  await expect(page.locator('.payment-modal')).toBeVisible();
});

test('gaming cards use orderable starting prices and compare four models by RAM', async ({ page }) => {
  test.skip(!catalogue.some((system: { sku: string }) => system.sku.endsWith('-32GB')), 'Requires the RAM catalogue migration');
  const allInStock = catalogue.map((system: { sku: string }) => ({ ...system, available: true, saleEnabled: true }));
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: allInStock, headers: { 'access-control-allow-origin': '*' } }));
  await home(page);
  const gaming = page.locator('.first-batch-card:not(.mini-system-card)');
  await expect(gaming).toHaveCount(4);
  const skus = ['JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI'];
  const format = (sku: string) => `A$${(allInStock.find((system: { sku: string }) => system.sku === sku).priceCents / 100).toLocaleString('en-AU')}`;
  const menu = skus.map(format);
  for (const [index, price] of menu.entries()) {
    const card = gaming.nth(index);
    await expect(card.locator('.first-batch-card-bottom strong')).toContainText(`From ${price}`);
    await expect(card.getByRole('link', { name: 'Choose configuration' })).toHaveCount(1);
    await expect(card.getByRole('button', { name: 'Add to cart' })).toHaveCount(0);
  }
  await page.getByRole('button', { name: /Compare gaming PCs/ }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('columnheader')).toHaveCount(5);
  await expect(dialog.getByRole('row', { name: /^16GB RAM/ }).getByRole('cell')).toHaveText(menu.map((price, index) => price + (index === 0 ? 'Default' : '')));
  await expect(dialog.getByRole('row', { name: /^32GB RAM/ }).getByRole('cell')).toHaveText(skus.map((sku, index) => format(`${sku}-32GB`) + (index ? 'Default' : '')));
  await dialog.getByRole('link', { name: 'Configure Strike Ti 5060 Ti' }).click();
  await expect(page).toHaveURL(/\/products\/strike-ti-5060-ti\/$/);
  await expect(page.getByRole('radio', { name: /32GB DDR5/ })).toBeChecked();
});

test('a sold-out starting configuration cannot set the homepage price', async ({ page }) => {
  test.skip(!catalogue.some((system: { sku: string }) => system.sku === 'JON-STK-5060-32GB'), 'Requires the RAM catalogue migration');
  const stock = catalogue.map((system: { sku: string }) => ({ ...system, available: system.sku !== 'JON-STK-5060', saleEnabled: true }));
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: stock, headers: { 'access-control-allow-origin': '*' } }));
  await home(page);
  const card = page.locator('.first-batch-card').filter({ has: page.getByRole('heading', { name: 'Strike 5060', exact: true }) });
  const price = stock.find((system: { sku: string }) => system.sku === 'JON-STK-5060-32GB').priceCents;
  await expect(card.locator('.first-batch-card-bottom strong')).toHaveText(`A$${(price / 100).toLocaleString('en-AU')} incl. GST`);
  await expect(card).toContainText('In stock');
});
