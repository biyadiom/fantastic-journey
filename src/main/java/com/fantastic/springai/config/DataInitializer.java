package com.fantastic.springai.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fantastic.springai.model.ActivityLog;
import com.fantastic.springai.model.Address;
import com.fantastic.springai.model.Category;
import com.fantastic.springai.model.Country;
import com.fantastic.springai.model.OrderItem;
import com.fantastic.springai.model.OrderStatus;
import com.fantastic.springai.model.Payment;
import com.fantastic.springai.model.PaymentMethod;
import com.fantastic.springai.model.PaymentStatus;
import com.fantastic.springai.model.Product;
import com.fantastic.springai.model.Review;
import com.fantastic.springai.model.Seller;
import com.fantastic.springai.model.ShopOrder;
import com.fantastic.springai.model.Tag;
import com.fantastic.springai.model.User;
import com.fantastic.springai.repository.ActivityLogRepository;
import com.fantastic.springai.repository.AddressRepository;
import com.fantastic.springai.repository.CategoryRepository;
import com.fantastic.springai.repository.CountryRepository;
import com.fantastic.springai.repository.OrderRepository;
import com.fantastic.springai.repository.PaymentRepository;
import com.fantastic.springai.repository.ProductRepository;
import com.fantastic.springai.repository.ReviewRepository;
import com.fantastic.springai.repository.SellerRepository;
import com.fantastic.springai.repository.TagRepository;
import com.fantastic.springai.repository.UserRepository;

/**
 * Données de démo pour le profil {@code dev}. Ne s'exécute que si la base est vide (aucun pays).
 */
