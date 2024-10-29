package org.example.blackjack;

public class Dealer extends Player {
    public boolean shouldHit() {
        return getScore() < 17; // Dealer táhne, pokud je jeho skóre menší než 17
    }
}
