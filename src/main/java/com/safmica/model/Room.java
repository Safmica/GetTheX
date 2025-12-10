package com.safmica.model;

public class Room {
    private int totalCard;
    private int totalRound;
    private int playerLimit = 1;

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

    public int getPlayerLimit() {
        return playerLimit;
    }

    public void setPlayerLimit(int playerLimit) {
        if (playerLimit < 1) playerLimit = 1;
        if (playerLimit > 10) playerLimit = 10;
        this.playerLimit = playerLimit;
    }
}
