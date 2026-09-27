package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Main admin window: a sidebar for navigation and one screen for each feature
 * (the "BusReservation" class from the proposal).
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class AdminPortal extends JFrame {

    public static final String DASHBOARD = "Dashboard";
    public static final String NEW_BOOKING = "New Booking";
    public static final String BOOKINGS = "Bookings";
    public static final String ROUTES = "Routes & Buses";
    public static final String REPORTS = "Reports";
    public static final String SETTINGS = "Settings";

    private final DataStore store;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, NavItem> navItems = new LinkedHashMap<>();
    private final JLabel pageTitle = Theme.label("", Theme.TITLE, Theme.TEXT);
    private final BookingPanel bookingPanel;

    public AdminPortal(DataStore store) {
        this.store = store;
        setTitle("Bus Reservation System - Admin Portal");
        setIconImage(Theme.appIcon());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        bookingPanel = new BookingPanel(store, this);
        content.setBackground(Theme.BACKGROUND);
        content.add(new DashboardPanel(store, this), DASHBOARD);
        content.add(bookingPanel, NEW_BOOKING);
        content.add(new BookingsPanel(store, this), BOOKINGS);
        content.add(new RoutesPanel(store), ROUTES);
        content.add(new ReportsPanel(store), REPORTS);
        content.add(new SettingsPanel(store), SETTINGS);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BACKGROUND);
        main.add(createHeader(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        add(createSidebar(), BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        navigate(DASHBOARD);
    }

    public void navigate(String page) {
        cards.show(content, page);
        pageTitle.setText(page);
        for (Map.Entry<String, NavItem> item : navItems.entrySet()) {
            item.getValue().setActive(item.getKey().equals(page));
        }
    }

    /** Opens the booking screen with an existing booking loaded for editing. */
    public void editBooking(Booking booking) {
        bookingPanel.edit(booking);
        navigate(NEW_BOOKING);
        pageTitle.setText("Edit Booking");
    }

    public void startNewBooking() {
        bookingPanel.reset();
        navigate(NEW_BOOKING);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
        JLabel date = Theme.label(today, Theme.BODY, Theme.MUTED);
        JLabel user = Theme.label("  Signed in as " + store.getUsername(), Theme.LABEL, Theme.PRIMARY);
        store.addChangeListener(() -> user.setText("  Signed in as " + store.getUsername()));
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        date.setAlignmentX(RIGHT_ALIGNMENT);
        user.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(date);
        right.add(user);
        header.add(pageTitle, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, 0, getHeight(), Theme.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 14, 22, 14));

        JPanel logo = new Theme.Card(new BorderLayout());
        logo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        logo.add(new JLabel(Theme.image("Bus.png", 170)), BorderLayout.CENTER);
        logo.setMaximumSize(new Dimension(202, 120));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(10));
        JLabel name = Theme.label("BUS RESERVATION", new Font("Segoe UI", Font.BOLD, 15), Color.WHITE);
        name.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        name.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(name);
        sidebar.add(Box.createVerticalStrut(22));

        String[][] items = {
            {DASHBOARD, "▣"}, {NEW_BOOKING, "+"}, {BOOKINGS, "☰"},
            {ROUTES, "⇄"}, {REPORTS, "▤"}, {SETTINGS, "⚙"}
        };
        for (String[] item : items) {
            NavItem nav = new NavItem(item[0], item[1]);
            nav.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (NEW_BOOKING.equals(item[0]) && !bookingPanel.isEditing()) {
                        bookingPanel.reset();
                    }
                    navigate(item[0]);
                }
            });
            navItems.put(item[0], nav);
            sidebar.add(nav);
            sidebar.add(Box.createVerticalStrut(4));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton logout = Theme.button("Log out", Theme.ButtonStyle.SECONDARY);
        logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(202, 42));
        logout.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(this, "Do you want to log out?", "Log out",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                store.clearChangeListeners();
                dispose();
                new LoginFrame(store).setVisible(true);
            }
        });
        sidebar.add(logout);
        return sidebar;
    }

    /** One clickable entry in the sidebar. */
    private static class NavItem extends JPanel {
        private boolean active;
        private boolean hover;

        NavItem(String text, String icon) {
            setLayout(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
            setMaximumSize(new Dimension(202, 44));
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JLabel iconLabel = Theme.label(icon, new Font("Dialog", Font.BOLD, 16), Color.WHITE);
            iconLabel.setPreferredSize(new Dimension(20, 20));
            add(iconLabel, BorderLayout.WEST);
            add(Theme.label(text, Theme.LABEL, Color.WHITE), BorderLayout.CENTER);
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

        void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, active ? 45 : 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (active) {
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
