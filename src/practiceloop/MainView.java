package practiceloop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.Optional;

public class MainView {
    private final SessionStore store;
    private final NotificationScheduler notifier;
    private final XpCalculator xpCalculator = new XpCalculator();
    private final ListView<Session> sessionList = new ListView<>();
    private final Label countdownLabel = new Label();
    private final Label activeLabel = new Label();
    private final Label totalXpLabel = new Label();
    private final Label claimLabel = new Label();
    private final Button addActivityButton = new Button("Add Activity");
    private final Button createSessionButton = new Button("Create Session");
    private final Button startButton = new Button("Start");
    private final Button stopEarlyButton = new Button("Stop Early");
    private final Button claimButton = new Button("Claim XP");
    private final Button historyToggleButton = new Button("View History");
    private final ComboBox<Activity> activityFilterBox = new ComboBox<>();
    private final CheckBox miscOnlyCheck = new CheckBox("Misc only");
    private final HBox filterRow = new HBox(10, activityFilterBox, miscOnlyCheck);

    private boolean showingHistory = false;

    private Session activeSession;
    private LocalDateTime activeStartTime;
    private Timeline activeTimer;

    private Session pendingClaimSession;
    private int pendingXp;

    public MainView(SessionStore store, NotificationScheduler notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    public Scene createScene() {
        addActivityButton.setOnAction(e -> showAddActivityDialog());
        createSessionButton.setOnAction(e -> showCreateSessionDialog());

        startButton.setOnAction(e -> startSoonestSession());
        stopEarlyButton.setOnAction(e -> stopEarly());
        stopEarlyButton.setDisable(true);
        claimButton.setOnAction(e -> claimPendingXp());
        claimButton.setDisable(true);

        historyToggleButton.setOnAction(e -> toggleHistory());
        activityFilterBox.setPromptText("All Activities");
        activityFilterBox.setOnAction(e -> refreshSessionList());
        miscOnlyCheck.setOnAction(e -> {
            activityFilterBox.setDisable(miscOnlyCheck.isSelected());
            refreshSessionList();
        });
        setShown(filterRow, false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(10, addActivityButton, createSessionButton, startButton,
                stopEarlyButton, historyToggleButton, spacer, totalXpLabel);
        buttons.setPadding(new Insets(10));

        VBox.setVgrow(sessionList, Priority.ALWAYS);
        VBox root = new VBox(10, buttons, filterRow, countdownLabel, activeLabel, claimLabel, claimButton, sessionList);
        root.setPadding(new Insets(10));

        refreshSessionList();
        updateCountdown();
        updateTotalXpLabel();

        Timeline countdownTicker = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown())
        );
        countdownTicker.setCycleCount(Timeline.INDEFINITE);
        countdownTicker.play();

        Scene scene = new Scene(root, 500, 400);
        return scene;
    }

    private void updateTotalXpLabel() {
        totalXpLabel.setText("XP: " + store.getTotalXp());
    }

    /** True if [start, start+plannedMinutes) overlaps any other still-scheduled session. */
    private boolean overlapsExistingSession(LocalDateTime start, int plannedMinutes) {
        LocalDateTime end = start.plusMinutes(plannedMinutes);
        return store.listSessions().stream()
                .filter(s -> "scheduled".equals(s.status))
                .anyMatch(s -> {
                    LocalDateTime existingEnd = s.scheduledTime.plusMinutes(s.plannedMinutes);
                    return start.isBefore(existingEnd) && s.scheduledTime.isBefore(end);
                });
    }

    private List<Session> upcomingSessions() 
    {
        return store.listSessions().stream()
                    .filter(s -> "scheduled".equals(s.status)).toList();
    }
    
    private List<Session> historySessions(Activity activityFilter, boolean miscOnly)
    {
        Stream<Session> returnList = store.listSessions().stream()
                    .filter(s -> "completed".equals(s.status));
        if(miscOnly)
        {
            returnList = returnList.filter(s -> null == s.activityId);
        }
        else if(activityFilter!= null)
        {
            returnList = returnList.filter(s -> s.activityId != null && activityFilter.id == s.activityId);
        }
        else
        {
            returnList = returnList;
        }
        return returnList.sorted((s1,s2) -> (s1.scheduledTime.isAfter(s2.scheduledTime)) ? 1: -1).toList();
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
        stopEarlyButton.setDisable(false);

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
            finishActiveSession(activeSession.plannedMinutes);
            return;
        }

