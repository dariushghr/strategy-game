# Strategy Game — Phase 1 (Pure Java 21 / Swing)

پیاده‌سازی فاز اول پروژه بازی استراتژیک تک‌نفره (درس برنامه‌نویسی پیشرفته)،
با گرافیک Swing خالص (بدون هیچ کتابخانه گرافیکی خارجی) و معماری MVC.

## پکیج پروژه

پکیج ریشه پروژه `org.strategygame` است.

## نیازمندی‌ها

- JDK **21** یا بالاتر (کد از سوییچ‌اکسپرشن‌ها، پترن‌متچینگ برای `instanceof`،
  `record`، متد‌های Sequenced Collections مثل `getFirst()` و لامبدا به‌جای
  کلاس‌های ناشناس استفاده می‌کند).
- Maven (فقط برای بیلد؛ پروژه هیچ وابستگی خارجی‌ای در زمان اجرا ندارد —
  فقط `javax.swing` / `java.awt` استاندارد JDK).

## اجرا با Maven

```bash
# اجرای مستقیم بازی
mvn exec:java

# یا ساخت jar قابل‌اجرا و سپس اجرای آن
mvn package
java -jar target/strategy-game.jar
```

## اجرا بدون Maven (فقط با JDK)

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out org.strategygame.Main
```

هر دو روش تست شده‌اند و پروژه بدون هیچ خطا یا هشدار کامپایل می‌شود.

## ساختار پروژه

```
pom.xml
src/main/java/org/strategygame/
├── Main.java
├── controller/        # GameController, TurnController, UnitController, BuildingController
├── model/
│   ├── GameState.java
│   ├── building/
│   ├── map/
│   ├── resource/
│   ├── unit/
│   └── upgrade/
└── view/
    ├── GameWindow.java
    ├── hud/
    ├── map/
    ├── menu/
    └── panel/
```

## نکات پیاده‌سازی

- **حرکت یونیت‌ها**: با الگوریتم دایکسترا روی گراف هگزی و بر اساس هزینه AP
  هر نوع زمین محاسبه می‌شود (`UnitController.reachable` / `move`).
- **چرخه نوبت** طبق ترتیب مستندسازی‌شده در راهنمای پروژه در
  `TurnController.execute()` پیاده شده: تجدید AP → تولید منابع → پیشرفت صف
  Town Hall → کسر Upkeep → مصرف غذا و بررسی قحطی.
- **Fog of War**، **Border Expansion**، **شارژ Builder**، و **استقرار
  Worker** طبق قوانین ثابت سند پیاده‌سازی شده‌اند.
- مقادیر آزاد (هزینه منابع، AP هر اقدام، نرخ تولید و ...) در
  `BuildingType`, `UpgradeType`, `BuildingController`, `Building*` تعیین
  شده و قابل تنظیم است.
