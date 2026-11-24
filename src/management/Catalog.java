package management;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

record Catalog(List<Product> products, List<Advertisement> advertisements) {
    Catalog {
        products = List.copyOf(products);
        advertisements = List.copyOf(advertisements);

        Set<UUID> productIds = new HashSet<>();
        Set<String> productCodes = new HashSet<>();
        for (Product product : products) {
            if (!productIds.add(product.id())) {
                throw new IllegalArgumentException("제품 ID가 중복되었습니다.");
            }
            if (!productCodes.add(product.code().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("이미 사용 중인 제품 코드입니다.");
            }
        }

        Set<UUID> advertisementIds = new HashSet<>();
        for (Advertisement advertisement : advertisements) {
            if (!advertisementIds.add(advertisement.id())) {
                throw new IllegalArgumentException("광고 ID가 중복되었습니다.");
            }
            if (!productIds.contains(advertisement.productId())) {
                throw new IllegalArgumentException("연결된 광고가 있습니다. 광고를 먼저 변경하거나 삭제하세요.");
            }
        }
    }

    Catalog withProduct(Product product) {
        return new Catalog(upsert(products, product, Product::id), advertisements);
    }

    Catalog withAdvertisement(Advertisement advertisement) {
        return new Catalog(products, upsert(advertisements, advertisement, Advertisement::id));
    }

    Catalog withoutProduct(UUID id) {
        return new Catalog(products.stream().filter(product -> !product.id().equals(id)).toList(), advertisements);
    }

    Catalog withoutAdvertisement(UUID id) {
        return new Catalog(products, advertisements.stream()
                .filter(advertisement -> !advertisement.id().equals(id)).toList());
    }

    private static <T> List<T> upsert(List<T> items, T value, Function<T, UUID> id) {
        List<T> updated = new ArrayList<>(items);
        UUID valueId = id.apply(value);
        for (int index = 0; index < items.size(); index++) {
            if (id.apply(items.get(index)).equals(valueId)) {
                updated.set(index, value);
                return updated;
            }
        }
        updated.add(value);
        return updated;
    }

    record Product(UUID id, String name, String code, long price, int stock, ProductStatus status) {
        Product {
            Objects.requireNonNull(id);
            name = required(name, "제품명");
            code = required(code, "제품 코드");
            nonNegative(price, "판매가");
            nonNegative(stock, "재고");
            Objects.requireNonNull(status);
        }
    }

    record Advertisement(UUID id, UUID productId, String name, String channel, long budget,
                         LocalDate start, LocalDate end, AdStatus status) {
        Advertisement {
            Objects.requireNonNull(id);
            Objects.requireNonNull(productId);
            name = required(name, "광고명");
            channel = required(channel, "채널");
            nonNegative(budget, "예산");
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            Objects.requireNonNull(status);
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("종료일은 시작일과 같거나 이후여야 합니다.");
            }
        }
    }

    enum ProductStatus {
        ACTIVE("판매 중"), INACTIVE("판매 중지");

        private final String label;

        ProductStatus(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum AdStatus {
        PLANNED("예정"), RUNNING("진행 중"), PAUSED("일시 중지"), FINISHED("종료");

        private final String label;

        AdStatus(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static String required(String value, String label) {
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " 항목을 입력하세요.");
        }
        return trimmed;
    }

    private static void nonNegative(long value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " 항목에는 0 이상의 정수를 입력하세요.");
        }
    }
}
