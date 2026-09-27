package busreservationsystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Colours, fonts and small helpers so every screen has the same modern look.
 * The indigo colour matches the bus logo.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public final class Theme {

    public static final Color PRIMARY = new Color(54, 51, 138);
    public static final Color PRIMARY_DARK = new Color(38, 36, 102);
    public static final Color PRIMARY_LIGHT = new Color(232, 231, 250);
    public static final Color ACCENT = new Color(245, 158, 11);
    public static final Color ACCENT_LIGHT = new Color(254, 243, 199);
    public static final Color BACKGROUND = new Color(244, 245, 251);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color BORDER = new Color(226, 228, 238);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color SUCCESS_LIGHT = new Color(220, 252, 231);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color DANGER_LIGHT = new Color(254, 226, 226);
    public static final Color SEAT_BOOKED = new Color(203, 206, 216);

    private static final String FAMILY = "Segoe UI";
    public static final Font TITLE = new Font(FAMILY, Font.BOLD, 26);
    public static final Font HEADING = new Font(FAMILY, Font.BOLD, 17);
    public static final Font LABEL = new Font(FAMILY, Font.BOLD, 13);
    public static final Font BODY = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font BIG_NUMBER = new Font(FAMILY, Font.BOLD, 28);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");
    private static final java.time.format.DateTimeFormatter TABLE_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH);

    private Theme() {
    }

    /** Sets Nimbus with the app colours. Falls back quietly to the default look. */
    public static void install() {
        try {
            UIManager.put("control", BACKGROUND);
            UIManager.put("nimbusBase", PRIMARY);
            UIManager.put("nimbusBlueGrey", new Color(190, 193, 214));
            UIManager.put("nimbusFocus", PRIMARY);
            UIManager.put("nimbusSelectionBackground", PRIMARY);
            UIManager.put("nimbusLightBackground", Color.WHITE);
            UIManager.put("text", TEXT);
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not set look and feel: " + ex.getMessage());
        }
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", LABEL);
        UIManager.put("ToolTip.font", SMALL);
    }

    public static String money(double amount) {
        return "Rs " + MONEY.format(amount);
    }

    // ===================== Images =====================

    /**
     * Loads an image from the package (works inside the .jar) and falls back to the
     * source folder, so it works from NetBeans, VS Code and the command line.
     */
    public static ImageIcon image(String name) {
        URL url = Theme.class.getResource(name);
        if (url != null) {
            return new ImageIcon(url);
        }
        File file = new File("src/busreservationsystem/" + name);
        return file.exists() ? new ImageIcon(file.getPath()) : new ImageIcon();
    }

    public static ImageIcon image(String name, int width) {
        ImageIcon icon = image(name);
        if (icon.getIconWidth() <= 0) {
            return icon;
        }
        int height = icon.getIconHeight() * width / icon.getIconWidth();
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public static Image appIcon() {
        return image("Bus.png").getImage();
    }

    // ===================== Components =====================

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JTextField textField() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleInput(field);
        return field;
    }

    public static void styleInput(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PRIMARY);
        field.setBorder(inputBorder(BORDER));
        field.setPreferredSize(new Dimension(200, 38));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(inputBorder(PRIMARY));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(inputBorder(BORDER));
            }
        });
    }

    private static Border inputBorder(Color color) {
        return BorderFactory.createCompoundBorder(new RoundBorder(color, 10),
                BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    public static <T> JComboBox<T> comboBox() {
        JComboBox<T> box = new JComboBox<>();
        styleComboBox(box);
        return box;
    }

    /** Flat white drop-down with a rounded border, matching the text fields. */
    public static void styleComboBox(JComboBox<?> box) {
        box.setUI(new FlatComboUI());
        box.setFont(BODY);
        box.setForeground(TEXT);
        box.setBackground(Color.WHITE);
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(BORDER, 10),
                BorderFactory.createEmptyBorder(1, 8, 1, 2)));
        box.setPreferredSize(new Dimension(200, 38));
        box.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, false);
                cell.setFont(BODY);
                cell.setBorder(BorderFactory.createEmptyBorder(6, index < 0 ? 4 : 10, 6, 10));
                cell.setForeground(TEXT);
                cell.setBackground(selected && index >= 0 ? PRIMARY_LIGHT : Color.WHITE);
                return cell;
            }
        });
        if (box.isEditable()) {
            makeEditorFlat(box);
        }
        box.addPropertyChangeListener("editable", e -> makeEditorFlat(box));
    }

    private static void makeEditorFlat(JComboBox<?> box) {
        Component editor = box.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField field = (JTextField) editor;
            field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            field.setFont(BODY);
            field.setBackground(Color.WHITE);
            field.setForeground(TEXT);
        }
    }

    /** Plain spinner (date or number) with flat arrows, matching the text fields. */
    public static void styleSpinner(javax.swing.JSpinner spinner) {
        spinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
            @Override
            protected Component createNextButton() {
                Component button = chevronButton(true);
                button.setName("Spinner.nextButton");
                installNextButtonListeners(button);
                return button;
            }

            @Override
            protected Component createPreviousButton() {
                Component button = chevronButton(false);
                button.setName("Spinner.previousButton");
                installPreviousButtonListeners(button);
                return button;
            }
        });
        spinner.setFont(BODY);
        spinner.setOpaque(true);
        spinner.setBackground(Color.WHITE);
        spinner.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(BORDER, 10),
                BorderFactory.createEmptyBorder(1, 8, 1, 2)));
        spinner.setPreferredSize(new Dimension(200, 38));
        if (spinner.getEditor() instanceof javax.swing.JSpinner.DefaultEditor) {
            JTextField field = ((javax.swing.JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            field.setUI(new javax.swing.plaf.basic.BasicFormattedTextFieldUI());
            field.setFont(BODY);
            field.setForeground(TEXT);
            field.setBackground(Color.WHITE);
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setHorizontalAlignment(JTextField.LEFT);
        }
    }

    /** Small borderless button that draws an up or down chevron. */
    private static JButton chevronButton(boolean up) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? PRIMARY : MUTED);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                if (up) {
                    g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy + 2, cy - 2, cy + 2}, 3);
                } else {
                    g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                }
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(22, 16));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** Panel that fills the width of its scroll pane and only scrolls vertically when needed. */
    public static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /**
     * Table that fills the available width, but scrolls sideways instead of squeezing
     * the columns when the window is too narrow.
     */
    public static JTable table(javax.swing.table.TableModel model) {
        return new JTable(model) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width <= getParent().getWidth();
            }
        };
    }

    /** Drop-down look without the Nimbus gradient. */
    public static class FlatComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton button = chevronButton(false);
            button.setPreferredSize(new Dimension(28, 28));
            return button;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
            g.setColor(Color.WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        public void paint(Graphics g, javax.swing.JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(comboBox.isEnabled() ? Color.WHITE : BACKGROUND);
            g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
            g2.dispose();
            super.paint(g, c);
        }
    }

    /** Thin rounded scroll bar in the app colours. */
    public static class FlatScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(196, 199, 224);
            trackColor = Color.WHITE;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private static JButton zeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, javax.swing.JComponent c, java.awt.Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, javax.swing.JComponent c, java.awt.Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? PRIMARY : thumbColor);
            g2.fillRoundRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 8, 8);
            g2.dispose();
        }
    }

    public static void flatScrollBars(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new FlatScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new FlatScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
    }

    public static void styleTable(JTable table) {
        table.setFont(BODY);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                String text = String.valueOf(value);
                if (t != null && t.getRowSorter() != null) {
                    for (javax.swing.RowSorter.SortKey key : t.getRowSorter().getSortKeys()) {
                        if (key.getColumn() == t.convertColumnIndexToModel(column)) {
                            text += key.getSortOrder() == javax.swing.SortOrder.ASCENDING ? "  \u25B2"
                                    : key.getSortOrder() == javax.swing.SortOrder.DESCENDING ? "  \u25BC" : "";
                        }
                    }
                }
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, text, selected, focus, row, column);
                cell.setToolTipText("Click to sort by " + value);
                cell.setFont(LABEL);
                cell.setForeground(Color.WHITE);
                cell.setBackground(PRIMARY);
                cell.setOpaque(true);
                cell.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return cell;
            }
        });
        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, display(value), selected, false,
                        row, column);
                cell.setHorizontalAlignment(SwingConstants.LEFT);
                cell.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!selected) {
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                cell.setForeground(TEXT);
                return cell;
            }
        };
        // Numbers and dates are stored as real values (so sorting is correct) and formatted here.
        table.setDefaultRenderer(Object.class, cells);
        table.setDefaultRenderer(Number.class, cells);
        table.setDefaultRenderer(Double.class, cells);
        table.setDefaultRenderer(Integer.class, cells);
    }

    /** Formats table values: dates as "28 Sep 2026", times as "10:00 AM", amounts as "Rs 2,500". */
    public static Object display(Object value) {
        if (value instanceof java.time.LocalDate) {
            return ((java.time.LocalDate) value).format(TABLE_DATE);
        }
        if (value instanceof java.time.LocalTime) {
            return ((java.time.LocalTime) value).format(DataStore.TIME_FORMAT);
        }
        if (value instanceof Double) {
            return money((Double) value);
        }
        return value;
    }

    /**
     * Read-only table model that knows the type of each column, so clicking a column
     * header sorts dates, times and amounts correctly instead of alphabetically.
     */
    public static javax.swing.table.DefaultTableModel tableModel(String[] columns, Class<?>... types) {
        return new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column < types.length ? types[column] : Object.class;
            }
        };
    }

    public static void columnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(new RoundBorder(BORDER, 12));
        scroll.getViewport().setBackground(Color.WHITE);
        flatScrollBars(scroll);
        JPanel corner = new JPanel();
        corner.setBackground(PRIMARY);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        return scroll;
    }

    /** Status cell with a coloured pill ("Confirmed" green, "Cancelled" red). */
    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, column);
                boolean ok = Booking.CONFIRMED.equals(value);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(LABEL);
                cell.setForeground(ok ? SUCCESS : DANGER);
                if (!selected) {
                    cell.setBackground(ok ? SUCCESS_LIGHT : DANGER_LIGHT);
                }
                return cell;
            }
        };
    }

    // ===================== Custom painted widgets =====================

    public enum ButtonStyle { PRIMARY, SECONDARY, DANGER, SUCCESS, GHOST }

    public static JButton button(String text, ButtonStyle style) {
        return new ModernButton(text, style);
    }

    /** Flat rounded button with a hover effect. */
    public static class ModernButton extends JButton {
        private final ButtonStyle style;
        private boolean hover;

        public ModernButton(String text, ButtonStyle style) {
            super(text);
            this.style = style;
            setFont(LABEL);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
            setForeground(style == ButtonStyle.SECONDARY || style == ButtonStyle.GHOST ? PRIMARY : Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill;
            switch (style) {
                case DANGER:
                    fill = hover ? DANGER.darker() : DANGER;
                    break;
                case SUCCESS:
                    fill = hover ? SUCCESS.darker() : SUCCESS;
                    break;
                case SECONDARY:
                    fill = hover ? new Color(214, 212, 245) : PRIMARY_LIGHT;
                    break;
                case GHOST:
                    fill = hover ? PRIMARY_LIGHT : new Color(0, 0, 0, 0);
                    break;
                default:
                    fill = hover ? PRIMARY_DARK : PRIMARY;
            }
            if (!isEnabled()) {
                fill = SEAT_BOOKED;
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** White panel with rounded corners and a soft border, used as a "card". */
    public static class Card extends JPanel {
        private final Color fill;

        public Card(LayoutManager layout) {
            this(layout, CARD);
        }

        public Card(LayoutManager layout, Color fill) {
            super(layout);
            this.fill = fill;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 12));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, 18, 18);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded line border. */
    public static class RoundBorder implements Border {
        private final Color color;
        private final int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(Component c) {
            return new java.awt.Insets(2, 2, 2, 2);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
