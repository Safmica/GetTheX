package com.safmica.model;

import java.util.List;

public class Game {
    private List<Integer> cards;
    private int x;
    private GameAnswer currentAnswer;

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public void setCurrentAnswer(GameAnswer currentAnswer) {
        this.currentAnswer = currentAnswer;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public GameAnswer getCurrentAnswer() {
        return currentAnswer;
    }
}
