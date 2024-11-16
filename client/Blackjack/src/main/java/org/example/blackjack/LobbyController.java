package org.example.blackjack;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyController {
    @FXML
    private ComboBox<String> gameModeComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> roomsListView;
    @FXML
    private Button refreshButton;

    private BlackjackClient client;

    List<String> rooms = new ArrayList<>();


    public void updateUi(Runnable uiTask) {
        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            Platform.runLater(uiTask);
        }
    }

    public void setClient(BlackjackClient client) {
        this.client = client;
    }

    // Metoda pro vstup do herní místnosti
    @FXML
    private void joinRoom() {
        String selectedMode = gameModeComboBox.getValue();
        if (selectedMode == null) {
            statusLabel.setText("Prosím vyberte herní mód.");
            return;
        }
        client.sendCommand("JOIN_ROOM " + selectedMode);

    }


    @FXML
    private void createRoom() {
        String selectedGameMode = gameModeComboBox.getSelectionModel().getSelectedItem();
        if (selectedGameMode != null) {
            // Vytvoření příkazu pro server
            int maxPlayers;
            switch(selectedGameMode){
                case"Solo":maxPlayers=1;break;
                case"Duo":maxPlayers=2;break;
                case"Trie":maxPlayers=3;break;
                case"Quadro":maxPlayers=4;break;
                default: maxPlayers=1; break;

            }
            String command = "CREATE_ROOM|" + maxPlayers; // Zde můžeš přizpůsobit formát příkazu
            // Odeslat příkaz na server (zde bys měl mít metodu pro odesílání příkazů)
            client.sendCommand(command);

            // Aktualizace statusu
            statusLabel.setText("Místnost byla vytvořena.");
            refreshRooms(); // Obnovit seznam místností po vytvoření
        } else {
            statusLabel.setText("Vyberte herní mód před vytvořením místnosti.");
        }
    }

    // Metoda pro načtení seznamu místností
    @FXML
    private void refreshRooms() {
        client.sendCommand("REFRESH");

    }

    public void rooms( List<String> rooms){
        updateUi(() -> {
            ObservableList<String> r = FXCollections.observableArrayList(rooms);
            roomsListView.setItems(r);

        });

    }

    // Metoda pro načtení herního okna
    public void loadBlackjackGame() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("blackjack-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            BlackjackController controller = fxmlLoader.getController();
            client.setBlackjackController(controller);
            client.setLobbyController(this);
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
