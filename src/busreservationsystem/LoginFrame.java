package busreservationsystem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Admin login screen (the "BusLogin" class from the proposal).
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class LoginFrame extends JFrame {

    private static final int MAX_ATTEMPTS = 3;

    private final DataStore store;
    private final JTextField usernameField = Theme.textField();
    private final JPasswordField passwordField = Theme.passwordField();
    private final JLabel messageLabel = Theme.label(" ", Theme.SMALL, Theme.DANGER);
    private final JButton loginButton = Theme.button("LOGIN", Theme.ButtonStyle.PRIMARY);
    private int failedAttempts;

    public LoginFrame(DataStore store) {
        this.store = store;
        setTitle("Bus Reservation System - Login");
        setIconImage(Theme.appIcon());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(createBrandPanel());
        add(createFormPanel());
        setSize(960, 580);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
    }

    /** Left side: indigo gradient with the bus logo and a short tagline. */
    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), getHeight(), Theme.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-80, -80, 260, 260);
                g2.fillOval(getWidth() - 160, getHeight() - 170, 300, 300);
                g2.dispose();
            }
        };

        JPanel logoCard = new Theme.Card(new BorderLayout());
        logoCard.add(new JLabel(Theme.image("1.png", 300)), BorderLayout.CENTER);

        JLabel title = Theme.label("Bus Reservation System", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28), Color.WHITE);
        JLabel tagline = Theme.label("<html><div style='text-align:center'>Book seats, manage routes and track "
                + "earnings,<br>all in one place.</div></html>", Theme.BODY, new Color(220, 220, 255));
        tagline.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(8, 20, 8, 20);
        panel.add(logoCard, c);
        c.insets = new Insets(26, 20, 4, 20);
        panel.add(title, c);
        c.insets = new Insets(4, 20, 8, 20);
        panel.add(tagline, c);
        return panel;
    }

    /** Right side: the login form. */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(330, 380));

        JCheckBox showPassword = new JCheckBox("Show password");
        showPassword.setFont(Theme.SMALL);
        showPassword.setOpaque(false);
        showPassword.setForeground(Theme.MUTED);
        char echo = passwordField.getEchoChar();
        showPassword.addActionListener(e -> passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : echo));

        JButton clearButton = Theme.button("CLEAR", Theme.ButtonStyle.SECONDARY);
        loginButton.addActionListener(e -> handleLogin());
        clearButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            messageLabel.setText(" ");
            usernameField.requestFocusInWindow();
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.add(clearButton);
        buttons.add(loginButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(Theme.label("Welcome back!", Theme.TITLE, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 26, 0);
        form.add(Theme.label("Sign in to the admin portal", Theme.BODY, Theme.MUTED), c);
        c.insets = new Insets(0, 0, 6, 0);
        form.add(Theme.label("Username", Theme.LABEL, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 16, 0);
        form.add(usernameField, c);
        c.insets = new Insets(0, 0, 6, 0);
        form.add(Theme.label("Password", Theme.LABEL, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 4, 0);
        form.add(passwordField, c);
        form.add(showPassword, c);
        c.insets = new Insets(4, 0, 12, 0);
        form.add(messageLabel, c);
        c.insets = new Insets(0, 0, 0, 0);
        form.add(buttons, c);

        panel.add(form);
        return panel;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty() || passwordField.getPassword().length == 0) {
            messageLabel.setText("Please enter both username and password.");
            return;
        }
        if (store.checkLogin(username, passwordField.getPassword())) {
            AdminPortal portal = new AdminPortal(store);
            portal.setVisible(true);
            dispose();
            return;
        }
        failedAttempts++;
        passwordField.setText("");
        if (failedAttempts >= MAX_ATTEMPTS) {
            messageLabel.setText("Too many failed attempts. Please restart the app.");
            loginButton.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
        } else {
            messageLabel.setText("Wrong username or password. " + (MAX_ATTEMPTS - failedAttempts)
                    + " attempt(s) left.");
            passwordField.requestFocusInWindow();
        }
    }
}
