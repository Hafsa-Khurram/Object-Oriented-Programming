package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Reports: passengers and earnings for every route, and the passenger list of one route.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class ReportsPanel extends JPanel {

    private static final String ALL_TIME = "All time";
    private static final String THIS_MONTH = "This month";
    private static final String TODAY = "Today";
    private static final String UPCOMING = "Upcoming trips";

    private final DataStore store;
    private final JComboBox<String> period = Theme.comboBox();
    private final JComboBox<String> routeBox = Theme.comboBox();
    private final DefaultTableModel summaryModel;
    private final DefaultTableModel detailModel;
    private final JLabel totalsLabel = Theme.label(" ", Theme.LABEL, Theme.PRIMARY);
    private final JLabel routeTotals = Theme.label(" ", Theme.LABEL, Theme.PRIMARY);
    private boolean updating;
    /** True once the admin picks a route; until then the busiest route is shown automatically. */
    private boolean routeChosenByUser;

    public ReportsPanel(DataStore store) {
        this.store = store;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        for (String p : new String[]{ALL_TIME, THIS_MONTH, TODAY, UPCOMING}) {
            period.addItem(p);
        }
        period.setPreferredSize(new java.awt.Dimension(170, 38));
        routeBox.setPreferredSize(new java.awt.Dimension(260, 38));
        period.addActionListener(e -> refresh());
        routeBox.addActionListener(e -> {
            if (!updating) {
                routeChosenByUser = true;
                refreshDetails();
            }
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.add(Theme.label("Period", Theme.LABEL, Theme.TEXT));
        bar.add(period);
        add(bar, BorderLayout.NORTH);

        summaryModel = Theme.tableModel(new String[]{"Route", "Seats", "Earnings"},
                String.class, Integer.class, Double.class);
        JTable summary = Theme.table(summaryModel);
        Theme.styleTable(summary);
        Theme.columnWidths(summary, 180, 70, 100);
        summary.getSelectionModel().addListSelectionListener(e -> {
            int row = summary.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                routeBox.setSelectedItem(summaryModel.getValueAt(summary.convertRowIndexToModel(row), 0));
            }
        });
        Theme.Card summaryCard = new Theme.Card(new BorderLayout(0, 12));
        summaryCard.add(Theme.label("Summary by Route", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        summaryCard.add(Theme.scroll(summary), BorderLayout.CENTER);
        summaryCard.add(totalsLabel, BorderLayout.SOUTH);

        detailModel = Theme.tableModel(new String[]{"Ticket", "Passenger", "Date", "Seats", "Payment", "Status"},
                String.class, String.class, LocalDate.class, Integer.class, Double.class, String.class);
        JTable details = Theme.table(detailModel);
        Theme.styleTable(details);
        details.getColumnModel().getColumn(5).setCellRenderer(Theme.statusRenderer());
        Theme.columnWidths(details, 85, 110, 110, 65, 92, 100);
        details.putClientProperty(Theme.EMPTY_TEXT, "No bookings on this route for the selected period.");
        JPanel detailTop = new JPanel(new BorderLayout(10, 0));
        detailTop.setOpaque(false);
        detailTop.add(Theme.label("Route Details", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        detailTop.add(routeBox, BorderLayout.EAST);
        Theme.Card detailCard = new Theme.Card(new BorderLayout(0, 12));
        detailCard.add(detailTop, BorderLayout.NORTH);
        detailCard.add(Theme.scroll(details), BorderLayout.CENTER);
        detailCard.add(routeTotals, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.38;
        c.insets = new Insets(0, 0, 0, 9);
        summaryCard.setPreferredSize(new java.awt.Dimension(100, 100));
        center.add(summaryCard, c);
        c.gridx = 1;
        c.weightx = 0.62;
        c.insets = new Insets(0, 9, 0, 0);
        detailCard.setPreferredSize(new java.awt.Dimension(100, 100));
        center.add(detailCard, c);
        add(center, BorderLayout.CENTER);

        store.addChangeListener(this::refresh);
        refresh();
    }

    private boolean inPeriod(Booking b) {
        LocalDate date = b.getTravelDate();
        LocalDate today = LocalDate.now();
        String p = (String) period.getSelectedItem();
        if (THIS_MONTH.equals(p)) {
            return date.getYear() == today.getYear() && date.getMonth() == today.getMonth();
        }
        if (TODAY.equals(p)) {
            return date.equals(today);
        }
        if (UPCOMING.equals(p)) {
            return !b.hasDeparted();
        }
        return true;
    }

    private void refresh() {
        Map<String, int[]> counts = new TreeMap<>();
        Map<String, Double> earnings = new TreeMap<>();
        for (Route r : store.getRoutes()) {
            counts.put(r.getName(), new int[3]);
            earnings.put(r.getName(), 0.0);
        }
        int totalBookings = 0;
        int totalSeats = 0;
        double totalEarnings = 0;
        for (Booking b : store.getBookings()) {
            if (!inPeriod(b)) {
                continue;
            }
            int[] c = counts.computeIfAbsent(b.getRouteName(), k -> new int[3]);
            earnings.putIfAbsent(b.getRouteName(), 0.0);
            if (b.isConfirmed()) {
                c[0]++;
                c[1] += b.getSeatCount();
                earnings.merge(b.getRouteName(), b.getFare(), Double::sum);
                totalBookings++;
                totalSeats += b.getSeatCount();
                totalEarnings += b.getFare();
            } else {
                c[2]++;
            }
        }
        summaryModel.setRowCount(0);
        List<Map.Entry<String, Double>> rows = new ArrayList<>(earnings.entrySet());
        rows.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<String, Double> row : rows) {
            int[] c = counts.get(row.getKey());
            summaryModel.addRow(new Object[]{row.getKey(), c[1], row.getValue()});
        }
        totalsLabel.setText(totalBookings + " tickets  |  " + totalSeats + " seats  |  " + Theme.money(totalEarnings));

        updating = true;
        Object current = routeBox.getSelectedItem();
        routeBox.removeAllItems();
        for (Map.Entry<String, Double> row : rows) {
            routeBox.addItem(row.getKey());
        }
        if (current != null && routeChosenByUser) {
            routeBox.setSelectedItem(current);
        } else if (routeBox.getItemCount() > 0) {
            // Routes are sorted by earnings, so this shows the busiest route first.
            routeBox.setSelectedIndex(0);
        }
        updating = false;
        refreshDetails();
    }

    private void refreshDetails() {
        detailModel.setRowCount(0);
        String route = (String) routeBox.getSelectedItem();
        if (route == null) {
            routeTotals.setText(" ");
            return;
        }
        int seats = 0;
        double total = 0;
        int passengers = 0;
        List<Booking> list = new ArrayList<>(store.getBookings());
        list.sort((a, b) -> a.getTravelDate().compareTo(b.getTravelDate()));
        for (Booking b : list) {
            if (!route.equals(b.getRouteName()) || !inPeriod(b)) {
                continue;
            }
            detailModel.addRow(new Object[]{b.getTicketNo(), b.getFullName(), b.getTravelDate(),
                b.getSeatCount(), b.getFare(), b.getStatus()});
            if (b.isConfirmed()) {
                passengers++;
                seats += b.getSeatCount();
                total += b.getFare();
            }
        }
        routeTotals.setText(passengers + " ticket(s)  |  " + seats + " seat(s)  |  " + Theme.money(total));
    }
}
