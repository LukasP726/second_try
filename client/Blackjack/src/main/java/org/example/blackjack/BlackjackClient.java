package org.example.blackjack;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BlackjackClient {
    private Socket socket;
    private String host;
    private String port;
    private BufferedReader in;
    private PrintWriter out;
    private ScheduledExecutorService pingScheduler; // Plánovač pro odesílání PING
    private ExecutorService responseListener;
    private boolean running = true;

    private String userName;

    //private BlockingQueue<CommandResponse> commandQueue = new LinkedBlockingQueue<>();
    //private BlockingQueue<CommandResponse> pingQueue = new LinkedBlockingQueue<>();
    private BlackjackController bc;
    private LobbyController lc;


    public BlackjackClient(String host, int port, String userName) throws IOException {
        this.host = host;
        this.port = String.valueOf(port);
        this.userName = userName; // Uložení uživatelského jména

        // Připojení k serveru
        /*
        socket = new Socket(host, port);
        System.out.println("Connected to server: " + host + ":" + port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Po připojení pošleme jméno na server
        sendCommand( "CONNECT|" + userName);
        */
        connectToServer();

        // Zahájíme poslouchání serveru
        startListeningToServer();
    }

    private void connectToServer() throws IOException {
        while (running) {
            try {
                System.out.println("Attempting to connect to server...");
                socket = new Socket(host, Integer.parseInt(port));
                System.out.println("Connected to server: " + host + ":" + port);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Po připojení pošleme serveru uživatelské jméno
                sendCommand("CONNECT|" + userName);
                break; // Připojení úspěšné, ukončíme smyčku
            } catch (IOException e) {
                System.out.println("Failed to connect to server. Retrying in 2 seconds...");
                try {
                    Thread.sleep(2000); // Čekání před dalším pokusem
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void tryReconnect() {
        try {
            connectToServer(); // Znovu navážeme připojení
            if(running)
                System.out.println("Reconnected successfully.");
        } catch (IOException e) {
            System.out.println("Reconnection failed. Retrying...");
            tryReconnect(); // Rekurzivně se pokusíme znovu připojit
        }
    }




    private void startListeningToServer() {
        new Thread(() -> {
            while (running) {
                try {
                    String message = getResponse();
                    if (message == null) {
                        throw new IOException("Server connection lost."); // Server ukončil spojení
                    }
                    if ("PING".equals(message)) {
                        sendPong();
                    }
                     else {
                        handleServerResponse(message);
                    }
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                    tryReconnect();
                    break;
                }
            }
        }).start();
    }


    public void sendCommand(String command) {
        System.out.println("Sending command to server: " + command);
        out.println(command);
    }

    public String getResponse() throws IOException {
        String response = in.readLine();
        System.out.println("getResponse: " + response);
        return response;
    }

    // Odpověď na PING
    private void sendPong() {
        sendCommand("PONG");
        System.out.println("PONG odeslán serveru.");
    }

    public void setBlackjackController(BlackjackController controller) {
        this.bc = controller;
    }

    public void setLobbyController(LobbyController controller) {
        this.lc= controller;
    }
    public LobbyController getLobbyController(){
        return this.lc;
    }

















    public void close() throws IOException {
        //stopPingingServer(); // Zastavení pingování před ukončením
        if (socket != null && !socket.isClosed()) {
            socket.close();
            running = false;
        }
    }

    public String getIp() {
        return this.host;
    }

    public String getPort() {
        return this.port;
    }

    // Nová metoda pro připojení do místnosti




    public void waitForOpponent() {
        final int maxAttempts = 20; // Maximální počet pokusů

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            sendCommand("WAIT_FOR_OPPONENT");

        }

    }

    public void handleServerResponse(String response)throws IOException{
        if (response.startsWith("PLAYER_CARD")) {

            String[] parts = response.split("\\|");
            String card = parts[1];
            bc.addToPlayersHand(card, parts[2]);
            String flag = parts[3];
            if(flag.equals("PLAYER_BUST")){
                //sendCommand("STAND");
                bc.stand();
            }


        }/*
        else if(response.startsWith("STAND_RECEIVED")){
            bc.disableButtons();
        }*/
        else if (response.startsWith("DEALER_CARDS")) {
            // Rozdělení odpovědi podle '|'
            String[] parts = response.split("\\|");

            // Zpracování karet dealera
            int dealerCardsIndex = 1; // První index pro karty dealera
            while (dealerCardsIndex < parts.length && !parts[dealerCardsIndex].equals("DEALER_SCORE")) {
                String dealerCard = parts[dealerCardsIndex].trim();
                bc.addToDealersHand(dealerCard); // Přidání karty do dealerovy ruky
                dealerCardsIndex++;
            }

            // Zpracování skóre dealera
            String dealerScore = parts[dealerCardsIndex + 1].trim(); // Očekáváme, že skóre je následující část po "DEALER_SCORE"
            bc.updateDealerScore(dealerScore); // Aktualizace skóre dealera

            // Zpracování karet hráčů a jejich skóre
            int playerIndex = dealerCardsIndex + 2; // Začínáme od hráčů po skóre dealera
            int playerCount = 2; // Počítadlo pro hráče (od Player 2 do Player 4)

            while (playerIndex < parts.length) {
                if (parts[playerIndex].equals("PLAYER_CARDS")) {
                    // Karty hráče (například Player 2, Player 3, ...)
                    playerIndex++; // Přesuneme se na karty
                    while (playerIndex < parts.length && !parts[playerIndex].equals("PLAYER_SCORE")) {
                        String playerCard = parts[playerIndex].trim();

                        // Přidání karty podle hráče (používáme playerCount pro rozlišení hráčů)
                        switch (playerCount) {
                            case 2:
                                bc.addCardToHand(bc.getPlayer2Cards(), playerCard);
                                break;
                            case 3:
                                bc.addCardToHand(bc.getPlayer3Cards(), playerCard);
                                break;
                            case 4:
                                bc.addCardToHand(bc.getPlayer4Cards(), playerCard);
                                break;
                            default:
                                // Pokud máme více než 4 hráče, můžeme přidat další logiku
                                break;
                        }
                        playerIndex++;
                    }

                    // Skóre hráče
                    if (playerIndex < parts.length && parts[playerIndex].equals("PLAYER_SCORE")) {
                        String playerScore = parts[playerIndex + 1].trim(); // Skóre hráče
                        // Aktualizace skóre pro příslušného hráče
                        switch (playerCount) {
                            case 2:
                                bc.updatePlayerScore(bc.getPlayer2Score(), playerScore);
                                break;
                            case 3:
                                bc.updatePlayerScore(bc.getPlayer3Score(), playerScore);
                                break;
                            case 4:
                                bc.updatePlayerScore(bc.getPlayer4Score(), playerScore);
                                break;
                            default:
                                // Pokud máme více než 4 hráče, můžeme přidat další logiku
                                break;
                        }
                        playerIndex += 2; // Přeskočíme "PLAYER_SCORE" a samotné skóre
                        playerCount++; // Přechod na dalšího hráče
                    }
                } else {
                    playerIndex++;
                }
            }

            // Zpracování vítěze
            //if (playerIndex < parts.length && parts[playerIndex].equals("WINNER")) {
                String winner = parts[parts.length-1].trim(); // Vítěz (DEALER nebo PLAYER1)
                bc.setLabelText("Winner: " + winner); // Nastavení vítěze
            //}

            // Konec hry
            bc.endGame(); // Ukončení hry
        }

        else if (response.startsWith("RESULT")) {
           bc.displayResult(response.split(" ")[1]);


        }

        else if (response.startsWith("ROOMS")) {
            List<String> rooms = new ArrayList<>(); //TODO: ZAJISTIT PŘEDÁVÁNÍ POKOJŮ
            String[] roomData = response.split("\\|");
            for (int i = 1; i < roomData.length; i += 3) {
                String roomId = roomData[i];
                String roomState = roomData[i + 1];
                String numOfPlayers = roomData[i + 2];
                rooms.add("ID: " + roomId + ", Stav: " + roomState + ", Hráči: " + numOfPlayers);
            }
            lc.rooms(rooms);
        }

        else if (response.equals("READY_TO_START")) {
            lc.loadBlackjackGame();
            //return true; // Hra může začít
        }
        else if(response.equals("GAME_START")){
            //lc.loadBlackjackGame();
            Platform.runLater(() -> lc.loadBlackjackGame());
            /*
            new Thread(() -> {
                try {
                    //Platform.runLater(lc::loadBlackjackGame);
                    Platform.runLater(() -> lc.loadBlackjackGame());
                } catch (Exception e) {
                    //Platform.runLater(() -> statusLabel.setText("Nebylo možné načíst herní okno."));
                    e.printStackTrace();
                }
            }).start();
            */

        }

        else if (response.equals("WAITING_FOR_OPPONENT")) {
            final int delayMillis = 5000;
            System.out.println("Čekání na dalšího hráče...");
            lc.disableAll();
            try {
                Thread.sleep(delayMillis); // Čekání před dalším pokusem
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Čekání na hráče bylo přerušeno.", e);
            }
        }






        else{
            System.out.println("zase to nefunguje");
        }
    }









}
