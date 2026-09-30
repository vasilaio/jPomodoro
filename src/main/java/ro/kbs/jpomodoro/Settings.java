package ro.kbs.jpomodoro;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent application settings backed by a JSON file.
 */
public final class Settings {

  private static final Path SETTINGS_FILE = Path.of(".").toAbsolutePath().resolve("settings.json");

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private boolean soundEnabled = true;
  private boolean windowsNotificationEnabled = true;
  private boolean autostartBreakEnabled = false;
  private boolean autostartFocusEnabled = false;
  private boolean minimizeOnCloseEnabled = false;
  private int completedSessions = 0;
  private int workMinutes = 25;
  private int breakMinutes = 5;

  public boolean isSoundEnabled() {
    return this.soundEnabled;
  }

  public void setSoundEnabled(final boolean soundEnabled) {
    this.soundEnabled = soundEnabled;
  }

  public boolean isWindowsNotificationEnabled() {
    return this.windowsNotificationEnabled;
  }

  public void setWindowsNotificationEnabled(final boolean windowsNotificationEnabled) {
    this.windowsNotificationEnabled = windowsNotificationEnabled;
  }

  public boolean isAutostartBreakEnabled() {
    return this.autostartBreakEnabled;
  }

  public void setAutostartBreakEnabled(final boolean autostartBreakEnabled) {
    this.autostartBreakEnabled = autostartBreakEnabled;
  }

  public boolean isAutostartFocusEnabled() {
    return this.autostartFocusEnabled;
  }

  public void setAutostartFocusEnabled(final boolean autostartFocusEnabled) {
    this.autostartFocusEnabled = autostartFocusEnabled;
  }

  public boolean isMinimizeOnCloseEnabled() {
    return this.minimizeOnCloseEnabled;
  }

  public void setMinimizeOnCloseEnabled(final boolean minimizeOnCloseEnabled) {
    this.minimizeOnCloseEnabled = minimizeOnCloseEnabled;
  }

  public int getCompletedSessions() {
    return this.completedSessions;
  }

  public void setCompletedSessions(final int completedSessions) {
    this.completedSessions = completedSessions;
  }

  public int getWorkMinutes() {
    return this.workMinutes;
  }

  public void setWorkMinutes(final int workMinutes) {
    this.workMinutes = workMinutes;
  }

  public int getBreakMinutes() {
    return this.breakMinutes;
  }

  public void setBreakMinutes(final int breakMinutes) {
    this.breakMinutes = breakMinutes;
  }

  /**
   * Load settings from the JSON file, or return defaults if the file is missing.
   */
  public static Settings load() {
    if (Files.exists(SETTINGS_FILE)) {
      try {
        final String json = Files.readString(SETTINGS_FILE, StandardCharsets.UTF_8);
        return GSON.fromJson(json, Settings.class);
      } catch (final IOException e) {
        System.err.println("Failed to read settings, using defaults: " + e.getMessage());
      }
    }
    return new Settings();
  }

  /**
   * Persist this settings instance to disk.
   */
  public void save() {
    try {
      final String json = GSON.toJson(this);
      Files.writeString(SETTINGS_FILE, json, StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);
    } catch (final IOException e) {
      System.err.println("Failed to save settings: " + e.getMessage());
    }
  }
}
