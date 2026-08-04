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
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises db/changelog/db.changelog-master.xml directly (bypassing Spring/JPA),
 * against a database that already has the pre-existing orders/order_items tables
 * (as every real environment does), to confirm changesets 006 and 007 create
 * product_digital_twin and digital_twin_repair_entry correctly, including their
 * foreign keys and required-column constraints.
 */
class DigitalTwinTablesMigrationTest {

    @Test
    void changelog_CreatesDigitalTwinTablesWithForeignKeysAndConstraints() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE orders (
                        id BIGINT PRIMARY KEY
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE order_items (
                        id BIGINT PRIMARY KEY,
                        order_id BIGINT
                    )
                    """);
            connection.createStatement().execute("INSERT INTO orders (id) VALUES (1)");
            connection.createStatement().execute("INSERT INTO order_items (id, order_id) VALUES (10, 1)");

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO product_digital_twin (id, order_item_id, user_id, product_id, purchase_date, warranty_end_date) " +
                                    "VALUES (1, 10, 1, 100, CURRENT_DATE, CURRENT_DATE)"),
                    "product_name should be required");

            connection.createStatement().execute(
                    "INSERT INTO product_digital_twin (id, order_item_id, user_id, product_id, product_name, purchase_date, warranty_end_date) " +
                            "VALUES (1, 10, 1, 100, 'Phone', CURRENT_DATE, CURRENT_DATE)");
            connection.createStatement().execute(
                    "INSERT INTO digital_twin_repair_entry (id, digital_twin_id, description, entry_date, created_at) " +
                            "VALUES (1, 1, 'Screen replaced', CURRENT_DATE, CURRENT_TIMESTAMP)");

            connection.createStatement().execute("DELETE FROM order_items WHERE id = 10");

            var rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM product_digital_twin");
            rs.next();
            org.junit.jupiter.api.Assertions.assertEquals(0, rs.getInt(1),
                    "deleting the order item should cascade-delete its digital twin");

            rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM digital_twin_repair_entry");
            rs.next();
            org.junit.jupiter.api.Assertions.assertEquals(0, rs.getInt(1),
                    "deleting the digital twin should cascade-delete its repair entries");
        }
    }
}
