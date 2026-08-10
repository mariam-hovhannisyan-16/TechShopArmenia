package am.techshop.order;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises db/changelog/db.changelog-master.xml directly (bypassing Spring/JPA), confirming
 * changeset 009 adds orders.language and backfills it correctly in both scenarios that matter
 * for a real rollout: a fresh database (the full changelog runs end-to-end, including 001
 * creating `orders` itself) and an already-migrated database that has real order rows with no
 * `language` column yet (matching current production before this migration ships) - those rows
 * must come out of the ADD COLUMN as 'HY', the same fallback the frontend's LanguageService
 * uses, not NULL.
 */
class AddLanguageToOrdersMigrationTest {

    @Test
    void changelog_OnFreshDatabase_CreatesOrdersWithLanguageColumnDefaultingToHy() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            connection.createStatement().execute("""
                    INSERT INTO orders (id, user_id, total_price, status,
                        shipping_full_name, shipping_phone, shipping_line1, shipping_city, shipping_postal_code, shipping_country,
                        billing_full_name, billing_phone, billing_line1, billing_city, billing_postal_code, billing_country)
                    VALUES (1, 1, 100.00, 'PENDING',
                        'Mariam', '+374000000', '1 Main St', 'Yerevan', '0001', 'Armenia',
                        'Mariam', '+374000000', '1 Main St', 'Yerevan', '0001', 'Armenia')
                    """);

            assertEquals("HY", languageOf(connection, 1L));

            connection.createStatement().execute("UPDATE orders SET language = 'RU' WHERE id = 1");
            assertEquals("RU", languageOf(connection, 1L));
        }
    }

    @Test
    void changelog_OnAlreadyMigratedDatabaseWithExistingOrders_BackfillsLanguageToHy() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        total_price DECIMAL(19,2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        notes VARCHAR(500)
                    )
                    """);
            connection.createStatement().execute(
                    "INSERT INTO orders (id, user_id, total_price, status) VALUES (1, 1, 250.00, 'PAID')");
            connection.createStatement().execute(
                    "INSERT INTO orders (id, user_id, total_price, status) VALUES (2, 2, 90.00, 'DELIVERED')");

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertEquals("HY", languageOf(connection, 1L));
            assertEquals("HY", languageOf(connection, 2L));

            // Re-running the changelog is a no-op: no error, no change to already-set values.
            connection.createStatement().execute("UPDATE orders SET language = 'EN' WHERE id = 1");
            liquibase.update(new Contexts());
            assertEquals("EN", languageOf(connection, 1L));
        }
    }

    private String languageOf(Connection connection, Long orderId) throws Exception {
        try (ResultSet rs = connection.createStatement().executeQuery(
                "SELECT language FROM orders WHERE id = " + orderId)) {
            assertEquals(true, rs.next(), "order " + orderId + " should exist");
            return rs.getString(1);
        }
    }
}
