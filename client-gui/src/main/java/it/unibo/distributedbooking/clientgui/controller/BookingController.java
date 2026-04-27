package it.unibo.distributedbooking.clientgui.controller;

import it.unibo.distributedbooking.clientgui.model.BookingViewModel;
import it.unibo.distributedbooking.clientgui.model.HotelViewModel;
import it.unibo.distributedbooking.clientgui.service.CoordinatorClient;
import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingCancellationRequest;
import it.unibo.distributedbooking.common.model.BookingModificationRequest;
import it.unibo.distributedbooking.common.model.BookingRequest;
import it.unibo.distributedbooking.common.model.BookingResponse;
import it.unibo.distributedbooking.coordinator.model.HotelNodeInfo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

    @FXML private Button bookButton;
    @FXML private Button cancelButton;
    @FXML private Button modifyButton;

    @FXML private TableView<HotelViewModel> hotelTable;
    @FXML private TableColumn<HotelViewModel, String> hotelIdCol;
    @FXML private TableColumn<HotelViewModel, String> hostCol;
    @FXML private TableColumn<HotelViewModel, String> portCol;
    @FXML private TableColumn<HotelViewModel, String> statusCol;

    @FXML private TableView<BookingViewModel> bookingTable;
    @FXML private TableColumn<BookingViewModel, String> bookingIdBookingCol;
    @FXML private TableColumn<BookingViewModel, String> hotelIdBookingCol;
    @FXML private TableColumn<BookingViewModel, String> roomIdBookingCol;
    @FXML private TableColumn<BookingViewModel, String> customerIdBookingCol;
    @FXML private TableColumn<BookingViewModel, String> checkInBookingCol;
    @FXML private TableColumn<BookingViewModel, String> checkOutBookingCol;
    @FXML private TableColumn<BookingViewModel, String> statusBookingCol;

    @FXML private Label statusLabel;

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_MODIFIED = "MODIFIED";

    private final CoordinatorClient coordinatorClient = new CoordinatorClient("http://localhost:8080");

    @FXML
    private void initialize() {
        hotelIdCol.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        portCol.setCellValueFactory(new PropertyValueFactory<>("port"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        bookingIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        hotelIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        roomIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        customerIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        checkInBookingCol.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        checkOutBookingCol.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        statusBookingCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        checkInDatePicker.setValue(LocalDate.now().plusDays(1));
        checkOutDatePicker.setValue(LocalDate.now().plusDays(2));
        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, selectedBooking) -> {
            if (selectedBooking != null) {
                populateFormFromSelectedBooking(selectedBooking);
                statusLabel.setText(buildSelectionMessage(selectedBooking));
            }
            updateActionButtonsState();
        });

        bookingTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(final BookingViewModel item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (STATUS_CANCELLED.equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: #888888;");
                } else if (STATUS_MODIFIED.equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: #fff8e1;");
                } else {
                    setStyle("");
                }
            }
        });
        updateActionButtonsState();
        refreshAllData();
    }

    @FXML
    private void bookRoom() {
        try {
            final String selectedHotel = hotelCombo.getValue();
            final String roomId = normalize(roomField.getText());
            final String customerId = normalize(customerField.getText());
            final LocalDate checkIn = checkInDatePicker.getValue();
            final LocalDate checkOut = checkOutDatePicker.getValue();
            if (selectedHotel == null || selectedHotel.isBlank()) {
                statusLabel.setText("Please select a hotel.");
                return;
            }
            if (roomId == null || roomId.isBlank()) {
                statusLabel.setText("Please enter a room.");
                return;
            }
            if (customerId == null || customerId.isBlank()) {
                statusLabel.setText("Please enter a customer.");
                return;
            }
            if (checkIn == null || checkOut == null) {
                statusLabel.setText("Please select check-in and check-out dates.");
                return;
            }
            if (!isValidDateRange(checkIn, checkOut)) {
                statusLabel.setText("Check-out date must be after check-in.");
                return;
            }
            final BookingRequest request = new BookingRequest(
                    "gui-book-" + System.currentTimeMillis(),
                    selectedHotel,
                    roomId,
                    customerId,
                    checkIn,
                    checkOut
            );
            final BookingResponse response = coordinatorClient.createBooking(request);
            statusLabel.setText(response.message());
            if (response.success() && response.booking() != null) {
                applyBookingToForm(response.booking());
            }
            refreshAllDataSilently();
        } catch (Exception e) {
            statusLabel.setText("Booking failed: " + rootMessage(e));
        }
    }

    @FXML
    private void cancelBooking() {
        try {
            final BookingViewModel selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
            if (selectedBooking != null && STATUS_CANCELLED.equalsIgnoreCase(selectedBooking.getStatus())) {
                statusLabel.setText("Selected booking is already cancelled.");
                return;
            }
            final String bookingId = resolveSelectedOrTypedBookingId();
            if (bookingId == null || bookingId.isBlank()) {
                statusLabel.setText("Please enter or select a booking ID.");
                return;
            }
            final BookingCancellationRequest request = new BookingCancellationRequest(
                    "gui-cancel-" + System.currentTimeMillis(),
                    bookingId
            );
            final BookingResponse response = coordinatorClient.cancelBooking(request);
            statusLabel.setText(response.message());
            if (response.success() && response.booking() != null) {
                applyBookingToForm(response.booking());
            }
            refreshAllDataSilently();
        } catch (Exception e) {
            statusLabel.setText("Cancellation failed: " + rootMessage(e));
        }
    }

    @FXML
    private void modifyBooking() {
        try {
            final BookingViewModel selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
            if (selectedBooking != null && STATUS_CANCELLED.equalsIgnoreCase(selectedBooking.getStatus())) {
                statusLabel.setText("Cannot modify a cancelled booking.");
                return;
            }
            if (selectedBooking != null && !STATUS_CONFIRMED.equalsIgnoreCase(selectedBooking.getStatus())) {
                statusLabel.setText("Only confirmed bookings can be modified.");
                return;
            }
            final String bookingId = resolveSelectedOrTypedBookingId();
            final String selectedHotel = hotelCombo.getValue();
            final String roomId = normalize(roomField.getText());
            final String customerId = normalize(customerField.getText());
            final LocalDate checkIn = checkInDatePicker.getValue();
            final LocalDate checkOut = checkOutDatePicker.getValue();
            if (bookingId == null || bookingId.isBlank()) {
                statusLabel.setText("Please enter or select a booking ID.");
                return;
            }
            if (selectedHotel == null || selectedHotel.isBlank()) {
                statusLabel.setText("Please select a hotel.");
                return;
            }
            if (roomId == null || roomId.isBlank()) {
                statusLabel.setText("Please enter a room.");
                return;
            }
            if (customerId == null || customerId.isBlank()) {
                statusLabel.setText("Please enter a customer.");
                return;
            }
            if (checkIn == null || checkOut == null) {
                statusLabel.setText("Please select new check-in and check-out dates.");
                return;
            }
            if (!isValidDateRange(checkIn, checkOut)) {
                statusLabel.setText("Check-out date must be after check-in.");
                return;
            }
            final BookingModificationRequest request = new BookingModificationRequest(
                    "gui-modify-" + System.currentTimeMillis(),
                    bookingId,
                    selectedHotel,
                    roomId,
                    customerId,
                    checkIn,
                    checkOut
            );
            final BookingResponse response = coordinatorClient.modifyBooking(request);
            statusLabel.setText(response.message());
            if (response.success() && response.booking() != null) {
                applyBookingToForm(response.booking());
            }
            refreshAllDataSilently();
        } catch (Exception e) {
            statusLabel.setText("Modify failed: " + rootMessage(e));
        }
    }

    @FXML
    private void refreshHotels() {
        try {
            refreshAllData();
            statusLabel.setText("Hotels and bookings refreshed.");
        } catch (Exception e) {
            statusLabel.setText("Refresh failed: " + rootMessage(e));
        }
    }

    private void refreshAllData() {
        final List<HotelNodeInfo> hotels = coordinatorClient.fetchHotels();
        applyHotelsToUi(hotels);
        final List<Booking> bookings = coordinatorClient.fetchBookings();
        applyBookingsToUi(bookings);
        updateActionButtonsState();
    }

    private void refreshAllDataSilently() {
        try {
            refreshAllData();
        } catch (Exception ignored) {
        }
    }

    private void applyHotelsToUi(final List<HotelNodeInfo> hotels) {
        final String currentSelection = hotelCombo.getValue();
        final List<String> hotelIds = hotels.stream()
                .map(HotelNodeInfo::getHotelId)
                .toList();
        hotelCombo.setItems(FXCollections.observableArrayList(hotelIds));
        if (currentSelection != null && hotelIds.contains(currentSelection)) {
            hotelCombo.setValue(currentSelection);
        } else if (!hotelIds.isEmpty() && hotelCombo.getValue() == null) {
            hotelCombo.setValue(hotelIds.get(0));
        }
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
    }

    private void applyBookingsToUi(final List<Booking> bookings) {
        final String selectedBookingId = resolveSelectedOrTypedBookingId();
        bookingTable.setItems(FXCollections.observableArrayList(
                bookings.stream()
                        .map(booking -> new BookingViewModel(
                                booking.bookingId(),
                                booking.hotelId(),
                                booking.roomId(),
                                booking.customerId(),
                                booking.checkInDate() != null ? booking.checkInDate().toString() : "",
                                booking.checkOutDate() != null ? booking.checkOutDate().toString() : "",
                                booking.status() != null ? booking.status().name() : ""
                        ))
                        .toList()
        ));
        if (selectedBookingId != null && !selectedBookingId.isBlank()) {
            bookingTable.getItems().stream()
                    .filter(item -> selectedBookingId.equals(item.getBookingId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> bookingTable.getSelectionModel().select(item),
                            () -> bookingTable.getSelectionModel().clearSelection()
                    );
        } else {
            bookingTable.getSelectionModel().clearSelection();
        }
    }

    private void populateFormFromSelectedBooking(final BookingViewModel selectedBooking) {
        bookingIdField.setText(selectedBooking.getBookingId());
        hotelCombo.setValue(selectedBooking.getHotelId());
        roomField.setText(selectedBooking.getRoomId());
        customerField.setText(selectedBooking.getCustomerId());
        if (selectedBooking.getCheckInDate() != null && !selectedBooking.getCheckInDate().isBlank()) {
            checkInDatePicker.setValue(LocalDate.parse(selectedBooking.getCheckInDate()));
        }
        if (selectedBooking.getCheckOutDate() != null && !selectedBooking.getCheckOutDate().isBlank()) {
            checkOutDatePicker.setValue(LocalDate.parse(selectedBooking.getCheckOutDate()));
        }
    }

    private void applyBookingToForm(final Booking booking) {
        bookingIdField.setText(booking.bookingId());
        hotelCombo.setValue(booking.hotelId());
        roomField.setText(booking.roomId());
        customerField.setText(booking.customerId());
        checkInDatePicker.setValue(booking.checkInDate());
        checkOutDatePicker.setValue(booking.checkOutDate());
    }

    private void updateActionButtonsState() {
        final BookingViewModel selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
        final boolean hasSelection = selectedBooking != null;
        final String status = hasSelection ? selectedBooking.getStatus() : null;
        final boolean isCancelled = hasSelection && STATUS_CANCELLED.equalsIgnoreCase(status);
        final boolean isConfirmed = hasSelection && STATUS_CONFIRMED.equalsIgnoreCase(status);
        cancelButton.setDisable(!hasSelection || isCancelled);
        modifyButton.setDisable(!hasSelection || !isConfirmed);
        bookButton.setDisable(false);
    }

    private String buildSelectionMessage(final BookingViewModel selectedBooking) {
        final String bookingId = selectedBooking.getBookingId();
        final String status = selectedBooking.getStatus();
        if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return "Selected booking " + bookingId + " is CANCELLED. Modify and Cancel are disabled.";
        }
        if (STATUS_MODIFIED.equalsIgnoreCase(status)) {
            return "Selected booking " + bookingId + " is MODIFIED. Cancel is available, modify is disabled.";
        }
        if (STATUS_CONFIRMED.equalsIgnoreCase(status)) {
            return "Selected booking " + bookingId + " is CONFIRMED. Cancel and Modify are available.";
        }
        return "Selected booking " + bookingId + " with status " + status + ".";
    }

    private String resolveSelectedOrTypedBookingId() {
        final BookingViewModel selectedBooking = bookingTable.getSelectionModel().getSelectedItem();
        if (selectedBooking != null && selectedBooking.getBookingId() != null && !selectedBooking.getBookingId().isBlank()) {
            return selectedBooking.getBookingId();
        }
        return normalize(bookingIdField.getText());
    }

    private boolean isValidDateRange(final LocalDate checkIn, final LocalDate checkOut) {
        return checkOut.isAfter(checkIn);
    }

    private String normalize(final String value) {
        return value == null ? null : value.trim();
    }

    private String rootMessage(final Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : exception.getMessage();
    }
}