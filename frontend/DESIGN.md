---
name: Clausify
description: Contract review drawn as an engineering specification sheet.
colors:
  vellum: "#f2f4f1"
  sheet: "#fbfcfa"
  ink: "#1a1e22"
  ink-2: "#454d55"
  ink-3: "#5f6870"
  rule: "#cfd5d1"
  rule-strong: "#9aa3a0"
  ghost: "#e6eae6"
  blue: "#3460d1"
  blue-ink: "#2449a8"
  blue-wash: "#e3eafb"
  amber: "#d7832a"
  amber-ink: "#8a4a0c"
  amber-wash: "#fbeedd"
  pass: "#2d7a4c"
  pass-wash: "#e2f1e7"
  fail: "#a4442b"
  fail-wash: "#f7e4de"
typography:
  display:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "clamp(2.4rem, 5.2vw, 4.25rem)"
    fontWeight: 600
    lineHeight: 1.02
    letterSpacing: "-0.035em"
    fontFeature: "'ss01', 'cv11'"
  headline:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "clamp(1.75rem, 3vw, 2.5rem)"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  page-title:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "-0.03em"
  title:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    lineHeight: 1.55
  body:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.625
  control:
    fontFamily: "Geist Variable, ui-sans-serif, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 550
  measure:
    fontFamily: "Geist Mono Variable, ui-monospace, SF Mono, Menlo, monospace"
    fontSize: "0.75rem"
    fontWeight: 400
    letterSpacing: "0.02em"
    fontFeature: "'tnum', 'zero'"
  stamp:
    fontFamily: "Geist Mono Variable, ui-monospace, SF Mono, Menlo, monospace"
    fontSize: "0.6875rem"
    fontWeight: 500
    letterSpacing: "0.08em"
rounded:
  stamp: "2px"
  control: "3px"
spacing:
  gutter-sm: "20px"
  gutter: "32px"
  section: "80px"
  section-lg: "112px"
  app-width: "76rem"
  site-width: "84rem"
components:
  button-primary:
    backgroundColor: "{colors.blue}"
    textColor: "#ffffff"
    typography: "{typography.control}"
    rounded: "{rounded.control}"
    padding: "0 1.15rem"
    height: "2.75rem"
  button-primary-hover:
    backgroundColor: "{colors.blue-ink}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.control}"
    rounded: "{rounded.control}"
    padding: "0 1.15rem"
    height: "2.75rem"
  button-secondary-hover:
    backgroundColor: "{colors.sheet}"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.ink-2}"
    rounded: "{rounded.control}"
    padding: "0 0.6rem"
    height: "2.25rem"
  button-quiet-hover:
    backgroundColor: "{colors.ghost}"
    textColor: "{colors.ink}"
  field-input:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0.7rem 0.75rem"
  stamp-uploaded:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink-2}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "0.2rem 0.5rem"
  stamp-analyzing:
    backgroundColor: "{colors.blue-wash}"
    textColor: "{colors.blue-ink}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "0.2rem 0.5rem"
  stamp-analyzed:
    backgroundColor: "{colors.pass-wash}"
    textColor: "{colors.pass}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "0.2rem 0.5rem"
  stamp-failed:
    backgroundColor: "{colors.fail-wash}"
    textColor: "{colors.fail}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "0.2rem 0.5rem"
  stamp-out-of-tolerance:
    backgroundColor: "{colors.amber-wash}"
    textColor: "{colors.amber-ink}"
    typography: "{typography.stamp}"
    rounded: "{rounded.stamp}"
    padding: "0.2rem 0.5rem"
  sheet:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
---

# Design System: Clausify

## Overview

**Creative North Star: "The Specification Sheet"**

Clausify reads a contract the way an engineer checks a drawing: every clause measured against tolerance, every deviation called out and pinned to its corrected spec. The interface is a set of drawing sheets laid on cool vellum. Graphite ink carries the words, construction blue carries linework, focus, and the one action that matters on a screen, and drafting amber appears only where something is out of tolerance. Status is a revision stamp, facts sit in title-block tables, and anything the product cannot show yet is drawn as a hatched ghost cell rather than faked or left blank.

