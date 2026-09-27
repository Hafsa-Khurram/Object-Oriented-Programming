package busreservationsystem;

import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Bus Reservation System - OOP Semester Project.
 * Entry point: loads the saved data and opens the login screen.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class BusReservationSystem {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            DataStore store = new DataStore();
            try {
                store.load();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not open the data folder:\n" + ex.getMessage(),
                        "Bus Reservation System", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new LoginFrame(store).setVisible(true);
        });
    }
}
