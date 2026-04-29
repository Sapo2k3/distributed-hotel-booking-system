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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

public class BookingController {

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_MODIFIED = "MODIFIED";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

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
    @FXML private TableColumn<BookingViewModel, String> stayBookingCol;
    @FXML private TableColumn<BookingViewModel, String> statusBookingCol;

    @FXML private Label statusLabel;

    private final CoordinatorClient coordinatorClient = new CoordinatorClient("http://localhost:8080");

    @FXML
    private void initialize() {
        bindColumns();
        configureTables();

        checkInDatePicker.setValue(LocalDate.now().plusDays(1));
        checkOutDatePicker.setValue(LocalDate.now().plusDays(2));

        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, selectedBooking) -> {
            if (selectedBooking != null) {
                populateFormFromSelectedBooking(selectedBooking);
                statusLabel.setText(buildSelectionMessage(selectedBooking));
            }
            updateActionButtonsState();
        });

        updateActionButtonsState();
        refreshAllData();
    }

    @FXML
    private void bookRoom() {
        try {
            final String hotelId = hotelCombo.getValue();
            final String roomId = normalize(roomField.getText());
            final String customerId = normalize(customerField.getText());
            final LocalDate checkIn = checkInDatePicker.getValue();
            final LocalDate checkOut = checkOutDatePicker.getValue();

            if (isBlank(hotelId)) {
                statusLabel.setText("Please select a hotel.");
                return;
            }
            if (isBlank(roomId)) {
                statusLabel.setText("Please enter a room.");
                return;
            }
            if (isBlank(customerId)) {
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
                    hotelId,
                    roomId,
                    customerId,
                    checkIn,
                    checkOut
            );

            handleBookingResponse(coordinatorClient.createBooking(request), "Booking failed");
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
            if (isBlank(bookingId)) {
                statusLabel.setText("Please enter or select a booking ID.");
                return;
            }

            final BookingCancellationRequest request = new BookingCancellationRequest(
                    "gui-cancel-" + System.currentTimeMillis(),
                    bookingId
            );

            handleBookingResponse(coordinatorClient.cancelBooking(request), "Cancellation failed");
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
            final String hotelId = hotelCombo.getValue();
            final String roomId = normalize(roomField.getText());
            final String customerId = normalize(customerField.getText());
            final LocalDate checkIn = checkInDatePicker.getValue();
            final LocalDate checkOut = checkOutDatePicker.getValue();

            if (isBlank(bookingId)) {
                statusLabel.setText("Please enter or select a booking ID.");
                return;
            }
            if (isBlank(hotelId)) {
                statusLabel.setText("Please select a hotel.");
                return;
            }
            if (isBlank(roomId)) {
                statusLabel.setText("Please enter a room.");
                return;
            }
            if (isBlank(customerId)) {
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
                    hotelId,
                    roomId,
                    customerId,
                    checkIn,
                    checkOut
            );

            handleBookingResponse(coordinatorClient.modifyBooking(request), "Modify failed");
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

    private void bindColumns() {
        hotelIdCol.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        portCol.setCellValueFactory(new PropertyValueFactory<>("port"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookingIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        hotelIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("hotelId"));
        roomIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        customerIdBookingCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        stayBookingCol.setCellValueFactory(new PropertyValueFactory<>("stay"));
        statusBookingCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void configureTables() {
        statusCol.setCellFactory(column -> createStatusCell());
        statusBookingCol.setCellFactory(column -> createStatusCell());

        hotelTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(final HotelViewModel item, final boolean empty) {
                super.updateItem(item, empty);
                setStyle(empty || item == null ? "" :
                        STATUS_DOWN.equalsIgnoreCase(item.getStatus()) ? "-fx-background-color: #fdecea;" : "");
            }
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
    }

    private <S> TableCell<S, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item.toUpperCase());
                setStyle(styleForStatus(item));
            }
        };
    }

    private String styleForStatus(final String status) {
        if (equalsAnyIgnoreCase(status, STATUS_UP, STATUS_CONFIRMED)) {
            return "-fx-text-fill: #1e8449; -fx-font-weight: bold;";
        }
        if (equalsAnyIgnoreCase(status, STATUS_DOWN)) {
            return "-fx-text-fill: #c0392b; -fx-font-weight: bold;";
        }
        if (equalsAnyIgnoreCase(status, STATUS_MODIFIED)) {
            return "-fx-text-fill: #b9770e; -fx-font-weight: bold;";
        }
        if (equalsAnyIgnoreCase(status, STATUS_CANCELLED)) {
            return "-fx-text-fill: #7f8c8d; -fx-font-weight: bold;";
        }
        return "-fx-font-weight: bold;";
    }

    private void handleBookingResponse(final BookingResponse response, final String fallbackMessage) {
        statusLabel.setText(response.message());

        if (response.success() && response.booking() != null) {
            applyBookingToForm(response.booking());
        }

        refreshAllDataSilently();

        if (response.message() == null || response.message().isBlank()) {
            statusLabel.setText(fallbackMessage);
        }
    }

    private void refreshAllData() {
        applyHotelsToUi(coordinatorClient.fetchHotels());
        applyBookingsToUi(coordinatorClient.fetchBookings());
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
                                hotel.isUp() ? STATUS_UP : STATUS_DOWN
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

        reselectBooking(selectedBookingId);
    }

    private void reselectBooking(final String selectedBookingId) {
        if (isBlank(selectedBookingId)) {
            bookingTable.getSelectionModel().clearSelection();
            return;
        }

        bookingTable.getItems().stream()
                .filter(item -> selectedBookingId.equals(item.getBookingId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> bookingTable.getSelectionModel().select(item),
                        () -> bookingTable.getSelectionModel().clearSelection()
                );
    }

    private void populateFormFromSelectedBooking(final BookingViewModel selectedBooking) {
        bookingIdField.setText(selectedBooking.getBookingId());
        hotelCombo.setValue(selectedBooking.getHotelId());
        roomField.setText(selectedBooking.getRoomId());
        customerField.setText(selectedBooking.getCustomerId());

        if (!isBlank(selectedBooking.getCheckInDate())) {
            checkInDatePicker.setValue(LocalDate.parse(selectedBooking.getCheckInDate()));
        }
        if (!isBlank(selectedBooking.getCheckOutDate())) {
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
        final String status = selectedBooking != null ? selectedBooking.getStatus() : null;

        cancelButton.setDisable(selectedBooking == null || STATUS_CANCELLED.equalsIgnoreCase(status));
        modifyButton.setDisable(selectedBooking == null || !STATUS_CONFIRMED.equalsIgnoreCase(status));
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
        return selectedBooking != null && !isBlank(selectedBooking.getBookingId())
                ? selectedBooking.getBookingId()
                : normalize(bookingIdField.getText());
    }

    private boolean isValidDateRange(final LocalDate checkIn, final LocalDate checkOut) {
        return checkOut.isAfter(checkIn);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private String normalize(final String value) {
        return value == null ? null : value.trim();
    }

    private boolean equalsAnyIgnoreCase(final String value, final String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private String rootMessage(final Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : exception.getMessage();
    }
}