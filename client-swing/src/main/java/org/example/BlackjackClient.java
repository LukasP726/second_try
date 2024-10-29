package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BlackjackClient {
    private Socket socket;
    private String host;
    private String port;
    private BufferedReader in;
    private PrintWriter out;

    public BlackjackClient(String host, int port) throws IOException {
        // Připojení k serveru
        socket = new Socket(host, port);
        this.host = host;
        this.port=String.valueOf(port);
        System.out.println("Connected to server: " + host + ":" + port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    // Odeslání příkazu na server
    public void sendCommand(String command) {
        System.out.println("Sending command to server: " + command);
        out.println(command);
    }

    // Přečtení odpovědi od serveru
    public String getResponse() throws IOException {
        String response = in.readLine();
        System.out.println("Response from server: " + response); // Přidání ladícího výstupu
        return response;
    }

    // Uzavření socketu
    public void close() throws IOException {
        socket.close();
    }

    public String getIp() {
        return this.host;
    }

    public String getPort() {
        return this.port;
    }
}

