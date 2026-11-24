package management;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxUI;

final class Theme {
    static final Color BACKGROUND = new Color(0xF5F5FC);
    static final Color WHITE = Color.WHITE;
    static final Color INK = new Color(0x28243F);
    static final Color MUTED = new Color(0x5E5872);
    static final Color OUTLINE = new Color(0xE4E0F0);
    static final Color ACCENT = new Color(0x5B4DC2);
    static final Color ACCENT_TINT = new Color(0xEDE8FF);
    static final Color ERROR = new Color(0xA5283C);

    private Theme() {
    }

    static void install() {
        for (String key : new String[]{"Label", "Button", "TextField", "ComboBox", "List", "Table",
                "TableHeader", "OptionPane"}) {
            UIManager.put(key + ".font", font(14, Font.PLAIN));
        }
        UIManager.put("OptionPane.messageFont", font(14, Font.PLAIN));
        UIManager.put("OptionPane.buttonFont", font(14, Font.PLAIN));
        UIManager.put("OptionPane.okButtonText", "확인");
        UIManager.put("TextField.caretForeground", INK);
        UIManager.put("TextField.selectionBackground", ACCENT_TINT);
        UIManager.put("ComboBox.selectionBackground", ACCENT_TINT);
        UIManager.put("ComboBox.selectionForeground", INK);
    }

    static Font font(int size, int style) {
        return new Font("Malgun Gothic", style, size);
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel card(LayoutManager layout, Color background) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = smooth(graphics);
                canvas.setColor(background);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                canvas.setColor(OUTLINE);
                canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                canvas.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    static JButton button(String text, ButtonStyle style) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = smooth(graphics);
                boolean selected = Boolean.TRUE.equals(getClientProperty("currentPage"));
                Color fill = style.background;
                if (isEnabled() && (selected || getModel().isRollover() || getModel().isPressed())) {
                    fill = style.activeBackground;
                }
                canvas.setColor(fill);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                if (style == ButtonStyle.SECONDARY || isFocusOwner()) {
                    canvas.setColor(isFocusOwner() ? ACCENT : OUTLINE);
                    canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                }
                canvas.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(font(14, Font.BOLD));
        button.setForeground(style.foreground);
        button.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 38));
        return button;
    }

    static JTextField field(String text, int columns) {
        JTextField field = new JTextField(text, columns) {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintField(graphics, this, isFocusOwner());
                super.paintComponent(graphics);
            }
        };
        field.setOpaque(false);
        field.setForeground(INK);
        field.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        return field;
    }

    static <T> JComboBox<T> combo(T[] choices) {
        JComboBox<T> box = new JComboBox<>(choices) {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintField(graphics, this, isFocusOwner());
                super.paintComponent(graphics);
            }
        };
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D canvas = smooth(graphics);
                        int x = getWidth() / 2;
                        int y = getHeight() / 2;
                        canvas.setColor(MUTED);
                        canvas.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        canvas.drawPolyline(new int[]{x - 4, x, x + 4}, new int[]{y - 2, y + 2, y - 2}, 3);
                        canvas.dispose();
                    }
                };
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setContentAreaFilled(false);
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean focused) {
                // The round field background is painted by the combo box.
            }
        });
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                           boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setOpaque(index >= 0);
                setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                setForeground(INK);
                return this;
            }
        });
        box.setOpaque(false);
        box.setBackground(WHITE);
        box.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 5));
        return box;
    }

    private static void paintField(Graphics graphics, JComponent field, boolean focused) {
        Graphics2D canvas = smooth(graphics);
        canvas.setColor(WHITE);
        canvas.fillRoundRect(0, 0, field.getWidth(), field.getHeight(), 16, 16);
        canvas.setColor(focused ? ACCENT : OUTLINE);
        canvas.drawRoundRect(0, 0, field.getWidth() - 1, field.getHeight() - 1, 16, 16);
        canvas.dispose();
    }

    static Graphics2D smooth(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return canvas;
    }

    enum ButtonStyle {
        PRIMARY(WHITE, ACCENT, new Color(0x5042B0)),
        SECONDARY(INK, WHITE, BACKGROUND),
        NAV(MUTED, WHITE, ACCENT_TINT);

        private final Color foreground;
        private final Color background;
        private final Color activeBackground;

        ButtonStyle(Color foreground, Color background, Color activeBackground) {
            this.foreground = foreground;
            this.background = background;
            this.activeBackground = activeBackground;
        }
    }

    enum Glyph implements Icon {
        HOME, PRODUCT, AD;

        @Override
        public int getIconWidth() {
            return 21;
        }

        @Override
        public int getIconHeight() {
            return 21;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D canvas = smooth(graphics);
            canvas.translate(x, y);
            canvas.setColor(component.getForeground());
            canvas.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (this) {
                case HOME -> {
                    canvas.drawRoundRect(3, 3, 6, 6, 2, 2);
                    canvas.drawRoundRect(12, 3, 6, 6, 2, 2);
                    canvas.drawRoundRect(3, 12, 6, 6, 2, 2);
                    canvas.drawRoundRect(12, 12, 6, 6, 2, 2);
                }
                case PRODUCT -> {
                    Path2D box = new Path2D.Double();
                    box.moveTo(10.5, 2.5);
                    box.lineTo(18, 6.5);
                    box.lineTo(18, 14.5);
                    box.lineTo(10.5, 18.5);
                    box.lineTo(3, 14.5);
                    box.lineTo(3, 6.5);
                    box.closePath();
                    box.moveTo(3, 6.5);
                    box.lineTo(10.5, 10.5);
                    box.lineTo(18, 6.5);
                    box.moveTo(10.5, 10.5);
                    box.lineTo(10.5, 18.5);
                    canvas.draw(box);
                }
                case AD -> {
                    Path2D horn = new Path2D.Double();
                    horn.moveTo(3, 8);
                    horn.lineTo(8, 8);
                    horn.lineTo(17, 4);
                    horn.lineTo(17, 16);
                    horn.lineTo(8, 12);
                    horn.lineTo(3, 12);
                    horn.closePath();
                    horn.moveTo(6, 12);
                    horn.lineTo(8, 17);
                    horn.lineTo(10, 17);
                    horn.lineTo(8.5, 12.5);
                    canvas.draw(horn);
                }
            }
            canvas.dispose();
        }
    }
}
