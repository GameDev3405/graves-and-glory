# Prints the builder server's health: up or down, players, overload warnings (TPS/MSPT proxy), last backup time and the
# installed mod stamp. Read-only. Exit code 0 = OK, 1 = WARN, 2 = DOWN. Also writes health.json next to the server.
param([string]$Python = 'python')
& $Python (Join-Path $PSScriptRoot 'builder_ops.py') --server-dir $PSScriptRoot health
exit $LASTEXITCODE
