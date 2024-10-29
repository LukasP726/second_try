package org.example.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();
    private int cardIndex = 0; // Aktuální index karty v balíčku

    // Konstruktor, který inicializuje balíček s 52 kartami
    public Deck() {
        String[] suits = {"Clubs", "Diamonds", "Hearts", "Spades"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
        int[] values = {2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11}; // Hodnoty karet, kde Jack, Queen a King = 10, Ace = 11

        // Vytvoření všech kombinací karet
        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                cards.add(new Card(suit, ranks[i], values[i]));
            }
        }
        shuffle();
    }

    // Zamíchání balíčku
    public void shuffle() {
        Collections.shuffle(cards);
        cardIndex = 0; // Resetování indexu po zamíchání
    }

    // Distribuce karty
    public Card dealCard() {
        if (cardIndex < cards.size()) {
            return cards.get(cardIndex++);
        }
        return null; // V případě, že nejsou další karty
    }
}
