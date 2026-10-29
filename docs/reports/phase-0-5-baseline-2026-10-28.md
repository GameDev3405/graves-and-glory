# Phase 0.5 baseline - 2026-10-28

Status: complete. Fast and game tiers pass on the clean `f1f6047` tip. This is the Phase 0 baseline candidate, not a release candidate.

| Item | Result |
|---|---|
| Head | `f1f60472517555ed1f6e1faac4ae373b0899ad27` |
| Source identity | `c41a0e1cef708e00a3a6030e1afde5b9a4194e0a83c93937116c050887efd0e4` (unchanged from `fa2f31e`) |
| Fast | 22 checks, exit 0, `work/verify/20261028-231020-35520/result.json` |
| Game | 23 checks, exit 0, `work/verify/20261028-231257-41152/result.json` |
| Worktree | clean at start; dirty false in both reports |
| Artifact | `work/candidates/c41a0e1cef708e00a3a6030e1afde5b9a4194e0a83c93937116c050887efd0e4/graves-and-glory-0.5.0.jar`, SHA-256 `61a1c8845ded9f9c1f66a2af2b337d6c11f13e5c3fb2a146b2033236042435a3` (built at `fa2f31e`; source unchanged) |

## Scope

Fast runs preflight, Python, UI, asset, 15 standalone Java checks and the mod build. Game adds one isolated Phase 1 combat client. It is a smoke test, not six-player, visual or full lobby/rewards acceptance. The 12 game evidence lines include `p1-combat-suite`; tick cost was 2.73 ms with a 3.07 ms 600-tick mean, 153 troops at start and 112 at end.

## Not tested

- Six networked players, dedicated-server reconnect/restart and persistence.
- Strict package gate; it still refuses 26 troops with incomplete tier art.
- Kort's visual review, 1440p/4K layouts and the full lobby/rewards loop.

## Notes

- JDK 17 and the documented `pwsh.exe` shim were used; no machine settings changed.
- Preflight produced line-ending-only changes in `Spec.java` and `sheets/preflight.json`; the generated content was unchanged, and the files were restored to keep the worktree clean.
- The candidate record is `docs/releases/current-candidate.json`.
