# Live capture for the native AAudio spike: snapshots audio_flinger every second
# while the matrix plays, so transient DIRECT/MMAP threads are preserved.
# Usage: pwsh -File scripts/spike1_native_capture.ps1 [-Serial <adb serial>] [-OutDir <dir>]
param(
    [string]$Serial = "",
    [string]$OutDir = "$PSScriptRoot\..\..\..\..\..\Project-Sertum\docs\evidence\spike1-native"
)
$ErrorActionPreference = 'Stop'
$adbArgs = @()
if ($Serial) { $adbArgs += '-s'; $adbArgs += $Serial }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# Bigger log buffer so the full matrix survives MIUI ring-buffer rotation.
& adb @adbArgs logcat -G 4M 2>&1 | Out-Null
& adb @adbArgs logcat -c

& adb @adbArgs shell am start -n com.sertum.player/com.sertum.player.spike1.Spike1NativeActivity 2>&1 | Out-Null
Write-Output 'capturing live audio_flinger for 45s...'
for ($i = 1; $i -le 45; $i++) {
    $stamp = Get-Date -Format 'HHmmss-fff'
    & adb @adbArgs shell 'dumpsys media.audio_flinger 2>/dev/null' 2>&1 |
        Out-File (Join-Path $OutDir "af-live-$i-$stamp.txt") -Encoding utf8
    Start-Sleep -Milliseconds 1000
}

& adb @adbArgs logcat -d -s SertumSpike *:S 2>&1 |
    Out-File (Join-Path $OutDir ('logcat-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.txt')) -Encoding utf8
Write-Output "Live capture saved to $OutDir"
