package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * All bookings in a searchable, filterable table with view / edit / cancel / delete.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class BookingsPanel extends JPanel {

    private static final String ALL_ROUTES = "All routes";

    private final DataStore store;
    private final AdminPortal portal;
    private final JTextField search = Theme.textField();
    private final JComboBox<String> routeFilter = Theme.comboBox();
    private final JComboBox<String> whenFilter = Theme.comboBox();
    private final JComboBox<String> statusFilter = Theme.comboBox();
    private final JLabel countLabel = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Booking> shown = new ArrayList<>();
    private boolean updating;

    public BookingsPanel(DataStore store, AdminPortal portal) {
        this.store = store;
        this.portal = portal;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        String[] columns = {"Ticket", "Passenger", "Route", "Date", "Time", "Seats", "Class", "Fare", "Status"};
        model = Theme.tableModel(columns, String.class, String.class, String.class, LocalDate.class,
                java.time.LocalTime.class, String.class, String.class, Double.class, String.class);
        table = new JTable(model);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(8).setCellRenderer(Theme.statusRenderer());
        Theme.columnWidths(table, 85, 150, 200, 110, 85, 85, 80, 90, 110);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selected() != null) {
                    new TicketDialog(portal, selected()).setVisible(true);
                }
            }
        });

        add(createToolbar(), BorderLayout.NORTH);
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        card.add(createActions(), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        store.addChangeListener(this::refresh);
        refresh();
    }

    private JPanel createToolbar() {
        search.setToolTipText("Search by ticket, name or phone");
        search.setPreferredSize(new java.awt.Dimension(260, 38));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });
        for (String s : new String[]{"All dates", "Upcoming", "Today", "Past"}) {
            whenFilter.addItem(s);
        }
        for (String s : new String[]{"All statuses", Booking.CONFIRMED, Booking.CANCELLED}) {
            statusFilter.addItem(s);
        }
        routeFilter.setPreferredSize(new java.awt.Dimension(230, 38));
        whenFilter.setPreferredSize(new java.awt.Dimension(140, 38));
        statusFilter.setPreferredSize(new java.awt.Dimension(150, 38));
        routeFilter.addActionListener(e -> applyFilters());
        whenFilter.addActionListener(e -> applyFilters());
        statusFilter.addActionListener(e -> applyFilters());

        JButton newBooking = Theme.button("+  New Booking", Theme.ButtonStyle.PRIMARY);
        newBooking.addActionListener(e -> portal.startNewBooking());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(Theme.label("Search", Theme.LABEL, Theme.TEXT));
        left.add(search);
        left.add(routeFilter);
        left.add(whenFilter);
        left.add(statusFilter);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(left, BorderLayout.WEST);
        bar.add(newBooking, BorderLayout.EAST);
        return bar;
    }

    private JPanel createActions() {
        JButton view = Theme.button("View Ticket", Theme.ButtonStyle.SECONDARY);
        JButton edit = Theme.button("Edit", Theme.ButtonStyle.PRIMARY);
        JButton cancel = Theme.button("Cancel Booking", Theme.ButtonStyle.SECONDARY);
        JButton delete = Theme.button("Delete", Theme.ButtonStyle.DANGER);
        view.addActionListener(e -> {
            Booking b = requireSelection();
            if (b != null) {
                new TicketDialog(portal, b).setVisible(true);
            }
        });
        edit.addActionListener(e -> editSelected());
        cancel.addActionListener(e -> cancelSelected());
        delete.addActionListener(e -> deleteSelected());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(view);
        buttons.add(edit);
        buttons.add(cancel);
        buttons.add(delete);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(countLabel, BorderLayout.WEST);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    /** Reloads the route list and the table after any change. */
    private void refresh() {
        updating = true;
        Object current = routeFilter.getSelectedItem();
        routeFilter.removeAllItems();
        routeFilter.addItem(ALL_ROUTES);
        List<String> names = new ArrayList<>();
        for (Booking b : store.getBookings()) {
            if (!names.contains(b.getRouteName())) {
                names.add(b.getRouteName());
            }
        }
        java.util.Collections.sort(names);
        for (String name : names) {
            routeFilter.addItem(name);
        }
        routeFilter.setSelectedItem(current != null && names.contains(current) ? current : ALL_ROUTES);
        updating = false;
        applyFilters();
    }

    private void applyFilters() {
        if (updating) {
            return;
        }
        String text = search.getText().trim().toLowerCase(Locale.ENGLISH);
        String route = (String) routeFilter.getSelectedItem();
        String when = (String) whenFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();
        LocalDate today = LocalDate.now();

        model.setRowCount(0);
        shown.clear();
        List<Booking> all = new ArrayList<>(store.getBookings());
        all.sort((a, b) -> b.getBookedAt().compareTo(a.getBookedAt()));
        for (Booking b : all) {
            String haystack = (b.getTicketNo() + " " + b.getFullName() + " " + b.getPhone()).toLowerCase(Locale.ENGLISH);
            if (!text.isEmpty() && !haystack.contains(text)) {
                continue;
            }
            if (route != null && !ALL_ROUTES.equals(route) && !route.equals(b.getRouteName())) {
                continue;
            }
            if ("Upcoming".equals(when) && b.hasDeparted()
                    || "Today".equals(when) && !b.getTravelDate().equals(today)
                    || "Past".equals(when) && !b.hasDeparted()) {
                continue;
            }
            if (status != null && !status.startsWith("All") && !status.equals(b.getStatus())) {
                continue;
            }
            shown.add(b);
            model.addRow(new Object[]{b.getTicketNo(), b.getFullName(), b.getRouteName(), b.getTravelDate(), DataStore.parseTime(b.getTime()),
                b.getSeatsText(), b.getSeatClass(), b.getFare(), b.getStatus()});
        }
        countLabel.setText("Showing " + shown.size() + " of " + store.getBookings().size() + " bookings"
                + "   |   Double-click a row to see the ticket");
    }

    private Booking selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return shown.get(table.convertRowIndexToModel(viewRow));
    }

    private Booking requireSelection() {
        Booking b = selected();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Please select a booking in the table first.", "No booking selected",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        return b;
    }

    private void editSelected() {
        Booking b = requireSelection();
        if (b == null) {
            return;
        }
        if (!b.isConfirmed()) {
            JOptionPane.showMessageDialog(this, "Cancelled bookings cannot be edited.", "Edit",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (b.hasDeparted()) {
            JOptionPane.showMessageDialog(this, "This bus has already left, so the booking can no longer be changed.", "Edit",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        portal.editBooking(b);
    }

    private void cancelSelected() {
        Booking b = requireSelection();
        if (b == null) {
            return;
        }
        if (!b.isConfirmed()) {
            JOptionPane.showMessageDialog(this, "This booking is already cancelled.", "Cancel booking",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (b.hasDeparted()) {
            JOptionPane.showMessageDialog(this, "This bus has already left, so the booking cannot be cancelled.",
                    "Cancel booking", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Cancel ticket " + b.getTicketNo() + " for "
                + b.getFullName() + "?\nSeats " + b.getSeatsText() + " will become available again.\n"
                + "Refund to give: " + Theme.money(b.getFare()), "Cancel booking", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                store.cancelBooking(b);
            } catch (IOException ex) {
                showSaveError(ex);
            }
        }
    }

    private void deleteSelected() {
        Booking b = requireSelection();
        if (b == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Permanently delete ticket " + b.getTicketNo()
                + "?\nThis cannot be undone and it will be removed from the reports.", "Delete booking",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                store.deleteBooking(b);
            } catch (IOException ex) {
                showSaveError(ex);
            }
        }
    }

    private void showSaveError(IOException ex) {
        JOptionPane.showMessageDialog(this, "Could not save the change:\n" + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
