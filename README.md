# Thaumcraft port — Minecraft 1.21.1

Экспериментальный неофициальный перенос Thaumcraft 6 на NeoForge. Текущая версия: **0.1.0-alpha.9.19-dev**. Перенос игровых систем ещё продолжается.

## Сборка

Нужны **JDK 21**, Git и интернет для загрузки зависимостей.

```sh
git clone https://github.com/Arcturn/taumcraft-port-1.21.1.git
cd taumcraft-port-1.21.1
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

Linux / macOS:

```sh
./gradlew build
```

`build` компилирует мод и запускает JUnit-тесты. Готовый мод: `build/libs/thaumcraft-port-1.21.1-0.1.0-alpha.9.19-dev.jar`. Для установки нужен основной JAR без суффикса `-sources`.

Для игры установите **NeoForge 21.1.259** и **Curios 9.5.1+1.21.1**. TerraBlender 1.21.1-4.1.0.8 включается в JAR автоматически. Клиент и сервер должны использовать одинаковую версию порта.

Для запуска клиента из исходников: `./gradlew runClient` (Windows: `.\gradlew.bat runClient`). Дополнительные проверки: `./gradlew runData runGameTestServer`.

В репозитории находятся код, игровые ресурсы, тесты и файлы сборки. Отчёты сравнений, эталонный архив TC6, снимки экрана и видео сюда не включаются. Исходники ведутся в GitHub; пользователю выдаётся готовый JAR.

Оригинальные материалы Thaumcraft принадлежат Azanor. Атрибуция и условия сторонних компонентов: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
