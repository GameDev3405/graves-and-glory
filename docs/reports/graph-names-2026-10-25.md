# Readable Graphify names — 2026-10-25

Replaced numbered community placeholders with names inferred from actual code symbols and files. Reviewed names are stored with symbol anchors in docs/graph-labels.json; graph refresh reapplies names and rebuilds the HTML viewer without external model calls. New groups fall back to real file/symbol names.

The updated main graph contains 6,610 nodes in 187 displayed groups. Example names include Battle HUD & Event Feed, War Table Recruitment, Troop Animation Engine and Troop AI & Tactics. These describe automatic code groupings, not exact subsystem boundaries.

Validation: two focused tests passed (names follow symbols across cluster renumbering; assigning names preserves graph membership and edges). The exported HTML contains the readable names and no numbered Community 1 placeholder. Content freshness check passes. No game build or gameplay test was needed for this navigation-only change.
