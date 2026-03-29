@echo off
chcp 65001
setlocal

echo ============================================
echo MemoryPal 构建环境配置脚本
echo ============================================

:: 配置 Flutter 国内镜像
set PUB_HOSTED_URL=https://pub.flutter-io.cn
set FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn

echo.
echo 已配置环境变量：
echo PUB_HOSTED_URL=%PUB_HOSTED_URL%
echo FLUTTER_STORAGE_BASE_URL=%FLUTTER_STORAGE_BASE_URL%
echo.

:: 进入项目目录
cd /d "D:\Claudeworkplace\memorypal_v2_optimized"

echo 开始构建 APK...
echo.

:: 清理旧构建
flutter clean

:: 获取依赖
flutter pub get

:: 构建 Debug APK
flutter build apk --debug

echo.
echo 构建完成！
pause
