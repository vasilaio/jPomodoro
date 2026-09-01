# jPomodoro

A focused JavaFX desktop Pomodoro timer built with Maven and Java 25.

## Run during development

```powershell
mvn javafx:run
```

## Build a portable Windows application

```powershell
mvn clean package
```

This creates a self-contained app image at `target/package/jPomodoro/`.
Run `target/package/jPomodoro/jPomodoro.exe`; zip that entire directory to distribute it.
The app includes a trimmed Java runtime and JavaFX, so end users do not need Java installed. The packaged launcher embeds the jPomodoro Windows icon for the title bar and taskbar.

The app starts with a 25-minute focus interval and 5-minute break. Change both durations while the timer is stopped.
Under **Completion alerts**, you can independently enable or disable the Windows notification and notification sound. These choices are remembered for future launches.

Use the **Settings** view to change interval durations, autostart the next break or focus interval, and choose whether closing the main window minimizes it to the system tray. By default, closing exits the application.

## Windows notification title

When launched with `mvn javafx:run`, Windows identifies the notification as **OpenJDK Platform binary** because that command runs the Java development process. Build and start the packaged launcher (`target/package/jPomodoro/jPomodoro.exe`) for Windows to identify it as **jPomodoro**.
