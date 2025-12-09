package com.safmica.listener;

import java.util.List;

import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.PlayerLeaderboard;

public interface GameListener {
    void onCardsBroadcast(Game game);
    void onSubmitAck(String message);
    void onGetGameResult(GameAnswer gameAnswer);
    void onLeaderboardUpdate(List<PlayerLeaderboard> leaderboards);
    void onNextRound();
    void onRoundOver(String winner);
}
