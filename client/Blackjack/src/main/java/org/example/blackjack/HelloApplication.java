package org.example.blackjack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("connect-view.fxml"));
        Scene connectScene = new Scene(fxmlLoader.load());
        stage.setTitle("Server Connection");

        // Získání controlleru a nastavení stage pro budoucí použití
        ConnectController controller = fxmlLoader.getController();
        controller.setStage(stage);

        stage.setScene(connectScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
