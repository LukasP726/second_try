package org.example.blackjack;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    private ScheduledExecutorService scheduler;
    private final long TIMEOUT_PERIOD = 20; // 60 sekund
    private final int MAX_RECONNECT_ATTEMPTS = 10;
    private final long RECONNECT_DELAY = 60000;


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
        //TODO:try-catch
        connectToServer();

        // Zahájíme poslouchání serveru
        //startListeningToServer();
        //startPingTimeoutTimer(); // Spuštění detekce nečinnosti
    }


    private synchronized void startPingTimeoutTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newScheduledThreadPool(1); // Nový časovač
        scheduler.scheduleAtFixedRate(() -> {
            String message = "No response from server for " + TIMEOUT_PERIOD + " seconds. Reconnecting...";
            if(bc != null)
                bc.setLabelText(message);
            if(lc != null)
                lc.setStatusLabel(message);
            System.out.println(message);
            tryReconnect();
        }, TIMEOUT_PERIOD, TIMEOUT_PERIOD, TimeUnit.SECONDS);
    }



    private void connectToServer() throws IOException {
        //while (running) {
            //try {
                System.out.println("Attempting to connect to server..."+host+":"+port);
                //System.out.println("Attempting to connect to server...");
                socket = new Socket(host, Integer.parseInt(port));
                System.out.println("Connected to server: " + host + ":" + port);
                System.out.println("socket: "+socket);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Po připojení pošleme serveru uživatelské jméno
                sendCommand("CONNECT|" + userName);
                startListeningToServer();
                startPingTimeoutTimer(); // Restartujeme timeout timer
                //break; // Připojení úspěšné, ukončíme smyčku
                /*
            } catch (IOException e) {
                System.out.println("Failed to connect to server. Retrying in 2 seconds...");
                try {
                    Thread.sleep(2000); // Čekání před dalším pokusem
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                }
                 */

        }
    //}

    private void tryReconnect() {
        int attempts = 0;
        boolean connected = false;

        while (!connected && attempts < MAX_RECONNECT_ATTEMPTS) { // Určité maximum pokusů
            try {
                connectToServer(); // Pokus o připojení
                connected = true;   // Pokud je připojení úspěšné, nastavíme connected na true
                System.out.println("Reconnected successfully.");
            } catch (IOException e) {
                attempts++;  // Zvyšujeme počet pokusů
                System.out.println("Reconnection failed. Attempt " + attempts + " of " + MAX_RECONNECT_ATTEMPTS);

                if (attempts < MAX_RECONNECT_ATTEMPTS) {
                    try {
                        Thread.sleep(RECONNECT_DELAY); // Počkej před dalším pokusem (např. 2 sekundy)
                    } catch (InterruptedException ie) {
                        System.out.println("Reconnect attempt interrupted.");
                        break;
                    }
                }
            }
        }

        if (!connected) {
            System.out.println("Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts.");
            // Můžete přidat logiku pro ukončení aplikace nebo jiný způsob, jak reagovat na neúspěšné připojení.
        }
    }


    private synchronized void resetPingTimeoutTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        startPingTimeoutTimer(); // Restartování časovače
    }





    private void startListeningToServer() {
        new Thread(() -> {
            while (running) {
                try {
                    String message = getResponse();
                   // System.out.println("message: "+message);
                    if (message == null) {
                        throw new IOException("Server connection lost."); // Server ukončil spojení
                    }
                    // Resetujeme timer při každé zprávě od serveru
                    resetPingTimeoutTimer();
                    //startPingTimeoutTimer();
                    if ("PING".equals(message)) {
                        sendPong();
                    }
                     else {
                        handleServerResponse(message);
                    }
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                    //tryReconnect();
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

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public String getIp() {
        return this.host;
    }

    public String getPort() {
        return this.port;
    }

    // Nová metoda pro připojení do místnosti

    private void waitForBlackjackControllerLoad(boolean inGame) {
        CountDownLatch latch = new CountDownLatch(1); // Vytvoření latch pro synchronizaci

        // Spuštění metody loadBlackjackGame ve JavaFX vlákni
        Platform.runLater(() -> {
            try {
                lc.loadBlackjackGame(inGame); // Volání metody
            } finally {
                latch.countDown(); // Signalizace dokončení metody
            }
        });

        // Čekání na dokončení metody
        try {
            latch.await(); // Blokuje aktuální vlákno, dokud latch nedosáhne hodnoty 0
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Obnovení přerušení vlákna
            System.err.println("Čekání na dokončení metody loadBlackjackGame bylo přerušeno");
            return; // Ukončení bloku v případě chyby
        }
    }

    void waitForBackToLobby(){
        CountDownLatch latch = new CountDownLatch(1); // Vytvoření latch pro synchronizaci

        // Spuštění metody loadBlackjackGame ve JavaFX vlákni
        Platform.runLater(() -> {
            try {
                bc.backToLobby(); // Volání metody
            } finally {
                latch.countDown(); // Signalizace dokončení metody
            }
        });

        // Čekání na dokončení metody
        try {
            latch.await(); // Blokuje aktuální vlákno, dokud latch nedosáhne hodnoty 0
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Obnovení přerušení vlákna
            System.err.println("Čekání na dokončení metody loadBlackjackGame bylo přerušeno");
            return; // Ukončení bloku v případě chyby
        }

    }


    public void waitForOpponent() {
        final int maxAttempts = 20; // Maximální počet pokusů

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            sendCommand("WAIT_FOR_OPPONENT");

        }

    }

    public void handleServerResponse(String response)throws IOException{

        try {
            if (response.startsWith("PLAYER_CARD")) {

                String[] parts = response.split("\\|");
                String card = parts[1];
                bc.addToPlayersHand(card, parts[2]);
                String flag = parts[3];
                if (flag.equals("PLAYER_BUST")) {
                    //sendCommand("STAND");
                    bc.stand();
                }


            } else if (response.startsWith("DEALER_CARD")) {
                String[] parts = response.split("\\|");
                String card = parts[1];
                bc.addToDealersHand(card, parts[2]);

            }

        /*
        else if(response.startsWith("STAND_RECEIVED")){
            bc.disableButtons();
        }*/


            else if (response.startsWith("RESULT")) {
                String[] parts = response.split("\\|");
                bc.displayResult("Winner: " + parts[2]);
                bc.endGame();


            } else if (response.startsWith("ROOMS")) {
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
        /*
        else if (response.equals("READY_TO_START")) {
            lc.loadBlackjackGame();
            //return true; // Hra může začít
        }

         */
            else if (response.startsWith("GAME_START")) {
                waitForBlackjackControllerLoad(true);
/*
            CountDownLatch latch = new CountDownLatch(1); // Vytvoření latch pro synchronizaci

            // Spuštění metody loadBlackjackGame ve JavaFX vlákni
            Platform.runLater(() -> {
                try {
                    lc.loadBlackjackGame(true); // Volání metody
                } finally {
                    latch.countDown(); // Signalizace dokončení metody
                }
            });

            // Čekání na dokončení metody
            try {
                latch.await(); // Blokuje aktuální vlákno, dokud latch nedosáhne hodnoty 0
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Obnovení přerušení vlákna
                System.err.println("Čekání na dokončení metody loadBlackjackGame bylo přerušeno");
                return; // Ukončení bloku v případě chyby
            }
*/
                String data = response.substring("GAME_START".length());
                String[] playerIds = data.split("\\|PLAYER_ID\\|");

                for (int i = 1; i < playerIds.length; i++) {
                    //"Player " + (i) + " ID: " +
                    bc.setPlayerText(i - 1, playerIds[i]);
                }

                //lc.loadBlackjackGame();
                //Platform.runLater(() -> lc.loadBlackjackGame(true));

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

            } else if (response.equals("WAITING_FOR_OPPONENT")) {
                final int delayMillis = 5000;
                System.out.println("Čekání na dalšího hráče...");
                lc.disableAll();
                try {
                    Thread.sleep(delayMillis); // Čekání před dalším pokusem
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Čekání na hráče bylo přerušeno.", e);
                }
            } else if (response.equals("YOUR_TURN")) {
                bc.enableButtons();
                sendCommand("YOUR_TURN|OK");
            } else if (response.equals("NOT_YOUR_TURN")) {
                bc.disableButtons();
            } else if (response.startsWith("ENEMY_CARD")) {
                String[] parts = response.split("\\|");
                String card = parts[1];
                bc.addToPlayers2Hand(card, parts[2]);


            } else if (response.startsWith("RECONNECT")) {
                waitForBlackjackControllerLoad(false);
/*
            CountDownLatch latch = new CountDownLatch(1); // Vytvoření latch pro synchronizaci

            // Spuštění metody loadBlackjackGame ve JavaFX vlákni
            Platform.runLater(() -> {
                try {
                    lc.loadBlackjackGame(false); // Volání metody
                } finally {
                    latch.countDown(); // Signalizace dokončení metody
                }
            });

            // Čekání na dokončení metody
            try {
                latch.await(); // Blokuje aktuální vlákno, dokud latch nedosáhne hodnoty 0
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Obnovení přerušení vlákna
                System.err.println("Čekání na dokončení metody loadBlackjackGame bylo přerušeno");
                return; // Ukončení bloku v případě chyby
            }
*/
                // Pokračování v zpracování zprávy
                // Rozdělení odpovědi podle '|'
                String[] parts = response.split("\\|");

                // Zpracování karet dealera
                int dealerCardsIndex = 2; // První index pro karty dealera
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
                int playerCount = 1; // Počítadlo pro hráče (od Player 1 do Player 4)

                while (playerIndex < parts.length) {
                    //if (parts[playerIndex].equals("PLAYER_CARDS")) {
                    if (parts[playerIndex].equals("PLAYER_ID")) {

                        if (playerIndex < parts.length && parts[playerIndex].equals("PLAYER_ID")) {
                            String playerID = parts[playerIndex + 1].trim(); // Skóre hráče
                            switch (playerCount) {
                                case 1:
                                    bc.setPlayer1Text(playerID);
                                    break;
                                case 2:
                                    bc.setPlayer2Text(playerID);
                                    break;
                                case 3:
                                    bc.setPlayer3Text(playerID);
                                    break;
                                case 4:
                                    bc.setPlayer4Text(playerID);
                                    break;
                                default:
                                    // Pokud máme více než 4 hráče, můžeme přidat další logiku
                                    break;
                            }
                        }

                        // Karty hráče (například Player 2, Player 3, ...)
                        playerIndex += 3; // Přesuneme se na karty
                        while (playerIndex < parts.length && !parts[playerIndex].equals("PLAYER_SCORE")) {
                            String playerCard = parts[playerIndex].trim();

                            // Přidání karty podle hráče (používáme playerCount pro rozlišení hráčů)
                            switch (playerCount) {
                                case 1:
                                    bc.addCardToHand(bc.getPlayer1Cards(), playerCard);
                                    break;
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
                                case 1:
                                    bc.updatePlayerScore(bc.getPlayer1Score(), playerScore);
                                    break;
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

            } else if (response.startsWith("DISCONNECTED")) {
                String[] parts = response.split("\\|");
                String message = "Player: " + parts[1] + " seems disconnected.";
                if (bc != null)
                    bc.setLabelText(message);
                if (lc != null)
                    lc.setStatusLabel(message);
            } else if (response.startsWith("KICKED")) {
                String[] parts = response.split("\\|");
                String message = "Player: " + parts[1] + " is unreachable.";
                if (bc != null) {
                    bc.setLabelText(message);
                    //Platform.runLater(() ->  bc.backToLobby());
                }
                waitForBackToLobby();
                if (lc != null)
                    lc.setStatusLabel(message);


            } else if (response.equals("DISCONNECT")) {
                System.out.println("Server didnt recognize message");
                close();
            } else {
                //System.out.println("zase to nefunguje, response: "+response);
                System.out.println("Unknown response from server");
                close();

            }
        }
        catch(Exception e){
            System.out.println("Wrong formatted message");
            close();
        }
    }









}
