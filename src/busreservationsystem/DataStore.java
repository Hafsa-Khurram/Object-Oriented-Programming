package busreservationsystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads and saves all application data (routes, bookings and the admin login)
 * as simple text files inside the "data" folder, so nothing is hard-coded.
 * Every change is saved immediately.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class DataStore {

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    private final Path dataDir;
    private final Path routesFile;
    private final Path bookingsFile;
    private final Path settingsFile;

    private final List<Route> routes = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final Properties settings = new Properties();
    private final List<Runnable> listeners = new ArrayList<>();

    public DataStore(Path dataDir) {
        this.dataDir = dataDir;
        this.routesFile = dataDir.resolve("routes.csv");
        this.bookingsFile = dataDir.resolve("bookings.csv");
        this.settingsFile = dataDir.resolve("settings.properties");
    }

    public DataStore() {
        this(Paths.get(System.getProperty("user.dir"), "data"));
    }

    // ===================== Loading =====================

    public void load() throws IOException {
        Files.createDirectories(dataDir);
        loadSettings();
        loadRoutes();
        loadBookings();
    }

    private void loadSettings() throws IOException {
        if (Files.exists(settingsFile)) {
            try (InputStream in = Files.newInputStream(settingsFile)) {
                settings.load(in);
            }
        }
        if (settings.getProperty("username") == null || settings.getProperty("passwordHash") == null) {
            settings.setProperty("username", DEFAULT_USERNAME);
            settings.setProperty("passwordHash", hash(DEFAULT_PASSWORD));
            saveSettings();
        }
    }

    private void loadRoutes() throws IOException {
        routes.clear();
        if (!Files.exists(routesFile)) {
            seedRoutes();
            saveRoutes();
            return;
        }
        for (List<String> f : readCsv(routesFile)) {
            try {
                routes.add(new Route(f.get(0), f.get(1), f.get(2), Integer.parseInt(f.get(3)),
                        Integer.parseInt(f.get(4)), Double.parseDouble(f.get(5)),
                        Double.parseDouble(f.get(6)), splitList(f.get(7))));
            } catch (RuntimeException ex) {
                System.err.println("Skipping invalid route line: " + f);
            }
        }
    }

    private void loadBookings() throws IOException {
        bookings.clear();
        if (!Files.exists(bookingsFile)) {
            return;
        }
        for (List<String> f : readCsv(bookingsFile)) {
            try {
                List<Integer> seats = new ArrayList<>();
                for (String s : splitList(f.get(9))) {
                    seats.add(Integer.parseInt(s));
                }
                bookings.add(new Booking(f.get(0), f.get(1), f.get(2), f.get(3), f.get(4), f.get(5), f.get(6),
                        LocalDate.parse(f.get(7)), f.get(8), seats, f.get(10),
                        Double.parseDouble(f.get(11)), Double.parseDouble(f.get(12)), f.get(13),
                        LocalDateTime.parse(f.get(14))));
            } catch (RuntimeException ex) {
                System.err.println("Skipping invalid booking line: " + f);
            }
        }
    }

    /** Sample routes used the very first time the app runs. The admin can change them all. */
    private void seedRoutes() {
        List<String> day = Arrays.asList("06:00 AM", "10:00 AM", "02:00 PM", "06:00 PM", "10:00 PM");
        List<String> hourly = Arrays.asList("08:00 AM", "11:00 AM", "02:00 PM", "05:00 PM");
        List<String> night = Arrays.asList("07:00 AM", "05:00 PM", "09:00 PM");
        addPair("Lahore", "Islamabad", "Daewoo Express", 40, 8, 2500, 4000, day);
        addPair("Lahore", "Karachi", "Faisal Movers", 44, 8, 6500, 9500, night);
        addPair("Lahore", "Multan", "Skyways", 40, 8, 2200, 3500, hourly);
        addPair("Lahore", "Faisalabad", "Daewoo Express", 40, 8, 1200, 2000, hourly);
        addPair("Islamabad", "Murree", "Niazi Express", 32, 4, 800, 1300, hourly);
        addPair("Islamabad", "Abbottabad", "Niazi Express", 32, 4, 1000, 1600, hourly);
        addPair("Islamabad", "Taxila", "City Coach", 28, 4, 400, 700, hourly);
        addPair("Islamabad", "Attock", "City Coach", 28, 4, 600, 1000, hourly);
        addPair("Karachi", "Multan", "Faisal Movers", 44, 8, 5000, 7500, night);
    }

    private void addPair(String a, String b, String bus, int seats, int business, double eco, double biz,
                         List<String> timings) {
        routes.add(new Route(a, b, bus, seats, business, eco, biz, timings));
        routes.add(new Route(b, a, bus, seats, business, eco, biz, timings));
    }

    // ===================== Saving =====================

    public void saveRoutes() throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (Route r : routes) {
            rows.add(Arrays.asList(r.getFrom(), r.getTo(), r.getBusName(), String.valueOf(r.getTotalSeats()),
                    String.valueOf(r.getBusinessSeats()), String.valueOf(r.getEconomyFare()),
                    String.valueOf(r.getBusinessFare()), String.join(";", r.getTimings())));
        }
        writeCsv(routesFile, rows);
    }

    public void saveBookings() throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (Booking b : bookings) {
            List<String> seats = new ArrayList<>();
            for (int s : b.getSeats()) {
                seats.add(String.valueOf(s));
            }
            rows.add(Arrays.asList(b.getTicketNo(), b.getFirstName(), b.getLastName(), b.getGender(),
                    b.getPhone(), b.getFrom(), b.getTo(), b.getTravelDate().toString(), b.getTime(),
                    String.join(";", seats), b.getSeatClass(), String.valueOf(b.getFare()),
                    String.valueOf(b.getPaid()), b.getStatus(), b.getBookedAt().toString()));
        }
        writeCsv(bookingsFile, rows);
    }

    private void saveSettings() throws IOException {
        Path temp = settingsFile.resolveSibling(settingsFile.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            settings.store(out, "Bus Reservation System settings");
        }
        Files.move(temp, settingsFile, StandardCopyOption.REPLACE_EXISTING);
    }

    // ===================== Change listeners =====================

    /** Screens register here so they refresh whenever data changes. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    /** Called on logout so screens of the closed window stop listening. */
    public void clearChangeListeners() {
        listeners.clear();
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    // ===================== Login =====================

    public boolean checkLogin(String username, char[] password) {
        String hashed = hash(new String(password));
        Arrays.fill(password, '\0');
        return settings.getProperty("username").equalsIgnoreCase(username.trim())
                && settings.getProperty("passwordHash").equals(hashed);
    }

    public String getUsername() {
        return settings.getProperty("username");
    }

    public void changeCredentials(String username, char[] password) throws IOException {
        settings.setProperty("username", username.trim());
        settings.setProperty("passwordHash", hash(new String(password)));
        Arrays.fill(password, '\0');
        saveSettings();
        fireChanged();
    }

    private static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ===================== Routes =====================

    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public Route findRoute(String from, String to) {
        for (Route r : routes) {
            if (r.connects(from, to)) {
                return r;
            }
        }
        return null;
    }

    /** All cities that have at least one outgoing route, sorted A-Z. */
    public List<String> getDepartureCities() {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Route r : routes) {
            cities.add(r.getFrom());
        }
        return new ArrayList<>(cities);
    }

    /** All cities known to the system (useful for the route editor). */
    public List<String> getAllCities() {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Route r : routes) {
            cities.add(r.getFrom());
            cities.add(r.getTo());
        }
        return new ArrayList<>(cities);
    }

    public List<String> getDestinationsFrom(String from) {
        Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Route r : routes) {
            if (r.getFrom().equalsIgnoreCase(from)) {
                cities.add(r.getTo());
            }
        }
        return new ArrayList<>(cities);
    }

    public void addRoute(Route route) throws IOException {
        if (findRoute(route.getFrom(), route.getTo()) != null) {
            throw new IllegalArgumentException("A route from " + route.getFrom() + " to " + route.getTo()
                    + " already exists. Select it in the table to edit it.");
        }
        routes.add(route);
        saveRoutes();
        fireChanged();
    }

    public void updateRoute(Route oldRoute, Route newRoute) throws IOException {
        Route existing = findRoute(newRoute.getFrom(), newRoute.getTo());
        if (existing != null && existing != oldRoute) {
            throw new IllegalArgumentException("Another route from " + newRoute.getFrom() + " to "
                    + newRoute.getTo() + " already exists.");
        }
        for (Booking b : upcomingBookings(oldRoute)) {
            if (!newRoute.getTimings().contains(b.getTime())) {
                throw new IllegalArgumentException("The " + b.getTime() + " departure has an upcoming booking (ticket "
                        + b.getTicketNo() + "), so this time cannot be removed.\nCancel that booking first.");
            }
            for (int seat : b.getSeats()) {
                if (seat > newRoute.getTotalSeats()) {
                    throw new IllegalArgumentException("Seat " + seat + " is already booked (ticket "
                            + b.getTicketNo() + "), so the bus must have at least " + seat + " seats.");
                }
            }
        }
        routes.set(routes.indexOf(oldRoute), newRoute);
        saveRoutes();
        fireChanged();
    }

    public void deleteRoute(Route route) throws IOException {
        routes.remove(route);
        saveRoutes();
        fireChanged();
    }

    /** Confirmed bookings on this route that have not departed yet. */
    public List<Booking> upcomingBookings(Route route) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.isConfirmed() && route.connects(b.getFrom(), b.getTo()) && !b.hasDeparted()) {
                result.add(b);
            }
        }
        return result;
    }

    // ===================== Bookings =====================

    public List<Booking> getBookings() {
        return Collections.unmodifiableList(bookings);
    }

    public Booking findBooking(String ticketNo) {
        for (Booking b : bookings) {
            if (b.getTicketNo().equals(ticketNo)) {
                return b;
            }
        }
        return null;
    }

    /** Seats already taken on one trip. The booking being edited (if any) is ignored. */
    public Set<Integer> getBookedSeats(String from, String to, LocalDate date, String time, String ignoreTicketNo) {
        Set<Integer> taken = new TreeSet<>();
        for (Booking b : bookings) {
            if (b.isConfirmed() && b.getFrom().equalsIgnoreCase(from) && b.getTo().equalsIgnoreCase(to)
                    && b.getTravelDate().equals(date) && b.getTime().equals(time)
                    && !b.getTicketNo().equals(ignoreTicketNo)) {
                taken.addAll(b.getSeats());
            }
        }
        return taken;
    }

    /**
     * Next ticket number. The last issued number is remembered in the settings file, so a
     * number is never given out twice, even after the newest booking is deleted.
     */
    public String nextTicketNo() {
        int max = Math.max(1000, lastIssuedTicket());
        for (Booking b : bookings) {
            max = Math.max(max, ticketNumber(b.getTicketNo()));
        }
        return "TKT-" + (max + 1);
    }

    private int lastIssuedTicket() {
        try {
            return Integer.parseInt(settings.getProperty("lastTicketNo", "0"));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static int ticketNumber(String ticketNo) {
        try {
            return Integer.parseInt(ticketNo.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public void addBooking(Booking booking) throws IOException {
        checkSeatsFree(booking, null);
        bookings.add(booking);
        saveBookings();
        if (ticketNumber(booking.getTicketNo()) > lastIssuedTicket()) {
            settings.setProperty("lastTicketNo", String.valueOf(ticketNumber(booking.getTicketNo())));
            saveSettings();
        }
        fireChanged();
    }

    public void updateBooking(Booking existing, Booking changed) throws IOException {
        checkSeatsFree(changed, existing.getTicketNo());
        existing.update(changed);
        saveBookings();
        fireChanged();
    }

    private void checkSeatsFree(Booking booking, String ignoreTicketNo) {
        Set<Integer> taken = getBookedSeats(booking.getFrom(), booking.getTo(), booking.getTravelDate(),
                booking.getTime(), ignoreTicketNo);
        for (int seat : booking.getSeats()) {
            if (taken.contains(seat)) {
                throw new IllegalArgumentException("Seat " + seat + " was just booked by someone else. "
                        + "Please choose another seat.");
            }
        }
    }

    public void cancelBooking(Booking booking) throws IOException {
        booking.cancel();
        saveBookings();
        fireChanged();
    }

    public void deleteBooking(Booking booking) throws IOException {
        bookings.remove(booking);
        saveBookings();
        fireChanged();
    }

    // ===================== Helpers =====================

    /** Parses and normalises a time like "8:00 pm" to "08:00 PM". */
    public static String normaliseTime(String text) {
        String t = text.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        if (t.matches("\\d:\\d\\d.*")) {
            t = "0" + t;
        }
        if (t.matches("\\d\\d:\\d\\d(AM|PM)")) {
            t = t.substring(0, 5) + " " + t.substring(5);
        }
        try {
            return LocalTime.parse(t, TIME_FORMAT).format(TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("\"" + text.trim() + "\" is not a valid time. Use a format like 08:30 AM.");
        }
    }

    public static LocalTime parseTime(String time) {
        return LocalTime.parse(time, TIME_FORMAT);
    }

    private static List<String> splitList(String text) {
        List<String> items = new ArrayList<>();
        for (String part : text.split(";")) {
            if (!part.trim().isEmpty()) {
                items.add(part.trim());
            }
        }
        return items;
    }

    // ----- Minimal CSV reader/writer (handles commas and quotes inside values) -----

    private static List<List<String>> readCsv(Path file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rows.add(parseCsvLine(line));
                }
            }
        }
        return rows;
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static void writeCsv(Path file, List<List<String>> rows) throws IOException {
        // Write to a temporary file first so a crash can never leave a half-written file.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (List<String> row : rows) {
                StringBuilder line = new StringBuilder();
                for (String field : row) {
                    if (line.length() > 0) {
                        line.append(',');
                    }
                    String value = field == null ? "" : field.replace("\r", " ").replace("\n", " ");
                    if (value.contains(",") || value.contains("\"")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    line.append(value);
                }
                writer.write(line.toString());
                writer.newLine();
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
