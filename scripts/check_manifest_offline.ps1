# Offline boundary guard: the app must never request INTERNET.
# Exits 0 when the manifest contains no INTERNET permission, 1 otherwise.
$ErrorActionPreference = 'Stop'
$manifest = Join-Path $PSScriptRoot '..\app\src\main\AndroidManifest.xml'
if (-not (Test-Path $manifest)) {
    Write-Error "Manifest not found: $manifest"
    exit 1
}
$content = Get-Content $manifest -Raw
if ($content -match 'android\.permission\.INTERNET') {
    Write-Error 'FAIL: INTERNET permission found in AndroidManifest.xml'
    exit 1
}
Write-Output 'OK: no INTERNET permission in AndroidManifest.xml'
exit 0
