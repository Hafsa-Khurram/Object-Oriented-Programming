package busreservationsystem;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Lets the admin change the login username and password.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class SettingsPanel extends JPanel {

    private final DataStore store;
    private final JTextField username = Theme.textField();
    private final JPasswordField current = Theme.passwordField();
    private final JPasswordField newPassword = Theme.passwordField();
    private final JPasswordField confirm = Theme.passwordField();

    public SettingsPanel(DataStore store) {
        this.store = store;
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        Theme.Card card = new Theme.Card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Theme.label("Admin Account", Theme.HEADING, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 18, 0);
        card.add(Theme.label("Change the username and password used to log in.", Theme.BODY, Theme.MUTED), c);
        add(card, "Username", username, c);
        add(card, "Current password", current, c);
        add(card, "New password (at least 4 characters)", newPassword, c);
        add(card, "Confirm new password", confirm, c);
        JButton save = Theme.button("Save Changes", Theme.ButtonStyle.PRIMARY);
        save.addActionListener(e -> save());
        c.insets = new Insets(6, 0, 18, 0);
        card.add(save, c);
        c.insets = new Insets(0, 0, 0, 0);
        javax.swing.JLabel dataNote = Theme.label("All data is saved automatically in the \"data\" folder.",
                Theme.SMALL, Theme.MUTED);
        dataNote.setToolTipText(Paths.get(System.getProperty("user.dir"), "data").toString());
        card.add(dataNote, c);
        username.setText(store.getUsername());

        GridBagConstraints outer = new GridBagConstraints();
        outer.anchor = GridBagConstraints.NORTHWEST;
        outer.weightx = 1;
        outer.weighty = 1;
        outer.ipadx = 260;
        add(card, outer);
    }

    private static void add(JPanel card, String label, JComponent field, GridBagConstraints c) {
        c.insets = new Insets(0, 0, 6, 0);
        card.add(Theme.label(label, Theme.LABEL, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 14, 0);
        card.add(field, c);
    }

    private void save() {
        String name = username.getText().trim();
        char[] newPass = newPassword.getPassword();
        char[] confirmPass = confirm.getPassword();
        try {
            if (!name.matches("[A-Za-z0-9_.]{3,20}")) {
                warn("Username must be 3-20 letters or numbers.");
                return;
            }
            if (!store.checkLogin(store.getUsername(), current.getPassword())) {
                warn("The current password is not correct.");
                return;
            }
            if (newPass.length < 4) {
                warn("The new password must have at least 4 characters.");
                return;
            }
            if (!Arrays.equals(newPass, confirmPass)) {
                warn("The new passwords do not match.");
                return;
            }
            store.changeCredentials(name, newPass);
            current.setText("");
            newPassword.setText("");
            confirm.setText("");
            JOptionPane.showMessageDialog(this, "Login details updated. Use them the next time you log in.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(newPass, '\0');
            Arrays.fill(confirmPass, '\0');
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }
}
