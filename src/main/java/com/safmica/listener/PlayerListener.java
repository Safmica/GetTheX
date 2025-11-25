package com.safmica.listener;

import com.safmica.model.Player;

public interface PlayerListener {
    Player onPlayerAdded(String name);
    Player onPlayerRemoved(String name);
}
