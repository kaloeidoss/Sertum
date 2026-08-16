# Device check helper for Windows. Lists adb devices and reports USB debugging state.
# Exit codes: 0 = no device needed for this check, 1 = adb missing, 2 = no authorized device.
$ErrorActionPreference = 'Stop'
$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    Write-Output 'adb not found on PATH; install platform-tools first.'
    exit 1
}
$out = & $adb.Source devices
Write-Output $out
$devices = $out | Select-Object -Skip 1 | Where-Object { $_.Trim() -ne '' }
if (-not $devices) {
    Write-Output 'No device attached. Enable USB debugging on the phone and plug it in.'
    exit 2
}
Write-Output 'At least one device is visible.'
exit 0
