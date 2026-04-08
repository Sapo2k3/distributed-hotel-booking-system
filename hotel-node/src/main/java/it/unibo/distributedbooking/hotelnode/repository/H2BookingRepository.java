package it.unibo.distributedbooking.hotelnode.repository;

import it.unibo.distributedbooking.common.model.Booking;
import it.unibo.distributedbooking.common.model.BookingStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2BookingRepository implements BookingRepository {

    private final String jdbcUrl;

    public H2BookingRepository(final String jdbcUrl) throws SQLException {
        this.jdbcUrl = jdbcUrl;
        initializeSchema();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS bookings (
                    id VARCHAR(64) PRIMARY KEY,
                    hotel_id VARCHAR(64) NOT NULL,
                    room_id VARCHAR(64) NOT NULL,
                    customer_id VARCHAR(64) NOT NULL,
                    check_in_date DATE NOT NULL,
                    check_out_date DATE NOT NULL,
                    status VARCHAR(32) NOT NULL
                )
                """;
        try (Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize H2 schema", e);
        }
    }

    private void fillStatement(PreparedStatement statement, Booking booking) throws SQLException {
        statement.setString(1, booking.getId());
        statement.setString(2, booking.getHotelId());
        statement.setString(3, booking.getRoomId());
        statement.setString(4, booking.getCustomerId());
        statement.setObject(5, booking.getCheckInDate());
        statement.setObject(6, booking.getCheckOutDate());
        statement.setString(7, booking.getStatus().name());
    }

    private Booking mapRow(ResultSet resultSet) throws SQLException {
        return new Booking(
                resultSet.getString("id"),
                resultSet.getString("hotel_id"),
                resultSet.getString("room_id"),
                resultSet.getString("customer_id"),
                resultSet.getObject("check_in_date", LocalDate.class),
                resultSet.getObject("check_out_date", LocalDate.class),
                BookingStatus.valueOf(resultSet.getString("status"))
        );
    }

    @Override
    public void save(Booking booking) {
        String sql = """
                INSERT INTO bookings (id, hotel_id, room_id, customer_id, check_in_date, check_out_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, booking);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save booking", e);
        }
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        String sql = "SELECT * FROM bookings WHERE id = ?";

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bookingId);
            try(ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()){
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find booking", e);
        }
    }

    @Override
    public List<Booking> findAll() {
        String sql = "SELECT * FROM bookings";
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                bookings.add(mapRow(resultSet));
            }
            return bookings;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to fetch bookings", e);
        }
    }

    @Override
    public void update(Booking booking) {
        String sql = """
                   UPDATE bookings
                   SET hotel_id = ?, room_id = ?, customer_id = ?, check_in_date = ?, check_out_date = ?, status = ?
                   WHERE id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, booking.getHotelId());
            statement.setString(2, booking.getRoomId());
            statement.setString(3, booking.getCustomerId());
            statement.setObject(4, booking.getCheckInDate());
            statement.setObject(5, booking.getCheckOutDate());
            statement.setString(6, booking.getStatus().name());
            statement.setString(7, booking.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update booking", e);
        }
    }
}
