# UI Flow and Design Specification

## Visual language
Clean, premium, calm learning experience. Spacious layout, readable type, restrained gradient accents, polished cards, subtle purposeful animation. Fully responsive; keyboard accessible; respect `prefers-reduced-motion`.

## Screen 1 — Landing `/`
Brand: DevPath AI. Headline: **Your QA Experience Is Your Starting Point.**
Subheading: **Move into Java backend engineering with a learning journey that adapts to your skills, schedule, and progress.**
CTA: **Start Your Journey** → `/onboarding`.
Three benefits: build on QA knowledge; learn at your pace; demonstrate skills. Label early release as a demo.

## Screen 2 — Onboarding `/onboarding`
One four-step wizard:
1. Background: role, years of experience, QA specialization, skills chips.
2. Goal: Java Backend Developer fixed; select Java proficiency.
3. Availability: select study weekdays, daily minutes, learning style.
4. Review: summary and edit controls, submit.

States: empty, inline validation errors, submitting (prevent double submit), API error with retry, saved confirmation. Success offers **View saved profile** (GET API). Optionally store demo UUID in localStorage, never the entire profile. Do not fake a roadmap or learning dashboard.

## Future screens (not yet implemented)
Diagnostic quiz; roadmap; learning dashboard; split-pane lesson/practice workspace; Learning Pulse/progress.
