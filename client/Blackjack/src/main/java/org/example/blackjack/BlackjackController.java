package org.example.blackjack;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class BlackjackController {
    private BlackjackClient client; // Instance klienta pro komunikaci se serverem
    private Deck deck;
    private Player player;
    private Dealer dealer;

    @FXML
    private HBox playerCards;
    @FXML
    private HBox dealerCards;
    @FXML
    private Label playerScore;
    @FXML
    private Label dealerScore;
    @FXML
    private Label messageLabel;
    @FXML
    private Button btnHit;
    @FXML
    private Button btnStand;
    @FXML
    private Button btnBackToLobby;
    @FXML
    private Button btnNewGame;

    private String ID;
/*
    public void initialize() {
        try {
            connectToServer(); // Připojení k serveru při spuštění GUI
            newGame(); // Spuštění nové hry při inicializaci GUI
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to connect to server.");
        }
    }
*/
    // Metoda pro připojení k serveru pomocí BlackjackClient

    /*
    public void connectToServer() throws IOException {
        client = new BlackjackClient("localhost", 8080); // Vytvoření klienta pro komunikaci se serverem
        messageLabel.setText("Connected to server.");
    }
    */

    public void updateUi(Runnable uiTask) {
        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            Platform.runLater(uiTask);
        }
    }

    // Akce při stisknutí tlačítka "Hit"
    public void hit() {
        client.sendCommand("HIT"); // Odeslání příkazu "HIT" pomocí klienta
    }

    // Akce při stisknutí tlačítka "Stand"
    public void stand() {
        client.sendCommand("STAND"); // Odeslání příkazu "STAND" pomocí klienta
    }

    // Akce pro spuštění nové hry
    public void newGame() {
        //client.sendCommand("NEW_GAME"); // Odeslání příkazu "NEW_GAME" pomocí klienta
        deck = new Deck();
        //client.sendCommand("NEW_GAME");
        /*
        player = new Player();
        dealer = new Dealer();
         */
        btnHit.setDisable(false);  // Povolit tlačítko "Hit"
        btnStand.setDisable(false); // Povolit tlačítko "Stand"
        btnNewGame.setDisable(true);
        btnBackToLobby.setDisable(true);
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
        messageLabel.setText("New game started. Your turn!");


        playerScore.setText("Player Score: 0");
        dealerScore.setText("Dealer Score: 0");

        //hit(); // Hráč dostane první kartu (může být upraven
    }


    // Zpracování odpovědi od serveru


    public void displayResult(String result){
        updateUi(() -> {
            messageLabel.setText(result);    // Zobrazení výsledku hry
            //btnHit.setDisable(true);        // Zakázání tlačítek po ukončení hry
            //btnStand.setDisable(true);

        });

    }

    public void setLabelText(String text){
        updateUi(() -> {
            messageLabel.setText(text);

        });

    }





    // Přidání karty do GUI ruky
    private void addCardToHand(HBox hand, String card) {
        ImageView cardView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/cards/" + card + ".png"))));
        cardView.setFitWidth(100); // Nastavení velikosti karet
        cardView.setFitHeight(125);
        //hand.getChildren().add(cardView); // Přidání obrázku karty do GUI
        updateUi(() -> {
            hand.getChildren().add(cardView);

        });
    }

    public void addToPlayersHand(String card, String score){
        addCardToHand(playerCards, card); // Přidání karty do hráčovy ruky
        updatePlayerScore(score);
    }

    public void addToDealersHand(String card){
        addCardToHand(dealerCards, card); // Přidání karty do hráčovy ruky
    }

    @FXML
    private void backToLobby() {
        try {
            // Načtení FXML pro lobby scénu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("lobby-view.fxml"));
            ///FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/blackjack/lobby-view.fxml"));
            Scene lobbyScene = new Scene(loader.load());
            client.sendCommand("LEAVE_ROOM|"+client.getLobbyController().getSelectedRoom());
            // Získání aktuálního okna (Stage) a nastavení nové scény
            Stage stage = (Stage) btnBackToLobby.getScene().getWindow();
            //client.sendCommand("LEAVE_ROOM|"+lobbyController.getSelectedRoom());
            LobbyController lobbyController = loader.getController();
            lobbyController.setClient(client); // Předání klienta
            client.setLobbyController(lobbyController);

            stage.setScene(lobbyScene);
            stage.show();


        } catch (IOException e) {
            System.out.println("Chyba při načítání lobby scény: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void updateScoreText(Label label,String string){
        updateUi(() -> {
            label.setText(string);
        });

    }
    // Aktualizace skóre hráče
    private void updatePlayerScore(String score) {
        updateScoreText(playerScore, "Player Score: " + score);
        //playerScore.setText("Player Score: " + score);
    }

    // Aktualizace skóre dealera
    public void updateDealerScore(String score) {
        updateScoreText(dealerScore, "Dealer Score: " + score);
        //dealerScore.setText("Dealer Score: " + score);
    }

    // Uzavření připojení při ukončení aplikace
    public void closeConnection() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void endGame() {
        updateUi(() -> {
            btnHit.setDisable(true);  // Zakáže tlačítko "Hit"
            btnStand.setDisable(true); // Zakáže tlačítko "Stand"
            btnNewGame.setDisable(false);
            btnBackToLobby.setDisable(false);
        });

    }


    public void setClient(BlackjackClient client) {
        this.client = client;
        newGame();
        //messageLabel.setText("Connected to server: " + client.getIp() + ":" + client.getPort());

    }



}