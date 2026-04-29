package it.unibo.distributedbooking.clientgui.controller;

import javafx.scene.control.TableCell;

public class StatusTableCell<S> extends TableCell<S, String> {
    @Override
    protected void updateItem(final String item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setStyle("");
        } else {
            setText(item);
            setStyle("-fx-font-weight: bold; " + getStyleForStatus(item));
        }
    }

    private String getStyleForStatus(String status) {
        return switch (status.toUpperCase()) {
            case "UP", "CONFIRMED" -> "-fx-text-fill: #1e8449;";
            case "MODIFIED" -> "-fx-text-fill: #b9770e;";
            case "CANCELLED", "DOWN" -> "-fx-text-fill: #c0392b;";
            default -> "-fx-text-fill: #2c3e50;";
        };
    }
}