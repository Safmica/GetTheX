package com.safmica.model;

public class Room {
    private int totalCard;
    private int totalRound;

    public Room(int totalCard, int totalRound) {
        this.totalCard = totalCard;
        this.totalRound = totalRound;
    }

    public int getTotalCard() {
        return totalCard;
    }

    public void setTotalCard(int totalCard) {
        this.totalCard = totalCard;
    }

    public int getTotalRound() {
        return totalRound;
    }

    public void setTotalRound(int totalRound) {
        this.totalRound = totalRound;
    }
}
