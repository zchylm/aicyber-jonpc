import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import type { PreviewSystem } from '../src/api/systems';

const document = readFileSync(new URL('../dist/products/strike-5060/index.html', import.meta.url), 'utf8');
const catalogue = (JSON.parse(document.match(/<script id="storefront-data"[^>]*>([\s\S]*?)<\/script>/)![1]).systems as PreviewSystem[])
  .map(system => ({ ...system, available: true, saleEnabled: true }));
const active = { available: true, programStatus: 'ACTIVE', currency: 'AUD', currentRateBasisPoints: 500, remainingPositions: 50 };
const money = (cents: number) => new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' }).format(cents / 100);

test.beforeEach(async ({ page }) => {
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    expect(['GET', 'OPTIONS']).toContain(route.request().method());
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/systems/preview') return route.fulfill({ json: catalogue, headers: { 'access-control-allow-origin': '*' } });
    if (path === '/api/rewards/summary') return route.fulfill({ json: active, headers: { 'access-control-allow-origin': '*' } });
    await route.continue();
  });
  page.on('pageerror', error => { throw error; });
});

test('homepage assurance links open the existing policy and leave product cards unchanged', async ({ page }) => {
  await page.goto('/');
  const assurance = page.getByRole('navigation', { name: 'Delivery and warranty', exact: true });
  await expect(assurance.getByRole('button')).toHaveCount(4);
  await expect(page.getByText('JON.PC CARE', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Here for your PC.', { exact: true })).toHaveCount(0);
  await assurance.scrollIntoViewIfNeeded();
  await page.screenshot({ path: '../output/assurance-simple/home-assurance.png' });
  await assurance.getByRole('button', { name: /3-year warranty/ }).click();
  await expect(page.getByRole('dialog')).toContainText('Three-year warranty.');
  await expect(page.getByRole('dialog')).toContainText('throughout the warranty period');
  await expect(page.getByRole('dialog')).toContainText('3-year parts and return-to-base warranty');
  await expect(page.getByRole('dialog')).not.toContainText('2-year return-to-base');
  await page.getByRole('button', { name: 'Close policies', exact: true }).click();
  await assurance.getByRole('button', { name: /Stress tested/ }).click();
  await expect(page.getByRole('dialog')).toContainText('stability and thermal stress testing before dispatch');
  await page.getByRole('button', { name: 'Close policies', exact: true }).click();
  await assurance.getByRole('button', { name: /Local pickup/ }).click();
  await expect(page.getByRole('dialog')).toContainText('by appointment');
  await page.getByRole('button', { name: 'Close policies', exact: true }).click();
  await assurance.getByRole('button', { name: /7–14 business days/ }).click();
  await expect(page.getByRole('dialog')).toContainText('cleared payment');
  await expect(page.locator('.first-batch-card')).toHaveCount(8);
  await expect(page.locator('.first-batch-card').first()).not.toContainText('Ryzen');
});

test('product keeps specifications below the purchase area and shows the exact Founder price', async ({ page }) => {
  await page.goto('/products/strike-5060/');
  const product = catalogue.find((item: { sku: string }) => item.sku === 'JON-STK-5060');
  const saving = Math.floor((product.priceCents * 500 + 5000) / 10000);
  const specifications = page.getByRole('region', { name: 'Specifications' });
  await expect(specifications).toContainText(product.specifications.CPU);
  await expect(specifications).toContainText(product.specifications.GPU);
  await expect(page.locator('.storefront-product-copy')).not.toContainText(product.specifications.CPU);
  await expect(page.locator('.storefront-product-copy')).not.toContainText(product.specifications.GPU);
  await expect(page.locator('.storefront-regular-price s')).toHaveText(money(product.priceCents));
  await expect(page.locator('.storefront-founder-savings')).toHaveText(`Save ${money(saving)} · 5% off`);
  await expect(page.locator('.storefront-price strong')).toHaveText(money(product.priceCents - saving));
  await expect(page.locator('.storefront-offer-note')).toContainText('Limited places at checkout');
  const purchase = page.locator('.storefront-product-copy');
  await expect(purchase).not.toContainText('Melbourne delivery');
  await expect(purchase).not.toContainText('West Melbourne pickup');
  await expect(page.getByRole('button', { name: /3-year warranty/ })).toContainText('Parts, labour & return shipping');
  await page.locator('.storefront-product').screenshot({ path: '../output/assurance-simple/product-desktop.png' });
  await page.getByRole('button', { name: /3-year warranty/ }).click();
  await expect(page.getByRole('dialog')).toContainText('Three-year warranty.');
});

test('Founder preview uses checkout rounding at a half-cent boundary', async ({ page }) => {
  const products = catalogue.map((item: { sku: string }) => item.sku === 'JON-STK-5060' ? { ...item, priceCents: 254910 } : item);
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: products, headers: { 'access-control-allow-origin': '*' } }));
  await page.goto('/products/strike-5060/');
  await expect(page.locator('.storefront-founder-savings')).toHaveText('Save $127.46 · 5% off');
  await expect(page.locator('.storefront-price strong')).toHaveText('$2,421.64');
});

