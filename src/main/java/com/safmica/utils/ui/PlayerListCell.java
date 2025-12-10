package com.safmica.utils.ui;

import com.safmica.model.Player;
import com.safmica.network.client.TcpClientHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.Node;

import java.util.function.Supplier;

public class PlayerListCell extends ListCell<Player> {

    private final TcpClientHandler clientHandler;
    private final Supplier<String> localUsernameSupplier;

    public PlayerListCell(TcpClientHandler clientHandler, Supplier<String> localUsernameSupplier) {
        this.clientHandler = clientHandler;
        this.localUsernameSupplier = localUsernameSupplier;
    }

    @Override
    protected void updateItem(Player player, boolean empty) {
        super.updateItem(player, empty);

        if (empty || player == null) {
            setText(null);
            setGraphic(null);
        } else {
            String display = player.getName();
            if (player.isHost()) {
                display = "(HOST) " + display;
            }

            Label nameLabel = new Label(display);
            HBox container = new HBox(6);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(nameLabel);

            String local = localUsernameSupplier == null ? null : localUsernameSupplier.get();
            if (local != null && local.equals(player.getName())) {

                Button settingsBtn = new Button("⚙");
                settingsBtn.setFocusTraversable(false);
                settingsBtn.setPrefWidth(28);
                settingsBtn.setPrefHeight(20);
                settingsBtn.setMinWidth(24);
                settingsBtn.setMinHeight(18);
                settingsBtn.setMaxWidth(32);
                settingsBtn.setMaxHeight(22);
                settingsBtn.setStyle("-fx-font-size:11px; -fx-padding:1px 4px; -fx-background-radius:4px;");
                settingsBtn.setOnAction(evt -> {
                    TextInputDialog dialog = new TextInputDialog(player.getName());
                    dialog.setTitle("Change Username");
                    dialog.setHeaderText("Change your username");
                    dialog.setContentText("");
                    dialog.initOwner(this.getListView().getScene().getWindow());

                    Node contentLabel = dialog.getDialogPane().lookup(".content.label");
                    if (contentLabel instanceof Label) {
                        ((Label) contentLabel).setTextFill(Color.WHITE);
                    } else {
                        Node content = dialog.getDialogPane().getContent();
                        if (content instanceof javafx.scene.Parent) {
                            for (Node n : ((javafx.scene.Parent) content).lookupAll(".label")) {
                                if (n instanceof Label) {
                                    Label lbl = (Label) n;
                                    if ("New username:".equals(lbl.getText())) {
                                        lbl.setTextFill(Color.WHITE);
                                    }
                                }
                            }
                        }
                    }
                    Node headerLabel = dialog.getDialogPane().lookup(".header-panel .label");
                    if (headerLabel instanceof Label) {
                        ((Label) headerLabel).setTextFill(Color.WHITE);
                    }
                    

                    dialog.showAndWait().ifPresent(newName -> {
                        String trimmed = newName.trim();
                        if (!trimmed.isEmpty() && clientHandler != null) {
                            clientHandler.requestChangeUsername(trimmed);
                        }
                    });
                });
                container.getChildren().add(settingsBtn);
            }

            setGraphic(container);
            setText(null);
        }
    }
}
