$WshShell = New-Object -ComObject WScript.Shell
$Desktop = [Environment]::GetFolderPath("Desktop")
$ShortcutPath = Join-Path $Desktop "Talkshow Gäste – Auf Gerät installieren.lnk"
$Shortcut = $WshShell.CreateShortcut($ShortcutPath)
$Shortcut.TargetPath = "powershell.exe"
$Shortcut.Arguments = "-ExecutionPolicy Bypass -File `"$PSScriptRoot\install-on-device.ps1`""
$Shortcut.WorkingDirectory = Split-Path $PSScriptRoot -Parent
$Shortcut.WindowStyle = 1
$Shortcut.Description = "Talkshow Gäste APK auf angeschlossenes Android-Gerät installieren"
$Shortcut.Save()
Write-Host "Shortcut erstellt: $ShortcutPath"
