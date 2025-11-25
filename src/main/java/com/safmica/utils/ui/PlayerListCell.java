package com.safmica.utils.ui;

import com.safmica.model.Player;
import javafx.scene.control.ListCell;

public class PlayerListCell extends ListCell<Player> {
    
    @Override
    protected void updateItem(Player player, boolean empty) {
        super.updateItem(player, empty);

        if (empty || player == null) {
            setText(null);
        } else {
            String display = player.getName();
            if (player.isHost()) {
                display = "(HOST) " + display;
            }
            setText(display);
        }
    }
}
