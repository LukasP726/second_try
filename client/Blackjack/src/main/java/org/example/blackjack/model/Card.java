package org.example.blackjack.model;

public class Card {
    private final String suit; // Barva (Clubs, Diamonds, Hearts, Spades)
    private final String rank; // Hodnota (2-10, Jack, Queen, King, Ace)
    private final int value; // Číselná hodnota karty (Ace může být 1 nebo 11)

    public Card(String suit, String rank, int value) {
        this.suit = suit;
        this.rank = rank;
        this.value = value;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
