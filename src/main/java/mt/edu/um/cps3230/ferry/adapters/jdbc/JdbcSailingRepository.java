package mt.edu.um.cps3230.ferry.adapters.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import mt.edu.um.cps3230.ferry.domain.Sailing;
import mt.edu.um.cps3230.ferry.domain.SailingAvailability;
import mt.edu.um.cps3230.ferry.domain.Terminal;
import mt.edu.um.cps3230.ferry.ports.SailingRepository;

/** Keeps sailings in the database. */
public final class JdbcSailingRepository implements SailingRepository {

    private static final String SELECT_BY_ID = """
            SELECT id, departs_from, arrives_at, departs_at, passenger_capacity, vehicle_capacity
              FROM sailings
             WHERE id = ?
            """;

    private static final String SELECT_BY_DATE_AND_ORIGIN = """
            SELECT id, departs_from, arrives_at, departs_at, passenger_capacity, vehicle_capacity
              FROM sailings
             WHERE departs_from = ?
               AND departs_at >= ?
               AND departs_at < ?
             ORDER BY departs_at
            """;

    private static final String COUNT_PASSENGERS_ON = """
            SELECT COUNT(*)
              FROM passengers p
              JOIN bookings b ON b.reference = p.booking_reference
             WHERE b.sailing_id = ?
               AND b.status <> 'CANCELLED'
               AND (b.status <> 'RESERVED' OR b.reserved_at > ?)
            """;

    private static final String COUNT_VEHICLES_ON = """
            SELECT COUNT(*)
              FROM bookings b
             WHERE b.sailing_id = ?
               AND b.vehicle_registration IS NOT NULL
               AND b.status <> 'CANCELLED'
               AND (b.status <> 'RESERVED' OR b.reserved_at > ?)
            """;

    private static final String UPDATE = """
            UPDATE sailings
               SET departs_from = ?, arrives_at = ?, departs_at = ?,
                   passenger_capacity = ?, vehicle_capacity = ?
             WHERE id = ?
            """;

    private static final String INSERT = """
            INSERT INTO sailings
                (id, departs_from, arrives_at, departs_at, passenger_capacity, vehicle_capacity)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;

    public JdbcSailingRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public Optional<Sailing> findById(String id) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(SELECT_BY_ID)) {
            select.setString(1, id);
            try (ResultSet rows = select.executeQuery()) {
                return rows.next() ? Optional.of(sailingIn(rows)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not read sailing " + id, failure);
        }
    }

    @Override
    public List<SailingAvailability> search(LocalDate date, Terminal from,
                                            LocalDateTime reservationsHeldSince) {
        List<Sailing> leaving = sailingsLeaving(date, from);
        List<SailingAvailability> results = new ArrayList<>();
        for (Sailing sailing : leaving) {
            int passengers = countOn(COUNT_PASSENGERS_ON, sailing.id(), reservationsHeldSince);
            int vehicles = countOn(COUNT_VEHICLES_ON, sailing.id(), reservationsHeldSince);
            results.add(new SailingAvailability(sailing, passengers, vehicles));
        }
        return results;
    }

    @Override
    public void save(Sailing sailing) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(UPDATE)) {
                update.setString(1, sailing.from().name());
                update.setString(2, sailing.to().name());
                update.setTimestamp(3, Timestamp.valueOf(sailing.departsAt()));
                update.setInt(4, sailing.passengerCapacity());
                update.setInt(5, sailing.vehicleCapacity());
                update.setString(6, sailing.id());
                if (update.executeUpdate() > 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
                insert.setString(1, sailing.id());
                insert.setString(2, sailing.from().name());
                insert.setString(3, sailing.to().name());
                insert.setTimestamp(4, Timestamp.valueOf(sailing.departsAt()));
                insert.setInt(5, sailing.passengerCapacity());
                insert.setInt(6, sailing.vehicleCapacity());
                insert.executeUpdate();
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not store sailing " + sailing.id(), failure);
        }
    }

    private List<Sailing> sailingsLeaving(LocalDate date, Terminal from) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(SELECT_BY_DATE_AND_ORIGIN)) {
            select.setString(1, from.name());
            select.setTimestamp(2, Timestamp.valueOf(date.atStartOfDay()));
            select.setTimestamp(3, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
            try (ResultSet rows = select.executeQuery()) {
                List<Sailing> found = new ArrayList<>();
                while (rows.next()) {
                    found.add(sailingIn(rows));
                }
                return found;
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not search sailings on " + date, failure);
        }
    }

    private int countOn(String query, String sailingId, LocalDateTime reservationsHeldSince) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(query)) {
            select.setString(1, sailingId);
            select.setTimestamp(2, Timestamp.valueOf(reservationsHeldSince));
            try (ResultSet rows = select.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("could not count what is booked on " + sailingId, failure);
        }
    }

    private static Sailing sailingIn(ResultSet rows) throws SQLException {
        return new Sailing(
                rows.getString("id"),
                Terminal.valueOf(rows.getString("departs_from")),
                Terminal.valueOf(rows.getString("arrives_at")),
                rows.getTimestamp("departs_at").toLocalDateTime(),
                rows.getInt("passenger_capacity"),
                rows.getInt("vehicle_capacity"));
    }
}
