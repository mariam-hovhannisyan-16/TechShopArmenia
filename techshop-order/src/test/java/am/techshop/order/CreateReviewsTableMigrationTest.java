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

class CreateReviewsTableMigrationTest {

    @Test
    void changelog_CreatesReviewsTableWithUniqueConstraintAndRequiredColumns() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO reviews (id, product_id, user_id, comment) VALUES (1, 5, 1, 'Great!')"),
                    "rating should be required");

            connection.createStatement().execute(
                    "INSERT INTO reviews (id, product_id, user_id, rating, comment) VALUES (1, 5, 1, 5, 'Great!')");

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO reviews (id, product_id, user_id, rating, comment) VALUES (2, 5, 1, 4, 'Another one')"),
                    "a second review from the same user for the same product should violate the unique constraint");

            connection.createStatement().execute(
                    "INSERT INTO reviews (id, product_id, user_id, rating, comment) VALUES (3, 5, 2, 4, 'Also good')");
            connection.createStatement().execute(
                    "INSERT INTO reviews (id, product_id, user_id, rating, comment) VALUES (4, 6, 1, 3, 'Different product')");
        }
    }

    @Test
    void changelog_RerunIsANoOp() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());
            liquibase.update(new Contexts());
        }
    }
}
