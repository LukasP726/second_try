package org.example.blackjack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    ConnectController controller;
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("connect-view.fxml"));
        Scene connectScene = new Scene(fxmlLoader.load());
        stage.setTitle("Server Connection");

        // Získání controlleru a nastavení stage pro budoucí použití ConnectController
        controller = fxmlLoader.getController();
        controller.setStage(stage);


        stage.setScene(connectScene);
        //stage.setMaximized(true); // Okno bude maximalizováno
        //stage.setFullScreen(false); // Fullscreen (volitelné, pokud nechcete okno s okraji)
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Zavření spojení při ukončení aplikace
        if(controller != null && controller.getClient()!=null){
            controller.getClient().close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
