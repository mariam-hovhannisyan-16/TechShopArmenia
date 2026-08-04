package am.techshop.product;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises db/changelog/db.changelog-master.xml directly (bypassing Spring/JPA) to
 * confirm the price_history changeset actually creates a usable table: correct
 * required-column constraints, and that it doesn't conflict with the pre-existing
 * products/categories changesets already in this changelog.
 */
class PriceHistoryTableMigrationTest {

    @Test
    void changelog_CreatesPriceHistoryTableWithExpectedColumns() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        is_new BOOLEAN,
                        stock INT
                    )
                    """);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            connection.createStatement().execute(
                    "INSERT INTO price_history (id, product_id, new_price, changed_at) " +
                            "VALUES (1, 42, 90.00, CURRENT_TIMESTAMP)");

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO price_history (id, product_id, changed_at) " +
                                    "VALUES (2, 42, CURRENT_TIMESTAMP)"),
                    "new_price should be required");
        }
    }
}
