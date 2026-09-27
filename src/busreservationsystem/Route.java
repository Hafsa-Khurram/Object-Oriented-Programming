package busreservationsystem;

import java.util.ArrayList;
import java.util.List;

/**
 * A bus route between two cities, managed by the admin from the "Routes & Buses" screen.
 * The first {@code businessSeats} seats of the bus are Business class, the rest are Economy.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class Route {

    private String from;
    private String to;
    private String busName;
    private int totalSeats;
    private int businessSeats;
    private double economyFare;
    private double businessFare;
    private List<String> timings;

    public Route(String from, String to, String busName, int totalSeats, int businessSeats,
                 double economyFare, double businessFare, List<String> timings) {
        this.from = from;
        this.to = to;
        this.busName = busName;
        this.totalSeats = totalSeats;
        this.businessSeats = businessSeats;
        this.economyFare = economyFare;
        this.businessFare = businessFare;
        this.timings = new ArrayList<>(timings);
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getBusName() { return busName; }
    public int getTotalSeats() { return totalSeats; }
    public int getBusinessSeats() { return businessSeats; }
    public double getEconomyFare() { return economyFare; }
    public double getBusinessFare() { return businessFare; }
    public List<String> getTimings() { return new ArrayList<>(timings); }

    public boolean isBusinessSeat(int seatNumber) {
        return seatNumber <= businessSeats;
    }

    public String seatClass(int seatNumber) {
        return isBusinessSeat(seatNumber) ? "Business" : "Economy";
    }

    public double seatFare(int seatNumber) {
        return isBusinessSeat(seatNumber) ? businessFare : economyFare;
    }

    /** Route name used as a key, e.g. "Lahore → Islamabad". */
    public String getName() {
        return from + " → " + to;
    }

    public boolean connects(String fromCity, String toCity) {
        return from.equalsIgnoreCase(fromCity) && to.equalsIgnoreCase(toCity);
    }

    @Override
    public String toString() {
        return getName();
    }
}
