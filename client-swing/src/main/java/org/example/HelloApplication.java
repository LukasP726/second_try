package org.example;

import javax.swing.*;
import java.awt.*;

public class HelloApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Server Connection");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            frame.setLayout(new BorderLayout());

            ConnectPanel connectPanel = new ConnectPanel();
            frame.add(connectPanel, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }
}
