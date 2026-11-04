Run 2026-11-04b (branch qa/2026-11-04b from main 577daa8b). Scratch dedicated server (tools/server-kit copy, port 25641), protocol-763 bot test-tools/qa/ggbot.py.
rows_a.out: E9, I10, D9, D16-D18, D21, I13. rows_b.out: D20, I11. rows_d.out/rows_e.out: J2 (rows_d: auto-opped joiner, rows_e: de-opped joiner).
c9-profile-on-join.txt: C9. prev-c11.log: C7/C11 server log. run1.txt/run2.txt: C10/C11/E10 stop and start transcripts.
Scratch-only note: the kit's start.ps1 reads the first line of `java -version`, so JAVA_TOOL_OPTIONS (used here for -Dgg.build=camp) breaks it; the scratch copy was patched, the repo kit was not.
