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
    private TextField nameField;
    @FXML
    private Button connectButton;
    @FXML
    private Label statusLabel;


    private BlackjackClient client;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void connectToServer() {
        String ip = ipField.getText();
        String portText = portField.getText();
        String userName = nameField.getText(); // Získání jména uživatele

        if (ip.isEmpty() || portText.isEmpty() || userName.isEmpty()) {
            statusLabel.setText("IP, Port, and Name cannot be empty.");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            // Vytvoříme nový klient
            BlackjackClient client = new BlackjackClient(ip, port, userName); // Předáváme jméno uživatele
            this.client =client;
            // Načteme hlavní herní okno nebo lobby
            loadLobby(client);

        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid port number.");
        } catch (Exception e) {
            statusLabel.setText("Failed to connect to server.");
        }
    }


    public BlackjackClient getClient() {
        return this.client;
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

