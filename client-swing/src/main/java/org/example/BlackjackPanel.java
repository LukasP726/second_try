package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BlackjackPanel extends JPanel {
    private JLabel playerScoreLabel;
    private JLabel dealerScoreLabel;
    private JLabel messageLabel;
    private JPanel playerCardsPanel;
    private JPanel dealerCardsPanel;

    public BlackjackPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Hlavní název
        JLabel titleLabel = new JLabel("Blackjack Game");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Hráčské a dealerové karty
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.X_AXIS));

        playerCardsPanel = new JPanel();
        playerCardsPanel.setLayout(new BoxLayout(playerCardsPanel, BoxLayout.Y_AXIS));
        playerScoreLabel = new JLabel("Player Score: 0");
        playerCardsPanel.add(new JLabel("Player Cards"));
        playerCardsPanel.add(playerScoreLabel);

        dealerCardsPanel = new JPanel();
        dealerCardsPanel.setLayout(new BoxLayout(dealerCardsPanel, BoxLayout.Y_AXIS));
        dealerScoreLabel = new JLabel("Dealer Score: 0");
        dealerCardsPanel.add(new JLabel("Dealer Cards"));
        dealerCardsPanel.add(dealerScoreLabel);

        cardsPanel.add(playerCardsPanel);
        cardsPanel.add(Box.createRigidArea(new Dimension(20, 0))); // mezera mezi hráčem a dealerem
        cardsPanel.add(dealerCardsPanel);

        add(cardsPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Ovládací tlačítka
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JButton btnHit = new JButton("Hit");
        btnHit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hit();
            }
        });
        buttonPanel.add(btnHit);

        JButton btnStand = new JButton("Stand");
        btnStand.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stand();
            }
        });
        buttonPanel.add(btnStand);

        JButton btnNewGame = new JButton("New Game");
        btnNewGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newGame();
            }
        });
        buttonPanel.add(btnNewGame);

        add(buttonPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // Zpráva
        messageLabel = new JLabel("Click 'New Game' to start!");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(messageLabel);
    }

    // Příklady metod pro zpracování akcí
    private void hit() {
        // Logika pro "Hit"
        messageLabel.setText("Hit action performed!");
    }

    private void stand() {
        // Logika pro "Stand"
        messageLabel.setText("Stand action performed!");
    }

    private void newGame() {
        // Logika pro novou hru
        messageLabel.setText("New game started!");
        playerScoreLabel.setText("Player Score: 0");
        dealerScoreLabel.setText("Dealer Score: 0");
        playerCardsPanel.removeAll();
        dealerCardsPanel.removeAll();
        revalidate();
        repaint();
    }

    public void addPlayerCard(JComponent card) {
        playerCardsPanel.add(card);
        playerCardsPanel.revalidate();
        playerCardsPanel.repaint();
    }

    public void addDealerCard(JComponent card) {
        dealerCardsPanel.add(card);
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
    }
}
