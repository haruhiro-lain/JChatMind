# Backup start.ps1 and convert it to UTF-16 LE (Windows PowerShell friendly)
$path = Join-Path $PSScriptRoot 'start.ps1'
if (-not (Test-Path $path)) {
    Write-Error "start.ps1 not found at $path"
    exit 1
}
$bak = "$path.bak"
Copy-Item $path $bak -Force

# Read bytes and try UTF8 first; if replacement char appears, use system default encoding
$bytes = [System.IO.File]::ReadAllBytes($path)
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
if ($text.Contains([char]0xFFFD)) {
    $text = [System.Text.Encoding]::Default.GetString($bytes)
}

# Write as UTF-16 LE (PowerShell 'Unicode' encoding) with BOM
[System.IO.File]::WriteAllText($path, $text, [System.Text.Encoding]::Unicode)
Write-Host "Backup created: $bak" -ForegroundColor Green
Write-Host "Converted $path to UTF-16 LE (with BOM)." -ForegroundColor Green
