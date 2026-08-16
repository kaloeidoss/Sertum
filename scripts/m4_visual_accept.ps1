# M4 visual acceptance capture.
# Captures each main screen with uiautomator taps, saves PNGs, and prints
# programmatic color facts (background/gold accents) via pymupdf.
# Usage: pwsh -File scripts/m4_visual_accept.ps1 [-Serial <adb serial>] [-OutDir <dir>]
param(
    [string]$Serial = "",
    [string]$OutDir = "E:\You and I - Gods Creation\Project-Sertum\docs\evidence\m4"
)
$ErrorActionPreference = 'Stop'
$adbArgs = @()
if ($Serial) { $adbArgs += '-s'; $adbArgs += $Serial }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Screencap([string]$name) {
    $file = Join-Path $OutDir "$name.png"
    cmd /c "adb $($adbArgs -join ' ') exec-out screencap -p > `"$file`""
    Write-Output ("captured: " + $file)
    return $file
}

function Tap-Text([string]$text) {
    $dump = & adb @adbArgs shell uiautomator dump /sdcard/window_dump.xml 2>&1 | Out-String
    $xml = & adb @adbArgs shell cat /sdcard/window_dump.xml 2>&1 | Out-String
    $m = [regex]::Match($xml, "text=`"$([regex]::Escape($text))`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if (-not $m.Success) {
        $m = [regex]::Match($xml, "content-desc=`"$([regex]::Escape($text))`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    }
    if (-not $m.Success) {
        Write-Warning "text not found: $text"
        return
    }
    $x = [int]($m.Groups[1].Value) + ([int]($m.Groups[3].Value) - [int]($m.Groups[1].Value)) / 2
    $y = [int]($m.Groups[2].Value) + ([int]($m.Groups[4].Value) - [int]($m.Groups[2].Value)) / 2
    & adb @adbArgs shell input tap $x $y | Out-Null
    Start-Sleep -Milliseconds 800
}

& adb @adbArgs shell am start -n com.sertum.player/com.sertum.player.MainActivity | Out-Null
Start-Sleep -Seconds 3
$songs = Screencap 'songs'
Tap-Text 'Albums'; $albums = Screencap 'albums'
Tap-Text 'Artists'; $artists = Screencap 'artists'
Tap-Text 'Settings'; $settings = Screencap 'settings'
Tap-Text 'No track playing'; $now = Screencap 'nowplaying'

$py = @'
import pymupdf, sys, pathlib
for path in sys.argv[1:]:
    p = pathlib.Path(path)
    if not p.exists():
        print(p.name, "MISSING")
        continue
    img = pymupdf.open(str(p))
    page = img[0]
    pix = page.get_pixmap()
    samples = [pix.pixel(x, y) for x, y in [(2,2),(pix.width-3,2),(2,pix.height-3),(pix.width-3,pix.height-3),(pix.width//2,2)]]
    gold = 0; total = 0
    for y in range(0, pix.height, 8):
        for x in range(0, pix.width, 8):
            r,g,b = pix.pixel(x,y)[:3]
            total += 1
            if abs(r-0xC9)<30 and abs(g-0xA9)<30 and abs(b-0x6E)<40:
                gold += 1
    print(f"{p.name}: corners={samples} goldPixelsRatio={gold/max(total,1):.4f}")
'@
$pyFile = Join-Path $env:TEMP 'm4-colors.py'
[System.IO.File]::WriteAllText($pyFile, $py)
python $pyFile $songs $albums $artists $settings $now
Remove-Item $pyFile -Force
Write-Output 'M4 capture complete.'
