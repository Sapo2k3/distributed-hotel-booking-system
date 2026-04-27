package it.unibo.distributedbooking.hotelnode.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.unibo.distributedbooking.common.model.BookingResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class H2ProcessedRequestRepository implements ProcessedRequestRepository {

    private final String jdbcUrl;
    private final ObjectMapper objectMapper;

    public H2ProcessedRequestRepository(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        initializeSchema();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initializeSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS processed_requests (
                    request_id VARCHAR(64) PRIMARY KEY,
                    response_json CLOB NOT NULL
                )
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize processed requests schema", e);
        }
    }

    @Override
    public void save(final String requestId, final BookingResponse response) {
        String sql = """
                MERGE INTO processed_requests (request_id, response_json)
                KEY(request_id)
                VALUES (?, ?)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            statement.setString(2, objectMapper.writeValueAsString(response));
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save processed request", e);
        }
    }

    @Override
    public Optional<BookingResponse> findByRequestId(final String requestId) {
        String sql = "SELECT response_json FROM processed_requests WHERE request_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String json = resultSet.getString("response_json");
                return Optional.of(objectMapper.readValue(json, BookingResponse.class));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load processed request", e);
        }
    }
}