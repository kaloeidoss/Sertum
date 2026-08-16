# Privacy & permission audit gate (PRD 7.14 / A-20).
#
# Hard rules:
#   - No INTERNET permission, ever.
#   - Only the PRD 7.14 minimal permission set may appear.
#   - No MANAGE_EXTERNAL_STORAGE, no location/camera/contacts/phone/bluetooth.
#
# Optional device mode:
#   ./privacy_audit.ps1 -Device
# prints the runtime permission state via `adb shell dumpsys package com.sertum.player`.
param(
    [switch]$Device
)

$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot -Parent
$manifest = Join-Path $root 'app\src\main\AndroidManifest.xml'
if (-not (Test-Path $manifest)) {
    Write-Error "Manifest not found: $manifest"
    exit 1
}

$allowed = @(
    'android.permission.READ_MEDIA_AUDIO'
    'android.permission.READ_EXTERNAL_STORAGE'
    'android.permission.POST_NOTIFICATIONS'
    'android.permission.FOREGROUND_SERVICE'
    'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK'
)

$forbiddenMarkers = @(
    'INTERNET'
    'ACCESS_NETWORK'
    'MANAGE_EXTERNAL_STORAGE'
    'CAMERA'
    'LOCATION'
    'CONTACTS'
    'READ_PHONE'
    'CALL_PHONE'
    'RECORD_AUDIO'
    'BLUETOOTH'
    'ACTIVITY_RECOGNITION'
    'BODY_SENSORS'
    'CALENDAR'
    'SMS'
)

[xml]$xml = Get-Content $manifest -Raw
$androidNs = 'http://schemas.android.com/apk/res/android'
$nodes = $xml.SelectNodes('//uses-permission')
$present = @()
foreach ($node in $nodes) {
    $name = $node.GetAttribute('name', $androidNs)
    if ($name) { $present += $name }
}

Write-Output ('Declared permissions: ' + (($present | Sort-Object) -join ', '))
if (-not $present) { Write-Output '(none)' }

foreach ($permission in $present) {
    foreach ($marker in $forbiddenMarkers) {
        if ($permission -match [regex]::Escape($marker)) {
            Write-Error "FAIL: forbidden permission marker '$marker' in '$permission' (PRD 7.14 / A-20)"
            exit 1
        }
    }
    if ($allowed -notcontains $permission) {
        Write-Error "FAIL: permission '$permission' is outside the PRD 7.14 minimal set"
        exit 1
    }
}

if ($present -contains 'android.permission.INTERNET') {
    Write-Error 'FAIL: INTERNET permission found (offline boundary violated)'
    exit 1
}

Write-Output 'OK: permission set is within the PRD 7.14 minimal set and contains no INTERNET.'

if ($Device) {
    Write-Output ''
    Write-Output '=== device runtime permission state ==='
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) {
        Write-Warning 'adb not found on PATH; skipping device check.'
        exit 0
    }
    adb shell dumpsys package com.sertum.player | Select-String -Pattern 'requested permissions|runtime permissions|android.permission'
}

exit 0
