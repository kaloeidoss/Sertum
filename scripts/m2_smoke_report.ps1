# M2 smoke evidence report.
# Usage: pwsh -File scripts/m2_smoke_report.ps1 [-OutDir <evidence dir>]
param([string]$OutDir = "$PSScriptRoot\..\docs\evidence\m2")
$ErrorActionPreference = 'Stop'

Write-Output '=== backend smoke ==='
$backend = Get-ChildItem $OutDir -Filter 'backend-*.txt' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $backend) { Write-Output 'NO BACKEND EVIDENCE' } else {
    $b = Get-Content $backend.FullName
    $passed = ($b | Select-String 'tone rate=.* ok=true').Count
    $summary = ($b | Select-String 'backend smoke done').Line
    Write-Output ('file: ' + $backend.Name)
    Write-Output ('tones ok: ' + $passed)
    Write-Output ('summary: ' + $summary)
}

Write-Output '=== gapless smoke ==='
$gapless = Get-ChildItem $OutDir -Filter 'gapless-*.txt' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $gapless) { Write-Output 'NO GAPLESS EVIDENCE (install the latest APK and run GaplessSmokeActivity)' } else {
    $g = Get-Content $gapless.FullName
    $g | Select-String 'transition|gapless smoke done' | ForEach-Object { $_.Line }
    $times = [regex]::Matches(($g -join "`n"), 'transition t=(\d+)ms')
    if ($times.Count -ge 2) {
        for ($i = 1; $i -lt $times.Count; $i++) {
            $delta = [int]$times[$i].Groups[1].Value - [int]$times[$i-1].Groups[1].Value
            Write-Output ("transition delta[$i] = ${delta}ms")
        }
    }
}
