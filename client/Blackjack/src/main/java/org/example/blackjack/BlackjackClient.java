package org.example.blackjack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BlackjackClient {
    private Socket socket;
    private String host;
    private String port;
    private BufferedReader in;
    private PrintWriter out;
    private ScheduledExecutorService pingScheduler; // Plánovač pro odesílání PING

    public BlackjackClient(String host, int port) throws IOException {
        this.host = host;
        this.port = String.valueOf(port);

        // Připojení k serveru
        socket = new Socket(host, port);
        System.out.println("Connected to server: " + host + ":" + port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Zahájíme pravidelné odesílání "PING"
        startPingingServer();
    }

    // Metoda pro pravidelné odesílání "PING"
    private void startPingingServer() {
        pingScheduler = Executors.newScheduledThreadPool(1);
        pingScheduler.scheduleAtFixedRate(() -> {
            sendPing();
        }, 0, 10, TimeUnit.SECONDS); // Odesílá PING každých 10 sekund
    }

    // Metoda pro odesílání "PING" zprávy serveru
    private void sendPing() {
        sendCommand("PING");
        try {
            String response = getResponse();
            if ("PONG".equals(response)) {
                System.out.println("Server je dostupný (PONG přijat).");
            } else {
                System.out.println("Neočekávaná odpověď od serveru: " + response);
            }
        } catch (IOException e) {
            System.out.println("Chyba při odesílání PING nebo přijímání PONG.");
            stopPingingServer(); // Zastaví pingování při chybě
        }
    }

    // Zastavení plánovače PING
    private void stopPingingServer() {
        if (pingScheduler != null && !pingScheduler.isShutdown()) {
            pingScheduler.shutdown();
        }
    }

    public void sendCommand(String command) {
        System.out.println("Sending command to server: " + command);
        out.println(command);
    }

    public String getResponse() throws IOException {
        String response = in.readLine();
        System.out.println("Response from server: " + response);
        return response;
    }

    public void close() throws IOException {
        stopPingingServer(); // Zastavení pingování před ukončením
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
    public boolean joinRoom(String gameMode) throws IOException {
        sendCommand("JOIN_ROOM " + gameMode); // Příkaz pro server k připojení do místnosti
        String response = getResponse();
        return response.equals("ROOM_JOINED"); // Server by měl odpovědět, že klient byl úspěšně připojen
    }

    // Metoda pro čekání na dalšího hráče
    public boolean waitForOpponent() throws IOException {
        sendCommand("WAIT_FOR_OPPONENT");
        String response = getResponse();
        return response.equals("OPPONENT_JOINED");
    }
}
