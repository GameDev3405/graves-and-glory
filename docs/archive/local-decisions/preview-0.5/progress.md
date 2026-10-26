# SDD ledger — plan: preview-0.5 (docs/HANDOFF.md, "The preview build Kort is waiting for")

Session 2026-10-21 (Opus 5.5 controller, rules from docs/WORKFLOW.md at c18e7e2).
Ruling: follow docs/WORKFLOW.md in the repo (four agents, Sonnet extra high) over the older copy in Kort's Downloads (two agents, Sonnet medium) — the repo commits are later the same day — cost if wrong: a pricier Sonnet effort on one agent.
Ruling: this session's one plan is the preview build; the cut-off reviews and implementers wait for the next session — WORKFLOW's one-plan-per-session rule — cost if wrong: one session of delay on Phase 2/3/UI.
BASE f45392c (preview-0.5).
Implementer (Sonnet xhigh) DONE_WITH_CONCERNS: commits f3fae2e (test tools compile against merged Profile API), d37fe0a (merge phase-2-houses 6264f6a, check id p2-gilded-art-timeout), 552a8c6 (docs/PREVIEW-0.5.md, docs/RELEASE-0.5.0-preview1.txt). Self-test 11/11 twice; Phase 2 suites 32/35 (p2-recruit-tabs and p2-hud-hearts stale, p2-class-picker real cosmetic cut). mrpack 22,821,222 bytes sha256 37857e08…d39be, verify_package PASS. Report file write refused; summary here.
Ruling: keep the 6264f6a merge (Task 6 batch 1, its Opus review still owed) in preview-0.5 — Kort asked for everything committed, and without it H does nothing for any class — cost if wrong: the preview carries unreviewed Task 6 code; reset to f3fae2e and repackage.
Ruling: no code review of the preview commits — the branch is throwaway, the fixes are test-tool renames and merge unions, the real base merge is reviewed later — cost if wrong: a merge-fix mistake shows up in Kort's notes instead of a review.
Preview: complete (f45392c..552a8c6). Controller verified the mrpack sha256 and verify_package. Graph built for bannerbound-preview.
