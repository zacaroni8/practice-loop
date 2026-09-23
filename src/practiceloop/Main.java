package practiceloop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        SessionStore store = new SessionStore("practiceloop.db");
        NotificationScheduler notifier = new NotificationScheduler(store);
        MainView view = new MainView(store);
        stage.setTitle("Practice Loop");
        stage.setScene(view.createScene());
        stage.show();

        Timeline notificationTicker = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> notifier.checkAndFireNotifications())
        );
        notificationTicker.setCycleCount(Timeline.INDEFINITE);
        notificationTicker.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
