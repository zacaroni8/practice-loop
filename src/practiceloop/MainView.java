package practiceloop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MainView {
    private final SessionStore store;
    private final NotificationScheduler notifier;
    private final ListView<Session> sessionList = new ListView<>();
    private final Label countdownLabel = new Label();
    private final Label activeLabel = new Label();
    private final Button startButton = new Button("Start");

    private Session activeSession;
    private LocalDateTime activeStartTime;
    private Timeline activeTimer;

    public MainView(SessionStore store, NotificationScheduler notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    public Scene createScene() {
        Button addActivityButton = new Button("Add Activity");
        addActivityButton.setOnAction(e -> showAddActivityDialog());

        Button createSessionButton = new Button("Create Session");
        createSessionButton.setOnAction(e -> showCreateSessionDialog());

        startButton.setOnAction(e -> startSoonestSession());

        HBox buttons = new HBox(10, addActivityButton, createSessionButton, startButton);
        buttons.setPadding(new Insets(10));

        VBox.setVgrow(sessionList, Priority.ALWAYS);
        VBox root = new VBox(10, buttons, countdownLabel, activeLabel, sessionList);
        root.setPadding(new Insets(10));

        refreshSessionList();
        updateCountdown();

        Timeline countdownTicker = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown())
        );
        countdownTicker.setCycleCount(Timeline.INDEFINITE);
        countdownTicker.play();

        Scene scene = new Scene(root, 500, 400);
        return scene;
    }

    private Optional<Session> soonestScheduled() {
        return store.listSessions().stream()
                .filter(s -> "scheduled".equals(s.status))
                .min(Comparator.comparing(s -> s.scheduledTime));
    }

    private void startSoonestSession() {
        Optional<Session> soonest = soonestScheduled();
        if (soonest.isEmpty()) return;

        activeSession = soonest.get();
        activeStartTime = LocalDateTime.now();
        startButton.setDisable(true);

        activeTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickActiveSession()));
        activeTimer.setCycleCount(Timeline.INDEFINITE);
        activeTimer.play();
    }

    private void tickActiveSession() {
        if (activeSession == null) return;

        long elapsedSeconds = java.time.Duration.between(activeStartTime, LocalDateTime.now()).getSeconds();
        long plannedSeconds = activeSession.plannedMinutes * 60L;

        if (elapsedSeconds >= plannedSeconds) {
            activeTimer.stop();
            store.completeSession(activeSession.id, activeSession.plannedMinutes);
            notifier.fireCompletion(activeSession);
            activeSession = null;
            activeLabel.setText("");
            refreshSessionList();
            return;
        }

        long remaining = plannedSeconds - elapsedSeconds;
        activeLabel.setText(activeSession.name + " running - " + (remaining / 60) + "m " + (remaining % 60) + "s left");
    }

    private void refreshSessionList() {
        List<Session> sessions = store.listSessions();
        sessionList.getItems().setAll(sessions);
    }

    private void updateCountdown() {
        Optional<Session> soonest = soonestScheduled();

        if (soonest.isEmpty()) {
            countdownLabel.setText("No upcoming sessions");
            startButton.setDisable(true);
            return;
        }

        Session s = soonest.get();
        LocalDateTime now = LocalDateTime.now();
        boolean ready = !now.isBefore(s.scheduledTime);
        startButton.setDisable(activeSession != null || !ready);

        if (ready) {
            countdownLabel.setText(s.name + " is ready to start");
            return;
        }

        java.time.Duration remaining = java.time.Duration.between(now, s.scheduledTime);
        long h = remaining.toHours();
        long m = remaining.toMinutesPart();
        long sec = remaining.toSecondsPart();
        countdownLabel.setText("Next: " + s.name + " in " + h + "h " + m + "m " + sec + "s");
    }

    private void showAddActivityDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Activity");

        TextField nameField = new TextField();
        TextField descriptionField = new TextField();
        TextField plannedMinutesField = new TextField("25");
        TextField leadMinutesField = new TextField("15");
        TextField xpPerMinuteField = new TextField("1.0");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Description"), descriptionField);
        grid.addRow(2, new Label("Default planned minutes"), plannedMinutesField);
        grid.addRow(3, new Label("Default lead minutes"), leadMinutesField);
        grid.addRow(4, new Label("Default XP per minute"), xpPerMinuteField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                store.createActivity(
                        nameField.getText(),
                        descriptionField.getText(),
                        Integer.parseInt(plannedMinutesField.getText()),
                        Integer.parseInt(leadMinutesField.getText()),
                        Double.parseDouble(xpPerMinuteField.getText())
                );
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showCreateSessionDialog() {
        List<Activity> activities = store.listActivities();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create Session");

        ComboBox<Activity> activityBox = new ComboBox<>();
        activityBox.getItems().addAll(activities);

        TextField nameField = new TextField();
        TextField descriptionField = new TextField();

        LocalDateTime defaultTime = LocalDateTime.now().plusMinutes(5);
        DatePicker datePicker = new DatePicker(defaultTime.toLocalDate());
        ComboBox<Integer> hourBox = new ComboBox<>();
        for (int h = 0; h < 24; h++) hourBox.getItems().add(h);
        hourBox.setValue(defaultTime.getHour());
        ComboBox<Integer> minuteBox = new ComboBox<>();
        for (int m = 0; m < 60; m += 5) minuteBox.getItems().add(m);
        minuteBox.setValue((defaultTime.getMinute() / 5) * 5);
        HBox timeBox = new HBox(5, datePicker, hourBox, new Label(":"), minuteBox);

        TextField plannedMinutesField = new TextField("25");
        TextField leadMinutesField = new TextField("15");

        activityBox.setOnAction(e -> {
            Activity a = activityBox.getValue();
            if (a != null) {
                nameField.setText(a.name);
                descriptionField.setText(a.description);
                plannedMinutesField.setText(String.valueOf(a.defaultPlannedMinutes));
                leadMinutesField.setText(String.valueOf(a.defaultLeadMinutes));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Activity (optional)"), activityBox);
        grid.addRow(1, new Label("Name"), nameField);
        grid.addRow(2, new Label("Description"), descriptionField);
        grid.addRow(3, new Label("Scheduled time"), timeBox);
        grid.addRow(4, new Label("Planned minutes"), plannedMinutesField);
        grid.addRow(5, new Label("Lead minutes"), leadMinutesField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK)
                .disableProperty().bind(datePicker.valueProperty().isNull());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Activity selected = activityBox.getValue();
                Integer activityId = selected == null ? null : selected.id;
                LocalDateTime scheduledTime = LocalDateTime.of(
                        datePicker.getValue(),
                        LocalTime.of(hourBox.getValue(), minuteBox.getValue())
                );
                store.createSession(
                        activityId,
                        nameField.getText(),
                        descriptionField.getText(),
                        scheduledTime,
                        Integer.parseInt(plannedMinutesField.getText()),
                        Integer.parseInt(leadMinutesField.getText())
                );
                refreshSessionList();
            }
            return null;
        });

        dialog.showAndWait();
    }
}
