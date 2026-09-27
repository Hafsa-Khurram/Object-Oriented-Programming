package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Home screen with live numbers, the busiest routes and the latest bookings.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class DashboardPanel extends JPanel {

    private final DataStore store;
    private final JLabel bookingsValue = Theme.label("0", Theme.BIG_NUMBER, Theme.TEXT);
    private final JLabel passengersValue = Theme.label("0", Theme.BIG_NUMBER, Theme.TEXT);
    private final JLabel revenueValue = Theme.label("0", Theme.BIG_NUMBER, Theme.TEXT);
    private final JLabel todayValue = Theme.label("0", Theme.BIG_NUMBER, Theme.TEXT);
    private final JLabel bookingsNote = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel passengersNote = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel revenueNote = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel todayNote = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final BarChart chart = new BarChart();
    private final DefaultTableModel recentModel;

    public DashboardPanel(DataStore store, AdminPortal portal) {
        this.store = store;
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel stats = new JPanel(new GridLayout(1, 4, 18, 0));
        stats.setOpaque(false);
        stats.add(statCard("TOTAL BOOKINGS", bookingsValue, bookingsNote, Theme.PRIMARY));
        stats.add(statCard("PASSENGERS (SEATS SOLD)", passengersValue, passengersNote, Theme.SUCCESS));
        stats.add(statCard("TOTAL EARNINGS", revenueValue, revenueNote, Theme.ACCENT));
        stats.add(statCard("TRIPS TODAY", todayValue, todayNote, new Color(14, 116, 144)));

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(createWelcome(portal), BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        recentModel = new DefaultTableModel(new String[]{"Passenger", "Route", "Date", "Fare", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable recent = new JTable(recentModel);
        Theme.styleTable(recent);
        recent.getColumnModel().getColumn(4).setCellRenderer(Theme.statusRenderer());
        Theme.columnWidths(recent, 125, 165, 80, 90, 110);

        Theme.Card recentCard = new Theme.Card(new BorderLayout(0, 12));
        recentCard.add(Theme.label("Latest Bookings", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        recentCard.add(Theme.scroll(recent), BorderLayout.CENTER);

        Theme.Card chartCard = new Theme.Card(new BorderLayout(0, 12));
        chartCard.add(Theme.label("Top Routes by Earnings", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        chart.setEmptyMessage("Earnings will appear here after the first booking.");
        chartCard.add(chart, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.64;
        c.insets = new Insets(0, 0, 0, 9);
        bottom.add(recentCard, c);
        c.gridx = 1;
        c.weightx = 0.36;
        c.insets = new Insets(0, 9, 0, 0);
        bottom.add(chartCard, c);
        add(bottom, BorderLayout.CENTER);

        store.addChangeListener(this::refresh);
        refresh();
    }

    private JComponent createWelcome(AdminPortal portal) {
        Theme.Card card = new Theme.Card(new BorderLayout(), Theme.PRIMARY);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        text.add(Theme.label("Welcome back, " + store.getUsername() + "!", Theme.HEADING, Color.WHITE));
        text.add(Theme.label("Here is what is happening with your buses today.", Theme.BODY,
                new Color(220, 220, 255)));
        JButton book = Theme.button("+  Book a Ticket", Theme.ButtonStyle.SECONDARY);
        book.addActionListener(e -> portal.startNewBooking());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(book);
        card.add(text, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JComponent statCard(String title, JLabel value, JLabel note, Color color) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 6));
        JPanel bar = new JPanel();
        bar.setBackground(color);
        bar.setPreferredSize(new java.awt.Dimension(5, 0));
        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        text.add(Theme.label(title, Theme.SMALL, Theme.MUTED));
        text.add(value);
        text.add(note);
        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void refresh() {
        int confirmed = 0;
        int cancelled = 0;
        int seats = 0;
        int upcomingSeats = 0;
        double revenue = 0;
        int todayTrips = 0;
        int todaySeats = 0;
        LocalDate today = LocalDate.now();
        Map<String, Double> perRoute = new LinkedHashMap<>();
        for (Booking b : store.getBookings()) {
            if (!b.isConfirmed()) {
                cancelled++;
                continue;
            }
            confirmed++;
            seats += b.getSeatCount();
            revenue += b.getFare();
            if (!b.getTravelDate().isBefore(today)) {
                upcomingSeats += b.getSeatCount();
            }
            if (b.getTravelDate().equals(today)) {
                todayTrips++;
                todaySeats += b.getSeatCount();
            }
            perRoute.merge(b.getRouteName(), b.getFare(), Double::sum);
        }
        bookingsValue.setText(String.valueOf(confirmed));
        bookingsNote.setText(cancelled + " cancelled");
        passengersValue.setText(String.valueOf(seats));
        passengersNote.setText(upcomingSeats + " still to travel");
        revenueValue.setText(Theme.money(revenue));
        revenueNote.setText(perRoute.size() + " route(s) with sales");
        todayValue.setText(String.valueOf(todayTrips));
        todayNote.setText(todaySeats + " seat(s) departing today");

        List<Map.Entry<String, Double>> entries = new ArrayList<>(perRoute.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < Math.min(7, entries.size()); i++) {
            labels.add(entries.get(i).getKey());
            values.add(entries.get(i).getValue());
            texts.add(Theme.money(entries.get(i).getValue()));
        }
        chart.setData(labels, values, texts);

        recentModel.setRowCount(0);
        List<Booking> all = new ArrayList<>(store.getBookings());
        all.sort((a, b) -> b.getBookedAt().compareTo(a.getBookedAt()));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
        for (int i = 0; i < Math.min(8, all.size()); i++) {
            Booking b = all.get(i);
            recentModel.addRow(new Object[]{b.getFullName(), b.getRouteName(),
                b.getTravelDate().format(format), Theme.money(b.getFare()), b.getStatus()});
        }
    }
}
