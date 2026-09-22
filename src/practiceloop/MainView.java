package practiceloop;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainView {
    private final SessionStore store;
    private final ListView<Session> sessionList = new ListView<>();

    public MainView(SessionStore store) {
        this.store = store;
    }

    public Scene createScene() {
        Button addActivityButton = new Button("Add Activity");
        addActivityButton.setOnAction(e -> showAddActivityDialog());

        Button createSessionButton = new Button("Create Session");
        createSessionButton.setOnAction(e -> showCreateSessionDialog());

        HBox buttons = new HBox(10, addActivityButton, createSessionButton);
        buttons.setPadding(new Insets(10));

        VBox.setVgrow(sessionList, Priority.ALWAYS);
        VBox root = new VBox(10, buttons, sessionList);
        root.setPadding(new Insets(10));

        refreshSessionList();

        Scene scene = new Scene(root, 500, 400);
        return scene;
    }

    private void refreshSessionList() {
        List<Session> sessions = store.listSessions();
        sessionList.getItems().setAll(sessions);
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
        TextField scheduledTimeField = new TextField(LocalDateTime.now().plusMinutes(5)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
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
        grid.addRow(3, new Label("Scheduled time (yyyy-MM-ddTHH:mm)"), scheduledTimeField);
        grid.addRow(4, new Label("Planned minutes"), plannedMinutesField);
        grid.addRow(5, new Label("Lead minutes"), leadMinutesField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Activity selected = activityBox.getValue();
                Integer activityId = selected == null ? null : selected.id;
                LocalDateTime scheduledTime = LocalDateTime.parse(scheduledTimeField.getText());
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
