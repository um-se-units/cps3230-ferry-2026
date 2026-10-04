package mt.edu.um.cps3230.ferry.adapters.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcDataSource;

/** Opens the database the adapters talk to, and puts the schema in place. */
public final class Database {

    private static final String SCHEMA = "/schema.sql";

    private Database() {
    }

    /**
     * A database that lives only as long as the application does.
     *
     * @param name the name to give it
     * @return the data source
     */
    public static DataSource inMemory(String name) {
        return at("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    /**
     * A database kept in a file, so that bookings outlive a restart.
     *
     * @param path where to keep it, without an extension
     * @return the data source
     */
    public static DataSource file(Path path) {
        return at("jdbc:h2:" + path.toAbsolutePath(), "sa", "");
    }

    public static DataSource at(String url, String user, String password) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    /**
     * Runs {@code schema.sql}. Every statement in it is written so that it can
     * run twice, and so that it runs on PostgreSQL as well as on H2.
     *
     * @param dataSource the database to put the schema in
     */
    public static void applySchema(DataSource dataSource) {
        for (String statement : statementsIn(SCHEMA)) {
            try (Connection connection = dataSource.getConnection();
                    Statement sql = connection.createStatement()) {
                sql.execute(statement);
            } catch (SQLException failure) {
                throw new IllegalStateException("could not apply " + SCHEMA, failure);
            }
        }
    }

    private static List<String> statementsIn(String resource) {
        try (InputStream source = Database.class.getResourceAsStream(resource)) {
            if (source == null) {
                throw new IllegalStateException("could not find " + resource + " on the classpath");
            }
            String sql = IOUtils.toString(source, StandardCharsets.UTF_8);
            return Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(statement -> !statement.isEmpty())
                    .toList();
        } catch (IOException failure) {
            throw new UncheckedIOException("could not read " + resource, failure);
        }
    }
}
