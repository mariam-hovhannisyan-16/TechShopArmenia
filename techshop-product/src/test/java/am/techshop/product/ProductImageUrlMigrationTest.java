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
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductImageUrlMigrationTest {

    @Test
    void changelog_RewritesEachSeededProductsImageUrlToAnAbsoluteStockPhotoUrl() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            connection.createStatement().execute("""
                    CREATE TABLE products (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255),
                        is_new BOOLEAN,
                        stock INT,
                        image_url VARCHAR(500)
                    )
                    """);
            connection.createStatement().execute(
                    "INSERT INTO products (id, name, image_url) VALUES " +
                            "(1, 'iPhone 15, 128GB', '/images/products/iphone-15.jpg')," +
                            "(2, 'Samsung Galaxy S24', '/images/products/galaxy-s24.jpg')," +
                            "(3, 'MacBook Air M2', '/images/products/macbook-air-m2.jpg')," +
                            "(4, 'LG 55\" 4K Smart TV', '/images/products/lg-55-4k.jpg')," +
                            "(5, 'Sony WH-1000XM5', '/images/products/sony-wh1000xm5.jpg')," +
                            "(6, 'Canon EOS R50', '/images/products/canon-eos-r50.jpg')");

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts());

            assertImageUrl(connection, "iPhone 15, 128GB",
                    "https://images.unsplash.com/photo-1736173155811-e8142fd553ee?w=900&q=82&fit=crop&auto=format");
            assertImageUrl(connection, "Samsung Galaxy S24",
                    "https://images.unsplash.com/photo-1706372124814-417e2f0c3fe0?w=900&q=82&fit=crop&auto=format");
            assertImageUrl(connection, "MacBook Air M2",
                    "https://images.unsplash.com/photo-1651241680016-cc9e407e7dc3?w=900&q=82&fit=crop&auto=format");
            assertImageUrl(connection, "LG 55\" 4K Smart TV",
                    "https://images.unsplash.com/photo-1689686998931-858488b0c62c?w=900&q=82&fit=crop&auto=format");
            assertImageUrl(connection, "Sony WH-1000XM5",
                    "https://images.unsplash.com/photo-1612858249816-5a91a9fb9886?w=900&q=82&fit=crop&auto=format");
            assertImageUrl(connection, "Canon EOS R50",
                    "https://images.unsplash.com/photo-1500634245200-e5245c7574ef?w=900&q=82&fit=crop&auto=format");
        }
    }

    private void assertImageUrl(Connection connection, String productName, String expectedUrl) throws Exception {
        try (ResultSet rs = connection.createStatement().executeQuery(
                "SELECT image_url FROM products WHERE name = '" + productName.replace("'", "''") + "'")) {
            assertEquals(true, rs.next(), "product " + productName + " should exist");
            assertEquals(expectedUrl, rs.getString(1));
        }
    }
}
