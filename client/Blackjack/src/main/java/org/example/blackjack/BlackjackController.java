package org.example.blackjack;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
    public void connectToServer() throws IOException {
        client = new BlackjackClient("localhost", 8080); // Vytvoření klienta pro komunikaci se serverem
        messageLabel.setText("Connected to server.");
    }

    // Akce při stisknutí tlačítka "Hit"
    public void hit() {
        client.sendCommand("HIT"); // Odeslání příkazu "HIT" pomocí klienta
        try {
            String response = client.getResponse(); // Získání odpovědi od serveru
            handleServerResponse(response); // Zpracování odpovědi
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Akce při stisknutí tlačítka "Stand"
    public void stand() {
        client.sendCommand("STAND"); // Odeslání příkazu "STAND" pomocí klienta
        try {
            String response = client.getResponse(); // Získání odpovědi od serveru
            handleServerResponse(response); // Zpracování odpovědi
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        playerCards.getChildren().clear();
        dealerCards.getChildren().clear();
        messageLabel.setText("New game started. Your turn!");


        playerScore.setText("Player Score: 0");
        dealerScore.setText("Dealer Score: 0");

        //hit(); // Hráč dostane první kartu (může být upraven
    }


    // Zpracování odpovědi od serveru
    private void handleServerResponse(String response) {
        if (response.startsWith("PLAYER_CARD")) {

            String[] parts = response.split(" ");
            String card = parts[1];
            addCardToHand(playerCards, card); // Přidání karty do hráčovy ruky
            updatePlayerScore(parts[2]); // Aktualizace skóre hráče

            /*
            Card card = deck.dealCard();
            player.addCard(card);
            addCardToView(card, playerCards);

            if (player.isBusted()) {
                messageLabel.setText("Player busts! Dealer wins.");
                endGame();
            }
            playerScore.setText("Player Score: " + player.getScore());

             */
        } else if (response.startsWith("DEALER_CARD")) {
            String[] parts = response.split("\\|");
            String cardPart = parts[0].trim(); // Seznam karet dealera
            String scorePart = parts[1].trim(); // Finální skóre
            String winnerPart = parts[2].trim();
            String[] dealerCardsArray = cardPart.replace("DEALER_CARDS", "").trim().split(" ");
            for (String card : dealerCardsArray) {
                addCardToHand(dealerCards, card); // Přidání karty do dealerovy ruky
            }

            // Zpracování skóre
            String finalScore = scorePart.replace("FINAL_SCORE", "").trim();

            updateDealerScore(finalScore); // Aktualizace skóre dealera
            messageLabel.setText(winnerPart);


            endGame();



            /*
            while (dealer.shouldHit()) {
                Card card = deck.dealCard();
                dealer.addCard(card);
                addCardToView(card, dealerCards);
            }

            if (dealer.isBusted() || player.getScore() > dealer.getScore()) {
                messageLabel.setText("Player wins!");
                //dealerScore.setText("Dealer Score: " + dealer.getScore());
            } else if (dealer.getScore() == player.getScore()) {
                messageLabel.setText("It's a tie!");
            } else {
                messageLabel.setText("Dealer wins!");
            }
            dealerScore.setText("Dealer Score: " + dealer.getScore());
            endGame();


             */

        } else if (response.startsWith("RESULT")) {
            messageLabel.setText(response.split(" ")[1]); // Zobrazení výsledku hry
            btnHit.setDisable(true); // Zakázání tlačítek po ukončení hry
            btnStand.setDisable(true);
        }else{
            System.out.print("zase to nefunguje");
        }


    }

    // Přidání karty do GUI ruky
    private void addCardToHand(HBox hand, String card) {
        ImageView cardView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/cards/" + card + ".png"))));
        cardView.setFitWidth(100); // Nastavení velikosti karet
        cardView.setFitHeight(125);
        hand.getChildren().add(cardView); // Přidání obrázku karty do GUI
    }

    private void addCardToView(Card card, HBox container) {


        if(card !=null) {

            ImageView cardImage = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/cards/" + card.getRank() + "_of_" + card.getSuit() + ".png"))));
            cardImage.setFitHeight(125);
            cardImage.setFitWidth(100);
            container.getChildren().add(cardImage);


        }


    }

    // Aktualizace skóre hráče
    private void updatePlayerScore(String score) {
        playerScore.setText("Player Score: " + score);
    }

    // Aktualizace skóre dealera
    private void updateDealerScore(String score) {
        dealerScore.setText("Dealer Score: " + score);
    }

    // Uzavření připojení při ukončení aplikace
    public void closeConnection() {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void endGame() {
        btnHit.setDisable(true);  // Zakáže tlačítko "Hit"
        btnStand.setDisable(true); // Zakáže tlačítko "Stand"
    }


    public void setClient(BlackjackClient client) {
        this.client = client;
        newGame();
        //messageLabel.setText("Connected to server: " + client.getIp() + ":" + client.getPort());

    }



}