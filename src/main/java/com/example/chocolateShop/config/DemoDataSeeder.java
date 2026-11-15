package com.example.chocolateShop.config;

import com.example.chocolateShop.entity.Category;
import com.example.chocolateShop.entity.Coupon;
import com.example.chocolateShop.entity.Product;
import com.example.chocolateShop.entity.ProductImage;
import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.enums.Role;
import com.example.chocolateShop.repository.CategoryRepository;
import com.example.chocolateShop.repository.CouponRepository;
import com.example.chocolateShop.repository.ProductRepository;
import com.example.chocolateShop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Seeds a small, repeatable catalog only when the explicit dev profile is active. */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-admin.email:}")
    private String demoAdminEmail;

    @Value("${app.demo-admin.password:}")
    private String demoAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Development demo data seeder started");
        seedCategories();
        seedProducts();
        seedCoupon();
        seedAdmin();
        log.info("Development demo data seeder completed");
    }

    private void seedAdmin() {
        String email = demoAdminEmail == null ? "" : demoAdminEmail.trim();
        String password = demoAdminPassword == null ? "" : demoAdminPassword;
        if (email.isEmpty() && password.isEmpty()) {
            return;
        }
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Set both DEMO_ADMIN_EMAIL and DEMO_ADMIN_PASSWORD to create a development admin");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("DEMO_ADMIN_PASSWORD must be at least 12 characters");
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        userRepository.save(User.builder()
                .firstName("Demo")
                .lastName("Administrator")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }

    private void seedCategories() {
        List<CategorySeed> categories = List.of(
                new CategorySeed("Signature Boxes", "signature-boxes", "Curated chocolate collections for gifting and sharing."),
                new CategorySeed("Dark Chocolate", "dark-chocolate", "Deep cocoa flavors from smooth to bold."),
                new CategorySeed("Milk Chocolate", "milk-chocolate", "Creamy, comforting chocolate favorites."),
                new CategorySeed("Truffles & Pralines", "truffles-pralines", "Hand-finished bites with rich centers and delicate shells.")
        );

        for (CategorySeed seed : categories) {
            if (categoryRepository.findBySlug(seed.slug()).isEmpty()) {
                categoryRepository.save(Category.builder()
                        .name(seed.name())
                        .slug(seed.slug())
                        .description(seed.description())
                        .build());
            }
        }
    }

    private void seedProducts() {
        List<ProductSeed> products = List.of(
                new ProductSeed("signature-boxes", "The Grand Tasting Box", "grand-tasting-box", "A generous assortment of customer-favorite pralines, caramels, and ganache pieces.", "48.00", "42.00", 24, "https://images.pexels.com/photos/29188958/pexels-photo-29188958.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("signature-boxes", "Little Celebration Box", "little-celebration-box", "A joyful small-format assortment made for thoughtful gifts.", "22.00", null, 36, "https://images.pexels.com/photos/13278153/pexels-photo-13278153.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("signature-boxes", "Cocoa Discovery Set", "cocoa-discovery-set", "Six bars that explore bright, fruity, nutty, and mellow cocoa notes.", "35.00", "31.50", 18, "https://images.pexels.com/photos/30222507/pexels-photo-30222507.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("dark-chocolate", "70% Single Origin Bar", "70-single-origin-bar", "Balanced dark chocolate with red-fruit notes and a clean finish.", "12.00", "10.50", 60, "https://images.pexels.com/photos/4113343/pexels-photo-4113343.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("dark-chocolate", "85% Midnight Bar", "85-midnight-bar", "A slow-melting, intense cocoa bar for dark chocolate lovers.", "13.00", null, 42, "https://images.unsplash.com/photo-1646988075739-2e2c859fd59e?auto=format&fit=crop&w=1200&q=85"),
                new ProductSeed("dark-chocolate", "Sea Salt Almond Bark", "sea-salt-almond-bark", "Roasted almonds and crisp sea salt folded through dark chocolate.", "18.00", "16.00", 28, "https://images.pexels.com/photos/17836527/pexels-photo-17836527.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("milk-chocolate", "Silky Milk Chocolate Bar", "silky-milk-chocolate-bar", "A smooth, mellow bar with toasted milk and caramel notes.", "10.00", null, 55, "https://images.pexels.com/photos/8573993/pexels-photo-8573993.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("milk-chocolate", "Hazelnut Crunch Bar", "hazelnut-crunch-bar", "Creamy milk chocolate with roasted hazelnuts and a delicate crunch.", "14.00", "12.50", 31, "https://images.pexels.com/photos/30222507/pexels-photo-30222507.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("milk-chocolate", "Salted Caramel Squares", "salted-caramel-squares", "Soft caramel centers wrapped in milk chocolate with a touch of salt.", "20.00", null, 22, "https://images.pexels.com/photos/20030860/pexels-photo-20030860.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("truffles-pralines", "Velvet Cocoa Truffles", "velvet-cocoa-truffles", "Dark cocoa-dusted ganache truffles with a soft, velvety center.", "26.00", "23.00", 19, "https://images.pexels.com/photos/19863265/pexels-photo-19863265.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("truffles-pralines", "Pistachio Praline Bites", "pistachio-praline-bites", "Pistachio praline in a crisp chocolate shell.", "24.00", null, 16, "https://images.pexels.com/photos/14275712/pexels-photo-14275712.jpeg?auto=compress&cs=tinysrgb&w=1200"),
                new ProductSeed("truffles-pralines", "Raspberry Ganache Hearts", "raspberry-ganache-hearts", "Bright raspberry ganache inside a glossy dark chocolate shell.", "28.00", "25.00", 14, "https://images.pexels.com/photos/25384566/pexels-photo-25384566.jpeg?auto=compress&cs=tinysrgb&w=1200")
        );

        for (ProductSeed seed : products) {
            Product existing = productRepository.findBySlug(seed.slug()).orElse(null);
            if (existing != null) {
                if (existing.getImages().isEmpty()) {
                    existing.addImage(createImage(seed.imageUrl()));
                } else {
                    ProductImage primary = existing.getImages().stream()
                            .filter(ProductImage::isPrimary)
                            .findFirst()
                            .orElse(existing.getImages().get(0));
                    if (primary.getImageUrl().startsWith("https://placehold.co/")) {
                        primary.setImageUrl(seed.imageUrl());
                    }
                    primary.setPrimary(true);
                }
                continue;
            }
            Category category = categoryRepository.findBySlug(seed.categorySlug())
                    .orElseThrow(() -> new IllegalStateException("Demo category missing: " + seed.categorySlug()));
            Product product = Product.builder()
                    .category(category)
                    .name(seed.name())
                    .slug(seed.slug())
                    .description(seed.description())
                    .price(new BigDecimal(seed.price()))
                    .discountPrice(seed.discountPrice() == null ? null : new BigDecimal(seed.discountPrice()))
                    .stockQuantity(seed.stock())
                    .active(true)
                    .build();
            product.addImage(createImage(seed.imageUrl()));
            productRepository.save(product);
        }

        // Also repair placeholder images on earlier demo rows with non-seed slugs.
        for (Product product : productRepository.findAll()) {
            String replacement = photoForProduct(product.getName());
            for (ProductImage image : product.getImages()) {
                if (image.getImageUrl().toLowerCase().contains("placehold.co")) {
                    image.setImageUrl(replacement);
                }
            }
        }
    }

    private String photoForProduct(String name) {
        String normalizedName = name.toLowerCase();
        if (normalizedName.contains("dark")) {
            return "https://images.pexels.com/photos/4113343/pexels-photo-4113343.jpeg?auto=compress&cs=tinysrgb&w=1200";
        }
        if (normalizedName.contains("milk")) {
            return "https://images.pexels.com/photos/8573993/pexels-photo-8573993.jpeg?auto=compress&cs=tinysrgb&w=1200";
        }
        if (normalizedName.contains("truffle") || normalizedName.contains("praline")) {
            return "https://images.pexels.com/photos/14275712/pexels-photo-14275712.jpeg?auto=compress&cs=tinysrgb&w=1200";
        }
        if (normalizedName.contains("box") || normalizedName.contains("gift")) {
            return "https://images.pexels.com/photos/29188958/pexels-photo-29188958.jpeg?auto=compress&cs=tinysrgb&w=1200";
        }
        return "https://images.pexels.com/photos/30222507/pexels-photo-30222507.jpeg?auto=compress&cs=tinysrgb&w=1200";
    }

    private ProductImage createImage(String imageUrl) {
        return ProductImage.builder()
                .imageUrl(imageUrl)
                .displayOrder(0)
                .primary(true)
                .build();
    }

    private void seedCoupon() {
        if (couponRepository.existsByCodeIgnoreCase("WELCOME10")) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        couponRepository.save(Coupon.builder()
                .code("WELCOME10")
                .discountPercent(new BigDecimal("10.00"))
                .validFrom(now.minusDays(1))
                .validUntil(now.plusYears(10))
                .maxUses(500)
                .timesUsed(0)
                .active(true)
                .build());
    }

    private record CategorySeed(String name, String slug, String description) {}

    private record ProductSeed(
            String categorySlug,
            String name,
            String slug,
            String description,
            String price,
            String discountPrice,
            int stock,
            String imageUrl
    ) {}
}
