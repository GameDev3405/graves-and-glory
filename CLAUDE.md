## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

## Added skills (Kort, 2026-10-21)

Project skills in `.claude/skills/`: `to-questionnaire`, `writing-for-agents`, `domain-modeling`, `improve-codebase-architecture`, `codebase-design` (mattpocock/skills) and `ui-ux-pro-max` (nextlevelbuilder/ui-ux-pro-max-skill).
- `ui-ux-pro-max` is built for web and app UI. Here use it only for UX checks (readability, contrast, hit targets, feedback, accessibility) via its `--domain ux` search. The approved War Table and Iron Ledger look, the UI plan and `work/ui-concepts/GRADING-RUBRIC.md` always win: never take a style, palette or font from it, and never run its `--design-system` or `--persist` steps.
- `to-questionnaire` fits `docs/QUESTIONS-FOR-KORT.md`: questions still go to that file, recommended option first.
- `domain-modeling` and `improve-codebase-architecture` write `GLOSSARY.md` and `docs/adr/`; use them only when Kort or a plan asks.
