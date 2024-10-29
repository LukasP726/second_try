package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ConnectController {
    private JTextField ipField;
    private JTextField portField;
    private JLabel statusLabel;
    private JFrame frame;

    public ConnectController() {
        // Vytvoření GUI pro připojení
        frame = new JFrame("Server Connection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new GridLayout(4, 2));

        ipField = new JTextField();
        portField = new JTextField();
        statusLabel = new JLabel("");

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        frame.add(new JLabel("Enter IP address:"));
        frame.add(ipField);
        frame.add(new JLabel("Enter Port:"));
        frame.add(portField);
        frame.add(connectButton);
        frame.add(statusLabel);

        frame.setVisible(true);
    }

    private void connectToServer() {
        String ip = ipField.getText();
        int port = Integer.parseInt(portField.getText());

        try {
            BlackjackClient client = new BlackjackClient(ip, port);
            statusLabel.setText("Connected to server.");

            // Načtení herního okna
            loadGameWindow(client);

        } catch (IOException e) {
            statusLabel.setText("Failed to connect to server.");
            e.printStackTrace();
        }
    }

    private void loadGameWindow(BlackjackClient client) {
        JFrame gameFrame = new JFrame("Blackjack Game");
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setSize(400, 300);

        // Přidej komponenty pro hru (např. JButton, JLabel atd.)
        // Například:
        JPanel gamePanel = new JPanel();
        gamePanel.add(new JLabel("Welcome to Blackjack!"));

        gameFrame.add(gamePanel);
        gameFrame.setVisible(true);

        // Zde bys měl také předat klienta do herního controlleru, pokud ho máš
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectController());
    }
}
