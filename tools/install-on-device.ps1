# Talkshow Gaste - Install on connected Android device
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
$dev = & $adb devices
Write-Host $dev
$lines = $dev | Where-Object { $_ -match "device$" -and $_ -notmatch "List" }
$lines = $lines | ForEach-Object { ($_ -split "\s+")[0] }
if ($lines.Count -eq 0) { Read-Host "Kein Gerat"; exit 1 }
$serial = $lines[0]
Write-Host "Nutze: $serial"
$apk = "TalkshowGaeste.apk"
if (-not (Test-Path $apk)) { $apk = "app\build\outputs\apk\release\app-release.apk" }
$apk = (Resolve-Path $apk).Path
& $adb -s $serial install -r $apk
& $adb -s $serial shell monkey -p de.trenits.talkshowgaeste -c android.intent.category.LAUNCHER 1 | Out-Null
Read-Host "Fertig"
