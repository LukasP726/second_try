package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ConnectPanel extends JPanel {
    private JTextField ipField;
    private JTextField portField;
    private JLabel statusLabel;

    public ConnectPanel() {
        setLayout(new GridLayout(4, 1));

        ipField = new JTextField("Enter IP address");
        portField = new JTextField("Enter Port");
        JButton connectButton = new JButton("Connect");
        statusLabel = new JLabel("");

        connectButton.addActionListener(new ConnectAction());

        add(new JLabel("Server Connection"));
        add(ipField);
        add(portField);
        add(connectButton);
        add(statusLabel);
    }

    private class ConnectAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());
            try {
                BlackjackClient client = new BlackjackClient(ip, port);
                statusLabel.setText("Connected to server.");
                // Můžete také přejít na další panel pro hru zde.
            } catch (IOException ex) {
                statusLabel.setText("Failed to connect to server.");
                ex.printStackTrace();
            }
        }
    }
}
