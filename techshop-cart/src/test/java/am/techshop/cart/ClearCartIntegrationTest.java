package am.techshop.cart;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.cart.repository.CartRepository;
import am.techshop.cart.service.CartService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ClearCartIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        cartRepository.deleteAll();
    }

    @Test
    void clearCart_PersistsEmptyItemsToDatabase() {
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        CartItem item1 = new CartItem();
        item1.setProductId(1L);
        item1.setProductName("Phone");
        item1.setProductPrice(BigDecimal.valueOf(100));
        item1.setQuantity(2);
        item1.setCart(cart);
        CartItem item2 = new CartItem();
        item2.setProductId(2L);
        item2.setProductName("Laptop");
        item2.setProductPrice(BigDecimal.valueOf(500));
        item2.setQuantity(1);
        item2.setCart(cart);
        cart.getItems().add(item1);
        cart.getItems().add(item2);
        cartRepository.save(cart);

        cartService.clearCart(USER_ID);

        entityManager.flush();
        entityManager.clear();

        Optional<Cart> reloaded = cartRepository.findByUserId(USER_ID);

        assertTrue(reloaded.isPresent());
        assertTrue(reloaded.get().getItems().isEmpty(),
                "Cart items should be empty after clearCart, but the database still has: " + reloaded.get().getItems());
    }

    @Test
    void clearCart_WhenCartHasNoItems_StaysEmptyWithoutError() {
        Cart cart = new Cart();
        cart.setUserId(USER_ID);
        cartRepository.save(cart);

        cartService.clearCart(USER_ID);
        entityManager.flush();
        entityManager.clear();

        Optional<Cart> reloaded = cartRepository.findByUserId(USER_ID);
        assertTrue(reloaded.isPresent());
        assertTrue(reloaded.get().getItems().isEmpty());
    }

    @Test
    void clearCart_WhenCartDoesNotExist_DoesNothing() {
        cartService.clearCart(999L);
        assertTrue(cartRepository.findByUserId(999L).isEmpty());
    }
}
