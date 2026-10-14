# UI Flow and Design Specification

## Visual language

Preserve the existing calm, spacious DevPath visual language: readable
typography, restrained green accents, responsive cards, keyboard access, visible
focus, and support for `prefers-reduced-motion`.

## Landing `/`

Keep the current DevPath AI landing page and its CTA:

- Headline: **Your QA Experience Is Your Starting Point.**
- CTA: **Start Your Journey** → `/onboarding`.
- Explain the value of building on QA knowledge, learning at a sustainable
  pace, and demonstrating skills.
- Clearly label the local capstone as a demo.

## Onboarding `/onboarding`

Keep the four-step wizard, progress indicators, review/edit interaction,
inline validation, duplicate-submit protection, loading/error/success states,
and real API persistence.

1. **Background:** display the fixed read-only role `QA Engineer`; select an
   experience band (`0–1`, `2–3`, `4–5`, `6–8`, `9+` years); searchable
   multi-select for QA specializations; optional searchable multi-select for
   technical skills.
2. **Java goal:** display the fixed Java Backend Developer goal and select
   Java programming proficiency.
3. **Availability:** select study weekdays, daily minutes, and learning style.
4. **Review:** show all selected information; allow editing earlier steps;
   submit the profile.

Specialization options: Manual Testing, API Testing, Test Automation, Mobile
Application Testing, Performance Testing, Security Testing, Database Testing,
Quality Engineering / SDET, Test Leadership / QA Management.

Skill options: Postman, REST APIs, SQL, JavaScript, TypeScript, Python, Java,
Selenium, Cypress, Playwright, Appium, JMeter, Git, Docker, CI/CD, PostgreSQL.

Success is shown only after a successful POST. The saved profile can be
retrieved from the API. Never store the complete profile in browser storage.

## Capstone screens — implement in order

### Learning entry/dashboard

After profile creation, the dashboard fetches persisted learner context and a
deterministic recommended starting lesson. It shows the saved-profile
milestone, clearly labels lesson activity as not yet tracked, and Learning
Recall as not yet available. Display only persisted or rule-derived
information; do not invent percentages, completed skills, or attempt counts.
The QA example should scale in complexity from straightforward cases for the
lowest experience band to layered regression and contract coverage for the
highest band, while reflecting the learner's selected specialization. Keep
Java concept depth based on the learner's declared programming level, not QA
tenure. The current curated lesson opens in the workspace; additional lessons
and progress reporting appears on the dashboard using persisted attempts,
recall statuses, latest available feedback, and a deterministic next activity.

### Assisted Learning Lab

On desktop, present lesson content and practice side by side; stack or tab
sections on mobile. The first lesson is Java Methods and Input Validation,
using a QA banking API analogy and a withdrawal-validation exercise. Label
unfinished lessons as upcoming. The practice draft is editable but remains
page-local until the learner submits it. Submission and feedback are handled by
the backend; code is not compiled or evaluated.

### Feedback, Recall, and progress

The lesson lab submits code to the backend and shows structured, explicitly
advisory AI feedback, competency tags, a next action, and a review
recommendation. If the provider is unconfigured or unavailable, show a
retryable error and preserve the draft. Clearly state that code was not
compiled or executed and feedback does not establish mastery.
For a local demo only, an explicit backend `AI_DEMO_MODE=true` setting may
return a clearly labeled, uncertain sample; never activate sample feedback
automatically after a provider error.

Persisted competency gaps link to Learning Recall, which offers a related bank
transfer validation task in a different context. Display persisted statuses
and show the saved code and advisory feedback from the original submission
that identified the selected gap. Keep the transfer draft on provider failure.
A `READY_FOR_REVIEW` response is positive advisory feedback, not verified
correctness. Mark a competency `DEMONSTRATED` only after positive advisory
recommendations for that competency on both the original and transfer
exercises and explicit learner confirmation. Progress reporting shows distinct
lessons attempted, exercise submissions, competencies needing review, in
practice, and demonstrated under the evidence policy, the latest available
feedback, and a rule-derived next activity. Empty states must not imply
completed work.

## Accessibility and trust

Use associated labels, keyboard-operable search/select/removal controls,
announced validation and API errors, clear focus, sufficient contrast, and
reduced-motion behavior. Do not claim that submitted Java was compiled or
tested unless execution actually occurred.
