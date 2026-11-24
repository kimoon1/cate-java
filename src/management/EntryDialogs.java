package management;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import management.Catalog.AdStatus;
import management.Catalog.Advertisement;
import management.Catalog.Product;
import management.Catalog.ProductStatus;

final class EntryDialogs {
    private EntryDialogs() {
    }

    static void product(JFrame owner, Product original, Save<Product> save) {
        Form form = new Form(owner, original == null ? "제품 등록" : "제품 수정");
        JTextField name = field(original == null ? "" : original.name());
        JTextField code = field(original == null ? "" : original.code());
        JTextField price = field(original == null ? "" : Long.toString(original.price()));
        JTextField stock = field(original == null ? "0" : Integer.toString(original.stock()));
        JComboBox<ProductStatus> status = Theme.combo(ProductStatus.values());
        status.setSelectedItem(original == null ? ProductStatus.ACTIVE : original.status());
        form.add("제품명", name);
        form.add("제품 코드", code);
        form.add("판매가 (원)", price);
        form.add("재고 (개)", stock);
        form.add("판매 상태", status);
        form.open(name, () -> save.accept(new Product(original == null ? UUID.randomUUID() : original.id(),
                name.getText(), code.getText(), number(price, "판매가", Long.MAX_VALUE),
                (int) number(stock, "재고", Integer.MAX_VALUE),
                status.getItemAt(status.getSelectedIndex()))));
    }

    static void advertisement(JFrame owner, Advertisement original, List<Product> products, Save<Advertisement> save) {
        Form form = new Form(owner, original == null ? "광고 등록" : "광고 수정");
        JTextField name = field(original == null ? "" : original.name());
        JComboBox<ProductOption> product = Theme.combo(products.stream().map(ProductOption::new).toArray(ProductOption[]::new));
        if (original != null) {
            products.stream().filter(item -> item.id().equals(original.productId())).findFirst()
                    .ifPresent(item -> product.setSelectedItem(new ProductOption(item)));
        } else if (products.size() > 1) {
            product.setSelectedIndex(-1);
        }
        JTextField channel = field(original == null ? "" : original.channel());
        JTextField budget = field(original == null ? "" : Long.toString(original.budget()));
        LocalDate today = LocalDate.now();
        JTextField start = field((original == null ? today : original.start()).toString());
        JTextField end = field((original == null ? today : original.end()).toString());
        JComboBox<AdStatus> status = Theme.combo(AdStatus.values());
        status.setSelectedItem(original == null ? AdStatus.PLANNED : original.status());
        form.add("광고명", name);
        form.add("연결 제품", product);
        form.add("채널", channel);
        form.add("예산 (원)", budget);
        form.add("시작일 (YYYY-MM-DD)", start);
        form.add("종료일 (YYYY-MM-DD)", end);
        form.add("진행 상태", status);
        form.open(name, () -> {
            if (product.getSelectedIndex() < 0) {
                throw new IllegalArgumentException("연결할 제품을 선택하세요.");
            }
            UUID productId = product.getItemAt(product.getSelectedIndex()).item().id();
            save.accept(new Advertisement(original == null ? UUID.randomUUID() : original.id(), productId,
                    name.getText(), channel.getText(), number(budget, "예산", Long.MAX_VALUE),
                    date(start, "시작일"), date(end, "종료일"), status.getItemAt(status.getSelectedIndex())));
        });
    }

    private static JTextField field(String value) {
        return Theme.field(value, 23);
    }

    private static long number(JTextField field, String label, long limit) {
        String input = field.getText().strip();
        try {
            if (input.matches("[0-9]+|[0-9]{1,3}(,[0-9]{3})+")) {
                long value = Long.parseLong(input.replace(",", ""));
                if (value <= limit) {
                    return value;
                }
            }
        } catch (NumberFormatException exception) {
            // An integer larger than a long uses the same field error.
        }
        throw new IllegalArgumentException(label + " 항목에 범위에 맞는 0 이상의 정수를 입력하세요.");
    }

    private static LocalDate date(JTextField field, String label) {
        try {
            String input = field.getText().strip();
            if (input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                return LocalDate.parse(input);
            }
        } catch (DateTimeParseException exception) {
            // Invalid calendar dates use the same field error.
        }
        throw new IllegalArgumentException(label + " 항목에 올바른 날짜를 입력하세요 (예: 2026-12-01).");
    }

    @FunctionalInterface
    interface Save<T> {
        void accept(T value) throws IOException;
    }

    @FunctionalInterface
    private interface Submit {
        void save() throws IOException;
    }

    private record ProductOption(Product item) {
        @Override
        public String toString() {
            return item.name() + " · " + item.code();
        }
    }

    private static final class Form {
        private final JDialog dialog;
        private final JPanel body = Theme.transparent(new GridBagLayout());
        private final JLabel error = Theme.label(" ", 13, Font.PLAIN, Theme.ERROR);
        private final JButton saveButton = Theme.button("저장", Theme.ButtonStyle.PRIMARY);
        private int row = 0;

        Form(JFrame owner, String title) {
            dialog = new JDialog(owner, title, true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);
            JPanel content = new JPanel(new BorderLayout(0, 15));
            content.setBackground(Theme.BACKGROUND);
            content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
            content.add(Theme.label(title, 23, Font.BOLD, Theme.INK), BorderLayout.NORTH);
            content.add(body, BorderLayout.CENTER);

            error.setPreferredSize(new Dimension(440, 21));
            JPanel footer = Theme.transparent(new BorderLayout(0, 9));
            footer.add(error, BorderLayout.NORTH);
            JPanel buttons = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            JButton cancel = Theme.button("취소", Theme.ButtonStyle.SECONDARY);
            cancel.addActionListener(event -> dialog.dispose());
            buttons.add(cancel);
            buttons.add(saveButton);
            footer.add(buttons, BorderLayout.SOUTH);
            content.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(content);
            dialog.getRootPane().setDefaultButton(saveButton);
            dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        void add(String title, JComponent field) {
            JLabel label = Theme.label(title, 14, Font.BOLD, Theme.INK);
            label.setLabelFor(field);
            GridBagConstraints placement = new GridBagConstraints();
            placement.gridx = 0;
            placement.gridy = row++;
            placement.anchor = GridBagConstraints.WEST;
            placement.insets = new Insets(4, 0, 4, 13);
            body.add(label, placement);
            placement.gridx = 1;
            placement.weightx = 1;
            placement.fill = GridBagConstraints.HORIZONTAL;
            placement.insets = new Insets(4, 0, 4, 0);
            body.add(field, placement);
        }

        void open(Component firstField, Submit submit) {
            saveButton.addActionListener(event -> {
                try {
                    submit.save();
                    dialog.dispose();
                } catch (IllegalArgumentException exception) {
                    error.setText(exception.getMessage());
                } catch (IOException exception) {
                    error.setText("파일에 저장할 수 없습니다. 폴더 접근 권한을 확인하세요.");
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(dialog.getOwner());
            SwingUtilities.invokeLater(firstField::requestFocusInWindow);
            dialog.setVisible(true);
        }
    }
}
