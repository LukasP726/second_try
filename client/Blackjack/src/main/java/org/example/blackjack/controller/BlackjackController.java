package org.example.blackjack.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.blackjack.client.BlackjackClient;
import org.example.blackjack.model.Dealer;
import org.example.blackjack.model.Deck;
import org.example.blackjack.model.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlackjackController {
    private BlackjackClient client; // Instance klienta pro komunikaci se serverem
    private Deck deck;
    private Player player;
    private Dealer dealer;

    @FXML
    private Label player1Text;
    @FXML
    private Label player2Text;
    @FXML
    private Label player3Text;
    @FXML
    private Label player4Text;

    @FXML
    private HBox player1Cards;
    @FXML
    private Label player1Score;

    @FXML
    private HBox player2Cards;
    @FXML
    private Label player2Score;



    @FXML
    private HBox player3Cards;
    @FXML
    private Label player3Score;

    @FXML
    private HBox player4Cards;
    @FXML
    private Label player4Score;

    @FXML
    private HBox dealerCards;
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
    //@FXML
    //private Button btnNewGame;
    @FXML private VBox player3Section;
    @FXML private VBox player4Section;
    private String ID;

    private List<Label> playerLabels ;

    private Stage stage;


    public void setStage(Stage stage) {
        this.stage = stage;
    }

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

    public void setPlayer1Text(String text) {
        updateUi(() -> {
            player1Text.setText(text);
        });
    }

    public void setPlayer2Text(String text) {
        updateUi(() -> {
            player2Text.setText(text);
        });
    }

    public void setPlayer3Text(String text) {
        updateUi(() -> {
            player3Text.setText(text);
        });
    }

    public void setPlayer4Text(String text) {
        updateUi(() -> {
            player4Text.setText(text);
        });
    }

    @FXML
    public void initialize() {
        // Naplníme seznam labely
        playerLabels = new ArrayList<>();
        playerLabels.add(player1Text);
        playerLabels.add(player2Text);
        playerLabels.add(player3Text);
        playerLabels.add(player4Text);
    }

    public void setPlayerText(int playerIndex, String text) {
        if (playerIndex >= 0 && playerIndex < playerLabels.size()) {
            updateUi(() -> {
                playerLabels.get(playerIndex).setText(text);
            });
        } else {
            System.err.println("Invalid player index: " + playerIndex);
        }
    }



    public void setupPlayers(int numberOfPlayers) {
        player3Section.setVisible(numberOfPlayers >= 3);
        player4Section.setVisible(numberOfPlayers >= 4);
    }

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
        //client.sendCommand("SHOT");
        disableButtons();
    }

    // Akce při stisknutí tlačítka "Stand"
    public void stand() {
        client.sendCommand("STAND"); // Odeslání příkazu "STAND" pomocí klienta
        btnHit.setDisable(true);  // Zakázat tlačítko "Hit"
        btnStand.setDisable(true); // Zakázat tlačítko "Stand"
    }

    public void disableButtons(){
        btnHit.setDisable(true);  // Zakázat tlačítko "Hit"
        btnStand.setDisable(true); // Zakázat tlačítko "Stand"
    }

    public void enableButtons(){
        btnHit.setDisable(false);  // Povolit tlačítko "Hit"
        btnStand.setDisable(false); // Povolit tlačítko "Stand"
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
        //btnNewGame.setDisable(true);
        btnBackToLobby.setDisable(true);
        player1Cards.getChildren().clear();
        dealerCards.getChildren().clear();
        messageLabel.setText("New game started. Your turn!");


        player1Score.setText("Player Score: 0");
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
    public void addCardToHand(HBox hand, String card) {
        ImageView cardView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/cards/" + card + ".png"))));
        cardView.setFitWidth(100); // Nastavení velikosti karet
        cardView.setFitHeight(125);
        //hand.getChildren().add(cardView); // Přidání obrázku karty do GUI
        updateUi(() -> {
            hand.getChildren().add(cardView);

        });
    }

    public void addToPlayersHand(String card, String score){
        addCardToHand(player1Cards, card); // Přidání karty do hráčovy ruky
        updatePlayerScore(player1Score, score);
    }

    public void addToPlayers2Hand(String card, String score){
        addCardToHand(player2Cards, card); // Přidání karty do hráčovy ruky
        updatePlayerScore(player2Score, score);
    }



    public void addToDealersHand(String card){

        addCardToHand(dealerCards, card); // Přidání karty do hráčovy ruky
    }
    public void addToDealersHand(String card, String score){
        addCardToHand(dealerCards, card); // Přidání karty do hráčovy ruky
        updateDealerScore(score);
    }
    @FXML
    public void backToLobby() {
        try {
            // Načtení FXML pro lobby scénu
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("lobby-view.fxml"));
            ///FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/blackjack/lobby-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            client.sendCommand("LEAVE_ROOM");
            // Získání aktuálního okna (Stage) a nastavení nové scény
            //Stage stage = (Stage) btnBackToLobby.getScene().getWindow();

            //client.sendCommand("LEAVE_ROOM|"+lobbyController.getSelectedRoom());
            LobbyController lobbyController = fxmlLoader.getController();
            //LobbyController lobbyController = client.getLobbyController();
            lobbyController.setStage(stage);
            lobbyController.setClient(client); // Předání klienta
            client.setLobbyController(lobbyController);
            stage.setTitle("Lobby");
            stage.setScene(scene);
            //stage.setMaximized(true); // Okno bude maximalizováno
            //stage.setFullScreen(false); // Fullscreen (volitelné, pokud nechcete okno s okraji)
            stage.show();
            client.sendCommand("REFRESH");


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
    public void updatePlayerScore(Label label, String score) {
        updateScoreText(label, "Player Score: " + score);
        //playerScore.setText("Player Score: " + score);
    }


    // Aktualizace skóre dealera
    public void updateDealerScore(String score) {
        updateScoreText(dealerScore, "Dealer Score: " + score);
        //dealerScore.setText("Dealer Score: " + score);
    }





    public void endGame() {
        updateUi(() -> {
            btnHit.setDisable(true);  // Zakáže tlačítko "Hit"
            btnStand.setDisable(true); // Zakáže tlačítko "Stand"
            //btnNewGame.setDisable(false);
            btnBackToLobby.setDisable(false);
        });

    }




    public void setClient(BlackjackClient client) {
        this.client = client;
        newGame();
        //messageLabel.setText("Connected to server: " + client.getIp() + ":" + client.getPort());

    }
    public Label getPlayer1Score() {
        return player1Score;
    }

    public Label getPlayer2Score() {
        return player2Score;
    }

    public Label getPlayer3Score() {
        return player3Score;
    }

    public Label getPlayer4Score() {
        return player4Score;
    }

    public HBox getPlayer1Cards() { return player1Cards; }

    public HBox getPlayer2Cards() { return player2Cards; }

    public HBox getPlayer3Cards() {
        return player3Cards;
    }

    public HBox getPlayer4Cards() {
        return player4Cards;
    }
}