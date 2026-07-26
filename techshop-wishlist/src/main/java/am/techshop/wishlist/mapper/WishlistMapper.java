package am.techshop.wishlist.mapper;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.WishlistItemResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.wishlist.entity.Wishlist;
import am.techshop.wishlist.entity.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WishlistMapper {

    @Mapping(target = "id", source = "item.id")
    WishlistItemResponse toItemResponse(WishlistItem item, ProductResponse product);

    @Mapping(target = "id", source = "wishlist.id")
    @Mapping(target = "userId", source = "wishlist.userId")
    @Mapping(target = "createdAt", source = "wishlist.createdAt")
    @Mapping(target = "items", source = "items")
    WishlistResponse toResponse(Wishlist wishlist, List<WishlistItemResponse> items);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Wishlist toEntity(Long userId);

    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "wishlist", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    WishlistItem toItem(Long productId);
}
