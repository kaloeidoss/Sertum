# Spike-2 evidence collection: logcat only (the probe itself never sends audio).
# Usage: pwsh -File scripts/spike2_collect.ps1 [-OutDir <dir>]
param(
    [string]$OutDir = "$PSScriptRoot\..\docs\evidence\spike2",
    [string]$Serial = ""
)
$ErrorActionPreference = 'Stop'
$adbArgs = @()
if ($Serial) { $adbArgs += '-s'; $adbArgs += $Serial }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
& adb @adbArgs logcat -d -s SertumSpike *:S 2>&1 | Out-File (Join-Path $OutDir "logcat-$stamp.txt") -Encoding utf8
Write-Output "Evidence saved to $OutDir (stamp $stamp)"
