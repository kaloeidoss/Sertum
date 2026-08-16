# Spike-1 evidence collection.
# Usage: pwsh -File scripts/spike1_collect.ps1 [-OutDir <dir>]
# Collects logcat (SertumSpike) and audio_flinger dumps for the system direct-path probe.
param(
    [string]$OutDir = "$PSScriptRoot\..\docs\evidence\spike1",
    [string]$Serial = ""
)
$ErrorActionPreference = 'Stop'
$adbArgs = @()
if ($Serial) { $adbArgs += '-s'; $adbArgs += $Serial }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

& adb @adbArgs logcat -d -s SertumSpike *:S 2>&1 | Out-File (Join-Path $OutDir "logcat-$stamp.txt") -Encoding utf8
& adb @adbArgs shell dumpsys media.audio_flinger 2>&1 | Out-File (Join-Path $OutDir "audio_flinger-$stamp.txt") -Encoding utf8
& adb @adbArgs shell dumpsys audio 2>&1 | Out-File (Join-Path $OutDir "dumpsys-audio-$stamp.txt") -Encoding utf8

Write-Output "Evidence saved to $OutDir (stamp $stamp)"
Get-ChildItem $OutDir | Select-Object Name, Length
