package org.example.blackjack;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class LobbyController {
    @FXML
    private ComboBox<String> gameModeComboBox;
    @FXML
    private Label statusLabel;

    private BlackjackClient client;

    public void setClient(BlackjackClient client) {
        this.client = client;
    }

    @FXML
    private void joinRoom() {
        String selectedMode = gameModeComboBox.getValue();
        if (selectedMode == null) {
            statusLabel.setText("Prosím vyberte herní mód.");
            return;
        }

        try {
            if (client.joinRoom(selectedMode)) {
                if (selectedMode.equals("Solo")) {
                    loadBlackjackGame();
                    //startSinglePlayerGame(); // Spuštění hry pro jednoho hráče
                } else {
                    //multiplayer
                    //TODO: vytvoření nového zobrazení hry pro více hráčů
                    //loadBlackjackGame();
                }
            } else {
                statusLabel.setText("Čekání na další hráče...");
                waitForOpponent(); // Čeká na připojení dalšího hráče
            }
        } catch (IOException e) {
            statusLabel.setText("Chyba při připojování do místnosti.");
            e.printStackTrace();
        }
    }

    private void waitForOpponent() {
        new Thread(() -> {
            try {
                if (client.waitForOpponent()) {
                    loadBlackjackGame(); // Spuštění hry po připojení druhého hráče
                } else {
                    statusLabel.setText("Nebylo možné připojit dalšího hráče.");
                }
            } catch (IOException e) {
                statusLabel.setText("Chyba při čekání na hráče.");
                e.printStackTrace();
            }
        }).start();
    }

    private void loadBlackjackGame() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("blackjack-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            BlackjackController controller = fxmlLoader.getController();
            controller.setClient(client);

            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setTitle("Blackjack Game");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            statusLabel.setText("Nebylo možné načíst herní okno.");
            e.printStackTrace();
        }
    }
}
