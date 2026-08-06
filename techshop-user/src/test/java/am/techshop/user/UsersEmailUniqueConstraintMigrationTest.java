package am.techshop.user;

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

/**
 * Exercises db/changelog/db.changelog-master.xml directly (bypassing Spring/JPA)
 * against a table seeded with the exact kind of dirty, pre-existing duplicate data
 * found on the live server, to confirm the migration is safe to run there: it must
 * dedupe first, then be able to add the unique constraint without failing.
 */
class UsersEmailUniqueConstraintMigrationTest {

    @Test
    void changelog_DedupesExistingDuplicatesAndEnforcesUniqueConstraint() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE users (
                        id BIGINT PRIMARY KEY,
                        email VARCHAR(255) NOT NULL
                    )
                    """);

            // Seed the exact scenario found on the live server: several rows sharing
            // one email (from retried/racing registration attempts), plus a normal,
            // never-duplicated row.
            connection.createStatement().execute("INSERT INTO users (id, email) VALUES (5, 'duplicated@test.com')");
            connection.createStatement().execute("INSERT INTO users (id, email) VALUES (6, 'duplicated@test.com')");
            connection.createStatement().execute("INSERT INTO users (id, email) VALUES (8, 'duplicated@test.com')");
            connection.createStatement().execute("INSERT INTO users (id, email) VALUES (12, 'unique@test.com')");

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            try (ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM users WHERE email = 'duplicated@test.com'")) {
                rs.next();
                assertEquals(1, rs.getInt(1), "duplicates should have been reduced to one row");
            }

            try (ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
                rs.next();
                assertEquals(2, rs.getInt(1), "the unrelated, never-duplicated row must be untouched");
            }

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO users (id, email) VALUES (99, 'duplicated@test.com')"),
                    "the unique constraint added by the migration should reject a fresh duplicate");
        }
    }

    @Test
    void changelog_CreatesUsersTableFromScratchOnFreshDatabase() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            connection.createStatement().execute(
                    "INSERT INTO users (id, email, name, password, role, email_verified, created_at) "
                            + "VALUES (1, 'fresh@test.com', 'Fresh User', 'hash', 'CUSTOMER', false, CURRENT_TIMESTAMP)");

            assertThrows(SQLException.class, () ->
                    connection.createStatement().execute(
                            "INSERT INTO users (id, email, name, password, role, email_verified, created_at) "
                                    + "VALUES (2, 'fresh@test.com', 'Dup User', 'hash', 'CUSTOMER', false, CURRENT_TIMESTAMP)"),
                    "the users table created from scratch must still get the unique email constraint");
        }
    }
}
