package practiceloop;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        /*
         * In dev mode (run.bat), there's no "app.dir" property, so this falls
         * back to "." - the current directory, exactly like before. In a
         * packaged build, package-windows.bat/the Mac workflow pass
         * -Dapp.dir=$APPDIR, jpackage's own token for "wherever this app
         * actually lives" - needed because a double-clicked app's working
         * directory can't be trusted to be that location (confirmed: macOS
         * resets it to "/" for a Finder-launched app, unrelated to where the
         * .app bundle actually sits).
         */
        File appDir = new File(System.getProperty("app.dir", "."));

        SessionStore store = new SessionStore(new File(appDir, "practiceloop.db").getPath());
        NotificationScheduler notifier = new NotificationScheduler(store);
        MainView view = new MainView(store, notifier);
        stage.setTitle("Practice Loop");
        stage.setScene(view.createScene(appDir));
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
