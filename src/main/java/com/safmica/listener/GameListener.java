package com.safmica.listener;

import com.safmica.model.Game;

public interface GameListener {
    void onCardsBroadcast(Game game);
    void onSubmitAck(String message);
}
