package com.safmica.model;

import java.util.List;

public class Game {
    private List<Integer> cards;
    private int x;
    public List<GameAnswer> listAnswer;

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public synchronized void setListAnswer(GameAnswer listAnswer) {
        this.listAnswer.add(listAnswer);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
}
