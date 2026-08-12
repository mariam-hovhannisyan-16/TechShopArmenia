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
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DropDigitalTwinTablesMigrationTest {

    @Test
    void changelog_DropsBothDigitalTwinTablesWhenTheyAlreadyExistWithData() throws Exception {
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
            connection.createStatement().execute("""
                    CREATE TABLE product_digital_twin (
                        id BIGINT PRIMARY KEY,
                        order_item_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        product_name VARCHAR(200) NOT NULL,
                        purchase_date DATE NOT NULL,
                        warranty_end_date DATE NOT NULL,
                        notes CLOB
                    )
                    """);
            connection.createStatement().execute("""
                    CREATE TABLE digital_twin_repair_entry (
                        id BIGINT PRIMARY KEY,
                        digital_twin_id BIGINT NOT NULL,
                        description VARCHAR(1000) NOT NULL,
                        entry_date DATE NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            connection.createStatement().execute(
                    "ALTER TABLE product_digital_twin ADD CONSTRAINT fk_digital_twin_order_item "
                            + "FOREIGN KEY (order_item_id) REFERENCES order_items(id)");
            connection.createStatement().execute(
                    "ALTER TABLE digital_twin_repair_entry ADD CONSTRAINT fk_repair_entry_digital_twin "
                            + "FOREIGN KEY (digital_twin_id) REFERENCES product_digital_twin(id)");

            connection.createStatement().execute("INSERT INTO orders (id) VALUES (1)");
            connection.createStatement().execute("INSERT INTO order_items (id, order_id) VALUES (10, 1)");
            connection.createStatement().execute(
                    "INSERT INTO product_digital_twin (id, order_item_id, user_id, product_id, product_name, purchase_date, warranty_end_date) "
                            + "VALUES (1, 10, 1, 100, 'Phone', CURRENT_DATE, CURRENT_DATE)");
            connection.createStatement().execute(
                    "INSERT INTO digital_twin_repair_entry (id, digital_twin_id, description, entry_date, created_at) "
                            + "VALUES (1, 1, 'Screen replaced', CURRENT_DATE, CURRENT_TIMESTAMP)");

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM product_digital_twin"),
                    "product_digital_twin should no longer exist");
            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM digital_twin_repair_entry"),
                    "digital_twin_repair_entry should no longer exist");

            try (ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM order_items")) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    void changelog_NoOpsCleanlyOnAFreshDatabaseThatNeverHadTheTables() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM product_digital_twin"));
            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM digital_twin_repair_entry"));
        }
    }

    @Test
    void changelog_RerunIsANoOpOnceTheTablesAreAlreadyDropped() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE product_digital_twin (
                        id BIGINT PRIMARY KEY,
                        order_item_id BIGINT NOT NULL,
                        user_id BIGINT NOT NULL,
                        product_id BIGINT NOT NULL,
                        product_name VARCHAR(200) NOT NULL,
                        purchase_date DATE NOT NULL,
                        warranty_end_date DATE NOT NULL
                    )
                    """);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());
            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM product_digital_twin"));

            liquibase.update(new Contexts());
            assertThrows(SQLException.class, () ->
                    connection.createStatement().executeQuery("SELECT * FROM product_digital_twin"));
        }
    }
}
