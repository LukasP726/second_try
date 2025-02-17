package org.example.blackjack.controller;

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
import org.example.blackjack.client.BlackjackClient;

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

    @FXML
    private Button joinRoomButton;

    @FXML
    private Button createRoomButton;

    private BlackjackClient client;

    List<String> rooms = new ArrayList<>();

    private Stage stage;


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

    public void disableAll(){
        joinRoomButton.setDisable(true);
        refreshButton.setDisable(true);
        createRoomButton.setDisable(true);
        roomsListView.setDisable(true);
    }

    public void enableAll(){
        joinRoomButton.setDisable(false);
        refreshButton.setDisable(false);
        createRoomButton.setDisable(false);
        roomsListView.setDisable(false);
    }




    @FXML
    private void joinRoom() {


        // Předpokládáme, že vybraná místnost má formát "ID: <room_id>, Stav: <status>, Hráči: <players>"
        // Budeme extrahovat pouze ID (první část před čárkou)
        String roomId = getSelectedRoom();

        // Odeslání zprávy ve formátu "JOIN_ROOM|<room_id>"
        client.sendCommand("JOIN_ROOM|" + roomId);
    }
    public String getSelectedRoom(){
        String selectedRoom = roomsListView.getSelectionModel().getSelectedItem();

        // Kontrola, zda byla vybrána nějaká místnost
        if (selectedRoom == null) {
            statusLabel.setText("Prosím vyberte místnost.");
            return null;
        }
        return selectedRoom.split(",")[0].split(":")[1].trim();

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
                case"Trio":maxPlayers=3;break;
                case"Quatro":maxPlayers=4;break;
                default: maxPlayers=1; break;

            }
            String command = "CREATE_ROOM|" + maxPlayers; // Zde můžeš přizpůsobit formát příkazu
            // Odeslat příkaz na server (zde bys měl mít metodu pro odesílání příkazů)
            client.sendCommand(command);

            // Aktualizace statusu
            statusLabel.setText("Místnost byla vytvořena.");
            //refreshRooms(); // Obnovit seznam místností po vytvoření
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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // Metoda pro načtení herního okna
    public void loadBlackjackGame(Boolean inGame) {
        try {
            /*
            Screen screen = Screen.getPrimary();
            double screenWidth = screen.getBounds().getWidth();
            double screenHeight = screen.getBounds().getHeight();
*/
// Nastavení velikosti okna

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("blackjack-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            BlackjackController controller = fxmlLoader.getController();

            //client.setLobbyController(this);
            controller.setClient(client);
            client.setBlackjackController(controller);
            controller.setStage(stage); //NEW


            //Stage stage = (Stage) statusLabel.getScene().getWindow();
            if(stage!=null){
            stage.setTitle("Blackjack Game");
            stage.setScene(scene);

                //stage.setWidth(screenWidth);
                //stage.setHeight(screenHeight);
/*
                stage.setOnShown(windowEvent -> {
                    stage.setMaximized(true);
                    stage.setFullScreen(false); // Volitelné
                });
                */

            //stage.setMaximized(true); // Okno bude maximalizováno
            //stage.setFullScreen(false); // Fullscreen (volitelné, pokud nechcete okno s okraji)
            stage.show();}
            controller.disableButtons();
            if(inGame) client.sendCommand("IN_GAME");

            //stage.setMaximized(true); // Okno bude maximalizováno
            //stage.setFullScreen(false); // Fullscreen (volitelné, pokud nechcete okno s okraji)
        } catch (Exception e) {
            statusLabel.setText("Nebylo možné načíst herní okno.");
            e.printStackTrace();
        }
    }


    // Inicilizace
    public void initialize() {
        // Deaktivace tlačítka, pokud není vybraná místnost
        joinRoomButton.setDisable(true);

        // Přidáme listener na výběr v ListView
        roomsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Pokud je vybrána místnost, aktivujeme tlačítko
            joinRoomButton.setDisable(newValue == null);
        });
    }

    public void setStatusLabel(String text){
        updateUi(() -> {
            statusLabel.setText(text);
        });
    }



}
