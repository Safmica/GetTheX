package com.safmica.listener;

import java.util.List;
import com.safmica.model.Player;
import com.safmica.model.Room;

public interface RoomListener {
    void onPlayerListChanged(List<Player> players);
    void onPlayerConnected(String username);
    void onPlayerDisconnected(String username);
    void onSettingChange(Room room);
}