for (const state of ['full', 'paused', 'failed']) {
  test(`Founder ${state} does not advertise a discounted price or block purchase`, async ({ page }) => {
    await page.route(url => url.pathname === '/api/rewards/summary', route => state === 'failed'
      ? route.fulfill({ status: 503, json: {}, headers: { 'access-control-allow-origin': '*' } })
      : route.fulfill({ json: { ...active, programStatus: state === 'paused' ? 'PAUSED' : 'CLOSED', remainingPositions: state === 'full' ? 0 : 50 }, headers: { 'access-control-allow-origin': '*' } }));
    await page.goto('/products/strike-5060/');
    await expect(page.locator('.storefront-product-actions').getByRole('button', { name: /Buy now/ })).toBeEnabled();
    await expect(page.locator('.storefront-offer-note')).toHaveCount(0);
    await expect(page.locator('.storefront-price strong')).toHaveText(money(catalogue.find((item: { sku: string }) => item.sku === 'JON-STK-5060').priceCents));
  });
}

test('mobile quick purchase appears only after the original actions, moves chat clear and uses the existing sign-in flow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/products/strike-5060/');
  await expect(page.locator('.storefront-offer-note')).toBeVisible();
  await expect(page.getByRole('complementary', { name: 'Quick purchase' })).toHaveCount(0);
  await page.locator('.storefront-specifications').scrollIntoViewIfNeeded();
  const quick = page.getByRole('complementary', { name: 'Quick purchase' });
  await expect(quick).toBeVisible();
  const chat = await page.locator('.ai-launcher').boundingBox();
  const bar = await quick.boundingBox();
  expect(chat!.y + chat!.height).toBeLessThanOrEqual(bar!.y);
  await page.screenshot({ path: '../output/assurance-simple/product-mobile-purchase.png' });
  await page.evaluate(() => scrollTo({ top: 0, behavior: 'instant' }));
  await expect(quick).toHaveCount(0);
  await page.locator('.storefront-specifications').scrollIntoViewIfNeeded();
  await expect(quick).toBeVisible();
  await quick.getByRole('button', { name: /Buy now/ }).click();
  await expect(page.locator('.auth-modal')).toBeVisible();
  await expect(quick).toBeHidden();
});

test('a slow promotion lookup leaves regular-price purchasing available', async ({ page }) => {
  let finish: (() => void) | undefined;
  await page.route(url => url.pathname === '/api/rewards/summary', async route => {
    await new Promise<void>(resolve => { finish = resolve; });
    await route.fulfill({ json: active, headers: { 'access-control-allow-origin': '*' } });
  });
  await page.goto('/products/strike-5060/');
  await expect(page.locator('.storefront-product-actions').getByRole('button', { name: /Buy now/ })).toBeEnabled();
  await expect(page.locator('.storefront-offer-note')).toHaveCount(0);
  await expect.poll(() => Boolean(finish)).toBe(true);
  finish!();
  await expect(page.locator('.storefront-offer-note')).toBeVisible();
});

test('Mini PC overview retains processor details and the same discount rules', async ({ page }) => {
  await page.goto('/products/edge-mini/');
  const product = catalogue.find((item: { sku: string }) => item.sku === 'JON-EDGE-MINI');
  const saving = Math.floor((product.priceCents * 500 + 5000) / 10000);
  await expect(page.getByRole('region', { name: 'Specifications' })).toContainText(product.specifications.Processor);
  await expect(page.locator('.storefront-product-copy')).not.toContainText(product.specifications.Processor);
  await expect(page.locator('.storefront-price strong')).toHaveText(money(product.priceCents - saving));
  await expect(page.getByRole('button', { name: /3-year warranty/ })).toBeVisible();
});

