# Cooler Control — Kotlin / Android Studio

این پروژه یک نمونه اولیه Android با Kotlin برای کنترل Arduino از طریق Bluetooth Classic و HC-05 است.

## تنظیمات
- Package: `com.example.coolercontrol`
- compileSdk / targetSdk: 33
- minSdk: 21
- JDK: 17
- Bluetooth SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`

## باز کردن و ساخت APK
1. Android Studio را باز کنید.
2. گزینه **Open** را بزنید و پوشه `CoolerControl` را انتخاب کنید.
3. نصب SDK Platform 33 و JDK 17 را تأیید کنید.
4. از **File > Sync Project with Gradle Files** استفاده کنید.
5. در صورت موفقیت Sync، از **Build > Build APK(s)** استفاده کنید.
6. خروجی معمولاً در `app/build/outputs/apk/debug/app-debug.apk` قرار می‌گیرد.

## اتصال
1. HC-05 را در تنظیمات بلوتوث گوشی Pair کنید.
2. اپ را اجرا کنید و مجوز Bluetooth را بدهید.
3. Settings > دستگاه را انتخاب کنید > اتصال.
4. تست کنید: `t` درخواست زمان و `s` درخواست وضعیت می‌فرستد.

## فرمان‌ها
- `1`: Pump
- `2`: Slow Motor
- `3`: Fast Motor
- `4`: تغییر حالت
- `T`: Timer
- `t`: درخواست زمان
- `s`: درخواست وضعیت
- `ONHHMM`: زمان روشن
- `OFFHHMM`: زمان خاموش
- `SHHMM`: تنظیم RTC
- `hFA` / `hEN`: راهنما

## فونت
فایل `Vazirmatn-Regular.ttf` را در مسیر زیر قرار دهید:
`app/src/main/assets/fonts/Vazirmatn-Regular.ttf`

اگر فایل فونت در دسترس نباشد، برنامه با فونت پیش‌فرض اجرا می‌شود.

## محدودیت‌ها
این یک نمونه اولیه است و روی گوشی/سخت‌افزار واقعی آزمایش نشده است. پاسخ‌های Arduino باید دقیقاً با قالب تعریف‌شده مطابقت داشته باشند. پیش از استفاده از رله‌ها، فرمان‌ها را بدون بار خطرناک آزمایش کنید.
