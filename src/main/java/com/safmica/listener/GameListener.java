package com.safmica.listener;

import com.safmica.model.Game;
import com.safmica.model.GameAnswer;

public interface GameListener {
    void onCardsBroadcast(Game game);
    void onSubmitAck(String message);
    void onGetGameResult(GameAnswer gameAnswer);
}
