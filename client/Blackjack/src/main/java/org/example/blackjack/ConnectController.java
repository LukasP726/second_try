package org.example.blackjack;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConnectController {
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void connectToServer() {
        String ip = ipField.getText();
        String portText = portField.getText();
        if (ip.isEmpty() || portText.isEmpty()) {
            statusLabel.setText("IP and Port cannot be empty.");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            // Pokus o vytvoření klienta pro testování připojení
            BlackjackClient client = new BlackjackClient(ip, port);

            // Při úspěchu načteme hlavní herní okno
            //loadBlackjackGame(client);
            loadLobby(client);

        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number.");
        } catch (Exception e) {
            statusLabel.setText("Failed to connect to server.");
        }
    }
/*
    private void loadBlackjackGame(BlackjackClient client) {
        try {
            // Načtení hlavního herního okna
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("blackjack-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            BlackjackController controller = fxmlLoader.getController();
            controller.setClient(client); // Předání klienta

            stage.setTitle("Blackjack Game");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Failed to load game window.");
            e.printStackTrace();
        }
    }*/

    private void loadLobby(BlackjackClient client) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("lobby-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            LobbyController lobbyController = fxmlLoader.getController();
            lobbyController.setClient(client); // Předání klienta
            client.setLobbyController(lobbyController);

            stage.setTitle("Lobby");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Failed to load lobby window.");
            e.printStackTrace();
        }
    }




}

