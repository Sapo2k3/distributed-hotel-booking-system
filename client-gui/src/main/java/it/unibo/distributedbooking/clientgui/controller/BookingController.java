package it.unibo.distributedbooking.clientgui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BookingController {

    @FXML private ComboBox<String> hotelCombo;
    @FXML private TextField roomField;
    @FXML private TextField customerField;
    @FXML private TextField bookingIdField;
    @FXML private Button bookButton;
    @FXML private Button cancelButton;
    @FXML private Button refreshButton;
    @FXML private TableView<?> hotelTable;
    @FXML private TableColumn<?, ?> hotelIdCol;
    @FXML private TableColumn<?, ?> statusCol;
    @FXML private Label statusLabel;

    @FXML
    private void bookRoom() {
        statusLabel.setText("Booking " + roomField.getText() + " for " + customerField.getText());
    }

    @FXML
    private void cancelBooking() {
        statusLabel.setText("Cancelled booking " + bookingIdField.getText());
    }

    @FXML
    private void refreshHotels() {
        statusLabel.setText("Hotels refreshed");
    }
}
