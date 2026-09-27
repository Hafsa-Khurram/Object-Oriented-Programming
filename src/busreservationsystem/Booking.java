package busreservationsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Passenger details for one booking (the "PassengersDetail" class from the proposal).
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class Booking {

    public static final String CONFIRMED = "Confirmed";
    public static final String CANCELLED = "Cancelled";

    private final String ticketNo;
    private String firstName;
    private String lastName;
    private String gender;
    private String phone;
    private String from;
    private String to;
    private LocalDate travelDate;
    private String time;
    private List<Integer> seats;
    private String seatClass;
    private double fare;
    private double paid;
    private String status;
    private final LocalDateTime bookedAt;

    public Booking(String ticketNo, String firstName, String lastName, String gender, String phone,
                   String from, String to, LocalDate travelDate, String time, List<Integer> seats,
                   String seatClass, double fare, double paid, String status, LocalDateTime bookedAt) {
        this.ticketNo = ticketNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.phone = phone;
        this.from = from;
        this.to = to;
        this.travelDate = travelDate;
        this.time = time;
        this.seats = new ArrayList<>(seats);
        Collections.sort(this.seats);
        this.seatClass = seatClass;
        this.fare = fare;
        this.paid = paid;
        this.status = status;
        this.bookedAt = bookedAt;
    }

    public String getTicketNo() { return ticketNo; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getRouteName() { return from + " → " + to; }
    public LocalDate getTravelDate() { return travelDate; }
    public String getTime() { return time; }
    public List<Integer> getSeats() { return new ArrayList<>(seats); }
    public int getSeatCount() { return seats.size(); }
    public String getSeatClass() { return seatClass; }
    public double getFare() { return fare; }
    public double getPaid() { return paid; }
    public double getChange() { return paid - fare; }
    public String getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }

    public boolean isConfirmed() {
        return CONFIRMED.equals(status);
    }

    public void cancel() {
        status = CANCELLED;
    }

    /** Replaces the editable details, keeping the ticket number and booking time. */
    public void update(Booking changed) {
        firstName = changed.firstName;
        lastName = changed.lastName;
        gender = changed.gender;
        phone = changed.phone;
        from = changed.from;
        to = changed.to;
        travelDate = changed.travelDate;
        time = changed.time;
        seats = new ArrayList<>(changed.seats);
        seatClass = changed.seatClass;
        fare = changed.fare;
        paid = changed.paid;
        status = changed.status;
    }

    /** Seats as text, e.g. "3, 4, 5". */
    public String getSeatsText() {
        StringBuilder sb = new StringBuilder();
        for (int seat : seats) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(seat);
        }
        return sb.toString();
    }
}
