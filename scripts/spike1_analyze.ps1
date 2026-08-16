# Spike-1 evidence analysis: checks audio_flinger dumps for the PRD 7.2 gates.
# Usage: pwsh -File scripts/spike1_analyze.ps1 [-OutDir <dir with collected dumps>]
param([string]$OutDir = "$PSScriptRoot\..\docs\evidence\spike1")
$ErrorActionPreference = 'Stop'

$afFile = Get-ChildItem $OutDir -Filter 'audio_flinger-*.txt' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$logFile = Get-ChildItem $OutDir -Filter 'logcat-*.txt' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $afFile) { Write-Error "No audio_flinger dump in $OutDir"; exit 1 }
$af = Get-Content $afFile.FullName -Raw
$log = if ($logFile) { Get-Content $logFile.FullName -Raw } else { '' }

Write-Output ('Analyzing: ' + $afFile.Name)
Write-Output ('--- gate 1: USB output thread present ---')
$usbThreads = [regex]::Matches($af, 'Output thread[^\r\n]*') | Where-Object { $_.Value -match 'usb|USB|Device' }
Write-Output ('usb-related output threads: ' + $usbThreads.Count)
$usbThreads | Select-Object -First 5 | ForEach-Object { Write-Output ('  ' + $_.Value.Trim()) }

Write-Output '--- gate 2: resampler evidence ---'
$resamp = [regex]::Matches($af, 'resampl\w*', 'IgnoreCase')
Write-Output ('resampler mentions: ' + $resamp.Count)
if ($resamp.Count -gt 0) { Write-Output '  WARNING: resampler strings found (inspect context)' }

Write-Output '--- gate 3: effects evidence ---'
$fx = [regex]::Matches($af, 'Effect Bundle|Effects|effect chain', 'IgnoreCase')
Write-Output ('effect mentions: ' + $fx.Count)
if ($fx.Count -gt 0) { Write-Output '  WARNING: effects strings found (inspect context)' }

Write-Output '--- gate 4: tone matrix from logcat ---'
$tones = [regex]::Matches($log, 'tone rate=(\d+) bits=(\d+) ok=(true|false)')
Write-Output ('tones logged: ' + $tones.Count)
$tones | ForEach-Object { Write-Output ('  ' + $_.Groups[1].Value + 'Hz/' + $_.Groups[2].Value + 'bit ok=' + $_.Groups[3].Value) }

Write-Output '--- verdict ---'
if ($tones.Count -ge 10 -and $usbThreads.Count -ge 1) {
    Write-Output 'EVIDENCE COLLECTED: manual inspection of resampler/effects sections still required.'
} else {
    Write-Output 'INCOMPLETE: collect dumps again or inspect logs manually.'
}
