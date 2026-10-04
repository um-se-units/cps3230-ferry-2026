package mt.edu.um.cps3230.ferry.adapters.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import mt.edu.um.cps3230.ferry.domain.Booking;
import mt.edu.um.cps3230.ferry.domain.BookingStatus;
import mt.edu.um.cps3230.ferry.domain.Passenger;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.domain.Vehicle;
import mt.edu.um.cps3230.ferry.ports.BookingRepository;

/** Keeps bookings, their passengers and their vehicle in the database. */
public final class JdbcBookingRepository implements BookingRepository {

    private static final String SELECT = """
            SELECT b.reference, b.contact_email, b.status, b.reserved_at, b.payment_reference,
                   b.vehicle_registration,
                   s.id AS sailing_id, s.departs_from, s.arrives_at, s.departs_at,
                   s.passenger_capacity, s.vehicle_capacity,
                   p.ordinal, p.name, p.date_of_birth, p.gozo_resident
              FROM bookings b
              JOIN sailings s ON s.id = b.sailing_id
              LEFT JOIN passengers p ON p.booking_reference = b.reference
            """;

    private static final String BY_REFERENCE = SELECT + " WHERE b.reference = ? ORDER BY p.ordinal";

    private static final String BY_SAILING =
            SELECT + " WHERE b.sailing_id = ? ORDER BY b.reference, p.ordinal";

    private static final String UPDATE = """
            UPDATE bookings
               SET sailing_id = ?, contact_email = ?, status = ?, reserved_at = ?,
                   payment_reference = ?, vehicle_registration = ?
             WHERE reference = ?
            """;

    private static final String INSERT = """
            INSERT INTO bookings
                (reference, sailing_id, contact_email, status, reserved_at,
                 payment_reference, vehicle_registration)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String DELETE_PASSENGERS =
            "DELETE FROM passengers WHERE booking_reference = ?";

    private static final String INSERT_PASSENGER = """
            INSERT INTO passengers (booking_reference, ordinal, name, date_of_birth, gozo_resident)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    public JdbcBookingRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Booking> findByReference(String reference) {
        List<Booking> found = query(BY_REFERENCE, reference);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<Booking> findBySailingId(String sailingId) {
        return query(BY_SAILING, sailingId);
    }

    @Override
    public void save(Booking booking) {
        try (Connection connection = dataSource.getConnection()) {
            storeBooking(connection, booking);
            storePassengers(connection, booking);
        } catch (SQLException failure) {
            throw new IllegalStateException("could not store booking " + booking.reference(), failure);
        }
    }

    private void storeBooking(Connection connection, Booking booking) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(UPDATE)) {
            bindBooking(update, booking);
            update.setString(7, booking.reference());
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, booking.reference());
            bindBooking(insert, booking, 2);
            insert.executeUpdate();
        }
    }

    private void bindBooking(PreparedStatement statement, Booking booking) throws SQLException {
        bindBooking(statement, booking, 1);
    }

    private void bindBooking(PreparedStatement statement, Booking booking, int firstParameter)
            throws SQLException {
        int index = firstParameter;
        statement.setString(index++, booking.sailing().id());
        statement.setString(index++, booking.contactEmail());
        statement.setString(index++, booking.status().name());
        statement.setTimestamp(index++, Timestamp.valueOf(booking.reservedAt()));
        setNullableString(statement, index++, booking.paymentReference().orElse(null));
        setNullableString(statement, index, booking.vehicle().map(Vehicle::registration).orElse(null));
    }

    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void storePassengers(Connection connection, Booking booking) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_PASSENGERS)) {
            delete.setString(1, booking.reference());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_PASSENGER)) {
            List<Passenger> travelling = booking.passengers();
            for (int ordinal = 0; ordinal < travelling.size(); ordinal++) {
                Passenger passenger = travelling.get(ordinal);
                insert.setString(1, booking.reference());
                insert.setInt(2, ordinal);
                insert.setString(3, passenger.name());
                insert.setDate(4, java.sql.Date.valueOf(passenger.dateOfBirth()));
                insert.setBoolean(5, passenger.gozoResident());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private List<Booking> query(String sql, String parameter) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(sql)) {
            select.setString(1, parameter);
            try (ResultSet rows = select.executeQuery()) {
                return assemble(rows);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read bookings for " + parameter, failure);
        }
    }

    /** Rolls the joined rows up into one booking per reference, passengers in order. */
    private static List<Booking> assemble(ResultSet rows) throws SQLException {
        Map<String, Header> headers = new LinkedHashMap<>();
        Map<String, List<Passenger>> passengers = new LinkedHashMap<>();
        while (rows.next()) {
            String reference = rows.getString("reference");
            if (!headers.containsKey(reference)) {
                headers.put(reference, headerIn(rows));
                passengers.put(reference, new ArrayList<>());
            }
            String name = rows.getString("name");
            if (name != null) {
                passengers.get(reference).add(new Passenger(name,
                        rows.getDate("date_of_birth").toLocalDate(),
                        rows.getBoolean("gozo_resident")));
            }
        }
        List<Booking> bookings = new ArrayList<>();
        headers.forEach((reference, header) ->
                bookings.add(header.toBooking(reference, passengers.get(reference))));
        return bookings;
    }

    private static Header headerIn(ResultSet rows) throws SQLException {
        Sailing sailing = new Sailing(
                rows.getString("sailing_id"),
                Terminal.valueOf(rows.getString("departs_from")),
                Terminal.valueOf(rows.getString("arrives_at")),
                rows.getTimestamp("departs_at").toLocalDateTime(),
                rows.getInt("passenger_capacity"),
                rows.getInt("vehicle_capacity"));
        return new Header(sailing, rows.getString("contact_email"),
                BookingStatus.valueOf(rows.getString("status")),
                rows.getTimestamp("reserved_at").toLocalDateTime(),
                rows.getString("payment_reference"), rows.getString("vehicle_registration"));
    }

    /** The columns of a booking row that do not repeat once per passenger. */
    private record Header(Sailing sailing, String contactEmail, BookingStatus status,
                          LocalDateTime reservedAt, String paymentReference,
                          String vehicleRegistration) {

        Booking toBooking(String reference, List<Passenger> passengers) {
            return new Booking(reference, sailing, passengers,
                    Optional.ofNullable(vehicleRegistration).map(Vehicle::new), contactEmail,
                    status, reservedAt, Optional.ofNullable(paymentReference));
        }
    }
}
