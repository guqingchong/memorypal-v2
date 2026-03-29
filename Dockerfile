FROM cirrusci/flutter:3.41.5

WORKDIR /app
COPY . .

RUN flutter pub get
RUN flutter build apk --release

# 输出路径: /app/build/app/outputs/flutter-apk/app-release.apk