Density is calm and exact. Pages are hairline-ruled rather than boxed into cards; tables and registers do the organizing. A workhorse grotesk (Geist) sets everything people read, and a technical monospace (Geist Mono) is reserved for what a draftsman would letter by stencil: identifiers, measurements, dates, stamps, and the field labels of title blocks and schedules. The voice of the type matches the product voice: formal, precise, never alarmed.

The world refuses the category default of a centered headline over three feature cards and a screenshot, and it refuses red-alert risk theatre. Risk is stated by text and position (a stamp, a leader line, a callout), never by color alone.

**Key Characteristics:**
- Vellum ground, raised sheets with hairline frames and corner registration ticks.
- Hairline rules and title-block grids instead of cards.
- Construction blue for linework, focus, and primary actions; amber only for out-of-tolerance callouts.
- Revision stamps carry status as words; color only supports them.
- Absent states are drawn as hatched ghost cells.
- Mono only for ids, measures, dates, stamps, and title-block field labels.
- One orchestrated entrance; grid texture only on the hero drawing surface.

## Colors

A cool, low-chroma drafting palette: graphite on vellum, with one blue for construction and one amber held in reserve.

### Primary
- **Construction Blue** (blue): Primary buttons, focus outlines, the active-nav underline, dimension lines and extension ticks, the upload gauge fill, the hero grid's linework. The caret is blue.
- **Pen Blue** (blue-ink): Hover state of primary buttons, blue text on blue-wash (Analyzing stamp, info notices, the privacy lock icon).
- **Blue Wash** (blue-wash): Text selection, the field focus halo, the active drop zone, the arrival flash on a newly uploaded register row.

### Secondary
- **Drafting Amber** (amber): The out-of-tolerance mark: callout border, leader line, the flagged phrase's underline and end dot, the tolerance rings in the hero field.
- **Amber Ink** (amber-ink): Text of the "Out of tolerance" stamp; the tint of the callout's shadow.
- **Amber Wash** (amber-wash): Ground of the out-of-tolerance stamp and the highlighter band behind a flagged phrase.

### Tertiary
- **Pass Green** (pass) on **Pass Wash** (pass-wash): The Analyzed stamp only.
- **Fail Rust** (fail) on **Fail Wash** (fail-wash): The Failed stamp, error notices, failed revision marks, invalid field borders. A muted rust, deliberately not alarm red.

### Neutral
- **Vellum** (vellum): The page ground everywhere.
- **Sheet** (sheet): Raised drawing sheets, title blocks, revision tables, fields, the alternate landing band.
- **Graphite** (ink): Headings, body text, the dark closing band, the filled revision mark.
- **Graphite 2** (ink-2): Secondary text, lead paragraphs, quiet buttons, the Uploaded stamp.
- **Graphite 3** (ink-3): Mono labels, metadata, placeholders, field hover border.
- **Hairline** (rule): Table and register rules, sheet frames, title-block dividers.
- **Strong Hairline** (rule-strong): Title-block and revision-table outer frames, field borders, secondary-button borders, registration ticks, scrollbar thumb.
- **Ghost** (ghost): The hatch of absent-state cells; quiet-button and nav hover ground.

### Named Rules
**The Tolerance Rule.** Amber means "out of tolerance" and nothing else. It is never an accent, a warning for ordinary errors, or a decorative highlight. The wordmark's amber callout point is the single brand exception, and it depicts exactly that callout.

**The Stamp Carries the Word Rule.** Status is never color-only. Every status color appears inside a stamp whose text states the status; a colored dot or tinted row with no label is not a status.

**The One Blue Action Rule.** A screen has at most one filled blue button per decision; everything else is secondary (outlined) or quiet.

## Typography

**Display Font:** Geist Variable (with ui-sans-serif, system-ui)
**Body Font:** Geist Variable, stylistic sets ss01 and cv11 on globally
**Label/Mono Font:** Geist Mono Variable (with ui-monospace, SF Mono, Menlo)

**Character:** A neutral, engineered grotesk set tight at large sizes, paired with a stencil-like mono that only ever letters data. Headings balance, paragraphs wrap pretty.

