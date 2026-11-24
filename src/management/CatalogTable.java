package management;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

final class CatalogTable<T> extends JScrollPane {
    private final List<Column<T>> columns;
    private final JTable table;
    private final AbstractTableModel model;
    private List<T> rows = List.of();
    private String emptyMessage = "";

    CatalogTable(List<Column<T>> columns) {
        this.columns = List.copyOf(columns);
        model = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return CatalogTable.this.columns.size();
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return CatalogTable.this.columns.get(column).type();
            }

            @Override
            public String getColumnName(int column) {
                return CatalogTable.this.columns.get(column).label();
            }

            @Override
            public Object getValueAt(int row, int column) {
                return CatalogTable.this.columns.get(column).value().apply(rows.get(row));
            }
        };
        table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (!rows.isEmpty()) {
                    return;
                }
                Graphics2D canvas = Theme.smooth(graphics);
                canvas.setFont(Theme.font(15, Font.PLAIN));
                canvas.setColor(Theme.MUTED);
                FontMetrics metrics = canvas.getFontMetrics();
                int x = Math.max(12, (getWidth() - metrics.stringWidth(emptyMessage)) / 2);
                canvas.drawString(emptyMessage, x, Math.max(30, getHeight() / 2));
                canvas.dispose();
            }
        };
        setViewportView(table);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        setOpaque(false);
        setBackground(Theme.WHITE);
        getViewport().setBackground(Theme.WHITE);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(43);
        table.setShowVerticalLines(false);
        table.setGridColor(Theme.OUTLINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionForeground(Theme.INK);
        table.setSelectionBackground(Theme.ACCENT_TINT);
        table.setBackground(Theme.WHITE);
        table.setForeground(Theme.INK);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBackground(Theme.BACKGROUND);
        table.getTableHeader().setForeground(Theme.MUTED);
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((source, value, selected, focused, row, index) -> {
            JLabel label = (JLabel) headerRenderer.getTableCellRendererComponent(source, value, selected, focused, row, index);
            label.setFont(Theme.font(13, Font.BOLD));
            label.setBackground(Theme.BACKGROUND);
            label.setForeground(Theme.MUTED);
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.OUTLINE),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            Class<?> type = columns.get(table.convertColumnIndexToModel(index)).type();
            label.setHorizontalAlignment(Number.class.isAssignableFrom(type) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return label;
        });

        for (int index = 0; index < columns.size(); index++) {
            Column<T> column = columns.get(index);
            table.getColumnModel().getColumn(index).setPreferredWidth(column.width());
            if (column.type() == LocalDate.class) {
                table.getColumnModel().getColumn(index).setMinWidth(105);
            }
            table.getColumnModel().getColumn(index).setCellRenderer(new CellRenderer(column.type()));
        }
    }

    @Override
    protected boolean isPaintingOrigin() {
        return true;
    }

    @Override
    public void paint(Graphics graphics) {
        Graphics2D canvas = Theme.smooth(graphics);
        var outline = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 21, 21);
        canvas.clip(outline);
        super.paint(canvas);
        canvas.dispose();

        Graphics2D border = Theme.smooth(graphics);
        border.setColor(Theme.OUTLINE);
        border.draw(outline);
        border.dispose();
    }

    JTable table() {
        return table;
    }

    void setRows(List<T> items, String emptyMessage) {
        rows = List.copyOf(items);
        this.emptyMessage = emptyMessage;
        table.getAccessibleContext().setAccessibleDescription(rows.isEmpty() ? emptyMessage : null);
        model.fireTableDataChanged();
        table.repaint();
    }

    T selection() {
        int viewIndex = table.getSelectedRow();
        return viewIndex < 0 ? null : rows.get(table.convertRowIndexToModel(viewIndex));
    }

    record Column<T>(String label, Class<?> type, int width, Function<T, ?> value) {
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private final Class<?> type;
        private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.KOREA);

        CellRenderer(Class<?> type) {
            this.type = type;
            setHorizontalAlignment(Number.class.isAssignableFrom(type) ? SwingConstants.RIGHT : SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Object display = value;
            if (value instanceof Number number) {
                display = numbers.format(number) + (type == Long.class ? "원" : "");
            }
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, display, selected, focused, row, column);
            if (!focused) {
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            }
            label.setFont(Theme.font(14, value instanceof Enum<?> ? Font.BOLD : Font.PLAIN));
            label.setToolTipText(display == null ? null : display.toString());
            return label;
        }
    }
}
