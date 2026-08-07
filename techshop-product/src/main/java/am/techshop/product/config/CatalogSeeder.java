package am.techshop.product.config;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.product.entity.ColorVariant;
import am.techshop.product.entity.Product;
import am.techshop.product.entity.StorageOption;
import am.techshop.product.mapper.CategoryMapper;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogSeeder implements ApplicationRunner {

    private static final List<String> CATEGORIES =
            List.of("Phones", "Laptops", "TVs", "Audio", "Cameras", "Tablets", "Gaming", "Monitors", "Accessories");

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            CATEGORIES.forEach(name -> categoryRepository.save(categoryMapper.toEntity(name)));
        }

        if (productRepository.count() == 0) {
            Product iphone15 = seedProduct("iPhone 15, 128GB", "Apple iPhone 15 with 128GB storage.",
                    new BigDecimal("450000.00"), 25, "Phones",
                    "https://images.unsplash.com/photo-1736173155811-e8142fd553ee?w=900&q=82&fit=crop&auto=format", true);
            iphone15.setStorageOptions(List.of(
                    storageOption("128GB", "0.00"),
                    storageOption("256GB", "40000.00"),
                    storageOption("512GB", "80000.00")));

            String proDeepBlueUrl = "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/be/d2/bed2ebac62b52c2580a68b9e0d67d995/250915140029572558.webp";
            Product iphone17Pro = seedProduct("iPhone 17 Pro", "Apple iPhone 17 Pro with A19 Pro chip and titanium design.",
                    new BigDecimal("620000.00"), 18, "Phones", proDeepBlueUrl, true);
            applyIphone17ProVariants(iphone17Pro,
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/8a/8a/8a8ae008f8b519511bfc41e8d16b6f81/250915140023667226.webp",
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/a9/6c/a96c19277c05e15cfeeacee84a370ba0/250915140025661602.webp",
                    proDeepBlueUrl);

            String proMaxDeepBlueUrl = "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/41/49/4149198f1713e718ba920b85acffb4f4/250915140038367093.webp";
            Product iphone17ProMax = seedProduct("iPhone 17 Pro Max",
                    "Apple iPhone 17 Pro Max with A19 Pro chip, titanium design, and the largest display in the lineup.",
                    new BigDecimal("690000.00"), 14, "Phones", proMaxDeepBlueUrl, true);
            applyIphone17ProVariants(iphone17ProMax,
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/2d/22/2d22e5e521d6491cf0dd8c0c8a47f2eb/250915140013863146.webp",
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/a3/44/a34423b7b08300fde0625964a0130f66/250915140035204152.webp",
                    proMaxDeepBlueUrl);

            // The rest of the catalog below (60 more products) mirrors techshop-frontend's
            // MOCK_PRODUCTS array (product.ts), backfilled into the real backend via Liquibase
            // changeset 007 for already-seeded databases. 5 of the 65 mock entries share an exact
            // name with a product already seeded above (Samsung Galaxy S24, MacBook Air M2,
            // Sony WH-1000XM5, iPhone 17 Pro, iPhone 17 Pro Max) and are deliberately omitted here
            // too, for the same reason 007 skips them: no duplicate-by-name products, and the
            // iPhone 17 pair keeps its verified per-color photos instead of the mock's stale ones.
            List<Product> products = new ArrayList<>(List.of(
                    iphone15,
                    seedProduct("Samsung Galaxy S24", "Samsung Galaxy S24 with 256GB storage.",
                            new BigDecimal("420000.00"), 30, "Phones",
                            "https://images.unsplash.com/photo-1706372124814-417e2f0c3fe0?w=900&q=82&fit=crop&auto=format", true),
                    iphone17Pro,
                    iphone17ProMax,
                    seedProduct("MacBook Air M2", "Apple MacBook Air with M2 chip, 13-inch.",
                            new BigDecimal("650000.00"), 15, "Laptops",
                            "https://images.unsplash.com/photo-1651241680016-cc9e407e7dc3?w=900&q=82&fit=crop&auto=format", false),
                    seedProduct("LG 55\" 4K Smart TV", "LG 55-inch 4K UHD Smart TV.",
                            new BigDecimal("380000.00"), 10, "TVs",
                            "https://images.unsplash.com/photo-1689686998931-858488b0c62c?w=900&q=82&fit=crop&auto=format", false),
                    seedProduct("Sony WH-1000XM5", "Sony WH-1000XM5 noise-cancelling headphones.",
                            new BigDecimal("165000.00"), 40, "Audio",
                            "https://images.unsplash.com/photo-1612858249816-5a91a9fb9886?w=900&q=82&fit=crop&auto=format", true),
                    seedProduct("Canon EOS R50", "Canon EOS R50 mirrorless camera with kit lens.",
                            new BigDecimal("520000.00"), 8, "Cameras",
                            "https://images.unsplash.com/photo-1500634245200-e5245c7574ef?w=900&q=82&fit=crop&auto=format", false)
            ));

            Product p1001 = seedProduct("iPhone 15 Pro", "Ֆլագման սմարթֆոն 128GB հիշողությամբ",
                    new BigDecimal("650000.00"), 12, "Phones", "https://images.unsplash.com/photo-1616348436168-de43ad0db179?w=900&q=82&fit=crop&auto=format", false);
            p1001.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1001);

            products.add(seedProduct("ASUS ROG Notebook", "Հզոր laptop՝ բարձր արտադրողականությամբ պրոցեսորով",
                    new BigDecimal("760000.00"), 4, "Laptops", "https://images.unsplash.com/photo-1771015310937-6754da25e49a?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("AirPods Pro", "Անլար earbud ականջակալներ",
                    new BigDecimal("115000.00"), 30, "Audio", "https://images.unsplash.com/photo-1606741965429-8d76ff50bb2f?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Samsung 55\" QLED TV", "Հեռուստացույց 4K լուծաչափով",
                    new BigDecimal("410000.00"), 6, "TVs", "https://images.unsplash.com/photo-1615986200762-a1ed9610d3b1?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("JBL Flip Portable Speaker", "Փոխադրելի Bluetooth speaker",
                    new BigDecimal("78000.00"), 10, "Audio", "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Canon EOS Camera", "Ֆոտո camera սիրողականների համար",
                    new BigDecimal("410000.00"), 7, "Cameras", "https://images.unsplash.com/photo-1500634245200-e5245c7574ef?w=900&q=82&fit=crop&auto=format", true));

            products.add(seedProduct("GoPro Hero 12", "Video action camera",
                    new BigDecimal("235000.00"), 15, "Cameras", "https://images.unsplash.com/photo-1604942177421-df466b7410f6?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Samsung 27\" Monitor", "Մոնիտոր՝ QHD լուծաչափով, բարձր թարմացման հաճախությամբ",
                    new BigDecimal("175000.00"), 9, "Monitors", "https://images.unsplash.com/photo-1666771410255-158c17cf8ff8?w=900&q=82&fit=crop&auto=format", false));

            Product p1012 = seedProduct("iPad Air", "Պլանշետ՝ M1 չիպով և Apple Pencil աջակցությամբ",
                    new BigDecimal("420000.00"), 11, "Tablets", "https://images.unsplash.com/photo-1527698266440-12104e498b76?w=900&q=82&fit=crop&auto=format", false);
            p1012.setStorageOptions(List.of(storageOption("128GB", "-50000.00"), storageOption("256GB", "0.00"), storageOption("512GB", "50000.00")));
            products.add(p1012);

            products.add(seedProduct("Apple Magic Keyboard", "Անլար ստեղնաշար աքսեսուար",
                    new BigDecimal("58000.00"), 25, "Accessories", "https://images.unsplash.com/photo-1510674485131-dc88d96369b4?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Sony PlayStation 5", "Խաղային կոնսուլ 825GB կրիչով",
                    new BigDecimal("285000.00"), 8, "Gaming", "https://images.unsplash.com/photo-1607853202273-797f1c22a38e?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Microsoft Xbox Series X", "Խաղային կոնսուլ 1TB կրիչով",
                    new BigDecimal("290000.00"), 6, "Gaming", "https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=900&q=82&fit=crop&auto=format", true));

            products.add(seedProduct("DualSense Wireless Controller", "Անլար խաղային վահանակ՝ հագեցած հպումային արձագանքով",
                    new BigDecimal("42000.00"), 22, "Gaming", "https://images.unsplash.com/photo-1649875951914-61057a5df533?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("HyperX Cloud Gaming Headset", "Ականջակալ՝ մարտկոցի երկար կյանքով և հստակ ձայնով",
                    new BigDecimal("70000.00"), 14, "Audio", "https://images.unsplash.com/photo-1612858249784-5883876e0d52?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("ASUS TUF Gaming Curved Monitor", "Մոնիտոր՝ 165Hz թարմացման հաճախությամբ և կորացած էկրանով",
                    new BigDecimal("270000.00"), 7, "Monitors", "https://images.unsplash.com/photo-1666771410003-8437c4781d49?w=900&q=82&fit=crop&auto=format", false));

            Product p1019 = seedProduct("Google Pixel 8", "Android հեռախոս 128GB հիշողությամբ",
                    new BigDecimal("430000.00"), 10, "Phones", "https://images.unsplash.com/photo-1760604359549-8921b6139a1c?w=900&q=82&fit=crop&auto=format", true);
            p1019.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1019);

            Product p1020 = seedProduct("Xiaomi 14", "Android հեռախոս 256GB հիշողությամբ",
                    new BigDecimal("500000.00"), 9, "Phones", "https://images.unsplash.com/photo-1773414422122-96165e640180?w=900&q=82&fit=crop&auto=format", false);
            p1020.setStorageOptions(List.of(storageOption("128GB", "-50000.00"), storageOption("256GB", "0.00"), storageOption("512GB", "50000.00")));
            products.add(p1020);

            Product p1021 = seedProduct("OnePlus 12", "Արագագործ հեռախոս 256GB հիշողությամբ",
                    new BigDecimal("495000.00"), 7, "Phones", "https://images.unsplash.com/photo-1591337676887-a217a6970a8a?w=900&q=82&fit=crop&auto=format", false);
            p1021.setStorageOptions(List.of(storageOption("128GB", "-50000.00"), storageOption("256GB", "0.00"), storageOption("512GB", "50000.00")));
            products.add(p1021);

            Product p1022 = seedProduct("Samsung Galaxy A55", "Մատչելի Android սմարթֆոն",
                    new BigDecimal("235000.00"), 15, "Phones", "https://images.unsplash.com/photo-1653179767387-35ce2dbdbb5d?w=900&q=82&fit=crop&auto=format", false);
            p1022.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1022);

            products.add(seedProduct("Dell XPS 13", "Կոմպակտ laptop՝ 13\" էկրանով",
                    new BigDecimal("715000.00"), 6, "Laptops", "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Lenovo ThinkPad X1 Carbon", "Բիզնես laptop՝ թեթև կորպուսով",
                    new BigDecimal("840000.00"), 5, "Laptops", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("HP Pavilion 15", "Ամենօրյա օգտագործման laptop",
                    new BigDecimal("305000.00"), 12, "Laptops", "https://images.unsplash.com/photo-1663354027456-ce6a7e07d212?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Acer Aspire 5", "Մատչելի գնով laptop",
                    new BigDecimal("255000.00"), 14, "Laptops", "https://images.unsplash.com/photo-1693206578613-144dd540b892?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("LG OLED55 C3", "OLED հեռուստացույց 4K լուծաչափով",
                    new BigDecimal("680000.00"), 5, "TVs", "https://images.unsplash.com/photo-1689686998931-858488b0c62c?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Sony Bravia 65\" 4K", "Հեռուստացույց Google TV հարթակով",
                    new BigDecimal("700000.00"), 4, "TVs", "https://images.unsplash.com/photo-1461151304267-38535e780c79?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Xiaomi TV A2 50\"", "Հեռուստացույց Android TV հարթակով",
                    new BigDecimal("260000.00"), 9, "TVs", "https://images.unsplash.com/photo-1780042731953-15dd1b3b5f00?w=900&q=82&fit=crop&auto=format", true));

            products.add(seedProduct("TCL 43\" Smart TV", "Մատչելի հեռուստացույց խելացի հնարավորություններով",
                    new BigDecimal("135000.00"), 11, "TVs", "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Samsung Crystal UHD 50\"", "Հեռուստացույց Crystal UHD տեխնոլոգիայով",
                    new BigDecimal("280000.00"), 8, "TVs", "https://images.unsplash.com/photo-1552975084-6e027cd345c2?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Bose QuietComfort Ultra", "Անլար headphone՝ լավագույն աղմուկի մեկուսացումով",
                    new BigDecimal("215000.00"), 10, "Audio", "https://images.unsplash.com/photo-1570132251442-d38a55360c44?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("JBL Tune 760NC", "Ականջակալ՝ ակտիվ աղմուկի մեկուսացումով",
                    new BigDecimal("65000.00"), 25, "Audio", "https://images.unsplash.com/photo-1579065560489-989b0cc394ce?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Beats Studio Pro", "Անլար headphone Apple չիպով",
                    new BigDecimal("165000.00"), 12, "Audio", "https://images.pexels.com/photos/6023144/pexels-photo-6023144.jpeg?auto=compress&cs=tinysrgb&w=900", false));

            products.add(seedProduct("Nintendo Switch OLED", "Խաղային կոնսուլ՝ շարժական և կանգնակի ռեժիմներով",
                    new BigDecimal("205000.00"), 13, "Gaming", "https://images.unsplash.com/photo-1591182136289-67ff16828fd4?w=900&q=82&fit=crop&auto=format", false));

            Product p1036 = seedProduct("Samsung Tab S9", "Պլանշետ S Pen գրիչով",
                    new BigDecimal("400000.00"), 8, "Tablets", "https://images.unsplash.com/photo-1661595677185-06202000a195?w=900&q=82&fit=crop&auto=format", false);
            p1036.setStorageOptions(List.of(storageOption("128GB", "-50000.00"), storageOption("256GB", "0.00"), storageOption("512GB", "50000.00")));
            products.add(p1036);

            Product p1037 = seedProduct("Lenovo Tab P11", "Պլանշետ՝ մեծ մարտկոցով",
                    new BigDecimal("155000.00"), 11, "Tablets", "https://images.unsplash.com/photo-1675109322863-2f4eef9fe032?w=900&q=82&fit=crop&auto=format", false);
            p1037.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1037);

            Product p1038 = seedProduct("Xiaomi Pad 6", "Պլանշետ՝ բարձր արագագործությամբ",
                    new BigDecimal("210000.00"), 9, "Tablets", "https://images.pexels.com/photos/35300031/pexels-photo-35300031.jpeg?auto=compress&cs=tinysrgb&w=900", true);
            p1038.setStorageOptions(List.of(storageOption("128GB", "-50000.00"), storageOption("256GB", "0.00"), storageOption("512GB", "50000.00")));
            products.add(p1038);

            Product p1039 = seedProduct("Huawei MatePad 11", "Պլանշետ՝ ստիլուսի աջակցությամբ",
                    new BigDecimal("185000.00"), 7, "Tablets", "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=900&q=82&fit=crop&auto=format", false);
            p1039.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1039);

            Product p1040 = seedProduct("Microsoft Surface Go 3", "Պլանշետ՝ հանվող ստեղնաշարի աջակցությամբ",
                    new BigDecimal("250000.00"), 6, "Tablets", "https://images.unsplash.com/photo-1617780421749-ebd0ef657b2e?w=900&q=82&fit=crop&auto=format", false);
            p1040.setStorageOptions(List.of(storageOption("128GB", "0.00"), storageOption("256GB", "50000.00"), storageOption("512GB", "100000.00")));
            products.add(p1040);

            products.add(seedProduct("Logitech MX Master 3S", "Անլար mouse՝ բարձր ճշգրտությամբ",
                    new BigDecimal("58000.00"), 20, "Accessories", "https://images.unsplash.com/photo-1625750435936-f97e1748410b?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Anker 20W Charger", "Արագ charger՝ 20W հզորությամբ",
                    new BigDecimal("14000.00"), 40, "Accessories", "https://images.unsplash.com/photo-1705147290571-dde8fb73cf98?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("USB-C to USB-C Cable", "Cable՝ արագ լիցքավորման և տվյալների փոխանցման համար",
                    new BigDecimal("8000.00"), 60, "Accessories", "https://images.unsplash.com/photo-1619459072761-496c0812331b?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Protective Sleeve Case 15\"", "Պաշտպանիչ պատյան 15 դյույմանոց սարքերի համար",
                    new BigDecimal("18000.00"), 22, "Accessories", "https://images.unsplash.com/photo-1657603571233-5e9860e96d00?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Wireless Charger Pad", "Անլար charger՝ Qi տեխնոլոգիայով",
                    new BigDecimal("17000.00"), 30, "Accessories", "https://images.unsplash.com/photo-1591290619618-904f6dd935e3?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Sony Alpha a6400", "Հայելային camera լուսանկարիչների համար",
                    new BigDecimal("510000.00"), 5, "Cameras", "https://images.unsplash.com/photo-1722842179244-b4a90215d04e?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Sonos Amp", "Ուժեղացուցիչ՝ խելացի ձայնային համակարգի համար",
                    new BigDecimal("380000.00"), 6, "Audio", "https://images.unsplash.com/photo-1743521442683-08ffd8ac9e14?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Apple iPhone 14", "Հզոր սմարթֆոն 128GB հիշողությամբ",
                    new BigDecimal("480000.00"), 11, "Phones", "https://images.unsplash.com/photo-1574755393849-623942496936?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Huawei P60 Pro", "Հզոր հեռախոս գերժամանակակից տեսախցիկով",
                    new BigDecimal("470000.00"), 6, "Phones", "https://images.unsplash.com/photo-1546706887-a24528987a75?w=900&q=82&fit=crop&auto=format", true));

            products.add(seedProduct("MacBook Pro M3", "Հզոր laptop՝ 14\" Liquid Retina XDR էկրանով",
                    new BigDecimal("1150000.00"), 4, "Laptops", "https://images.unsplash.com/photo-1629131726692-1accd0c53ce0?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("ASUS Zenbook 14", "Նրբագեղ laptop՝ թեթև և հզոր",
                    new BigDecimal("455000.00"), 10, "Laptops", "https://images.unsplash.com/photo-1693206816304-642a705045a1?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Hisense 55\" ULED TV", "Հեռուստացույց ULED տեխնոլոգիայով",
                    new BigDecimal("340000.00"), 8, "TVs", "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Panasonic 43\" 4K TV", "Մատչելի 4K հեռուստացույց",
                    new BigDecimal("195000.00"), 10, "TVs", "https://images.unsplash.com/photo-1567690187548-f07b1d7bf5a9?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Sennheiser Momentum 4", "Անլար headphone բարձրորակ ձայնով",
                    new BigDecimal("185000.00"), 9, "Audio", "https://images.unsplash.com/photo-1754142654796-cafbcb9f633b?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Samsung Buds2 Pro", "Անլար earbud ականջակալներ",
                    new BigDecimal("95000.00"), 18, "Audio", "https://images.pexels.com/photos/19320592/pexels-photo-19320592.jpeg?auto=compress&cs=tinysrgb&w=900", false));

            products.add(seedProduct("Xbox Wireless Controller", "Անլար խաղային վահանակ Xbox կոնսուլների համար",
                    new BigDecimal("39000.00"), 24, "Gaming", "https://images.unsplash.com/photo-1615556626922-68da0c9f4350?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Steam Deck", "Խաղային կոնսուլ՝ ներկառուցված էկրանով",
                    new BigDecimal("340000.00"), 5, "Gaming", "https://images.pexels.com/photos/12670693/pexels-photo-12670693.jpeg?auto=compress&cs=tinysrgb&w=900", true));

            products.add(seedProduct("Apple iPad 10th Gen", "Պլանշետ՝ A14 չիպով",
                    new BigDecimal("245000.00"), 13, "Tablets", "https://images.unsplash.com/photo-1659748097081-3d2c4c92fec7?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Amazon Fire HD 10", "Պլանշետ՝ մատչելի գնով",
                    new BigDecimal("95000.00"), 20, "Tablets", "https://images.unsplash.com/photo-1629131704989-c74179b0ce16?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Logitech G502 Mouse", "Ճշգրիտ mouse՝ բազմաթիվ ծրագրավորվող կոճակներով",
                    new BigDecimal("46000.00"), 26, "Accessories", "https://images.unsplash.com/photo-1762180463317-b5e7886b38c1?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("Anker Power Bank 10000mAh", "Շարժական charger՝ 10000mAh հզորությամբ",
                    new BigDecimal("23000.00"), 35, "Accessories", "https://images.unsplash.com/photo-1706275399494-fb26bbc5da63?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("MacBook Air M1", "Թեթև laptop՝ 13\" էկրանով, մատչելի Apple chip-ով",
                    new BigDecimal("730000.00"), 8, "Laptops", "https://images.unsplash.com/photo-1484788984921-03950022c9ef?w=900&q=82&fit=crop&auto=format", false));

            products.add(seedProduct("MacBook Pro M4", "Հզոր laptop՝ 14\" Liquid Retina XDR էկրանով, M4 սերնդի չիպով",
                    new BigDecimal("1280000.00"), 4, "Laptops", "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=900&q=82&fit=crop&auto=format", true));

            productRepository.saveAll(products);
        }
    }

    private Product seedProduct(String name, String description, BigDecimal price, int stock,
                                 String category, String imageUrl, boolean isNew) {
        return productMapper.toEntity(new ProductRequest(name, description, price, stock, category, imageUrl, isNew));
    }

    private void applyIphone17ProVariants(Product product, String cosmicOrangeImageUrl, String silverImageUrl, String deepBlueImageUrl) {
        product.setStorageOptions(List.of(
                storageOption("256GB", "0.00"),
                storageOption("512GB", "50000.00"),
                storageOption("1TB", "100000.00")));
        product.setSimOptions(List.of("Dual eSIM", "Nano-SIM & eSIM"));
        product.setColorVariants(List.of(
                colorVariant("Cosmic Orange", cosmicOrangeImageUrl),
                colorVariant("Silver", silverImageUrl),
                colorVariant("Deep Blue", deepBlueImageUrl)));
    }

    private StorageOption storageOption(String label, String priceDelta) {
        return new StorageOption(label, new BigDecimal(priceDelta));
    }

    private ColorVariant colorVariant(String label, String imageUrl) {
        return new ColorVariant(label, imageUrl);
    }
}
