# M1 spike orchestrator for a connected device.
# Usage: pwsh -File scripts/m1_run.ps1 [-Serial <adb serial>]
# Runs: install -> Spike-1 matrix -> collect -> Spike-2 probe -> collect -> analyze.
param([string]$Serial = "")
$ErrorActionPreference = 'Stop'
$adbArgs = @()
if ($Serial) { $adbArgs += '-s'; $adbArgs += $Serial }

$dev = (& adb @adbArgs devices | Select-String '\tdevice' | ForEach-Object { ($_ -split '\s')[0] } | Select-Object -First 1)
if (-not $dev) { Write-Error 'No authorized adb device. Enable USB debugging and reconnect.'; exit 1 }
Write-Output "Device: $dev"

$apk = Join-Path $PSScriptRoot '..\app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { Write-Error "APK missing: $apk"; exit 1 }

Write-Output '--- install APK ---'
& adb @adbArgs install -r $apk
if ($LASTEXITCODE -ne 0) { Write-Error 'install failed'; exit 1 }

Write-Output '--- clear logcat and run Spike-1 (about 45s) ---'
& adb @adbArgs logcat -c
& adb @adbArgs shell am start -n com.sertum.player/com.sertum.player.spike1.Spike1Activity
Start-Sleep -Seconds 48

$outDir = Join-Path $PSScriptRoot '..\docs\evidence\spike1'
Write-Output '--- collect Spike-1 evidence ---'
& (Join-Path $PSScriptRoot 'spike1_collect.ps1') -Serial $Serial -OutDir $outDir

Write-Output '--- run Spike-2 (accept the USB permission dialog on the phone!) ---'
& adb @adbArgs logcat -c
& adb @adbArgs shell am start -n com.sertum.player/com.sertum.player.spike2.Spike2Activity
Write-Output 'Waiting 15s for you to tap Allow on the phone...'
Start-Sleep -Seconds 15
& (Join-Path $PSScriptRoot 'spike2_collect.ps1') -Serial $Serial -OutDir (Join-Path $PSScriptRoot '..\docs\evidence\spike2')

Write-Output '--- analyze Spike-1 evidence ---'
& (Join-Path $PSScriptRoot 'spike1_analyze.ps1') -OutDir $outDir
Write-Output 'M1 run complete. Review the analysis output above.'
