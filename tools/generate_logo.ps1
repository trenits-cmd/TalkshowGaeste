# Erzeugt das App-Logo und alle Android-Launcher-Icons.
# Ausführen: powershell.exe -ExecutionPolicy Bypass -File tools\generate_logo.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$densitySizes = @{ "mdpi" = 48; "hdpi" = 72; "xhdpi" = 96; "xxhdpi" = 144; "xxxhdpi" = 192 }

function New-RoundedPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-LogoBitmap([int]$size, [bool]$round) {
    $scale = $size / 1024.0
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $bg = [System.Drawing.ColorTranslator]::FromHtml("#0B0F14")
    if ($round) {
        $g.FillEllipse((New-Object System.Drawing.SolidBrush($bg)), 0, 0, $size, $size)
    } else {
        $path = New-RoundedPath (48 * $scale) (48 * $scale) (928 * $scale) (928 * $scale) (220 * $scale)
        $gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
            (New-Object System.Drawing.Point(0, 0)),
            (New-Object System.Drawing.Point(0, $size)),
            [System.Drawing.ColorTranslator]::FromHtml("#151C25"),
            [System.Drawing.ColorTranslator]::FromHtml("#080B0F"))
        $g.FillPath($gradient, $path)
        $gradient.Dispose()
        $path.Dispose()
    }

    # dezenter Gold-Ring
    $ringPen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml("#333022"), (10 * $scale))
    if ($round) {
        $g.DrawEllipse($ringPen, (20 * $scale), (20 * $scale), (984 * $scale), (984 * $scale))
    } else {
        $ringPath = New-RoundedPath (62 * $scale) (62 * $scale) (900 * $scale) (900 * $scale) (205 * $scale)
        $g.DrawPath($ringPen, $ringPath)
        $ringPath.Dispose()
    }
    $ringPen.Dispose()

    # Sprechblase
    $amber = [System.Drawing.ColorTranslator]::FromHtml("#E7B45B")
    $amberBrush = New-Object System.Drawing.SolidBrush($amber)
    $bubble = New-RoundedPath (176 * $scale) (214 * $scale) (672 * $scale) (420 * $scale) (96 * $scale)
    $g.FillPath($amberBrush, $bubble)
    $bubble.Dispose()

    # Sprechblasen-Schwanz
    $tail = New-Object System.Drawing.Drawing2D.GraphicsPath
    $tail.AddPolygon(@(
        (New-Object System.Drawing.PointF((318 * $scale), (612 * $scale))),
        (New-Object System.Drawing.PointF((306 * $scale), (790 * $scale))),
        (New-Object System.Drawing.PointF((486 * $scale), (628 * $scale)))
    ))
    $g.FillPath($amberBrush, $tail)
    $tail.Dispose()

    # drei Punkte
    $darkBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#0B0F14"))
    foreach ($cx in @(386, 512, 638)) {
        $r = 38 * $scale
        $g.FillEllipse($darkBrush, ($cx * $scale - $r), (424 * $scale - $r), ($r * 2), ($r * 2))
    }

    # kleine Live-Ecke
    $redBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#E45C5C"))
    $g.FillEllipse($redBrush, (742 * $scale), (612 * $scale), (54 * $scale), (54 * $scale))

    $amberBrush.Dispose()
    $darkBrush.Dispose()
    $redBrush.Dispose()
    $g.Dispose()
    return $bmp
}

function Save-Resized([System.Drawing.Bitmap]$source, [int]$size, [string]$path) {
    $target = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($target)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($source, 0, 0, $size, $size)
    $g.Dispose()
    $dir = Split-Path -Parent $path
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $target.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $target.Dispose()
}

$square = New-LogoBitmap 1024 $false
$roundBmp = New-LogoBitmap 1024 $true

$resDir = Join-Path $root "app\src\main\res"
$assetDir = Join-Path $root "assets"
if (-not (Test-Path $assetDir)) { New-Item -ItemType Directory -Force -Path $assetDir | Out-Null }

Save-Resized $square 512 (Join-Path $root "logo.png")
Save-Resized $square 512 (Join-Path $assetDir "logo.png")
Save-Resized $square 1024 (Join-Path $assetDir "logo-1024.png")
Save-Resized $square 1280 (Join-Path $assetDir "feature-graphic.png") # wird unten auf 1024x500 gesetzt

foreach ($density in $densitySizes.Keys) {
    $size = $densitySizes[$density]
    $folder = Join-Path $resDir "mipmap-$density"
    if (-not (Test-Path $folder)) { New-Item -ItemType Directory -Force -Path $folder | Out-Null }
    Save-Resized $square $size (Join-Path $folder "ic_launcher.png")
    Save-Resized $roundBmp $size (Join-Path $folder "ic_launcher_round.png")
}

# Feature-Graphic 1024 x 500 für GitHub
$feature = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$fg = [System.Drawing.Graphics]::FromImage($feature)
$fg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$fg.Clear([System.Drawing.ColorTranslator]::FromHtml("#0B0F14"))
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(1024, 500)),
    [System.Drawing.ColorTranslator]::FromHtml("#151C25"),
    [System.Drawing.ColorTranslator]::FromHtml("#080B0F"))
$fg.FillRectangle($brush, 0, 0, 1024, 500)
$logoSmall = New-Object System.Drawing.Bitmap(300, 300)
$lg = [System.Drawing.Graphics]::FromImage($logoSmall)
$lg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$lg.DrawImage($square, 0, 0, 300, 300)
$lg.Dispose()
$fg.DrawImage($logoSmall, 82, 100, 300, 300)
$titleFont = New-Object System.Drawing.Font("Segoe UI", 46, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$subFont = New-Object System.Drawing.Font("Segoe UI", 24, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$white = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#F4F1EA"))
$muted = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#98A4B3"))
$gold = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#E7B45B"))
$fg.DrawString("Talkshow Gäste", $titleFont, $white, 430, 165)
$fg.DrawString("Sendezeiten & Gäste der politischen Talkshows", $subFont, $muted, 434, 235)
$fg.DrawString("JETZT IM TV", (New-Object System.Drawing.Font("Segoe UI", 20, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)), $gold, 436, 300)
$fg.Dispose()
$feature.Save((Join-Path $assetDir "feature-graphic.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$feature.Dispose()

$square.Dispose()
$roundBmp.Dispose()
Write-Host "Logo und Icons erzeugt."
