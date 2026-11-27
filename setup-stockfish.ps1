$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$engineDir = Join-Path $scriptDir "engine"
$targetExe = Join-Path $engineDir "stockfish.exe"

New-Item -ItemType Directory -Force -Path $engineDir | Out-Null

Write-Host "Fetching latest official Stockfish release info..."
$release = Invoke-RestMethod -Uri "https://api.github.com/repos/official-stockfish/Stockfish/releases/latest" -Headers @{"User-Agent"="ChessMasterSetup"}
$asset = $release.assets | Where-Object { $_.name -match 'windows-x86-64-universal\.zip$' } | Select-Object -First 1
if (-not $asset) {
    $asset = $release.assets | Where-Object { $_.name -match 'windows-x86-64.*\.zip$' } | Select-Object -First 1
}
if (-not $asset) {
    throw "Could not find a Windows x86-64 Stockfish asset in release $($release.tag_name)."
}

$zipPath = Join-Path $engineDir "stockfish.zip"
$extractDir = Join-Path $engineDir "extracted"

Write-Host "Downloading $($asset.name) ($($release.tag_name))..."
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zipPath -Headers @{"User-Agent"="ChessMasterSetup"}

Write-Host "Extracting Stockfish binary..."
Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force
$exe = Get-ChildItem -Path $extractDir -Recurse -Filter "*.exe" | Select-Object -First 1
if (-not $exe) {
    throw "No .exe binary found inside Stockfish archive."
}

Copy-Item $exe.FullName $targetExe -Force
Remove-Item $zipPath -Force
Remove-Item $extractDir -Recurse -Force

Write-Host "Stockfish ready at $targetExe"