test('unavailable products never show Founder pricing or an enabled quick purchase', async ({ page }) => {
  const products = catalogue.map((item: { sku: string }) => item.sku === 'JON-STK-5060' ? { ...item, available: false } : item);
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: products, headers: { 'access-control-allow-origin': '*' } }));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/products/strike-5060/');
  await expect(page.locator('.storefront-availability')).toHaveText('Currently unavailable');
  await expect(page.locator('.storefront-offer-note')).toHaveCount(0);
  await expect(page.locator('.storefront-product-actions').getByRole('button', { name: /Buy now/ })).toBeDisabled();
  await page.locator('.storefront-specifications').scrollIntoViewIfNeeded();
  await expect(page.getByRole('complementary', { name: 'Quick purchase' }).getByRole('button')).toBeDisabled();
});

for (const width of [1440, 768, 390, 320]) {
  test(`assurance cards have their own space and a readable grid at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: width < 620 ? 844 : 1000 });
    await page.goto('/');
    const region = page.getByRole('region', { name: 'The JON.PC promise', exact: true });
    const hero = await page.locator('.hero').boundingBox();
    const section = await region.boundingBox();
    expect(section!.y - (hero!.y + hero!.height)).toBeGreaterThanOrEqual(24);
    const cards = region.getByRole('button');
    await expect(cards).toHaveCount(4);
    await expect(cards.first()).toContainText('3-year warranty');
    const boxes = await Promise.all([0, 1, 2, 3].map(index => cards.nth(index).boundingBox()));
    for (const box of boxes) {
      expect(box!.x).toBeGreaterThanOrEqual(0);
      expect(box!.x + box!.width).toBeLessThanOrEqual(width);
    }
    if (width === 1440) expect(boxes[3]!.y).toBe(boxes[0]!.y);
    else {
      expect(boxes[1]!.y).toBe(boxes[0]!.y);
      expect(boxes[2]!.y).toBeGreaterThan(boxes[0]!.y + boxes[0]!.height);
    }
    if (width === 1440 || width === 390) {
      await region.evaluate(node => node.scrollIntoView({ block: 'center', behavior: 'instant' }));
      await region.screenshot({ path: `../output/assurance-simple/cards-${width}.png` });
    }
  });
}

test('Strike Ti shows the starting option, defaults to 32GB and adds the chosen SKU', async ({ page }) => {
  const upgrade = catalogue.find(system => system.sku === 'JON-STK-5060TI-32GB');
  test.skip(!upgrade, 'Requires the RAM catalogue migration');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/products/strike-ti-5060-ti/');
  const memory = page.getByRole('group', { name: /System RAM/ });
  const standard = catalogue.find(system => system.sku === 'JON-STK-5060TI')!;
  const option16 = memory.getByRole('radio', { name: `16GB DDR5 ${money(standard.priceCents)}` });
  const option32 = memory.getByRole('radio', { name: `32GB DDR5 ${money(upgrade!.priceCents)}` });
  const founderPrice = (cents: number) => money(cents - Math.floor((cents * 500 + 5000) / 10000));
  await expect(option32).toBeChecked();
  await expect(memory).toBeInViewport({ ratio: 1 });
  await expect(page.locator('.storefront-price strong')).toHaveText(founderPrice(upgrade!.priceCents));
  await expect(page.locator('.storefront-regular-price s')).toHaveText(money(upgrade!.priceCents));
  await expect(page.locator('.storefront-founder-savings')).toContainText(money(Math.floor((upgrade!.priceCents * 500 + 5000) / 10000)));
  await expect(page.locator('.storefront-price-block')).toContainText('Includes Logitech MK120 wired keyboard and mouse');
  await option32.focus();
  await page.keyboard.press('ArrowLeft');
  await expect(option16).toBeChecked();
  await expect(page.locator('.storefront-price strong')).toHaveText(founderPrice(standard.priceCents));
  await expect(page.locator('.storefront-regular-price s')).toHaveText(money(standard.priceCents));
  await expect(page.locator('.storefront-founder-savings')).toContainText(money(Math.floor((standard.priceCents * 500 + 5000) / 10000)));
  await expect(page.getByRole('region', { name: 'Specifications' })).toContainText('16GB DDR5-6000');
  await page.evaluate(() => window.addEventListener('jonpc:add-to-bag', event => {
    document.body.dataset.selectedSystem = (event as CustomEvent<{ sku: string }>).detail.sku;
  }));
  await page.getByRole('button', { name: 'Add to cart' }).click();
  await expect(page.locator('body')).toHaveAttribute('data-selected-system', 'JON-STK-5060TI');
  await expect(page.getByRole('dialog')).toContainText('Sign in to add items');
});

test('an unavailable RAM option stays selected and cannot be purchased until the buyer chooses another', async ({ page }) => {
  test.skip(!catalogue.some(system => system.sku === 'JON-STM-5070-32GB'), 'Requires the RAM catalogue migration');
  const stock = catalogue.map(system => system.sku === 'JON-STM-5070-32GB' ? { ...system, available: false, saleEnabled: false } : system);
  await page.route(url => url.pathname === '/api/systems/preview', route => route.fulfill({ json: stock, headers: { 'access-control-allow-origin': '*' } }));
  await page.goto('/products/storm-5070/');
  await expect(page.locator('.storefront-availability')).toHaveText('Currently unavailable');
  await expect(page.getByRole('radio', { name: /32GB DDR5.*Unavailable/ })).toBeChecked();
  await expect(page.getByRole('button', { name: 'Add to cart' })).toBeDisabled();
  await page.locator('.storefront-memory-options label').filter({ hasText: '16GB DDR5' }).click();
  await expect(page.getByRole('button', { name: 'Add to cart' })).toBeEnabled();
});

const checkoutStyleUser = {
  id: 'checkout-style-customer',
  email: 'checkout-style@example.test',
  displayName: 'Checkout Style QA',
  role: 'CUSTOMER',
  emailVerified: true,
};
const checkoutCors = { 'access-control-allow-origin': '*' };

test('checkout action is readable before hover on the mobile delivery form', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.addInitScript(() => localStorage.setItem('jonpc.auth.token', 'checkout-style-token'));
  await page.route(url => url.pathname === '/api/auth/me', route => route.fulfill({ json: checkoutStyleUser, headers: checkoutCors }));
  await page.route(url => url.pathname === '/api/orders/delivery/STYLE-DELIVERY', route => route.fulfill({ status: 204, headers: checkoutCors }));
  await page.goto('/products/strike-5060/');
  await expect(page.getByRole('button', { name: 'Open account for Checkout Style QA' })).toBeVisible();
  await page.evaluate(() => window.dispatchEvent(new CustomEvent('jonpc:open-checkout', { detail: { requestReference: 'STYLE-DELIVERY' } })));
  const action = page.getByRole('button', { name: 'Continue to payment' });
  await expect(action).toBeEnabled();
  await expect(action).toHaveCSS('background-color', 'rgb(195, 239, 128)', { timeout: 4000 });
  await expect(action).toHaveCSS('color', 'rgb(10, 17, 16)');
  await action.focus();
  await expect(action).toHaveCSS('outline-style', 'solid');
});

test('checkout action is readable before hover on the secure payment review', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('jonpc.auth.token', 'checkout-style-token'));
  await page.route(url => url.pathname === '/api/auth/me', route => route.fulfill({ json: checkoutStyleUser, headers: checkoutCors }));
  await page.route(url => url.pathname === '/api/payments/stripe/style-payment', route => route.fulfill({
    headers: checkoutCors,
    json: {
      paymentId: 'style-payment', orderReference: 'STYLE-ORDER',
      status: 'CREATED', checkoutUrl: 'https://checkout.example.test/preview',
      amountCents: 258905, productSubtotalCents: 269900, discountCents: 13495, shippingFeeCents: 2500,
    },
  }));
  await page.goto('/products/strike-5060/?stripe_result=cancelled&stripe_payment=style-payment');
  const action = page.getByRole('button', { name: 'Return to secure payment' });
  await expect(action).toBeEnabled();
  await expect(action).toHaveCSS('background-color', 'rgb(195, 239, 128)', { timeout: 4000 });
  await expect(action).toHaveCSS('color', 'rgb(10, 17, 16)');
  await expect(page.getByRole('button', { name: 'View My Orders' })).toHaveCSS('color', 'rgb(211, 231, 227)');
});
