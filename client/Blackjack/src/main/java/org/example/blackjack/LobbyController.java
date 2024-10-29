package org.example.blackjack;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

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

        // Logika pro vstup do místnosti nebo čekání na další hráče
        // Tady byste mohli poslat příkaz na server, aby zjistil, zda je místnost dostupná.
        // Pro účely tohoto příkladu předpokládejme, že místnost je dostupná.

        if (isRoomAvailable(selectedMode)) {
            // Načtěte hlavní herní okno
            loadBlackjackGame();
        } else {
            // Zobrazit čekací okno
            statusLabel.setText("Čekání na další hráče...");
            // Zde můžete přidat logiku pro čekání
        }
    }

    private boolean isRoomAvailable(String mode) {
        // Implementujte logiku pro kontrolu dostupnosti místnosti
        // Například můžete zkontrolovat počet hráčů v místnosti
        return true; // Pro testování předpokládáme, že místnost je vždy dostupná
    }

    private void loadBlackjackGame() {
        // Načtení hlavního herního okna
        // Použijte metodu loadBlackjackGame jako dříve
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("blackjack-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            BlackjackController controller = fxmlLoader.getController();
            controller.setClient(client); // Předání klienta

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
