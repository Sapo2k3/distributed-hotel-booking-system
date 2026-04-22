package it.unibo.distributedbooking.clientgui.controller;

import it.unibo.distributedbooking.clientgui.model.HotelViewModel;
import it.unibo.distributedbooking.clientgui.service.CoordinatorClient;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class BookingController {

    @FXML private ComboBox<String> hotelCombo;
    @FXML private TextField roomField;
    @FXML private TextField customerField;
    @FXML private TextField bookingIdField;
    @FXML private DatePicker checkInDatePicker;
    @FXML private DatePicker checkOutDatePicker;
    @FXML private TableView<HotelViewModel> hotelTable;
    @FXML private TableColumn<HotelViewModel, String> hotelIdCol;
    @FXML private TableColumn<HotelViewModel, String> hostCol;
    @FXML private TableColumn<HotelViewModel, String> portCol;
    @FXML private TableColumn<HotelViewModel, String> statusCol;
    @FXML private Label statusLabel;

    private final CoordinatorClient coordinatorClient = new CoordinatorClient("http://localhost:8080");

    @FXML
    private void initialize() {
        hotelIdCol.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        portCol.setCellValueFactory(new PropertyValueFactory<>("port"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        refreshHotels();
    }

    @FXML
    private void bookRoom() {
        try {
            String selectedHotel = hotelCombo.getValue();
            String roomId = roomField.getText();
            String customerId = customerField.getText();

            if (selectedHotel == null || selectedHotel.isBlank()
                    || roomId == null || roomId.isBlank()
                    || customerId == null || customerId.isBlank()) {
                statusLabel.setText("Please fill hotel, room and customer.");
                return;
            }

            BookingRequest request = new BookingRequest(
                    "gui-book-" + System.currentTimeMillis(),
                    selectedHotel,
                    roomId,
                    customerId,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(2)
            );

            BookingResponse response = coordinatorClient.createBooking(request);
            statusLabel.setText(response.message());

            if (response.success() && response.booking() != null) {
                bookingIdField.setText(response.booking().bookingId());
            }

            refreshHotels();
        } catch (Exception e) {
            statusLabel.setText("Booking failed: " + e.getMessage());
        }
    }

    @FXML
    private void cancelBooking() {
        try {
            String bookingId = bookingIdField.getText();

            if (bookingId == null || bookingId.isBlank()) {
                statusLabel.setText("Please enter a booking ID.");
                return;
            }

            BookingCancellationRequest request = new BookingCancellationRequest(
                    "gui-cancel-" + System.currentTimeMillis(),
                    bookingId
            );

            BookingResponse response = coordinatorClient.cancelBooking(request);
            statusLabel.setText(response.message());
        } catch (Exception e) {
            statusLabel.setText("Cancellation failed: " + e.getMessage());
        }
    }

    @FXML
    private void modifyBooking() {
        try {
            String bookingId = bookingIdField.getText();
            LocalDate checkIn = checkInDatePicker.getValue();
            LocalDate checkOut = checkOutDatePicker.getValue();
            if (bookingId == null || bookingId.isBlank()) {
                statusLabel.setText("Please enter a booking ID.");
                return;
            }
            if (checkIn == null || checkOut == null) {
                statusLabel.setText("Please select new check-in and check-out dates.");
                return;
            }
            if (checkOut.isBefore(checkIn)) {
                statusLabel.setText("Check-out date must be after check-in.");
                return;
            }
            BookingModificationRequest request = new BookingModificationRequest(
                    "gui-modify-" + System.currentTimeMillis(),
                    bookingId,
                    hotelCombo.getValue(),
                    roomField.getText(),
                    customerField.getText(),
                    checkIn,
                    checkOut
            );
            BookingResponse response = coordinatorClient.modifyBooking(request);
            statusLabel.setText(response.message());

            if (response.success() && response.booking() != null) {
                bookingIdField.setText(response.booking().bookingId());
            }
            refreshHotels();
        } catch (Exception e) {
            statusLabel.setText("Modify failed: " + e.getMessage());
        }
    }

    @FXML
    private void refreshHotels() {
        try {
            List<HotelNodeInfo> hotels = coordinatorClient.fetchHotels();

            hotelCombo.setItems(FXCollections.observableArrayList(
                    hotels.stream()
                            .map(HotelNodeInfo::getHotelId)
                            .toList()
            ));

            hotelTable.setItems(FXCollections.observableArrayList(
                    hotels.stream()
                            .map(hotel -> new HotelViewModel(
                                    hotel.getHotelId(),
                                    hotel.getHost(),
                                    String.valueOf(hotel.getPort()),
                                    hotel.isUp() ? "UP" : "DOWN"
                            ))
                            .toList()
            ));

            statusLabel.setText("Hotels refreshed.");
        } catch (Exception e) {
            statusLabel.setText("Refresh failed: " + e.getMessage());
        }
    }
}