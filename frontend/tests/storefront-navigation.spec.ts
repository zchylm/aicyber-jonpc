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

test('all eight View system controls retain the original detail dialogs and browser Back behavior', async ({ page }) => {
  await home(page);
  for (let index = 0; index < 8; index++) {
    const card = page.locator('.first-batch-card').nth(index);
    const name = await card.locator('h3').innerText();
    const url = page.url();
    const button = card.getByRole('button', { name: 'View system', exact: false });
    await expect(button).toBeVisible();
    await button.click();
    const dialog = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name, exact: true }) });
    await expect(dialog).toBeVisible();
    expect(page.url()).toBe(url);
    await expect(dialog.getByRole('button', { name: 'Buy now', exact: false })).toBeVisible();
    await expect(dialog.getByRole('button', { name: 'Add to cart', exact: false })).toBeVisible();
    await dialog.getByText('Full specifications', { exact: true }).click();
    await expect(dialog.locator('.first-batch-full-specs > div').first()).toBeVisible();
    if (index === 4) await expect(dialog.locator('.mini-system-detail-image img')).toHaveAttribute('src', /edge-mini-exploded/);
    await expect(dialog.getByRole('link', { name: 'Open product page', exact: false })).toHaveAttribute('href', /^\/products\//);
    if (index === 0) await page.goBack();
    else await dialog.getByRole('button', { name: 'Close', exact: true }).click();
    await expect(dialog).toBeHidden();
  }
});

test('existing comparisons and guest cart remain available from the homepage', async ({ page }) => {
  await home(page);
  for (const [button, title] of [['Compare gaming PCs', 'Choose your graphics.'], ['Compare mini PCs', 'Choose your edge.']]) {
    await page.getByRole('button', { name: button, exact: false }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Close', exact: true }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeHidden();
  }
  const available = catalogue.find((system: { available: boolean; saleEnabled: boolean }) => system.available && system.saleEnabled);
  if (available) {
    const card = page.locator('.first-batch-card').filter({ has: page.getByRole('heading', { name: available.name.replace('JON PC ', ''), exact: true }) });
    await card.getByRole('button', { name: 'View system', exact: false }).click();
    await page.locator('.first-batch-dialog').getByRole('button', { name: 'Add to cart', exact: false }).click();
  } else {
    await page.getByRole('button', { name: 'Open shopping bag', exact: true }).click();
  }
  await expect(page.getByText('Sign in to add items and view your shopping bag.', { exact: true })).toBeVisible();
});

test('separate product pages open explicitly, refresh correctly and never fall back to the homepage', async ({ page }) => {
  await home(page);
  await page.locator('.first-batch-card').first().getByRole('button', { name: 'View system', exact: false }).click();
  await page.getByRole('link', { name: 'Open product page', exact: false }).click();
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
