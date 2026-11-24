package management;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import management.Catalog.AdStatus;
import management.Catalog.Advertisement;
import management.Catalog.Product;
import management.Catalog.ProductStatus;
import management.CatalogTable.Column;
import management.Theme.ButtonStyle;
import management.Theme.Glyph;

final class ManagementFrame extends JFrame {
    private final CatalogStore store;
    private final JPanel pages = new JPanel(new CardLayout());
    private final Map<Page, JButton> navigation = new EnumMap<>(Page.class);
    private final JLabel productCount = metricValue();
    private final JLabel liveCount = metricValue();
    private final JLabel totalBudget = metricValue();
    private final CatalogTable<Advertisement> recentAds;
    private final ManagementPanel<Product> products;
    private final ManagementPanel<Advertisement> ads;
    private Map<UUID, Product> productById = Map.of();

    ManagementFrame(CatalogStore store) {
        super("제품과 광고 관리");
        this.store = store;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                try {
                    store.close();
                } catch (IOException exception) {
                    System.err.println(exception.getMessage());
                }
            }
        });
        setMinimumSize(new Dimension(1030, 620));
        setSize(1200, 760);
        setLocationRelativeTo(null);
        getContentPane().add(sidebar(), BorderLayout.WEST);
        getContentPane().add(pages, BorderLayout.CENTER);

        CatalogTable<Product> productTable = new CatalogTable<>(List.of(
                new Column<>("제품명", String.class, 240, Product::name),
                new Column<>("제품 코드", String.class, 154, Product::code),
                new Column<>("판매가", Long.class, 132, Product::price),
                new Column<>("재고", Integer.class, 96, Product::stock),
                new Column<>("상태", ProductStatus.class, 112, Product::status)));
        Column<Advertisement> adName = new Column<>("광고명", String.class, 155, Advertisement::name);
        Column<Advertisement> linkedProduct = new Column<>("연결 제품", String.class, 145, this::productName);
        Column<Advertisement> budget = new Column<>("예산", Long.class, 122, Advertisement::budget);
        Column<Advertisement> adStatus = new Column<>("상태", AdStatus.class, 89, Advertisement::status);
        CatalogTable<Advertisement> adTable = new CatalogTable<>(List.of(adName, linkedProduct,
                new Column<>("채널", String.class, 110, Advertisement::channel), budget,
                new Column<>("시작일", LocalDate.class, 107, Advertisement::start),
                new Column<>("종료일", LocalDate.class, 107, Advertisement::end), adStatus));
        recentAds = new CatalogTable<>(List.of(adName, linkedProduct, budget, adStatus));

        products = new ManagementPanel<>("제품", "제품명 또는 제품 코드로 검색", productTable,
                labels(ProductStatus.values()), item -> item.name() + " " + item.code(),
                item -> item.status().toString(), this::editProduct, this::deleteProduct);
        ads = new ManagementPanel<>("광고", "광고명, 제품명 또는 채널로 검색", adTable, labels(AdStatus.values()),
                item -> item.name() + " " + productName(item) + " " + item.channel(),
                item -> item.status().toString(), this::editAd, this::deleteAd);

        pages.add(page(Page.OVERVIEW, "등록된 제품과 광고의 현황을 확인하세요.", dashboard(),
                action("제품 등록", ButtonStyle.SECONDARY, () -> editProduct(null)),
                action("광고 등록", ButtonStyle.PRIMARY, () -> editAd(null))), Page.OVERVIEW.name());
        pages.add(page(Page.PRODUCTS, "판매가와 재고, 판매 상태를 관리하세요.", products,
                action("제품 등록", ButtonStyle.PRIMARY, () -> editProduct(null))), Page.PRODUCTS.name());
        pages.add(page(Page.ADS, "제품별 광고 채널, 예산, 일정을 관리하세요.", ads,
                action("광고 등록", ButtonStyle.PRIMARY, () -> editAd(null))), Page.ADS.name());
        refresh();
        showPage(Page.OVERVIEW);
    }

    private JPanel sidebar() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setPreferredSize(new Dimension(205, 0));
        rail.setBackground(Theme.WHITE);
        rail.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.OUTLINE),
                BorderFactory.createEmptyBorder(28, 17, 24, 17)));

        JPanel brand = Theme.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel emblem = Theme.card(new GridLayout(1, 1), Theme.ACCENT_TINT);
        emblem.setPreferredSize(new Dimension(40, 40));
        JLabel symbol = new JLabel(Glyph.PRODUCT, SwingConstants.CENTER);
        symbol.setForeground(Theme.ACCENT);
        emblem.add(symbol);
        brand.add(emblem);
        JLabel brandName = Theme.label("제품과 광고", 17, Font.BOLD, Theme.INK);
        brandName.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));
        brand.add(brandName);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        rail.add(brand);
        rail.add(Box.createVerticalStrut(30));

        for (Page page : Page.values()) {
            JButton button = action(page.title, ButtonStyle.NAV, () -> showPage(page));
            button.setIcon(page.icon);
            button.setIconTextGap(11);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.setPreferredSize(new Dimension(158, 44));
            navigation.put(page, button);
            rail.add(button);
            rail.add(Box.createVerticalStrut(7));
        }
        return rail;
    }

    private JPanel page(Page page, String description, JPanel body, JButton... actions) {
        JPanel panel = new JPanel(new BorderLayout(0, 23));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 32, 28, 32));

        JPanel header = Theme.transparent(new BorderLayout(12, 0));
        JPanel titles = Theme.transparent(new BorderLayout(0, 7));
        titles.add(Theme.label(page.title, 32, Font.BOLD, Theme.INK), BorderLayout.NORTH);
        titles.add(Theme.label(description, 14, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        header.add(titles, BorderLayout.CENTER);
        JPanel buttons = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        for (JButton button : actions) {
            buttons.add(button);
        }
        header.add(buttons, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel dashboard() {
        JPanel content = Theme.transparent(new BorderLayout(0, 22));
        JPanel metrics = Theme.transparent(new GridLayout(1, 3, 14, 0));
        metrics.add(metric("등록 제품", productCount));
        metrics.add(metric("진행 중 광고", liveCount));
        metrics.add(metric("전체 광고 예산", totalBudget));
        content.add(metrics, BorderLayout.NORTH);

        JPanel recent = Theme.card(new BorderLayout(0, 15), Theme.WHITE);
        recent.setBorder(BorderFactory.createEmptyBorder(15, 17, 17, 17));
        JPanel header = Theme.transparent(new BorderLayout());
        header.add(Theme.label("최근 등록 광고", 18, Font.BOLD, Theme.INK), BorderLayout.WEST);
        header.add(action("광고 목록 보기", ButtonStyle.SECONDARY, () -> showPage(Page.ADS)), BorderLayout.EAST);
        recent.add(header, BorderLayout.NORTH);
        recent.add(recentAds, BorderLayout.CENTER);
        content.add(recent, BorderLayout.CENTER);
        return content;
    }

    private JPanel metric(String title, JLabel value) {
        JPanel tile = Theme.card(new BorderLayout(0, 10), Theme.WHITE);
        tile.setBorder(BorderFactory.createEmptyBorder(17, 19, 17, 19));
        tile.add(Theme.label(title, 14, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    private static JLabel metricValue() {
        JLabel value = Theme.label("0", 28, Font.BOLD, Theme.INK);
        value.setPreferredSize(new Dimension(130, 41));
        return value;
    }

    private static JButton action(String label, ButtonStyle style, Runnable run) {
        JButton button = Theme.button(label, style);
        button.addActionListener(event -> run.run());
        return button;
    }

    private static List<String> labels(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::toString).toList();
    }

    private void showPage(Page page) {
        ((CardLayout) pages.getLayout()).show(pages, page.name());
        navigation.forEach((candidate, button) -> {
            boolean active = candidate == page;
            button.putClientProperty("currentPage", active);
            button.setForeground(active ? Theme.ACCENT : Theme.MUTED);
            button.repaint();
        });
    }

    private void refresh() {
        Catalog catalog = store.catalog();
        productById = catalog.products().stream().collect(Collectors.toUnmodifiableMap(Product::id, product -> product));
        products.setRows(catalog.products());
        ads.setRows(catalog.advertisements());
        recentAds.setRows(catalog.advertisements().reversed().stream().limit(5).toList(),
                "아직 등록한 광고가 없습니다.");

        long running = catalog.advertisements().stream().filter(ad -> ad.status() == AdStatus.RUNNING).count();
        BigInteger budget = catalog.advertisements().stream().map(ad -> BigInteger.valueOf(ad.budget()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.KOREA);
        productCount.setText(numbers.format(catalog.products().size()) + "개");
        liveCount.setText(numbers.format(running) + "건");
        String budgetText = numbers.format(budget) + "원";
        totalBudget.setText(budgetText);
        totalBudget.setToolTipText(budgetText);
    }

    private String productName(Advertisement advertisement) {
        return productById.get(advertisement.productId()).name();
    }

    private void editProduct(Product product) {
        EntryDialogs.product(this, product, item -> commit(catalog -> catalog.withProduct(item)));
    }

    private void editAd(Advertisement advertisement) {
        List<Product> choices = store.catalog().products();
        if (choices.isEmpty()) {
            Object[] options = {"취소", "제품 등록"};
            int answer = JOptionPane.showOptionDialog(this,
                    "광고를 등록하려면 제품이 필요합니다. 제품을 등록할까요?", "광고 등록",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (answer == 1) {
                editProduct(null);
                if (!store.catalog().products().isEmpty()) {
                    editAd(null);
                }
            }
            return;
        }
        EntryDialogs.advertisement(this, advertisement, choices, item -> commit(catalog -> catalog.withAdvertisement(item)));
    }

    private void deleteProduct(Product product) {
        if (store.catalog().advertisements().stream().anyMatch(ad -> ad.productId().equals(product.id()))) {
            JOptionPane.showMessageDialog(this, "연결된 광고의 제품을 변경하거나 해당 광고를 먼저 삭제하세요.",
                    "제품 삭제", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        delete("제품", product.name(), catalog -> catalog.withoutProduct(product.id()));
    }

    private void deleteAd(Advertisement advertisement) {
        delete("광고", advertisement.name(), catalog -> catalog.withoutAdvertisement(advertisement.id()));
    }

    private void delete(String type, String name, UnaryOperator<Catalog> update) {
        String message = "‘" + name + "’ 항목을 삭제할까요?";
        Object[] options = {"취소", "삭제"};
        int answer = JOptionPane.showOptionDialog(this, message, type + " 삭제", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (answer == 1) {
            try {
                commit(update);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(this, "파일에 저장할 수 없습니다. 폴더 접근 권한을 확인하세요.",
                        "삭제 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void commit(UnaryOperator<Catalog> update) throws IOException {
        store.save(update.apply(store.catalog()));
        refresh();
    }

    private enum Page {
        OVERVIEW("전체 현황", Glyph.HOME), PRODUCTS("제품 관리", Glyph.PRODUCT), ADS("광고 관리", Glyph.AD);

        private final String title;
        private final Glyph icon;

        Page(String title, Glyph icon) {
            this.title = title;
            this.icon = icon;
        }
    }
}
