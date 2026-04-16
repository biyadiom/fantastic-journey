package com.fantastic.springai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fantastic.springai.dto.CategoryStatsDto;
import com.fantastic.springai.dto.CountrySummaryDto;
import com.fantastic.springai.dto.OrderDetailDto;
import com.fantastic.springai.dto.OrderItemDto;
import com.fantastic.springai.dto.OrderSummaryDto;
import com.fantastic.springai.dto.PaymentDto;
import com.fantastic.springai.dto.PaymentStatsDto;
import com.fantastic.springai.dto.ProductSalesDto;
import com.fantastic.springai.dto.ProductSummaryDto;
import com.fantastic.springai.dto.RevenueStatsDto;
import com.fantastic.springai.dto.ReviewStatsDto;
import com.fantastic.springai.dto.SellerStatsDto;
import com.fantastic.springai.dto.UserCountByCountryDto;
import com.fantastic.springai.dto.UserOrderStatsDto;
import com.fantastic.springai.dto.UserSummaryDto;
import com.fantastic.springai.model.Address;
import com.fantastic.springai.model.Category;
import com.fantastic.springai.model.Country;
import com.fantastic.springai.model.OrderItem;
import com.fantastic.springai.model.OrderStatus;
import com.fantastic.springai.model.PaymentStatus;
import com.fantastic.springai.model.Product;
import com.fantastic.springai.model.Seller;
import com.fantastic.springai.model.ShopOrder;
import com.fantastic.springai.model.User;
import com.fantastic.springai.repository.ActivityLogRepository;
import com.fantastic.springai.repository.CategoryRepository;
import com.fantastic.springai.repository.CountryRepository;
import com.fantastic.springai.repository.OrderRepository;
import com.fantastic.springai.repository.PaymentRepository;
import com.fantastic.springai.repository.ProductRepository;
import com.fantastic.springai.repository.ReviewRepository;
import com.fantastic.springai.repository.SellerRepository;
import com.fantastic.springai.repository.UserRepository;

import org.springframework.ai.tool.annotation.Tool;

@Service
@Transactional(readOnly = true)
public class DatabaseToolsService {

    private static final int MAX_RESULTS = 50;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final SellerRepository sellerRepository;
    private final PaymentRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CountryRepository countryRepository;

