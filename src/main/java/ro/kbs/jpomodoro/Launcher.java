package ro.kbs.jpomodoro;

import javafx.application.Application;

/** JVM entry point used by the packaged desktop launcher. */
public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    Application.launch(PomodoroApp.class, args);
  }
}
