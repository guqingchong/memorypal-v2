# MemoryPal APK 构建脚本
$env:PUB_HOSTED_URL = "https://pub.flutter-io.cn"
$env:FLUTTER_STORAGE_BASE_URL = "https://storage.flutter-io.cn"

Set-Location "D:\Claudeworkplace\memorypal_v2_optimized"

Write-Host "========================================"
Write-Host "MemoryPal APK 构建"
Write-Host "========================================"

# 清理旧构建
Write-Host "`n[1/4] 清理旧构建..."
Remove-Item -Path "android\app\.cxx" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "android\app\build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue

# 获取依赖
Write-Host "`n[2/4] 获取Flutter依赖..."
flutter pub get

if ($LASTEXITCODE -ne 0) {
    Write-Error "依赖获取失败"
    exit 1
}

# 构建 Debug APK
Write-Host "`n[3/4] 构建 Debug APK..."
flutter build apk --debug

if ($LASTEXITCODE -ne 0) {
    Write-Error "APK构建失败"
    exit 1
}

# 验证APK
Write-Host "`n[4/4] 验证APK..."
$apkPath = "build\app\outputs\flutter-apk\app-debug.apk"
if (Test-Path $apkPath) {
    $size = (Get-Item $apkPath).Length / 1MB
    Write-Host "`n✅ 构建成功!" -ForegroundColor Green
    Write-Host "📱 APK路径: $apkPath" -ForegroundColor Cyan
    Write-Host "📦 文件大小: $([math]::Round($size, 2)) MB" -ForegroundColor Cyan
} else {
    Write-Error "APK文件未找到"
    exit 1
}

Write-Host "`n========================================"
Write-Host "构建完成!"
Write-Host "========================================"