    public DatabaseToolsService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ReviewRepository reviewRepository,
            SellerRepository sellerRepository,
            PaymentRepository paymentRepository,
            CategoryRepository categoryRepository,
            ActivityLogRepository activityLogRepository,
            CountryRepository countryRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.sellerRepository = sellerRepository;
        this.paymentRepository = paymentRepository;
        this.categoryRepository = categoryRepository;
        this.activityLogRepository = activityLogRepository;
        this.countryRepository = countryRepository;
    }

    private static PageRequest page(int limit) {
        return PageRequest.of(0, Math.clamp(limit, 1, MAX_RESULTS));
    }

    /**
     * Spring Data {@code findAll()} sur {@link Country} → SQL {@code SELECT} sur {@code shop.countries} (plafonné à 50 lignes).
     */
    @Tool(description = "Liste tous les pays enregistrés : identifiant, code ISO à 2 lettres, nom et devise")
    public List<CountrySummaryDto> listCountries() {
        return countryRepository.findAll().stream()
                .sorted(Comparator.comparing(Country::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_RESULTS)
                .map(c -> new CountrySummaryDto(c.getId(), c.getCode(), c.getName(), c.getCurrency()))
                .toList();
    }

    /**
     * JPQL : {@link UserRepository#countUsersByCountry()} — groupement par pays sur {@code shop.users.country_id}.
     */
    @Tool(description = "Retourne le nombre d'utilisateurs par pays (code, nom du pays et effectif)")
    public List<UserCountByCountryDto> getUserCountByCountry() {
        return userRepository.countUsersByCountry();
    }

    private static String requireNonBlank(String s, String label) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(label + " est vide.");
        }
        return s.trim();
    }

    private static Optional<OrderStatus> parseOrderStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OrderStatus.valueOf(raw.trim().toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static UserSummaryDto toUserSummary(User u) {
        return new UserSummaryDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.isActive(),
                u.isSeller());
    }

    private static ProductSummaryDto toProductSummary(Product p) {
        return new ProductSummaryDto(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getBasePrice(),
                p.getStock(),
                p.getCategory() != null ? p.getCategory().getName() : "",
                p.getSeller() != null ? p.getSeller().getStoreName() : "",
                p.isActive());
    }

    private static BigDecimal lineTotal(OrderItem oi) {
        BigDecimal qty = BigDecimal.valueOf(oi.getQuantity());
        BigDecimal sub = oi.getUnitPrice().multiply(qty);
        BigDecimal disc = oi.getDiscount();
        if (disc == null || disc.compareTo(BigDecimal.ZERO) == 0) {
            return sub;
        }
        return sub.subtract(sub.multiply(disc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private Optional<User> resolveUser(String emailOrUsername) {
        String q = requireNonBlank(emailOrUsername, "Identifiant");
        return userRepository.findByEmailIgnoreCase(q)
                .or(() -> userRepository.findByUsernameIgnoreCase(q));
    }

    // --- Utilisateurs ---

    /**
     * JPQL : {@code findByEmailContainingIgnoreCaseOr...} — OR sur 4 champs ; résultats plafonnés (page 50).
     * Cas limites : chaîne vide → exception ; aucun match → liste vide.
     */
    @Tool(description = "Recherche des utilisateurs par nom, prénom ou email (partiel)")
    public List<UserSummaryDto> searchUsers(String nameOrEmail) {
        String q = requireNonBlank(nameOrEmail, "Recherche");
        return userRepository
                .findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        q, q, q, q, page(MAX_RESULTS))
                .stream()
                .map(DatabaseToolsService::toUserSummary)
                .toList();
    }

    /**
     * Agrégation en mémoire sur les commandes chargées (LAZY dans transaction read-only) — évite N requêtes si peu de commandes.
     * Cas limites : utilisateur introuvable → exception explicite.
     */
    @Tool(description = "Retourne les statistiques de commandes d'un utilisateur : total dépensé, nb commandes, dernière commande")
    public UserOrderStatsDto getUserOrderStats(String emailOrUsername) {
        User u = resolveUser(emailOrUsername).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        List<ShopOrder> orders = orderRepository.findByUser_Id(u.getId());
        long count = orders.size();
        BigDecimal spent = orders.stream()
                .map(ShopOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime last = orders.stream()
                .map(ShopOrder::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new UserOrderStatsDto(u.getId(), u.getUsername(), count, spent, last);
    }

    /**
     * JPQL : requête dédiée {@code findTopSpenders} avec GROUP BY utilisateur.
     */
    @Tool(description = "Retourne les N utilisateurs qui ont le plus dépensé")
    public List<UserOrderStatsDto> getTopSpenders(int limit) {
        return userRepository.findTopSpenders(page(limit));
    }

    /**
     * Comptages simples {@code countByActiveTrue} / {@code countBySellerTrue}.
     */
    @Tool(description = "Retourne le nombre total d'utilisateurs actifs et vendeurs")
    public Map<String, Long> getUserCountStats() {
        Map<String, Long> m = new HashMap<>();
        m.put("activeUsers", userRepository.countByActiveTrue());
        m.put("sellers", userRepository.countBySellerTrue());
        return m;
    }

    // --- Commandes ---

    /**
     * JPQL : {@code findByStatus} — statut validé par énumération.
     */
    @Tool(description = "Retourne les commandes selon leur statut : pending, confirmed, shipped, delivered, cancelled, refunded")
    public List<OrderSummaryDto> getOrdersByStatus(String status) {
        OrderStatus st = parseOrderStatus(status)
                .orElseThrow(() -> new IllegalArgumentException("Statut inconnu : " + status));
        return orderRepository.findByStatus(st).stream()
                .sorted(Comparator.comparing(ShopOrder::getCreatedAt).reversed())
                .limit(MAX_RESULTS)
                .map(this::toOrderSummary)
                .toList();
    }

    /**
     * Filtre {@code createdAt} entre minuit début et fin de journée fin ; borne invalide → exception.
     */
    @Tool(description = "Retourne les commandes entre deux dates au format yyyy-MM-dd")
    public List<OrderSummaryDto> getOrdersByDateRange(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(requireNonBlank(startDate, "startDate"), ISO_DATE);
        LocalDate end = LocalDate.parse(requireNonBlank(endDate, "endDate"), ISO_DATE);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate doit être >= startDate.");
        }
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay().minusNanos(1);
        return orderRepository.findByCreatedAtBetween(from, to).stream()
                .sorted(Comparator.comparing(ShopOrder::getCreatedAt).reversed())
                .limit(MAX_RESULTS)
                .map(this::toOrderSummary)
                .toList();
    }

    private OrderSummaryDto toOrderSummary(ShopOrder o) {
        return new OrderSummaryDto(
                o.getId(),
                o.getCreatedAt(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getUser() != null ? o.getUser().getEmail() : "");
    }

    /**
     * JPQL : {@code findDetailById} avec JOIN FETCH (commande, lignes, produits, user, adresse, paiement).
     * Cas limites : id inconnu → exception.
     */
    @Tool(description = "Retourne le détail complet d'une commande : articles, adresse, paiement")
    public OrderDetailDto getOrderDetails(Long orderId) {
        ShopOrder o = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable : " + orderId));
        List<OrderItemDto> items = o.getOrderItems().stream()
                .map(oi -> new OrderItemDto(
                        oi.getProduct().getName(),
                        oi.getProduct().getSku(),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getDiscount(),
                        lineTotal(oi)))
                .toList();
        String addr = formatAddress(o.getAddress());
        com.fantastic.springai.model.Payment pay = o.getPayment();
        PaymentDto payDto = pay == null
                ? null
                : new PaymentDto(
                        pay.getMethod(),
                        pay.getStatus(),
                        pay.getAmount(),
                        pay.getPaidAt(),
                        pay.getTransactionRef());
        return new OrderDetailDto(
                o.getId(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getCurrency(),
                o.getNotes(),
                o.getCreatedAt(),
                o.getUser() != null ? o.getUser().getEmail() : "",
                addr,
                items,
                payDto);
    }

    private static String formatAddress(Address a) {
        if (a == null) {
            return null;
        }
        String country = a.getCountry() != null ? a.getCountry().getName() : "";
        return a.getStreet() + ", " + a.getPostalCode() + " " + a.getCity() + ", " + country;
    }

    /**
     * Résolution utilisateur puis {@code findByUser_Id}.
     */
    @Tool(description = "Retourne les commandes d'un utilisateur identifié par email ou username")
    public List<OrderSummaryDto> getOrdersByUser(String emailOrUsername) {
        User u = resolveUser(emailOrUsername).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return orderRepository.findByUser_Id(u.getId()).stream()
                .sorted(Comparator.comparing(ShopOrder::getCreatedAt).reversed())
                .limit(MAX_RESULTS)
                .map(this::toOrderSummary)
                .toList();
    }

    /**
     * JPQL : {@code countByStatusGrouped} puis conversion enum → libellé.
     */
    @Tool(description = "Retourne la répartition du nombre de commandes par statut")
    public Map<String, Long> getOrderCountByStatus() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusGrouped()) {
            OrderStatus st = (OrderStatus) row[0];
            Long c = (Long) row[1];
            map.put(st.name(), c);
        }
        return map;
    }

    /**
     * JPQL : {@code totalRevenueBetween} sur {@code totalAmount}.
     */
    @Tool(description = "Calcule le chiffre d'affaires total entre deux dates (yyyy-MM-dd)")
    public String getTotalRevenue(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(requireNonBlank(startDate, "startDate"), ISO_DATE);
        LocalDate end = LocalDate.parse(requireNonBlank(endDate, "endDate"), ISO_DATE);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay().minusNanos(1);
        BigDecimal rev = orderRepository.totalRevenueBetween(from, to);
        DecimalFormat df = euroFormat();
        return df.format(rev) + " €";
    }

    private static DecimalFormat euroFormat() {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    /**
     * Combine sommes globales ({@code OrderRepository}) et paiements complétés ({@code PaymentRepository}).
     */
    @Tool(description = "Retourne les statistiques globales : CA total, nb commandes, panier moyen")
    public RevenueStatsDto getRevenueStats() {
        long orderCount = orderRepository.count();
        BigDecimal total = orderRepository.sumTotalRevenue();
        BigDecimal avg = orderRepository.avgOrderValue();
        return new RevenueStatsDto(total, orderCount, avg, "all_time");
    }

    // --- Produits ---

    /**
     * JPQL : {@code findByNameContainingIgnoreCase} + limite 50.
     */
    @Tool(description = "Recherche des produits par nom (partiel)")
    public List<ProductSummaryDto> searchProducts(String name) {
        String q = requireNonBlank(name, "Nom");
        return productRepository.findByNameContainingIgnoreCase(q).stream()
                .limit(MAX_RESULTS)
                .map(DatabaseToolsService::toProductSummary)
                .toList();
    }

    /**
     * JPQL : {@code findTopSellingProducts} — groupement sur order_items.
     */
    @Tool(description = "Retourne les N produits les plus vendus avec leur chiffre d'affaires")
    public List<ProductSalesDto> getTopSellingProducts(int limit) {
        List<Product> products = productRepository.findTopSellingProducts(page(limit));
        List<ProductSalesDto> out = new ArrayList<>();
        for (Product p : products) {
            long qty = p.getOrderItems().stream().mapToLong(OrderItem::getQuantity).sum();
            BigDecimal revenue = p.getOrderItems().stream()
                    .map(DatabaseToolsService::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new ProductSalesDto(p.getId(), p.getName(), qty, revenue));
        }
        return out;
    }

    /**
     * JPQL : {@code findByStockLessThan}.
     */
    @Tool(description = "Retourne les produits dont le stock est inférieur à un seuil")
    public List<ProductSummaryDto> getLowStockProducts(int threshold) {
        return productRepository.findByStockLessThan(threshold).stream()
                .limit(MAX_RESULTS)
                .map(DatabaseToolsService::toProductSummary)
                .toList();
    }

    /**
     * Résolution catégorie par slug exact ou nom (contient, insensible à la casse).
     */
    @Tool(description = "Retourne les produits d'une catégorie donnée (nom ou slug)")
    public List<ProductSummaryDto> getProductsByCategory(String categoryNameOrSlug) {
        String q = requireNonBlank(categoryNameOrSlug, "Catégorie");
        Optional<Category> cat = categoryRepository.findBySlug(q);
        if (cat.isEmpty()) {
            cat = categoryRepository.findAll().stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
                    .findFirst();
        }
        Category c = cat.orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable."));
        return productRepository.findByCategory_Id(c.getId()).stream()
                .limit(MAX_RESULTS)
                .map(DatabaseToolsService::toProductSummary)
                .toList();
    }

    /**
     * Vendeur résolu par nom de boutique exact (insensible à la casse).
     */
    @Tool(description = "Retourne les produits d'un vendeur identifié par nom de boutique")
    public List<ProductSummaryDto> getProductsBySeller(String storeName) {
        Seller s = sellerRepository.findByStoreNameIgnoreCase(requireNonBlank(storeName, "Boutique"))
                .orElseThrow(() -> new IllegalArgumentException("Vendeur introuvable."));
        return productRepository.findBySeller_Id(s.getId()).stream()
                .limit(MAX_RESULTS)
                .map(DatabaseToolsService::toProductSummary)
                .toList();
    }

    /**
     * JPQL : {@code findByBasePriceBetweenAndActiveTrue}.
     */
    @Tool(description = "Retourne les produits dans une fourchette de prix (ex: min=10, max=100)")
    public List<ProductSummaryDto> getProductsByPriceRange(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("min doit être <= max.");
        }
        BigDecimal a = BigDecimal.valueOf(min);
        BigDecimal b = BigDecimal.valueOf(max);
        return productRepository.findByBasePriceBetweenAndActiveTrue(a, b).stream()
                .limit(MAX_RESULTS)
                .map(DatabaseToolsService::toProductSummary)
                .toList();
    }

    // --- Vendeurs ---

    /**
     * Lecture vendeur + comptage produits (collection lazy dans transaction).
     */
    @Tool(description = "Retourne les statistiques d'un vendeur : ventes totales, note, nb produits")
    public SellerStatsDto getSellerStats(String storeName) {
        Seller s = sellerRepository.findByStoreNameIgnoreCase(requireNonBlank(storeName, "Boutique"))
                .orElseThrow(() -> new IllegalArgumentException("Vendeur introuvable."));
        long pc = s.getProducts().size();
        return new SellerStatsDto(
                s.getId(),
                s.getStoreName(),
                s.getTotalSales(),
                s.getRating(),
                s.isVerified(),
                pc);
    }

    /**
     * JPQL : {@code findTopSellers} tri {@code totalSales} DESC.
     */
    @Tool(description = "Retourne les N meilleurs vendeurs classés par nombre de ventes")
    public List<SellerStatsDto> getTopSellers(int limit) {
        return sellerRepository.findTopSellers(page(limit)).getContent().stream()
                .map(s -> new SellerStatsDto(
                        s.getId(),
                        s.getStoreName(),
                        s.getTotalSales(),
                        s.getRating(),
                        s.isVerified(),
                        s.getProducts().size()))
                .toList();
    }

    /**
     * JPQL : {@code findByVerifiedTrue}.
     */
    @Tool(description = "Retourne tous les vendeurs vérifiés")
    public List<SellerStatsDto> getVerifiedSellers() {
        return sellerRepository.findByVerifiedTrue().stream()
                .map(s -> new SellerStatsDto(
                        s.getId(),
                        s.getStoreName(),
                        s.getTotalSales(),
                        s.getRating(),
                        s.isVerified(),
                        s.getProducts().size()))
                .limit(MAX_RESULTS)
                .toList();
    }

    // --- Avis / paiements / catégories / logs ---

    /**
     * JPQL : {@code avgRatingByProductId} + {@code countByRatingGrouped}.
     */
    @Tool(description = "Retourne les statistiques d'avis d'un produit : note moyenne, distribution des notes")
    public ReviewStatsDto getProductReviewStats(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable."));
        Double avg = reviewRepository.avgRatingByProductId(productId);
        Map<Short, Long> dist = new LinkedHashMap<>();
        for (Object[] row : reviewRepository.countByRatingGrouped(productId)) {
            Short rating = ((Number) row[0]).shortValue();
            Long cnt = (Long) row[1];
            dist.put(rating, cnt);
        }
        long total = dist.values().stream().mapToLong(Long::longValue).sum();
        return new ReviewStatsDto(p.getId(), p.getName(), avg, total, dist);
    }

    /**
     * JPQL : {@code aggregateByMethod} — taux de complétion = complétés / total par méthode.
     */
    @Tool(description = "Retourne les statistiques de paiements par méthode (card, paypal, etc.)")
    public List<PaymentStatsDto> getPaymentStats() {
        List<Object[]> rows = paymentRepository.aggregateByMethod(PaymentStatus.completed);
        List<PaymentStatsDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            com.fantastic.springai.model.PaymentMethod method = (com.fantastic.springai.model.PaymentMethod) row[0];
            long total = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            long completed = ((Number) row[3]).longValue();
            double rate = total == 0 ? 0.0 : (completed * 100.0 / total);
            list.add(new PaymentStatsDto(method, total, amount, rate));
        }
        return list;
    }

    /**
     * JPQL : {@code findCategoryAggregation}.
     */
    @Tool(description = "Retourne l'arborescence des catégories avec le nombre de produits")
    public List<CategoryStatsDto> getCategoryStats() {
        return categoryRepository.findCategoryAggregation().stream()
                .map(row -> new CategoryStatsDto(
                        (Integer) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]))
                .toList();
    }

    /**
     * Filtre {@code findByUser_Id} + tri par date décroissante ; format chaîne lisible.
     */
    @Tool(description = "Retourne les dernières actions d'un utilisateur (logs d'activité)")
    public List<String> getUserActivityLogs(String emailOrUsername, int limit) {
        User u = resolveUser(emailOrUsername).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return activityLogRepository.findByUser_IdOrderByLoggedAtDesc(u.getId(), page(limit)).stream()
                .map(log -> log.getLoggedAt() + " | " + log.getAction() + " | " + log.getEntity() + " | " + log.getEntityId())
                .toList();
    }
}
