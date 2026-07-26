package am.techshop.wishlist.repository;

import am.techshop.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    @Query("SELECT COUNT(i) FROM Wishlist w JOIN w.items i WHERE w.userId = :userId")
    long countItemsByUserId(@Param("userId") Long userId);

    @Query("SELECT w.userId FROM Wishlist w JOIN w.items i WHERE i.productId = :productId")
    List<Long> findUserIdsByProductId(@Param("productId") Long productId);
}
