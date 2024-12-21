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
        socket = new Socket(host, port);
        System.out.println("Connected to server: " + host + ":" + port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Po připojení pošleme jméno na server
        sendCommand( "CONNECT|" + userName);


        // Zahájíme poslouchání serveru
        startListeningToServer();
    }





    private void startListeningToServer() {
        new Thread(() -> {
            while (running) {
                try {
                    String message = getResponse();
                    if ("PING".equals(message)) {
                        sendPong();
                    }
                     else {
                        handleServerResponse(message);
                    }

                } catch (IOException e) {
                    System.out.println("Chyba při čtení zprávy od serveru: " + e.getMessage());
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
                sendCommand("STAND");
            }


        } else if (response.startsWith("DEALER_CARD")) {
            String[] parts = response.split("\\|");
            String cardPart = parts[0].trim(); // Seznam karet dealera
            String scorePart = parts[1].trim(); // Finální skóre
            String winnerPart = parts[2].trim();
            String[] dealerCardsArray = cardPart.replace("DEALER_CARDS", "").trim().split(" ");
            for (String card : dealerCardsArray) {
                bc.addToDealersHand(card); // Přidání karty do dealerovy ruky

            }

            // Zpracování skóre
            String finalScore = scorePart.replace("FINAL_SCORE", "").trim();

            bc.updateDealerScore(finalScore); // Aktualizace skóre dealera
            bc.setLabelText(winnerPart);
            bc.endGame();





        } else if (response.startsWith("RESULT")) {
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
