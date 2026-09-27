package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

/**
 * Screen for booking new tickets and editing existing ones. Cities, times, fares and seat
 * counts all come from the routes the admin has set up, so nothing here is hard-coded.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class BookingPanel extends JPanel {

    private static final String NAME_PATTERN = "[A-Za-z][A-Za-z .'-]{1,29}";
    private static final String PHONE_PATTERN = "\\+?[0-9][0-9 -]{6,15}";

    private final DataStore store;
    private final AdminPortal portal;

    private final JTextField firstName = Theme.textField();
    private final JTextField lastName = Theme.textField();
    private final JTextField phone = Theme.textField();
    private final JRadioButton male = new JRadioButton("Male");
    private final JRadioButton female = new JRadioButton("Female");
    private final ButtonGroup genderGroup = new ButtonGroup();
    private final JComboBox<String> fromBox = Theme.comboBox();
    private final JComboBox<String> toBox = Theme.comboBox();
    private final JComboBox<String> timeBox = Theme.comboBox();
    private final JSpinner dateSpinner;

    private final JLabel formTitle = Theme.label("Passenger & Trip", Theme.HEADING, Theme.TEXT);
    private final JLabel tripInfo = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel seatsInfo = Theme.label(" ", Theme.LABEL, Theme.PRIMARY);
    private final JLabel selectedLabel = Theme.label("No seats selected", Theme.BODY, Theme.TEXT);
    private final JLabel classLabel = Theme.label(" ", Theme.SMALL, Theme.MUTED);
    private final JLabel totalLabel = Theme.label(Theme.money(0), Theme.BIG_NUMBER, Theme.PRIMARY);
    private final SeatMapPanel seatMap = new SeatMapPanel();
    private final JButton bookButton = Theme.button("Book Ticket", Theme.ButtonStyle.SUCCESS);
    private final JButton cancelEditButton = Theme.button("Cancel Editing", Theme.ButtonStyle.GHOST);

    private Booking editing;
    private boolean updating;

    public BookingPanel(DataStore store, AdminPortal portal) {
        this.store = store;
        this.portal = portal;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        SpinnerDateModel model = new SpinnerDateModel(new Date(), startOfToday(), null, Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(model);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "EEE, dd MMM yyyy"));
        dateSpinner.setFont(Theme.BODY);
        dateSpinner.setPreferredSize(new Dimension(200, 38));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.45;
        c.insets = new Insets(0, 0, 0, 12);
        JComponent form = createFormCard();
        form.setPreferredSize(new Dimension(100, 100));
        add(form, c);
        c.gridx = 1;
        c.weightx = 0.55;
        c.insets = new Insets(0, 12, 0, 0);
        JComponent seats = createSeatCard();
        seats.setPreferredSize(new Dimension(100, 100));
        add(seats, c);

        fromBox.addActionListener(e -> {
            if (!updating) {
                refreshDestinations();
            }
        });
        toBox.addActionListener(e -> {
            if (!updating) {
                refreshTimes();
            }
        });
        dateSpinner.addChangeListener(e -> {
            if (!updating) {
                refreshTimes();
            }
        });
        timeBox.addActionListener(e -> {
            if (!updating) {
                loadTrip(null);
            }
        });
        seatMap.addSelectionListener(this::updateSummary);
        bookButton.addActionListener(e -> saveBooking());
        cancelEditButton.addActionListener(e -> {
            reset();
            portal.navigate(AdminPortal.BOOKINGS);
        });

        store.addChangeListener(() -> {
            if (editing == null) {
                refreshCitiesKeepingSelection();
            }
        });
        reset();
    }

    // ===================== Layout =====================

    private JComponent createFormCard() {
        Theme.Card card = new Theme.Card(new GridBagLayout());
        for (JRadioButton radio : new JRadioButton[]{male, female}) {
            radio.setFont(Theme.BODY);
            radio.setOpaque(false);
            genderGroup.add(radio);
        }
        JPanel genderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        genderRow.setOpaque(false);
        genderRow.add(male);
        genderRow.add(javax.swing.Box.createHorizontalStrut(18));
        genderRow.add(female);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(formTitle, c);

        c.gridwidth = 1;
        addField(card, c, 1, 0, "First name", firstName);
        addField(card, c, 1, 1, "Last name", lastName);
        addField(card, c, 3, 0, "Gender", genderRow);
        addField(card, c, 3, 1, "Phone (optional)", phone);
        addField(card, c, 5, 0, "From", fromBox);
        addField(card, c, 5, 1, "To", toBox);
        addField(card, c, 7, 0, "Travel date", dateSpinner);
        addField(card, c, 7, 1, "Departure time", timeBox);

        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 0, 0);
        JPanel info = new Theme.Card(new GridLayout(2, 1, 0, 4), Theme.PRIMARY_LIGHT);
        info.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        info.add(tripInfo);
        info.add(seatsInfo);
        card.add(info, c);

        c.gridy = 10;
        c.weighty = 1;
        card.add(new JLabel(), c);
        return card;
    }

    private void addField(JPanel card, GridBagConstraints c, int row, int col, String label, JComponent field) {
        c.gridx = col;
        c.gridy = row;
        c.weighty = 0;
        c.insets = new Insets(0, col == 0 ? 0 : 8, 5, col == 0 ? 8 : 0);
        card.add(Theme.label(label, Theme.LABEL, Theme.TEXT), c);
        c.gridy = row + 1;
        c.insets = new Insets(0, col == 0 ? 0 : 8, 14, col == 0 ? 8 : 0);
        card.add(field, c);
    }

    private JComponent createSeatCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label("Choose Seats", Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        top.add(Theme.label("Click a seat to select it", Theme.SMALL, Theme.MUTED), BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(seatMap);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        center.add(SeatMapPanel.legend(), BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);

        JPanel summary = new Theme.Card(new BorderLayout(10, 0), new Color(250, 250, 255));
        summary.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel textCol = new JPanel(new GridLayout(3, 1, 0, 2));
        textCol.setOpaque(false);
        textCol.add(Theme.label("TOTAL FARE", Theme.SMALL, Theme.MUTED));
        textCol.add(totalLabel);
        textCol.add(selectedLabel);
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.add(classLabel, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton clear = Theme.button("Clear", Theme.ButtonStyle.SECONDARY);
        clear.addActionListener(e -> {
            if (editing == null) {
                reset();
            } else {
                seatMap.clearSelection();
            }
        });
        buttons.add(cancelEditButton);
        buttons.add(clear);
        buttons.add(bookButton);
        right.add(buttons, BorderLayout.SOUTH);
        summary.add(textCol, BorderLayout.WEST);
        summary.add(right, BorderLayout.EAST);
        card.add(summary, BorderLayout.SOUTH);
        return card;
    }

    // ===================== Public actions =====================

    public boolean isEditing() {
        return editing != null;
    }

    /** Clears the form for a brand new booking. */
    public void reset() {
        editing = null;
        formTitle.setText("Passenger & Trip");
        bookButton.setText("Book Ticket");
        cancelEditButton.setVisible(false);
        firstName.setText("");
        lastName.setText("");
        phone.setText("");
        genderGroup.clearSelection();
        updating = true;
        dateSpinner.setValue(new Date());
        updating = false;
        refreshCitiesKeepingSelection();
    }

    /** Loads a booking into the form so it can be changed. */
    public void edit(Booking booking) {
        editing = booking;
        formTitle.setText("Edit Booking " + booking.getTicketNo());
        bookButton.setText("Save Changes");
        cancelEditButton.setVisible(true);
        firstName.setText(booking.getFirstName());
        lastName.setText(booking.getLastName());
        phone.setText(booking.getPhone());
        male.setSelected("Male".equals(booking.getGender()));
        female.setSelected("Female".equals(booking.getGender()));

        updating = true;
        fromBox.removeAllItems();
        for (String city : store.getDepartureCities()) {
            fromBox.addItem(city);
        }
        fromBox.setSelectedItem(booking.getFrom());
        fillDestinations();
        toBox.setSelectedItem(booking.getTo());
        dateSpinner.setValue(toDate(booking.getTravelDate()));
        fillTimes();
        timeBox.setSelectedItem(booking.getTime());
        updating = false;
        loadTrip(new HashSet<>(booking.getSeats()));
    }

    // ===================== Cascading choices =====================

    private void refreshCitiesKeepingSelection() {
        Object from = fromBox.getSelectedItem();
        Object to = toBox.getSelectedItem();
        Object time = timeBox.getSelectedItem();
        updating = true;
        fromBox.removeAllItems();
        for (String city : store.getDepartureCities()) {
            fromBox.addItem(city);
        }
        if (from != null) {
            fromBox.setSelectedItem(from);
        }
        fillDestinations();
        if (to != null) {
            toBox.setSelectedItem(to);
        }
        fillTimes();
        if (time != null) {
            timeBox.setSelectedItem(time);
        }
        updating = false;
        loadTrip(null);
    }

    private void refreshDestinations() {
        updating = true;
        fillDestinations();
        fillTimes();
        updating = false;
        loadTrip(null);
    }

    private void refreshTimes() {
        updating = true;
        Object time = timeBox.getSelectedItem();
        fillTimes();
        if (time != null) {
            timeBox.setSelectedItem(time);
        }
        updating = false;
        loadTrip(null);
    }

    private void fillDestinations() {
        toBox.removeAllItems();
        String from = (String) fromBox.getSelectedItem();
        if (from != null) {
            for (String city : store.getDestinationsFrom(from)) {
                toBox.addItem(city);
            }
        }
    }

    /** Only departure times that are still in the future are offered. */
    private void fillTimes() {
        timeBox.removeAllItems();
        Route route = currentRoute();
        if (route == null) {
            return;
        }
        LocalDate date = selectedDate();
        for (String time : route.getTimings()) {
            boolean alreadyLeft = date.equals(LocalDate.now()) && DataStore.parseTime(time).isBefore(LocalTime.now());
            boolean keepForEdit = editing != null && time.equals(editing.getTime()) && date.equals(editing.getTravelDate());
            if (!alreadyLeft || keepForEdit) {
                timeBox.addItem(time);
            }
        }
    }

    private Route currentRoute() {
        String from = (String) fromBox.getSelectedItem();
        String to = (String) toBox.getSelectedItem();
        return from == null || to == null ? null : store.findRoute(from, to);
    }

    /** Shows the seats for the chosen trip. */
    private void loadTrip(Set<Integer> preselect) {
        Route route = currentRoute();
        String time = (String) timeBox.getSelectedItem();
        if (route == null) {
            tripInfo.setText("No routes yet. Add one in \"Routes & Buses\".");
            seatsInfo.setText(" ");
            seatMap.showTrip(null, new HashSet<>(), null);
            return;
        }
        tripInfo.setText(route.getBusName() + "   |   Economy " + Theme.money(route.getEconomyFare())
                + "   |   Business " + Theme.money(route.getBusinessFare()));
        if (time == null) {
            seatsInfo.setText("No more departures on this date. Please pick another day.");
            seatsInfo.setForeground(Theme.DANGER);
            seatMap.showTrip(null, new HashSet<>(), null);
            return;
        }
        String ignore = editing == null ? null : editing.getTicketNo();
        Set<Integer> booked = store.getBookedSeats(route.getFrom(), route.getTo(), selectedDate(), time, ignore);
        Set<Integer> keep = preselect != null ? preselect : new HashSet<>(seatMap.getSelectedSeats());
        seatMap.showTrip(route, booked, keep);
        int left = seatMap.getAvailableCount();
        seatsInfo.setForeground(left == 0 ? Theme.DANGER : Theme.PRIMARY);
        seatsInfo.setText(left == 0 ? "This bus is fully booked." : left + " of " + route.getTotalSeats()
                + " seats available");
    }

    private void updateSummary() {
        Route route = currentRoute();
        List<Integer> seats = seatMap.getSelectedSeats();
        if (route == null || seats.isEmpty()) {
            selectedLabel.setText("No seats selected");
            classLabel.setText(" ");
            totalLabel.setText(Theme.money(0));
            return;
        }
        selectedLabel.setText(seats.size() + (seats.size() == 1 ? " seat: " : " seats: ") + joinSeats(seats));
        classLabel.setText("Class: " + seatClass(route, seats));
        totalLabel.setText(Theme.money(totalFare(route, seats)));
    }

    // ===================== Saving =====================

    private void saveBooking() {
        String error = validateForm();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Please check the form", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Route route = currentRoute();
        List<Integer> seats = seatMap.getSelectedSeats();
        double total = totalFare(route, seats);

        double alreadyPaid = editing == null ? 0 : editing.getPaid();
        double paid = alreadyPaid;
        if (total > alreadyPaid) {
            Double payment = askPayment(total - alreadyPaid);
            if (payment == null) {
                return;
            }
            paid = alreadyPaid + payment;
        }

        String gender = male.isSelected() ? "Male" : "Female";
        Booking booking = new Booking(editing == null ? store.nextTicketNo() : editing.getTicketNo(),
                capitalise(firstName.getText()), capitalise(lastName.getText()), gender, phone.getText().trim(),
                route.getFrom(), route.getTo(), selectedDate(), (String) timeBox.getSelectedItem(), seats,
                seatClass(route, seats), total, paid, Booking.CONFIRMED,
                editing == null ? LocalDateTime.now() : editing.getBookedAt());
        try {
            if (editing == null) {
                store.addBooking(booking);
            } else {
                store.updateBooking(editing, booking);
                booking = editing;
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Seat not available", JOptionPane.WARNING_MESSAGE);
            loadTrip(null);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the booking:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean wasEditing = editing != null;
        new TicketDialog(portal, booking).setVisible(true);
        reset();
        if (wasEditing) {
            portal.navigate(AdminPortal.BOOKINGS);
        }
    }

    private String validateForm() {
        String first = firstName.getText().trim();
        String last = lastName.getText().trim();
        String phoneText = phone.getText().trim();
        if (!first.matches(NAME_PATTERN)) {
            return "First name must be 2-30 letters (spaces, . ' - are allowed).";
        }
        if (!last.matches(NAME_PATTERN)) {
            return "Last name must be 2-30 letters (spaces, . ' - are allowed).";
        }
        if (!male.isSelected() && !female.isSelected()) {
            return "Please select the passenger's gender.";
        }
        if (!phoneText.isEmpty() && !phoneText.matches(PHONE_PATTERN)) {
            return "Phone number can only contain digits, spaces, '-' and a leading '+'.";
        }
        if (currentRoute() == null) {
            return "Please choose where the passenger is travelling from and to.";
        }
        if (timeBox.getSelectedItem() == null) {
            return "There are no more departures on this date. Please pick another day.";
        }
        LocalDate date = selectedDate();
        LocalTime time = DataStore.parseTime((String) timeBox.getSelectedItem());
        boolean unchangedTrip = editing != null && date.equals(editing.getTravelDate())
                && Objects.equals(timeBox.getSelectedItem(), editing.getTime());
        if (!unchangedTrip && LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
            return "This bus has already left. Please choose a later time or date.";
        }
        if (seatMap.getSelectedSeats().isEmpty()) {
            return "Please click on the seat map to choose at least one seat.";
        }
        return null;
    }

    /** Asks how much cash the passenger paid. Returns null if cancelled. */
    private Double askPayment(double due) {
        JTextField amount = Theme.textField();
        amount.setText(String.valueOf((long) Math.ceil(due)));
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(Theme.label("Amount due: " + Theme.money(due), Theme.HEADING, Theme.PRIMARY));
        panel.add(Theme.label("Cash received from passenger (Rs):", Theme.LABEL, Theme.TEXT));
        panel.add(amount);
        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, panel, "Payment", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            try {
                double value = Double.parseDouble(amount.getText().trim().replace(",", ""));
                if (value >= due) {
                    return value;
                }
                JOptionPane.showMessageDialog(this, "The payment must cover the full amount of " + Theme.money(due) + ".",
                        "Not enough", JOptionPane.WARNING_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter the amount as a number, e.g. 5000.",
                        "Invalid amount", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ===================== Small helpers =====================

    static double totalFare(Route route, List<Integer> seats) {
        double total = 0;
        for (int seat : seats) {
            total += route.seatFare(seat);
        }
        return total;
    }

    static String seatClass(Route route, List<Integer> seats) {
        boolean business = false;
        boolean economy = false;
        for (int seat : seats) {
            if (route.isBusinessSeat(seat)) {
                business = true;
            } else {
                economy = true;
            }
        }
        return business && economy ? "Mixed" : business ? "Business" : "Economy";
    }

    private static String joinSeats(List<Integer> seats) {
        StringBuilder sb = new StringBuilder();
        for (int seat : seats) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(seat);
        }
        return sb.toString();
    }

    private static String capitalise(String text) {
        String t = text.trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : t.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private LocalDate selectedDate() {
        Date date = (Date) dateSpinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date startOfToday() {
        return toDate(LocalDate.now());
    }
}