@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private final CountryRepository countryRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SellerRepository sellerRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final ActivityLogRepository activityLogRepository;

    public DataInitializer(
            CountryRepository countryRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            SellerRepository sellerRepository,
            AddressRepository addressRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ReviewRepository reviewRepository,
            ActivityLogRepository activityLogRepository) {
        this.countryRepository = countryRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.sellerRepository = sellerRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    private void seed() {
        if (countryRepository.count() > 0) {
            return;
        }

        Country fr = countryRepository.save(country("FR", "France", "EUR"));
        Country us = countryRepository.save(country("US", "United States", "USD"));
        countryRepository.save(country("DE", "Germany", "EUR"));

        Category catElec = categoryRepository.save(category(null, "Électronique", "electronique"));
        Category catCloth = categoryRepository.save(category(null, "Vêtements", "vetements"));
        Category catBooks = categoryRepository.save(category(null, "Livres", "livres"));
        Category catPhones = categoryRepository.save(category(catElec, "Smartphones", "smartphones"));
        categoryRepository.save(category(catCloth, "Chaussures", "chaussures"));

        List<Tag> tags = List.of(
                tag("promo"),
                tag("nouveau"),
                tag("bio"),
                tag("bestseller"),
                tag("fragile"),
                tag("digital"));
        tags.forEach(tagRepository::save);

        User u1 = userRepository.save(user("alice@example.com", "alice", "Alice", "Martin", fr, true, true));
        User u2 = userRepository.save(user("bob@example.com", "bob", "Bob", "Durand", fr, true, false));
        User u3 = userRepository.save(user("carol@example.com", "carol", "Carol", "Bernard", us, true, true));
        User u4 = userRepository.save(user("dan@example.com", "dan", "Dan", "Petit", deCountry(), true, false));
        User u5 = userRepository.save(user("eve@example.com", "eve", "Eve", "Roux", fr, true, true));
        User u6 = userRepository.save(user("frank@example.com", "frank", "Frank", "Lopez", us, true, false));
        User u7 = userRepository.save(user("grace@example.com", "grace", "Grace", "Kim", fr, true, false));
        User u8 = userRepository.save(user("henry@example.com", "henry", "Henry", "Wang", us, true, false));

        Seller s1 = sellerRepository.save(seller(u1, "TechNova", "High-tech certifié", true));
        Seller s2 = sellerRepository.save(seller(u3, "ModeZen", "Mode durable", true));
        Seller s3 = sellerRepository.save(seller(u5, "BookHaven", "Livres neufs & occasion", false));

        addressRepository.save(address(u1, fr, "Domicile", "10 rue de Paris", "Paris", "75001", true));
        addressRepository.save(address(u2, fr, "Bureau", "2 av. Victor Hugo", "Lyon", "69001", true));
        addressRepository.save(address(u3, us, "Home", "400 Market St", "San Francisco", "94105", true));
        addressRepository.save(address(u4, deCountry(), "Zuhause", "Hauptstr. 5", "Berlin", "10115", true));
        addressRepository.save(address(u6, us, "Home", "88 Broadway", "New York", "10005", false));

        LocalDateTime now = LocalDateTime.now();

        Product p1 = product(s1, catPhones, "PHONE-X1", "Smartphone X1", new BigDecimal("599.00"), 40, now);
        Product p2 = product(s1, catElec, "TAB-A7", "Tablette A7", new BigDecimal("329.99"), 25, now);
        Product p3 = product(s1, catElec, "NC-USB", "Chargeur USB-C", new BigDecimal("19.90"), 200, now);
        Product p4 = product(s2, catCloth, "TEE-ORG", "T-shirt bio", new BigDecimal("29.00"), 80, now);
        Product p5 = product(s2, catCloth, "JEAN-SL", "Jean slim", new BigDecimal("79.99"), 50, now);
        Product p6 = product(s2, catCloth, "VEST-W", "Veste laine", new BigDecimal("149.00"), 15, now);
        Product p7 = product(s3, catBooks, "BK-JAVA", "Java efficace", new BigDecimal("42.00"), 100, now);
        Product p8 = product(s3, catBooks, "BK-SPRING", "Spring Boot en action", new BigDecimal("48.50"), 60, now);
        Product p9 = product(s3, catBooks, "BK-DB", "PostgreSQL pratique", new BigDecimal("39.90"), 45, now);
        Product p10 = product(s1, catElec, "BT-SPK", "Enceinte Bluetooth", new BigDecimal("89.00"), 35, now);
        Product p11 = product(s2, catCloth, "SNK-RUN", "Baskets running", new BigDecimal("119.00"), 22, now);
        Product p12 = product(s1, catPhones, "CASE-X1", "Coque Smartphone X1", new BigDecimal("15.00"), 150, now);
        Product p13 = product(s3, catBooks, "BK-ML", "Introduction ML", new BigDecimal("55.00"), 30, now);
        Product p14 = product(s2, catCloth, "HAT-W", "Bonnet laine", new BigDecimal("24.00"), 70, now);
        Product p15 = product(s1, catElec, "WEBCAM-HD", "Webcam HD", new BigDecimal("69.00"), 18, now);

        List<Product> products = List.of(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15);
        tagProduct(products.get(0), tags.get(1), tags.get(3));
        tagProduct(products.get(1), tags.get(3));
        tagProduct(products.get(3), tags.get(2), tags.get(4));
        tagProduct(products.get(6), tags.get(3));
        tagProduct(products.get(9), tags.get(0));
        products.forEach(productRepository::save);

        List<User> buyers = List.of(u2, u4, u6, u7, u8, u1);
        List<OrderStatus> statuses = List.of(
                OrderStatus.delivered, OrderStatus.shipped, OrderStatus.confirmed,
                OrderStatus.pending, OrderStatus.cancelled, OrderStatus.refunded,
                OrderStatus.delivered, OrderStatus.delivered, OrderStatus.shipped,
                OrderStatus.confirmed, OrderStatus.delivered, OrderStatus.pending,
                OrderStatus.delivered, OrderStatus.shipped, OrderStatus.confirmed,
                OrderStatus.delivered, OrderStatus.delivered, OrderStatus.shipped,
                OrderStatus.delivered, OrderStatus.confirmed);

        for (int i = 0; i < 20; i++) {
            User buyer = buyers.get(i % buyers.size());
            LocalDateTime created = now.minusMonths(i % 6).minusDays(i * 3L);
            ShopOrder ord = new ShopOrder();
            ord.setUser(buyer);
            ord.setAddress(addressRepository.findAll().stream().filter(a -> a.getUser().getId().equals(buyer.getId())).findFirst().orElse(null));
            ord.setStatus(statuses.get(i));
            ord.setCurrency("EUR");
            ord.setNotes(i % 5 == 0 ? "Livraison rapide souhaitée" : null);
            ord.setCreatedAt(created);
            ord.setUpdatedAt(created.plusHours(2));

            Product a = products.get(i % products.size());
            Product b = products.get((i + 3) % products.size());
            OrderItem oi1 = new OrderItem();
            oi1.setProduct(a);
            oi1.setQuantity(1 + (i % 3));
            oi1.setUnitPrice(a.getBasePrice());
            oi1.setDiscount(BigDecimal.ZERO);
            OrderItem oi2 = new OrderItem();
            oi2.setProduct(b);
            oi2.setQuantity(1);
            oi2.setUnitPrice(b.getBasePrice());
            oi2.setDiscount(new BigDecimal("5.00"));

            BigDecimal total = line(oi1).add(line(oi2));
            ord.setTotalAmount(total);
            oi1.setOrder(ord);
            oi2.setOrder(ord);
            ord.setOrderItems(List.of(oi1, oi2));
            orderRepository.save(ord);

            if (ord.getStatus() != OrderStatus.cancelled && ord.getStatus() != OrderStatus.refunded) {
                Payment pay = new Payment();
                pay.setOrder(ord);
                pay.setMethod(i % 2 == 0 ? PaymentMethod.card : PaymentMethod.paypal);
                pay.setStatus(ord.getStatus() == OrderStatus.pending ? PaymentStatus.pending : PaymentStatus.completed);
                pay.setAmount(total);
                pay.setPaidAt(ord.getStatus() == OrderStatus.pending ? null : created.plusDays(1));
                paymentRepository.save(pay);
            }
        }

        reviewRepository.save(review(p1, u2, (short) 5, "Excellent", "Très satisfait"));
        reviewRepository.save(review(p1, u4, (short) 4, "Bien", "Bon rapport qualité prix"));
        reviewRepository.save(review(p7, u6, (short) 5, "Top", "Clair et complet"));
        reviewRepository.save(review(p8, u7, (short) 3, "Correct", "Quelques coquilles"));
        reviewRepository.save(review(p4, u8, (short) 4, "Doux", "Taille conforme"));
        reviewRepository.save(review(p10, u2, (short) 5, "Son nickel", null));
        reviewRepository.save(review(p11, u4, (short) 2, "Serré", "Pointure petite"));
        reviewRepository.save(review(p2, u6, (short) 4, "Léger", "Bonne autonomie"));
        reviewRepository.save(review(p5, u7, (short) 5, "Parfait", "Coupe au top"));
        reviewRepository.save(review(p9, u8, (short) 4, "Utile", "Bons exemples SQL"));

        activityLogRepository.save(log(u1, "LOGIN", "session", null, "203.0.113.1"));
        activityLogRepository.save(log(u2, "VIEW_PRODUCT", "product", p1.getId(), "203.0.113.2"));
        activityLogRepository.save(log(u3, "PLACE_ORDER", "order", null, "198.51.100.3"));
        activityLogRepository.save(log(u4, "UPDATE_PROFILE", "user", u4.getId(), "192.0.2.4"));
        activityLogRepository.save(log(u5, "LOGIN", "session", null, "203.0.113.5"));
    }

    private Country deCountry() {
        return countryRepository.findAll().stream().filter(c -> "DE".equals(c.getCode())).findFirst().orElseThrow();
    }

    private static BigDecimal line(OrderItem oi) {
        return oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
    }

    private void tagProduct(Product p, Tag... t) {
        for (Tag tag : t) {
            p.getTags().add(tag);
            tag.getProducts().add(p);
        }
    }

    private static Country country(String code, String name, String currency) {
        Country c = new Country();
        c.setCode(code);
        c.setName(name);
        c.setCurrency(currency);
        return c;
    }

    private static Category category(Category parent, String name, String slug) {
        Category c = new Category();
        c.setParent(parent);
        c.setName(name);
        c.setSlug(slug);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private static Tag tag(String name) {
        Tag t = new Tag();
        t.setName(name);
        return t;
    }

    private static User user(String email, String username, String fn, String ln, Country country, boolean active, boolean seller) {
        LocalDateTime now = LocalDateTime.now();
        User u = new User();
        u.setEmail(email);
        u.setUsername(username);
        u.setPasswordHash("{noop}changeme");
        u.setFirstName(fn);
        u.setLastName(ln);
        u.setBirthDate(LocalDate.of(1990, 1, 15));
        u.setCountry(country);
        u.setActive(active);
        u.setSeller(seller);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return u;
    }

    private static Seller seller(User user, String store, String desc, boolean verified) {
        Seller s = new Seller();
        s.setUser(user);
        s.setStoreName(store);
        s.setDescription(desc);
        s.setRating(new BigDecimal("4.50"));
        s.setTotalSales(0);
        s.setVerified(verified);
        s.setJoinedAt(LocalDateTime.now());
        return s;
    }

    private static Address address(User user, Country country, String label, String street, String city, String postal, boolean def) {
        Address a = new Address();
        a.setUser(user);
        a.setCountry(country);
        a.setLabel(label);
        a.setStreet(street);
        a.setCity(city);
        a.setPostalCode(postal);
        a.setDefaultAddress(def);
        return a;
    }

    private static Product sellerProduct(Seller seller, Category cat, String sku, String name, BigDecimal price, int stock, LocalDateTime now) {
        Product p = new Product();
        p.setSeller(seller);
        p.setCategory(cat);
        p.setSku(sku);
        p.setName(name);
        p.setDescription("Description pour " + name);
        p.setBasePrice(price);
        p.setStock(stock);
        p.setWeightKg(new BigDecimal("0.500"));
        p.setActive(true);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    private Product product(Seller seller, Category cat, String sku, String name, BigDecimal price, int stock, LocalDateTime now) {
        return productRepository.save(sellerProduct(seller, cat, sku, name, price, stock, now));
    }

    private static Review review(Product p, User u, short rating, String title, String body) {
        Review r = new Review();
        r.setProduct(p);
        r.setUser(u);
        r.setRating(rating);
        r.setTitle(title);
        r.setBody(body);
        r.setVerified(true);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private static ActivityLog log(User u, String action, String entity, Long entityId, String ip) {
        ActivityLog l = new ActivityLog();
        l.setUser(u);
        l.setAction(action);
        l.setEntity(entity);
        l.setEntityId(entityId);
        l.setIpAddress(ip);
        l.setUserAgent("Mozilla/5.0");
        l.setLoggedAt(LocalDateTime.now());
        return l;
    }
}
