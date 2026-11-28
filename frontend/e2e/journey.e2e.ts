import { expect, test } from '@playwright/test';
import { signUp, uploadAndOpen } from './helpers';

/** The main flow (FR-1, FR-3, FR-6, FR-9, FR-11): sign up, upload, read results, and use them. */
test('sign up, upload a contract, and review its analysis', async ({ page }) => {
  await signUp(page);
  await uploadAndOpen(page, 'risky-consulting-agreement.pdf');

  // FR-6: score, risk level, and findings. The local fake analyzer always scores 80 with 3 risky findings.
  await expect(page.getByText('High risk', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'All: 3 findings' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Unlimited liability' })).toBeVisible();
  await expect(page.getByRole('heading', { name: "What's in your favor" })).toBeVisible();

  // FR-6 example 2: the High filter shows only High findings.
  await page.getByRole('button', { name: 'High: 2 findings' }).click();
  await expect(page.getByRole('heading', { name: 'Net 60 payment terms' })).toBeHidden();
  await expect(page.getByRole('heading', { name: 'All IP assigned before payment' })).toBeVisible();

  // FR-9 example 1: compare a finding with standard clauses.
  await page.getByRole('button', { name: 'Compare to standard' }).first().click();
  await expect(page.getByText('Unlimited contractor liability')).toBeVisible();
  await expect(page.getByText(/\d+% similar/).first()).toBeVisible();

  // FR-11 example 1: the report downloads with the documented name.
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Download report' }).click();
  expect((await download).suggestedFilename()).toBe('Clausify_Report_risky-consulting-agreement.pdf');
});

/** FR-10: two versions are compared; unrelated documents are flagged before the diff (example 2). */
test('compare two contracts and warn when they are unrelated', async ({ page }) => {
  await signUp(page);
  await uploadAndOpen(page, 'fair-nda.pdf');
  await uploadAndOpen(page, 'risky-consulting-agreement.pdf');

  const nda = await page.locator('#compare-with option', { hasText: 'fair-nda.pdf' }).getAttribute('value');
  await page.getByLabel('Compare with…').selectOption(nda!);
  await page.getByRole('button', { name: 'Compare versions' }).click();

  await expect(page.getByRole('heading', { name: 'Compare versions' })).toBeVisible();
  await expect(page.getByText('These documents look unrelated')).toBeVisible();
  await page.getByRole('button', { name: 'Show the comparison anyway' }).click();
  await expect(page.getByRole('heading', { name: 'What changed in the text' })).toBeVisible();
});

/** FR-9 example 2 and FR-12: the clause library filter and the account statistics. */
test('browse the clause library and see account statistics', async ({ page }) => {
  await signUp(page);
  await uploadAndOpen(page, 'risky-consulting-agreement.pdf');

  await page.getByRole('link', { name: 'Library' }).click();
  await page.getByRole('button', { name: 'IP rights' }).click();
  await expect(page.getByRole('heading', { name: /Favorable · 6/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Unfavorable · 7/ })).toBeVisible();

  await page.getByRole('link', { name: 'Account' }).click();
  // The fake analyzer's one analysis scored 80, so that is the average.
  await expect(page.getByText('Average risk').locator('..')).toContainText('80');
  await page.getByLabel('Phone').fill('5135550142');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
});
