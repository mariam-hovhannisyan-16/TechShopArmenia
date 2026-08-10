package am.techshop.product.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String category;
    private String imageUrl;

    private boolean isNew = false;

    private Integer discountPercentage;

    private BigDecimal rating = BigDecimal.ZERO;

    private Integer reviewCount = 0;

    @ElementCollection
    @CollectionTable(name = "product_storage_options", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    private List<StorageOption> storageOptions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_sim_options", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "sim_value")
    private List<String> simOptions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_color_variants", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    private List<ColorVariant> colorVariants = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;
}