### Hierarchy
- **Display** (600, clamp(2.4rem, 5.2vw, 4.25rem), 1.02, -0.035em): The landing hero headline only.
- **Headline** (600, clamp(1.75rem, 3vw, 2.5rem), tight, -0.025em): Landing section headings.
- **Page Title** (600, 2rem, tight, -0.03em): App page h1 (Contracts, Account). Contract detail uses 1.875rem and the auth sheet 1.75rem at the same weight and tracking.
- **Title** (600, 1.125rem): Section h2 inside app pages (Register, Revisions, Analysis) and step titles; sits on a rule, never under a label.
- **Body** (400, 1rem, 1.625): Reading text, max measure about 44rem; lead paragraphs step up to 1.125rem in ink-2.
- **Control** (550, 0.9375rem): Buttons, nav links, field text.
- **Measure** (mono 400, 0.75rem, 0.02em, tabular and slashed-zero numerals): Ids (CL-00002), sizes, page counts, dates in tables, sheet captions, column heads.
- **Stamp / Field Label** (mono 500, 0.6875rem, 0.08em, uppercase): Status stamps and title-block field labels.

### Named Rules
**The Stencil Rule.** Mono is for what a draftsman would letter: identifiers, measurements, dates, stamps, and the field and column labels of title blocks, revision tables, and schedules. Prose, headings, buttons, and notices are always Geist.

**The No Kicker Rule.** No eyebrow or kicker label above a heading. A heading stands on its own or on a hairline rule; mono labels belong to fields and columns, not to sections.

**The Tabular Numerals Rule.** Every number in data is tabular with a slashed zero, so columns line up like a dimension table.

## Layout

Two containers: the app shell at 76rem and the marketing site at 84rem, both with 1.25rem gutters on mobile and 2rem from 640px. App pages start 2.5rem under a sticky 4rem header and end with 6rem of runway; landing sections breathe at 5rem vertical padding, 7rem from 1024px.

Structure comes from tables and rules rather than cards. The dashboard is a register (a table whose whole row is the link, collapsing to a two-column grid under 640px with mono metadata). Contract facts sit in a title block of bordered cells that tile edge to edge (collapsed borders by negative margins). The landing hero is an asymmetric two-column grid (0.92fr / 1.08fr) with a title block pinned bottom-right on wide screens and inline in the sheet below 1280px. The three-step flow is a dimension chain: one blue rule with extension ticks across three columns.

Long identifiers (filenames, emails) break only after separators (underscore, dot, at, hyphen), never mid-word.

## Elevation & Depth

Flat by default, with exactly one kind of lift: the drawing sheet. Depth is otherwise conveyed by tone (sheet on vellum) and by line weight (rule versus rule-strong).

### Shadow Vocabulary
- **Sheet lift** (`box-shadow: 0 1px 2px rgb(26 30 34 / 0.04), 0 8px 24px -12px rgb(26 30 34 / 0.12)`): Raised drawing sheets (hero clause plate, auth sheet, upload sheet).
- **Callout lift** (`box-shadow: 0 10px 28px -16px rgb(138 74 12 / 0.35)`): The out-of-tolerance callout only; deepens slightly when its flag is hovered.
- **Focus halo** (`box-shadow: 0 0 0 3px` blue-wash): Focused fields and the current revision mark.

### Named Rules
**The One Sheet Rule.** Only sheets lift. Tables, title blocks, notices, and ghost cells lie flat on the page.

**The Drawing Surface Rule.** Grid texture appears only on an actual drawing surface: the hero's WebGL construction field (static under reduced motion, CSS grid fallback without WebGL). Auth, app, and content surfaces sit on plain vellum.

## Shapes

Nearly square. Controls, fields, notices, and ghost cells use a 3px corner; stamps and revision marks a 2px corner; focus outlines 2px. Nothing is pill-shaped except the status pulse dot and the scrollbar thumb. Sheets are square-cornered with a hairline frame and two registration ticks (top-left, bottom-right, 10px, rule-strong). Linework is 1px; icons are a single 24px set drawn with a 1.5px stroke, square caps, and miter joins.

## Components

