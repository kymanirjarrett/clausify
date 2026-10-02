---
version: 1
slug: "src-app"
primary_target: "src/app"
related_targets: []
---

# Surface brief: Clausify web app (landing, auth, dashboard, contract detail, account)

Scope: the whole Angular frontend. Landing is Persuade; sign-up, login, dashboard, contract detail, and account are Operate. Code-led build (no image generation).

Audience and job: freelancers and small business owners with a contract PDF and a signing deadline. Landing must make the offer intelligible and get them to sign up. App priorities: status at a glance, fast upload. Avoid: alarmist (red-alert energy), stuffy legal (dusty law-firm look). Reference for hero ambition: unicorn.studio (live, interactive WebGL hero fields).

Sprint 1 truth: analysis results, retry, delete, comparison, reports, and account statistics are not in the API yet. Show honest states for them; never fake findings as real results. The landing's sample clause and callout are labeled as a sample.

## Direction contract

THESIS: Clausify reads a contract the way an engineer checks a drawing: every clause measured against tolerance, every deviation called out with its corrected spec. Refuses the category default of a centered headline over three feature cards and a screenshot.

OWN-WORLD: Cool drafting vellum ground (#F2F4F1), graphite technical-pen ink (#1A1E22), construction blue (#3460D1) for linework, focus, and primary actions, and drafting amber (#D7832A) reserved for out-of-tolerance callouts. Hairline rules, dimension lines with arrowheads and extension ticks, leader lines ending in a dot, title-block tables, revision-stamp status labels, and faint ghost cells for absent states. Workhorse grotesk for UI; a technical monospace for dimensions, ids, and stamps.

STORY: The visitor sees a real-looking clause being measured and called out with its fix, understands that Clausify flags risk and supplies the corrected wording, learns the PDF is never kept, and signs up. In the app they drop a PDF on the sheet and watch its revision stamp progress.

FIRST VIEWPORT: Full-viewport live construction-grid field (WebGL, cursor-reactive, reduced-motion static) behind a drawing sheet. Left: headline, one-line offer, Sign up (primary) and Log in. Right: a sample clause plate with a dimension line spanning it and an amber leader callout ("Out of tolerance: uncapped liability") pinned to its corrected wording. A title block anchors the bottom-right corner. Raises: every flag is pinned to its revision (from tensegrity); absent states keep ghost cells (from seven-segment); one governing dimension line organizes the hero (from curved crease).

FORM: Specification Sheet (engineering drawing), candidate 3 of 7 on the grounded list; seed key 80efb483.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
