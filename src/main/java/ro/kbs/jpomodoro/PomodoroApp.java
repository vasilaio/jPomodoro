package ro.kbs.jpomodoro;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * A compact desktop Pomodoro timer.
 */
public final class PomodoroApp extends Application {

  private final Label modeLabel = new Label();
  private final Label timerLabel = new Label();
  private final Label sessionsLabel = new Label();
  private final ProgressBar progress = new ProgressBar(1);
  private final Button startPauseButton = new Button("Start");
  private final Button resetButton = new Button("Reset");
  private final Button settingsButton = new Button("\u2699");
  private final Button workMinusButton = new Button("\u2212");
  private final Button workPlusButton = new Button("\u002b");
  private final Button breakMinusButton = new Button("\u2212");
  private final Button breakPlusButton = new Button("\u002b");
  private final ToggleButton soundToggle = new ToggleButton();
  private final ToggleButton windowsNotificationToggle = new ToggleButton();
  private final ToggleButton autostartBreakToggle = new ToggleButton();
  private final ToggleButton autostartFocusToggle = new ToggleButton();
  private final ToggleButton minimizeOnCloseToggle = new ToggleButton();
  private final Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1), _ -> this.tick()));

  private Settings settings;
  private Spinner<Integer> workSpinner;
  private Spinner<Integer> breakSpinner;

  private boolean running;
  private boolean onBreak;
  private boolean settingsOpen;
  private int remainingSeconds;
  private int totalSeconds;
  private TrayIcon trayIcon;
  private Stage primaryStage;
  private StackPane appRoot;
  private VBox settingsPanel;
  private Region settingsOverlay;

  @Override
  public void start(final Stage stage) {
    this.primaryStage = stage;
    Platform.setImplicitExit(false);
    this.ticker.setCycleCount(Animation.INDEFINITE);
    this.settings = Settings.load();
    this.configureSpinners();
    this.configureSettings();
    this.resetTimer();

    this.modeLabel.getStyleClass().add("mode-label");
    this.timerLabel.getStyleClass().add("timer-label");
    this.sessionsLabel.getStyleClass().add("sessions-label");
    this.progress.getStyleClass().add("timer-progress");
    this.progress.setMaxWidth(Double.MAX_VALUE);
    this.progress.setPrefHeight(10);

    final Label title = new Label("Focus flow");
    title.getStyleClass().add("title");
    final Label subtitle = new Label("One task. One interval. Full attention.");
    subtitle.getStyleClass().add("subtitle");

    final VBox header = new VBox(6, title, subtitle);
    header.setAlignment(Pos.CENTER);

    final BorderPane headerPane = new BorderPane();
    headerPane.setCenter(header);
    this.settingsButton.getStyleClass().add("icon-button");
    this.settingsButton.setOnAction(_ -> this.openSettings());
    headerPane.setRight(this.settingsButton);

    final VBox clock = new VBox(12, this.modeLabel, this.timerLabel, this.progress, this.sessionsLabel);
    clock.setAlignment(Pos.CENTER);
    clock.setMaxWidth(360);

    this.startPauseButton.getStyleClass().add("primary-button");
    this.resetButton.getStyleClass().add("secondary-button");
    this.startPauseButton.setOnAction(_ -> this.toggleTimer());
    this.resetButton.setOnAction(_ -> this.resetTimer());
    final HBox controls = new HBox(12, this.startPauseButton, this.resetButton);
    controls.setAlignment(Pos.CENTER);

    this.workSpinner.valueProperty().addListener((_, _, _) -> this.updateIdleDuration());
    this.breakSpinner.valueProperty().addListener((_, _, _) -> this.updateIdleDuration());
    this.workMinusButton.setOnAction(_ -> this.workSpinner.decrement());
    this.workMinusButton.setFocusTraversable(false);
    this.workPlusButton.setOnAction(_ -> this.workSpinner.increment());
    this.workPlusButton.setFocusTraversable(false);
    this.breakMinusButton.setOnAction(_ -> this.breakSpinner.decrement());
    this.breakMinusButton.setFocusTraversable(false);
    this.breakPlusButton.setOnAction(_ -> this.breakSpinner.increment());
    this.breakPlusButton.setFocusTraversable(false);

    final BorderPane mainView = new BorderPane();
    mainView.setTop(headerPane);
    mainView.setCenter(new VBox(28, clock, controls));
    BorderPane.setAlignment(mainView.getCenter(), Pos.CENTER);
    mainView.setPadding(new Insets(42, 48, 36, 48));
    mainView.getStyleClass().add("root");

    this.settingsOverlay = new Region();
    this.settingsOverlay.getStyleClass().add("settings-overlay");
    this.settingsOverlay.setOpacity(0);
    this.settingsOverlay.setVisible(false);
    this.settingsOverlay.setMouseTransparent(true);
    this.settingsOverlay.setOnMouseClicked(_ -> this.closeSettings());

    this.settingsPanel = this.createSettingsPanel();
    this.settingsPanel.setVisible(false);
    this.settingsPanel.setMouseTransparent(true);

    this.appRoot = new StackPane(mainView, this.settingsOverlay, this.settingsPanel);
    StackPane.setAlignment(this.settingsPanel, Pos.CENTER);

    final Scene scene = new Scene(this.appRoot, 520, 460);
    scene.getStylesheets().add(this.getClass().getResource("/style.css").toExternalForm());
    scene.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ESCAPE && this.settingsOpen) {
        this.closeSettings();
      }
    });

    stage.setTitle("jPomodoro");
    stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/jPomodoro.png")));
    stage.setWidth(460);
    stage.setHeight(500);
    stage.setResizable(false);
    stage.setScene(scene);
    stage.setOnCloseRequest(event -> {
      event.consume();
      if (!this.minimizeOnCloseToggle.isSelected() || !this.minimizeToTray()) {
        this.shutdownApplication();
      }
    });
    stage.show();
  }

  private void configureSpinners() {
    this.workSpinner = this.minutesSpinner(this.settings.getWorkMinutes());
    this.workSpinner.valueProperty().addListener((_, _, _) -> this.settings.setWorkMinutes(this.workSpinner.getValue()));
//    this.workSpinner.getEditor().setOnKeyPressed(event -> {
//      if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
//        event.consume();
//        this.breakSpinner.getEditor().requestFocus();
//      }
//    });

    this.breakSpinner = this.minutesSpinner(this.settings.getBreakMinutes());
    this.breakSpinner.valueProperty().addListener((_, _, _) -> this.settings.setBreakMinutes(this.breakSpinner.getValue()));
//    this.breakSpinner.getEditor().setOnKeyPressed(event -> {
//      if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
//        event.consume();
//        this.workSpinner.getEditor().requestFocus();
//      }
//    });
    this.settings.save();
  }

  private Spinner<Integer> minutesSpinner(final int value) {
    final Spinner<Integer> spinner = new Spinner<>();
    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, value));
    spinner.setEditable(true);
    spinner.getStyleClass().add("interval-spinner");
    spinner.setPrefWidth(52);
    spinner.getEditor().focusedProperty().addListener((_, _, focused) -> {
      if (focused) {
        Platform.runLater(() -> spinner.getEditor().selectAll());
      }
    });
    return spinner;
  }

  private VBox createSettingsPanel() {
    final Button backButton = new Button("\u2190");
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(_ -> this.closeSettings());

    final Label panelTitle = new Label("Settings");
    panelTitle.getStyleClass().add("settings-header-title");

    final HBox panelHeader = new HBox(10, backButton, panelTitle);
    panelHeader.setAlignment(Pos.CENTER_LEFT);

    final HBox intervals = new HBox(16, this.intervalStepper("Focus", this.workMinusButton, this.workSpinner, this.workPlusButton),
        this.intervalStepper("Break", this.breakMinusButton, this.breakSpinner, this.breakPlusButton));
    intervals.setAlignment(Pos.CENTER);

    final VBox content = new VBox(14, panelHeader, this.sectionBlock("Intervals", intervals),
        this.sectionBlock("Alerts", this.toggleRow("Sound on complete", this.soundToggle), this.toggleRow("Windows notification", this.windowsNotificationToggle)),
        this.sectionBlock("Behavior", this.toggleRow("Auto-start break", this.autostartBreakToggle), this.toggleRow("Auto-start focus", this.autostartFocusToggle),
            this.toggleRow("Minimize on close", this.minimizeOnCloseToggle)));
    content.setAlignment(Pos.TOP_LEFT);
    content.setPadding(new Insets(20, 24, 24, 24));
    content.getStyleClass().add("settings-panel");
    content.setMaxWidth(460);
    content.setMaxHeight(500);
    return content;
  }

  private VBox intervalStepper(final String label, final Button minus, final Spinner<Integer> spinner, final Button plus) {
    minus.getStyleClass().add("stepper-button");
    plus.getStyleClass().add("stepper-button");

    final Label nameLabel = new Label(label);
    nameLabel.getStyleClass().add("setting-label");
    final HBox stepper = new HBox(6, minus, spinner, plus);
    stepper.setAlignment(Pos.CENTER);

    final VBox box = new VBox(4, nameLabel, stepper);
    box.setAlignment(Pos.CENTER);
    HBox.setHgrow(box, Priority.ALWAYS);
    return box;
  }

  private VBox sectionBlock(final String title, final javafx.scene.Node... rows) {
    final VBox block = new VBox(8, this.sectionTitle(title));
    block.getChildren().addAll(rows);
    block.getStyleClass().add("settings-section");
    return block;
  }

  private HBox toggleRow(final String label, final ToggleButton toggle) {
    toggle.getStyleClass().add("toggle-switch");

    final Label rowLabel = new Label(label);
    rowLabel.getStyleClass().add("toggle-label");
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    final HBox row = new HBox(12, rowLabel, spacer, toggle);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add("toggle-row");
    return row;
  }

  private void openSettings() {
    if (this.settingsOpen) {
      return;
    }
    this.settingsOpen = true;
    this.settingsButton.setDisable(true);

    this.settingsOverlay.setVisible(true);
    this.settingsOverlay.setMouseTransparent(false);
    this.settingsPanel.setVisible(true);
    this.settingsPanel.setMouseTransparent(false);

    final double width = this.appRoot.getWidth() > 0 ? this.appRoot.getWidth() : 460;
    this.settingsPanel.setTranslateX(width);

    final FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this.settingsOverlay);
    fadeIn.setFromValue(0);
    fadeIn.setToValue(1);

    final TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), this.settingsPanel);
    slideIn.setFromX(width);
    slideIn.setToX(0);

    final ParallelTransition open = new ParallelTransition(fadeIn, slideIn);
    open.setOnFinished(_ -> {
      Platform.runLater(this.workSpinner.getEditor()::requestFocus);
    });
    open.play();
  }

  private void closeSettings() {
    if (!this.settingsOpen) {
      return;
    }
    this.settingsOpen = false;

    final double width = this.appRoot.getWidth() > 0 ? this.appRoot.getWidth() : 460;

    final FadeTransition fadeOut = new FadeTransition(Duration.millis(180), this.settingsOverlay);
    fadeOut.setFromValue(this.settingsOverlay.getOpacity());
    fadeOut.setToValue(0);

    final TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), this.settingsPanel);
    slideOut.setFromX(this.settingsPanel.getTranslateX());
    slideOut.setToX(width);

    final ParallelTransition close = new ParallelTransition(fadeOut, slideOut);
    close.setOnFinished(_ -> {
      this.settingsOverlay.setVisible(false);
      this.settingsOverlay.setMouseTransparent(true);
      this.settingsPanel.setVisible(false);
      this.settingsPanel.setMouseTransparent(true);
      this.settingsPanel.setTranslateX(0);
      this.settingsButton.setDisable(false);
    });
    close.play();
  }

  private void toggleTimer() {
    if (this.running) {
      this.ticker.pause();
      this.running = false;
      this.startPauseButton.setText("Resume");
    } else {
      this.startTimer();
    }
    this.setSettingsDisabled(this.running);
  }

  private void startTimer() {
    this.ticker.play();
    this.running = true;
    this.startPauseButton.setText("Pause");
    this.setSettingsDisabled(true);
  }

  private void tick() {
    if (this.remainingSeconds > 0) {
      this.remainingSeconds--;
      if (this.remainingSeconds > 0) {
        this.refreshClock();
        return;
      }
    }

    this.completeInterval();
  }

  private void completeInterval() {
    final boolean focusIntervalFinished = !this.onBreak;
    if (this.soundToggle.isSelected()) {
      ToolkitBeep.play();
    }
    if (this.windowsNotificationToggle.isSelected()) {
      this.showWindowsNotification(focusIntervalFinished);
    }
    if (!this.onBreak) {
      this.settings.setCompletedSessions(this.settings.getCompletedSessions() + 1);
      this.settings.save();
      this.onBreak = true;
      this.totalSeconds = this.breakSpinner.getValue() * 60;
    } else {
      this.onBreak = false;
      this.totalSeconds = this.workSpinner.getValue() * 60;
    }
    this.remainingSeconds = this.totalSeconds;
    this.refreshClock();

    final boolean shouldAutostart = focusIntervalFinished ? this.autostartBreakToggle.isSelected() : this.autostartFocusToggle.isSelected();
    if (shouldAutostart) {
      this.startTimer();
    } else {
      this.ticker.stop();
      this.running = false;
      this.startPauseButton.setText("Start");
      this.setSettingsDisabled(false);
    }
  }

  private void resetTimer() {
    this.ticker.stop();
    this.running = false;
    this.onBreak = false;
    this.totalSeconds = this.workSpinner.getValue() * 60;
    this.remainingSeconds = this.totalSeconds;
    this.startPauseButton.setText("Start");
    this.setSettingsDisabled(false);
    this.refreshClock();
  }

  private void updateIdleDuration() {
    if (!this.running) {
      this.totalSeconds = (this.onBreak ? this.breakSpinner : this.workSpinner).getValue() * 60;
      this.remainingSeconds = this.totalSeconds;
      this.refreshClock();
    }
  }

  private void refreshClock() {
    final int minutes = this.remainingSeconds / 60;
    final int seconds = this.remainingSeconds % 60;
    this.timerLabel.setText("%02d:%02d".formatted(minutes, seconds));
    this.modeLabel.setText(this.onBreak ? "BREAK" : "FOCUS");
    this.modeLabel.pseudoClassStateChanged(PseudoClass.getPseudoClass("break"), this.onBreak);
    final String sessionText = "%d focus session%s completed".formatted(this.settings.getCompletedSessions(), this.settings.getCompletedSessions() == 1 ? "" : "s");
    this.sessionsLabel.setText(sessionText);
    this.progress.setProgress(this.totalSeconds == 0 ? 0 : (double) this.remainingSeconds / this.totalSeconds);
  }

  private void setSettingsDisabled(final boolean disabled) {
    this.workSpinner.setDisable(disabled);
    this.workMinusButton.setDisable(disabled);
    this.workPlusButton.setDisable(disabled);
    this.breakSpinner.setDisable(disabled);
    this.breakMinusButton.setDisable(disabled);
    this.breakPlusButton.setDisable(disabled);
  }

  private void configureSettings() {
    this.styleToggle(this.soundToggle);
    this.styleToggle(this.windowsNotificationToggle);
    this.styleToggle(this.autostartBreakToggle);
    this.styleToggle(this.autostartFocusToggle);
    this.styleToggle(this.minimizeOnCloseToggle);

    this.soundToggle.setSelected(this.settings.isSoundEnabled());
    this.windowsNotificationToggle.setSelected(this.settings.isWindowsNotificationEnabled());
    this.autostartBreakToggle.setSelected(this.settings.isAutostartBreakEnabled());
    this.autostartFocusToggle.setSelected(this.settings.isAutostartFocusEnabled());
    this.minimizeOnCloseToggle.setSelected(this.settings.isMinimizeOnCloseEnabled());
    this.soundToggle.selectedProperty().addListener((_, _, enabled) -> {
      this.settings.setSoundEnabled(enabled);
      this.settings.save();
    });
    this.windowsNotificationToggle.selectedProperty().addListener((_, _, enabled) -> {
      this.settings.setWindowsNotificationEnabled(enabled);
      this.settings.save();
    });
    this.autostartBreakToggle.selectedProperty().addListener((_, _, enabled) -> {
      this.settings.setAutostartBreakEnabled(enabled);
      this.settings.save();
    });
    this.autostartFocusToggle.selectedProperty().addListener((_, _, enabled) -> {
      this.settings.setAutostartFocusEnabled(enabled);
      this.settings.save();
    });
    this.minimizeOnCloseToggle.selectedProperty().addListener((_, _, enabled) -> {
      this.settings.setMinimizeOnCloseEnabled(enabled);
      this.settings.save();
    });
  }

  private void styleToggle(final ToggleButton toggle) {
    toggle.setMinSize(44, 24);
    toggle.setPrefSize(44, 24);
    toggle.setMaxSize(44, 24);
  }

  private Label sectionTitle(final String text) {
    final Label label = new Label(text);
    label.getStyleClass().add("settings-title");
    return label;
  }

  private void showWindowsNotification(final boolean focusIntervalFinished) {
    if (!this.ensureTrayIcon()) {
      return;
    }
    final String message = focusIntervalFinished ? "Focus interval complete. Time for a break." : "Break complete. Ready for your next focus interval.";
    this.trayIcon.displayMessage("jPomodoro", message, TrayIcon.MessageType.INFO);
  }

  private boolean minimizeToTray() {
    if (this.ensureTrayIcon()) {
      this.primaryStage.hide();
      return true;
    }
    return false;
  }

  private boolean ensureTrayIcon() {
    if (!SystemTray.isSupported()) {
      return false;
    }
    if (this.trayIcon != null) {
      return true;
    }

    try {
      this.trayIcon = new TrayIcon(this.notificationIcon(), "jPomodoro", this.createTrayMenu());
      this.trayIcon.setImageAutoSize(true);
      this.trayIcon.addActionListener(_ -> Platform.runLater(this::showMainWindow));
      SystemTray.getSystemTray().add(this.trayIcon);
      return true;
    } catch (AWTException | SecurityException ignored) {
      return false;
    }
  }

  private PopupMenu createTrayMenu() {
    final PopupMenu menu = new PopupMenu();
    final MenuItem showItem = new MenuItem("Show jPomodoro");
    showItem.addActionListener(_ -> Platform.runLater(this::showMainWindow));
    final MenuItem exitItem = new MenuItem("Exit jPomodoro");
    exitItem.addActionListener(_ -> Platform.runLater(this::shutdownApplication));
    menu.add(showItem);
    menu.addSeparator();
    menu.add(exitItem);
    return menu;
  }

  private void showMainWindow() {
    this.primaryStage.show();
    this.primaryStage.toFront();
    this.primaryStage.requestFocus();
  }

  private void shutdownApplication() {
    if (this.trayIcon != null) {
      SystemTray.getSystemTray().remove(this.trayIcon);
      this.trayIcon = null;
    }
    if (this.settingsOpen) {
      this.closeSettings();
    }
    this.ticker.stop();
    Platform.exit();
    System.exit(0);
  }

  private java.awt.Image notificationIcon() {
    final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    graphics.setColor(new Color(17, 36, 63));
    graphics.fillOval(0, 0, 16, 16);
    graphics.setColor(new Color(107, 227, 186));
    graphics.fillOval(2, 2, 12, 12);
    graphics.setColor(new Color(24, 36, 59));
    graphics.fillOval(4, 4, 8, 8);
    graphics.setColor(Color.WHITE);
    graphics.drawLine(8, 8, 8, 5);
    graphics.drawLine(8, 8, 10, 9);
    graphics.dispose();
    return image;
  }

  public static void main(final String[] args) {
    launch(args);
  }

  /** Keeps system feedback encapsulated and harmless on platforms without a toolkit. */
  private static final class ToolkitBeep {
    private static void play() {
      Platform.runLater(() -> Toolkit.getDefaultToolkit().beep());
    }
  }
}
