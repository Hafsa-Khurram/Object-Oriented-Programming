package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * Lets the admin add, change and remove routes: cities, bus, seat count, fares and
 * departure times. Everything the booking screen offers comes from here.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class RoutesPanel extends JPanel {

    private final DataStore store;
    private final DefaultTableModel model;
    private final JTable table;
    private final List<Route> shown = new ArrayList<>();

    private final JComboBox<String> fromBox = Theme.comboBox();
    private final JComboBox<String> toBox = Theme.comboBox();
    private final JTextField busName = Theme.textField();
    private final JSpinner totalSeats = new JSpinner(new SpinnerNumberModel(40, 8, 60, 4));
    private final JSpinner businessSeats = new JSpinner(new SpinnerNumberModel(8, 0, 60, 4));
    private final JTextField economyFare = Theme.textField();
    private final JTextField businessFare = Theme.textField();
    private final JTextField timings = Theme.textField();
    private final JCheckBox returnRoute = new JCheckBox("Also add the return route (same bus, fares and times)");
    private Route editing;

    public RoutesPanel(DataStore store) {
        this.store = store;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        String[] columns = {"Route", "Bus", "Seats", "Fare (Eco/Biz)", "Trips/day"};
        model = Theme.tableModel(columns, String.class, String.class, Integer.class, String.class, Integer.class);
        table = new JTable(model);
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Theme.columnWidths(table, 180, 120, 60, 130, 85);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                load(shown.get(table.convertRowIndexToModel(table.getSelectedRow())));
            }
        });

        Theme.Card tableCard = new Theme.Card(new BorderLayout(0, 12));
        tableCard.add(Theme.label("All Routes", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        tableCard.add(Theme.scroll(table), BorderLayout.CENTER);
        tableCard.add(Theme.label("Click a route to edit it. Changes are saved immediately.", Theme.SMALL,
                Theme.MUTED), BorderLayout.SOUTH);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.66;
        c.insets = new Insets(0, 0, 0, 12);
        add(tableCard, c);
        c.gridx = 1;
        c.weightx = 0.34;
        c.insets = new Insets(0, 12, 0, 0);
        add(createForm(), c);

        store.addChangeListener(this::refresh);
        refresh();
        clearForm();
    }

    private JComponent createForm() {
        Theme.Card card = new Theme.Card(new GridBagLayout());
        fromBox.setEditable(true);
        toBox.setEditable(true);
        for (JSpinner spinner : new JSpinner[]{totalSeats, businessSeats}) {
            spinner.setFont(Theme.BODY);
            spinner.setPreferredSize(new Dimension(100, 38));
        }
        timings.setToolTipText("Separate times with commas, e.g. 08:00 AM, 02:30 PM");
        returnRoute.setFont(Theme.SMALL);
        returnRoute.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(Theme.label("Route Details", Theme.HEADING, Theme.TEXT), c);
        c.gridwidth = 1;
        field(card, c, 1, 0, "From city", fromBox);
        field(card, c, 1, 1, "To city", toBox);
        c.gridwidth = 2;
        field(card, c, 3, 0, "Bus / company name", busName);
        c.gridwidth = 1;
        field(card, c, 5, 0, "Total seats", totalSeats);
        field(card, c, 5, 1, "Business seats (front)", businessSeats);
        field(card, c, 7, 0, "Economy fare (Rs)", economyFare);
        field(card, c, 7, 1, "Business fare (Rs)", businessFare);
        c.gridwidth = 2;
        field(card, c, 9, 0, "Departure times (comma separated)", timings);
        c.gridy = 11;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(returnRoute, c);

        JButton add = Theme.button("Add Route", Theme.ButtonStyle.PRIMARY);
        JButton update = Theme.button("Update", Theme.ButtonStyle.SUCCESS);
        JButton delete = Theme.button("Delete", Theme.ButtonStyle.DANGER);
        JButton clear = Theme.button("Clear", Theme.ButtonStyle.SECONDARY);
        add.addActionListener(e -> addRoute());
        update.addActionListener(e -> updateRoute());
        delete.addActionListener(e -> deleteRoute());
        clear.addActionListener(e -> clearForm());
        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(update);
        buttons.add(clear);
        buttons.add(delete);
        c.gridy = 12;
        card.add(buttons, c);
        c.gridy = 13;
        c.weighty = 1;
        card.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, c);
        return card;
    }

    private void field(JPanel card, GridBagConstraints c, int row, int col, String label, JComponent input) {
        c.gridx = col;
        c.gridy = row;
        c.insets = new Insets(0, col == 0 ? 0 : 6, 5, c.gridwidth == 2 || col == 1 ? 0 : 6);
        card.add(Theme.label(label, Theme.LABEL, Theme.TEXT), c);
        c.gridy = row + 1;
        c.insets = new Insets(0, col == 0 ? 0 : 6, 12, c.gridwidth == 2 || col == 1 ? 0 : 6);
        card.add(input, c);
    }

    private void refresh() {
        model.setRowCount(0);
        shown.clear();
        List<Route> routes = new ArrayList<>(store.getRoutes());
        routes.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (Route r : routes) {
            shown.add(r);
            model.addRow(new Object[]{r.getName(), r.getBusName(), r.getTotalSeats(),
                money(r.getEconomyFare()) + " / " + money(r.getBusinessFare()),
                r.getTimings().size()});
        }
        Object from = fromBox.getEditor().getItem();
        Object to = toBox.getEditor().getItem();
        fromBox.removeAllItems();
        toBox.removeAllItems();
        for (String city : store.getAllCities()) {
            fromBox.addItem(city);
            toBox.addItem(city);
        }
        fromBox.getEditor().setItem(from);
        toBox.getEditor().setItem(to);
    }

    private static String money(double value) {
        return new java.text.DecimalFormat("#,##0").format(value);
    }

    private void load(Route r) {
        editing = r;
        fromBox.getEditor().setItem(r.getFrom());
        toBox.getEditor().setItem(r.getTo());
        busName.setText(r.getBusName());
        totalSeats.setValue(r.getTotalSeats());
        businessSeats.setValue(r.getBusinessSeats());
        economyFare.setText(String.valueOf((long) r.getEconomyFare()));
        businessFare.setText(String.valueOf((long) r.getBusinessFare()));
        timings.setText(String.join(", ", r.getTimings()));
        returnRoute.setSelected(false);
        returnRoute.setEnabled(false);
    }

    private void clearForm() {
        editing = null;
        table.clearSelection();
        fromBox.getEditor().setItem("");
        toBox.getEditor().setItem("");
        busName.setText("");
        totalSeats.setValue(40);
        businessSeats.setValue(8);
        economyFare.setText("");
        businessFare.setText("");
        timings.setText("08:00 AM, 02:00 PM, 08:00 PM");
        returnRoute.setEnabled(true);
        returnRoute.setSelected(true);
    }

    /** Reads and checks the form. Shows a message and returns null if something is wrong. */
    private Route readForm() {
        try {
            String from = cleanCity(String.valueOf(fromBox.getEditor().getItem()));
            String to = cleanCity(String.valueOf(toBox.getEditor().getItem()));
            if (from.isEmpty() || to.isEmpty()) {
                throw new IllegalArgumentException("Please enter both the From and To cities.");
            }
            if (from.equalsIgnoreCase(to)) {
                throw new IllegalArgumentException("From and To cannot be the same city.");
            }
            if (!from.matches("[A-Za-z .'-]{2,30}") || !to.matches("[A-Za-z .'-]{2,30}")) {
                throw new IllegalArgumentException("City names can only contain letters (2-30 characters).");
            }
            String bus = busName.getText().trim();
            if (bus.isEmpty()) {
                throw new IllegalArgumentException("Please enter the bus or company name.");
            }
            int seats = (Integer) totalSeats.getValue();
            int business = (Integer) businessSeats.getValue();
            if (business > seats) {
                throw new IllegalArgumentException("Business seats cannot be more than the total seats.");
            }
            double eco = parseFare(economyFare.getText(), "Economy fare");
            double biz = parseFare(businessFare.getText(), "Business fare");
            if (business > 0 && biz < eco) {
                throw new IllegalArgumentException("Business fare should not be lower than the Economy fare.");
            }
            Set<String> times = new LinkedHashSet<>();
            for (String part : timings.getText().split(",")) {
                if (!part.trim().isEmpty()) {
                    times.add(DataStore.normaliseTime(part));
                }
            }
            if (times.isEmpty()) {
                throw new IllegalArgumentException("Please enter at least one departure time, e.g. 08:00 AM.");
            }
            List<String> sorted = new ArrayList<>(times);
            sorted.sort((a, b) -> DataStore.parseTime(a).compareTo(DataStore.parseTime(b)));
            return new Route(from, to, bus, seats, business, eco, biz, sorted);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Please check the route", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private static double parseFare(String text, String name) {
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (value <= 0 || value > 100000) {
                throw new IllegalArgumentException(name + " must be between 1 and 100,000.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be a number, e.g. 2500.");
        }
    }

    private static String cleanCity(String text) {
        String t = text == null || "null".equals(text) ? "" : text.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private void addRoute() {
        Route route = readForm();
        if (route == null) {
            return;
        }
        try {
            store.addRoute(route);
            String message = "Route " + route.getName() + " added.";
            if (returnRoute.isSelected() && store.findRoute(route.getTo(), route.getFrom()) == null) {
                store.addRoute(new Route(route.getTo(), route.getFrom(), route.getBusName(), route.getTotalSeats(),
                        route.getBusinessSeats(), route.getEconomyFare(), route.getBusinessFare(), route.getTimings()));
                message = "Routes " + route.getName() + " and back added.";
            }
            clearForm();
            JOptionPane.showMessageDialog(this, message, "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Route exists", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            showSaveError(ex);
        }
    }

    private void updateRoute() {
        if (editing == null) {
            JOptionPane.showMessageDialog(this, "Please click a route in the table to edit it.", "No route selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Route route = readForm();
        if (route == null) {
            return;
        }
        boolean citiesChanged = !route.connects(editing.getFrom(), editing.getTo());
        int upcoming = store.upcomingBookings(editing).size();
        if (citiesChanged && upcoming > 0) {
            JOptionPane.showMessageDialog(this, "This route has " + upcoming + " upcoming booking(s), so its cities "
                    + "cannot be changed.\nAdd a new route instead.", "Route in use", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            store.updateRoute(editing, route);
            clearForm();
            JOptionPane.showMessageDialog(this, "Route " + route.getName() + " updated.", "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot update", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            showSaveError(ex);
        }
    }

    private void deleteRoute() {
        if (editing == null) {
            JOptionPane.showMessageDialog(this, "Please click a route in the table to delete it.", "No route selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int upcoming = store.upcomingBookings(editing).size();
        if (upcoming > 0) {
            JOptionPane.showMessageDialog(this, "This route has " + upcoming + " upcoming booking(s).\n"
                    + "Cancel those bookings first, then delete the route.", "Route in use",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete the route " + editing.getName() + "?",
                "Delete route", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                store.deleteRoute(editing);
                clearForm();
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
