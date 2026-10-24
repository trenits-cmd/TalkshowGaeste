# Talkshow Gäste - Install on connected Android device (USB oder Wireless)
param(
    [switch]$Wireless
)

$ErrorActionPreference = "Continue"

function Find-Adb {
    $cands = @("adb","$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe","$env:ANDROID_HOME\platform-tools\adb.exe")
    foreach ($c in $cands) {
        if ($c -eq "adb") { $cmd = Get-Command adb -ErrorAction SilentlyContinue; if ($cmd) { return $cmd.Source } }
        elseif (Test-Path $c) { return $c }
    }
    throw "adb nicht gefunden"
}

$adb = Find-Adb
& $adb start-server | Out-Null

# === WIRELESS MODE ===
if ($Wireless) {
    Write-Host "=== Wireless Mode ==="
    $ip = Read-Host "IP-Adresse des Android-Geräts eingeben (z.B. 192.168.1.50)"
    if (-not $ip) { Write-Host "Keine IP eingegeben."; Read-Host "Fertig"; exit 1 }
    Write-Host "Verbinde mit $ip:5555..."
    & $adb connect "$ip:5555" | Out-Null
    Start-Sleep -Seconds 2
}

$dev = & $adb devices
Write-Host $dev
$lines = $dev | Where-Object { $_ -match "device$" -and $_ -notmatch "List" }
$lines = $lines | ForEach-Object { ($_ -split "\s+")[0] }
if ($lines.Count -eq 0) { 
    Write-Host "Kein Gerät gefunden." 
    if ($Wireless) { Write-Host "Prüfen: 'ADB über Netzwerk' in Entwickleroptionen aktiviert?" }
    Read-Host "Fertig"; exit 1 
}
$serial = $lines[0]
Write-Host "Nutze: $serial"

$apk = "TalkshowGaeste.apk"
if (-not (Test-Path $apk)) { $apk = "app\build\outputs\apk\release\app-release.apk" }
$apk = (Resolve-Path $apk).Path

Write-Host "Installiere $apk auf $serial..."
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { Write-Host "Installation fehlgeschlagen."; Read-Host "Fertig"; exit 1 }

Write-Host "Starte App..."
& $adb -s $serial shell monkey -p de.trenits.talkshowgaeste -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Fertig!"
if ($Wireless) { Write-Host "Hinweis: Bei WLAN-Verbindung kann die IP beim nächsten Neustart anders sein." }
Read-Host "Fertig"