package org.example.blackjack;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.util.regex.Pattern;

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


    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private boolean isValidPort(String value) {
        try {
            int port = Integer.parseInt(value);
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    public void initialize() {
        // Validace IP adresy
        ipField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!IPV4_PATTERN.matcher(newValue).matches() && !newValue.isEmpty()) {
                ipField.setStyle("-fx-border-color: red;");
                statusLabel.setText("Invalid IP address");
            } else {
                ipField.setStyle(null);
                statusLabel.setText("");
            }
        });

        // Validace portu
        portField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*") || !newValue.isEmpty() && !isValidPort(newValue)) {
                portField.setStyle("-fx-border-color: red;");
                statusLabel.setText("Invalid port");
            } else {
                portField.setStyle(null);
                statusLabel.setText("");
            }
        });
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
        String alert ="-fx-border-color: red;";
        if (ipField.getStyle() == alert || portField.getStyle() == alert) {
            statusLabel.setText("Fix invalid inputs before connecting.");
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
        //Screen screen = Screen.getPrimary();
        //double screenWidth = screen.getBounds().getWidth();
        //double screenHeight = screen.getBounds().getHeight();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("lobby-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            LobbyController lobbyController = fxmlLoader.getController();
            lobbyController.setClient(client); // Předání klienta
            lobbyController.setStage(stage);
            client.setLobbyController(lobbyController);
            stage.setTitle("Lobby");
            stage.setScene(scene);
            //stage.setWidth(screenWidth);
            //stage.setHeight(screenHeight);
            //stage.setMaximized(true); // Okno bude maximalizováno
            //stage.setFullScreen(false); // Fullscreen (volitelné, pokud nechcete okno s okraji)
            stage.show();
            client.sendCommand("REFRESH");
        } catch (Exception e) {
            statusLabel.setText("Failed to load lobby window.");
            e.printStackTrace();
        }
    }




}

