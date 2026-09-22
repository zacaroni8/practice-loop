package practiceloop;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        SessionStore store = new SessionStore("practiceloop.db");
        MainView view = new MainView(store);
        stage.setTitle("Practice Loop");
        stage.setScene(view.createScene());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
