# Demo script

A five-minute walkthrough of Clausify for the final presentation (#60). It uses the live site and the synthetic sample contracts in [`backend/src/test/resources/contracts/`](../backend/src/test/resources/contracts/). None of them is a real client contract.

## Before you start (15 minutes ahead)

1. **Wake the API.** The free Render instance sleeps when idle and takes up to two minutes to start. Open <https://clausifysolutions.vercel.app> and wait until sign-in works.
2. **Check the Groq quota.** One analysis uses a few thousand tokens of the free daily budget. Don't run many test analyses on presentation day.
3. **Prepare an account** that already has `risky-consulting-agreement.pdf` analyzed. A live analysis takes 20 to 30 seconds, so show a fresh upload once and use the prepared one for everything else.
4. Download the three sample PDFs (`risky-consulting-agreement.pdf`, `fair-nda.pdf`, `scanned-image-only.pdf`) to the presenting machine.
5. For FR-10, prepare a "revised" contract: a copy of the risky agreement with a few terms changed (for example, payment within fifteen days instead of "a reasonable time"). Upload and analyze it ahead of time.

## The walkthrough

| Time | Show | Say |
|---|---|---|
| 0:00 | Landing page | Freelancers sign contracts they can't afford to have a lawyer review. Clausify flags the risky clauses and suggests fairer wording. |
| 0:30 | **Sign up**, then the dashboard | Stateless JWT auth with BCrypt. Every contract and analysis is visible only to its owner, and another user's ids return 404. |
| 0:50 | Upload `scanned-image-only.pdf` | It's rejected with a clear message: a scanned PDF has no text layer (422). |
| 1:00 | Upload `risky-consulting-agreement.pdf` | The PDF is read in memory and never stored. Analysis runs in the background; the status moves from Analyzing to Analyzed on its own. |
| 1:30 | Open the prepared analyzed contract | The overall score, gauge, risk by category, and findings, riskiest first. |
| 2:00 | Filter **High**, open a finding, click **Copy** | Each finding quotes the clause, explains why it matters, and gives replacement wording. Clauses in your favor are listed at the end. |
| 2:30 | **Compare to standard** on the non-compete | The three closest clauses from the 104-clause library, each marked favorable or unfavorable. Mention [ADR 0012](../docs/adr/0012-word-based-embeddings-for-clause-similarity.md): the neural model didn't fit the free server's 512 MB, so similarity is word-based. |
| 3:00 | **Library**, filter IP rights | Fair and unfair wording side by side. |
| 3:20 | **Compare with…** the revised version | The word-level diff and the risk change, per category. |
| 3:50 | **Download report** | The PDF a freelancer could send to an advisor, with the disclaimer on page 1. |
| 4:10 | **Account** | Name and phone editing, plus contracts analyzed and the average risk. |
| 4:30 | GitHub: Actions on a recent PR | Every PR runs backend tests on real MySQL with a coverage floor, the Playwright journeys, and a smoke test of the deploy image. |

## If something goes wrong

- **"Still starting" or a slow first request:** the API is waking up. Keep talking and retry in a minute.
- **Analysis failed:** click **Retry analysis**. If Groq's daily limit is reached, the failure message says so; switch to the prepared contract.
- **Anything else:** the prepared, already-analyzed contract covers every screen after upload.
