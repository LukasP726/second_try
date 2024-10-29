package org.example.blackjack;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final List<Card> hand = new ArrayList<>();
    private int score;

    public void addCard(Card card) {
        hand.add(card);
        score += card.getValue();
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getScore() {
        return score;
    }

    public void reset() {
        hand.clear();
        score = 0;
    }

    public boolean hasBlackjack() {
        return score == 21 && hand.size() == 2;
    }

    public boolean isBusted() {
        return score > 21;
    }
}
