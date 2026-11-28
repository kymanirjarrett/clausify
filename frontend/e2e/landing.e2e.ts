import { expect, test } from '@playwright/test';

/** The public landing page: its section links scroll and its main call to action reaches sign-up. */
test('landing: section links scroll and the call to action reaches sign-up', async ({ page }) => {
  await page.goto('/');
  const nav = page.getByRole('navigation', { name: 'Main' });

  await nav.getByRole('link', { name: 'Risk categories' }).click();
  await expect(page).toHaveURL(/#categories$/);
  await expect(page.getByRole('heading', { name: 'Eight places contracts hide risk' })).toBeInViewport();

  await page.goto('/');
  await page.locator('#hero-title').locator('..').getByRole('link', { name: 'Create an account' }).click();
  await expect(page).toHaveURL(/\/signup$/);
});
