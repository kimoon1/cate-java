package management;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class ManagementPanel<T> extends JPanel {
    private final CatalogTable<T> table;
    private final JTextField search = Theme.field("", 16);
    private final JComboBox<String> status;
    private final JButton edit = Theme.button("수정", Theme.ButtonStyle.SECONDARY);
    private final JButton delete = Theme.button("삭제", Theme.ButtonStyle.SECONDARY);
    private final JLabel count = Theme.label("", 13, Font.PLAIN, Theme.MUTED);
    private final Function<T, String> searchable;
    private final Function<T, String> statusValue;
    private final String itemName;
    private List<T> rows = List.of();

    ManagementPanel(String itemName, String searchLabel, CatalogTable<T> table, List<String> statuses,
                    Function<T, String> searchable, Function<T, String> statusValue,
                    Consumer<T> onEdit, Consumer<T> onDelete) {
        super(new BorderLayout(0, 15));
        this.table = table;
        this.itemName = itemName;
        this.searchable = searchable;
        this.statusValue = statusValue;
        status = Theme.combo(new String[]{"전체"});
        setOpaque(false);

        JPanel toolbar = Theme.transparent(new BorderLayout(8, 0));
        JPanel filters = Theme.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel searchTitle = Theme.label("검색", 13, Font.BOLD, Theme.INK);
        searchTitle.setLabelFor(search);
        search.getAccessibleContext().setAccessibleDescription(searchLabel);
        search.setToolTipText(searchLabel);
        JLabel statusTitle = Theme.label("상태", 13, Font.BOLD, Theme.INK);
        statusTitle.setLabelFor(status);
        statuses.forEach(status::addItem);
        filters.add(searchTitle);
        filters.add(search);
        filters.add(statusTitle);
        filters.add(status);
        toolbar.add(filters, BorderLayout.WEST);

        JPanel actions = Theme.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        edit.setEnabled(false);
        delete.setEnabled(false);
        actions.add(edit);
        actions.add(delete);
        toolbar.add(actions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        add(count, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filter();
            }
        });
        status.addActionListener(event -> filter());
        table.table().getSelectionModel().addListSelectionListener(event -> {
            boolean selected = table.table().getSelectedRow() >= 0;
            edit.setEnabled(selected);
            delete.setEnabled(selected);
        });
        edit.addActionListener(event -> onEdit.accept(table.selection()));
        delete.addActionListener(event -> onDelete.accept(table.selection()));
        table.table().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.table().rowAtPoint(event.getPoint()) >= 0) {
                    edit.doClick();
                }
            }
        });
        table.table().registerKeyboardAction(event -> edit.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_FOCUSED);
    }

    void setRows(List<T> items) {
        rows = List.copyOf(items);
        filter();
    }

    private void filter() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        int statusIndex = status.getSelectedIndex();
        String selectedStatus = status.getItemAt(statusIndex);
        List<T> matching = rows.stream()
                .filter(row -> statusIndex <= 0 || statusValue.apply(row).equals(selectedStatus))
                .filter(row -> searchable.apply(row).toLowerCase(Locale.ROOT).contains(query))
                .toList();
        String message = rows.isEmpty() ? "등록된 " + itemName + " 항목이 없습니다." : "검색 조건과 일치하는 결과가 없습니다.";
        table.setRows(matching, message);
        count.setText("전체 " + rows.size() + "건 / 표시 " + matching.size() + "건");
    }
}