        long remaining = plannedSeconds - elapsedSeconds;
        activeLabel.setText(activeSession.name + " running - " + (remaining / 60) + "m " + (remaining % 60) + "s left");
    }

    private void stopEarly() {
        if (activeSession == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Stop this session early? You'll earn less XP than finishing it.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                long elapsedMinutes = java.time.Duration.between(activeStartTime, LocalDateTime.now()).toMinutes();
                activeTimer.stop();
                finishActiveSession((int) elapsedMinutes);
            }
        });
    }

    private void finishActiveSession(int completedMinutes) {
        Session finished = activeSession;
        store.completeSession(finished.id, completedMinutes);
        notifier.fireCompletion(finished);

        int xp = xpCalculator.computeXp(finished.baseXp, finished.plannedMinutes, completedMinutes);
        pendingClaimSession = finished;
        pendingXp = xp;
        claimLabel.setText("Claim " + xp + " XP for " + finished.name);
        claimButton.setDisable(false);

        activeSession = null;
        activeLabel.setText("");
        stopEarlyButton.setDisable(true);
        refreshSessionList();
    }

    private void claimPendingXp() {
        if (pendingClaimSession == null) return;
        store.claimXp(pendingClaimSession.id, pendingXp);
        pendingClaimSession = null;
        claimLabel.setText("");
        claimButton.setDisable(true);
        updateTotalXpLabel();
        refreshSessionList();
    }

    private void refreshSessionList() {
        List<Session> sessions = showingHistory
                ? historySessions(activityFilterBox.getValue(), miscOnlyCheck.isSelected())
                : upcomingSessions();
        sessionList.getItems().setAll(sessions);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /** A TextField that rejects any keystroke that wouldn't leave it as a valid whole number. */
    private static TextField wholeNumberField(int defaultValue) {
        TextField field = new TextField();
        field.setTextFormatter(new TextFormatter<>(
                new IntegerStringConverter(), defaultValue,
                change -> change.getControlNewText().matches("\\d*") ? change : null
        ));
        return field;
    }

    /** Same idea, but allows one decimal point (for a rate like XP-per-minute). */
    private static TextField decimalField(double defaultValue) {
        TextField field = new TextField();
        field.setTextFormatter(new TextFormatter<>(
                new DoubleStringConverter(), defaultValue,
                change -> change.getControlNewText().matches("\\d*\\.?\\d*") ? change : null
        ));
        return field;
    }

    private void toggleHistory() {
        showingHistory = !showingHistory;
        historyToggleButton.setText(showingHistory ? "Back to Upcoming" : "View History");

        addActivityButton.setDisable(showingHistory);
        createSessionButton.setDisable(showingHistory);
        setShown(stopEarlyButton, !showingHistory);
        setShown(claimButton, !showingHistory);
        setShown(claimLabel, !showingHistory);
        setShown(countdownLabel, !showingHistory);
        setShown(activeLabel, !showingHistory);
        setShown(filterRow, showingHistory);

        if (showingHistory) {
            activityFilterBox.getItems().setAll(store.listActivities());
            startButton.setDisable(true);
        }

        refreshSessionList();
        updateCountdown();
    }

    private void updateCountdown() {
        if (showingHistory) {
            return;
        }
        if (activeSession != null) {
            countdownLabel.setText("");
            startButton.setDisable(true);
            return;
        }

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
        TextField plannedMinutesField = wholeNumberField(25);
        TextField leadMinutesField = wholeNumberField(15);
        TextField xpPerMinuteField = decimalField(1.0);

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
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
                nameField.textProperty().isEmpty()
                        .or(plannedMinutesField.textProperty().isEmpty())
                        .or(leadMinutesField.textProperty().isEmpty())
                        .or(xpPerMinuteField.textProperty().isEmpty())
        );

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
        Callback<ListView<Integer>, ListCell<Integer>> twoDigitCellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%02d", item));
            }
        };

        ComboBox<Integer> hourBox = new ComboBox<>();
        for (int h = 0; h < 24; h++) hourBox.getItems().add(h);
        hourBox.setValue(defaultTime.getHour());
        hourBox.setCellFactory(twoDigitCellFactory);
        hourBox.setButtonCell(twoDigitCellFactory.call(null));

        ComboBox<Integer> minuteBox = new ComboBox<>();
        for (int m = 0; m < 60; m++) minuteBox.getItems().add(m);
        minuteBox.setValue((defaultTime.getMinute()));
        minuteBox.setCellFactory(twoDigitCellFactory);
        minuteBox.setButtonCell(twoDigitCellFactory.call(null));
        HBox timeBox = new HBox(5, datePicker, hourBox, new Label(":"), minuteBox);

        TextField plannedMinutesField = wholeNumberField(25);
        TextField leadMinutesField = wholeNumberField(15);
        TextField baseXpField = wholeNumberField(25);

        activityBox.setOnAction(e -> {
            Activity a = activityBox.getValue();
            if (a != null) {
                nameField.setText(a.name);
                descriptionField.setText(a.description);
                plannedMinutesField.setText(String.valueOf(a.defaultPlannedMinutes));
                leadMinutesField.setText(String.valueOf(a.defaultLeadMinutes));
                int recommendedXp = (int) Math.round(a.defaultXpPerMinute * a.defaultPlannedMinutes);
                baseXpField.setText(String.valueOf(recommendedXp));
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
        grid.addRow(6, new Label("XP (Whole Session)"), baseXpField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(
                datePicker.valueProperty().isNull()
                        .or(nameField.textProperty().isEmpty())
                        .or(plannedMinutesField.textProperty().isEmpty())
                        .or(leadMinutesField.textProperty().isEmpty())
                        .or(baseXpField.textProperty().isEmpty())
        );
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            LocalDateTime candidateStart = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourBox.getValue(), minuteBox.getValue())
            );
            int candidateMinutes = Integer.parseInt(plannedMinutesField.getText());
            if (overlapsExistingSession(candidateStart, candidateMinutes)) {
                new Alert(Alert.AlertType.ERROR,
                        "This overlaps an already-scheduled session. Pick a different time.")
                        .showAndWait();
                event.consume();
            }
        });

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
                        Integer.parseInt(leadMinutesField.getText()),
                        Integer.parseInt(baseXpField.getText())
                );
                refreshSessionList();
            }
            return null;
        });

        dialog.showAndWait();
    }
}