### Buttons
Precise and unshowy.
- **Shape:** 3px corners, 2.75rem minimum height (2.5rem in the landing nav).
- **Primary:** Construction blue fill, white text, 550 weight, optional trailing arrow icon.
- **Hover / Focus:** Primary darkens to pen blue; secondary's border goes to graphite on sheet ground; all transitions 160ms on the expo-out curve; press nudges down 1px; focus is the global 2px blue outline offset 3px. Disabled is 55% opacity.
- **Secondary:** Transparent with a strong-hairline border. **Quiet:** No border, ink-2 text, ghost ground on hover, 2.25rem tall. On the dark closing band, the button inverts to vellum on graphite.

### Status Stamps
- **Style:** Mono uppercase in a 1px current-color box, tinted ground: Uploaded (ink-2 on sheet), Analyzing (pen blue on blue wash, with a pulsing dot), Analyzed (pass), Failed (fail). Out of tolerance (amber ink on amber wash) is used only in callouts.
- **State:** Each stamp carries a title describing the state in a sentence.

### Drawing Sheet
- **Corner Style:** Square, with registration ticks.
- **Background:** Sheet on vellum.
- **Shadow Strategy:** Sheet lift (see Elevation).
- **Border:** 1px hairline.
- **Internal Padding:** 1.5rem to 2.25rem.

### Inputs / Fields
- **Style:** Sheet ground, 1px strong-hairline border, 3px corners, 0.9375rem text, ink-3 placeholder.
- **Focus:** Border turns construction blue with a 3px blue-wash halo; hover darkens the border to ink-3.
- **Error:** Border turns fail rust, with the message beside it in text.

### Navigation
App header: sticky, vellum at 90% with a 6px backdrop blur and a hairline bottom rule. Links are 0.9375rem ink-2, ghost ground on hover; the active link goes to ink at 550 weight with a 2px construction-blue rule sitting on the header's bottom edge.

### Title Block and Revision Table
Bordered grid of cells, strong-hairline outer frame, hairline inner dividers, sheet ground. Each cell has a mono uppercase field label in ink-3 over its value; ids in the value are mono. The revision table lists events with a lettered square mark: filled graphite when done, blue outline with halo when current, rust fill when failed, dashed outline over hatched cells when the event is not yet available.

### Ghost Cell
A hatched (-45deg, 6px ghost stripes) cell with a dashed hairline border, sized like the content it stands in for, optionally labeled in mono ("Risk score"). It is the only way an unavailable capability is shown.

### Callout and Leader
The world's signature: a flagged phrase gets an amber highlighter band and underline ending in an amber dot; an amber leader line (1.25px, thickening to 2px on hover) runs from that dot to a sheet-ground callout with a 1px amber border, which holds the out-of-tolerance stamp, a plain-language explanation, and the suggested revision under a hairline.

### Upload Sheet and Gauge
The drop zone is a sheet with a dashed strong-hairline border that turns blue on a blue wash when a file is dragged over. Upload progress is drawn as a dimension line filling blue between two extension ticks.

## Do's and Don'ts

### Do:
- **Do** reserve amber (#d7832a family) for out-of-tolerance callouts: stamp, leader, flagged phrase, callout border.
- **Do** put every status in a stamp whose text names it; color only supports the word.
- **Do** draw unavailable data as ghost cells, sized like the content to come, with honest copy beside them.
- **Do** use mono only for ids, measurements, dates, stamps, and title-block or column field labels, with tabular slashed-zero numerals.
- **Do** organize with hairline rules, registers, and title blocks; lift only drawing sheets.
- **Do** give a page one orchestrated entrance (settle 600 to 900ms on cubic-bezier(0.16, 1, 0.3, 1)) that starts from an already-visible state, and honor reduced motion.
- **Do** let long identifiers break only after _ . @ -.

### Don't:
- **Don't** use amber as an accent, a generic warning, or decoration.
- **Don't** convey status or risk with color alone.
- **Don't** place an eyebrow or kicker label above a heading.
- **Don't** set prose, headings, buttons, or notices in mono.
- **Don't** put grid textures on anything but the hero's drawing surface.
- **Don't** stack per-element entrance animations or animate content in from invisible.
- **Don't** wrap content in rounded, shadowed cards; corners stay at 2 to 3px and only sheets lift.
- **Don't** present a capability the API does not support as working; draw it as a ghost.
