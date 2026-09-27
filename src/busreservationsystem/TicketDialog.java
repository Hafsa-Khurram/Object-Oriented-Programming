package busreservationsystem;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Shows a printable bus ticket for a booking (the "Book" details from the proposal).
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class TicketDialog extends JDialog {

    public TicketDialog(Frame owner, Booking booking) {
        super(owner, "Ticket " + booking.getTicketNo(), true);
        TicketView ticket = new TicketView(booking);

        JButton print = Theme.button("Print", Theme.ButtonStyle.SECONDARY);
        JButton close = Theme.button("Done", Theme.ButtonStyle.PRIMARY);
        print.addActionListener(e -> print(ticket));
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(print);
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));
        root.add(ticket, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(close);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void print(TicketView ticket) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Bus ticket");
        job.setPrintable((graphics, format, page) -> {
            if (page > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(format.getImageableX(), format.getImageableY());
            double scale = Math.min(1, format.getImageableWidth() / ticket.getWidth());
            g2.scale(scale, scale);
            ticket.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(), "Print",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** The ticket itself, drawn like a boarding pass. */
    private static class TicketView extends JPanel {
        private final Booking b;

        TicketView(Booking booking) {
            this.b = booking;
            setPreferredSize(new Dimension(640, 372));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;

            // Card with header band
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 24, 24);
            g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, w, 0, Theme.PRIMARY_DARK));
            g2.fillRoundRect(0, 0, w, 78, 24, 24);
            g2.fillRect(0, 50, w, 28);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w, h, 24, 24);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(18, 14, 72, 50, 12, 12);
            g2.drawImage(Theme.image("Bus.png", 62).getImage(), 23, 21, null);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.drawString("BUS TICKET", 104, 38);
            g2.setFont(Theme.SMALL);
            g2.drawString("Bus Reservation System", 104, 58);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            String ticket = b.getTicketNo();
            g2.drawString(ticket, w - 24 - g2.getFontMetrics().stringWidth(ticket), 46);

            // Route
            int stub = w - 190;
            g2.setColor(Theme.TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
            g2.drawString(b.getFrom().toUpperCase(Locale.ENGLISH), 26, 124);
            int fromWidth = g2.getFontMetrics().stringWidth(b.getFrom().toUpperCase(Locale.ENGLISH));
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 5f}, 0f));
            g2.drawLine(36 + fromWidth, 115, 86 + fromWidth, 115);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(80 + fromWidth, 109, 86 + fromWidth, 115);
            g2.drawLine(80 + fromWidth, 121, 86 + fromWidth, 115);
            g2.setColor(Theme.TEXT);
            g2.drawString(b.getTo().toUpperCase(Locale.ENGLISH), 96 + fromWidth, 124);

            // Details grid
            String date = b.getTravelDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH));
            String[][] fields = {
                {"PASSENGER", b.getFullName()}, {"GENDER", b.getGender()},
                {"DATE", date}, {"DEPARTURE", b.getTime()},
                {"SEAT(S)", b.getSeatsText()}, {"CLASS", b.getSeatClass()},
                {"PHONE", b.getPhone().isEmpty() ? "-" : b.getPhone()}, {"STATUS", b.getStatus()}
            };
            for (int i = 0; i < fields.length; i++) {
                int x = 26 + (i % 2) * 210;
                int y = 162 + (i / 2) * 42;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(Theme.MUTED);
                g2.drawString(fields[i][0], x, y);
                g2.setFont(Theme.LABEL);
                g2.setColor(Booking.CANCELLED.equals(fields[i][1]) ? Theme.DANGER : Theme.TEXT);
                g2.drawString(fit(g2, fields[i][1], 195), x, y + 18);
            }

            // Perforation and payment stub
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{6f, 6f}, 0f));
            g2.drawLine(stub, 92, stub, h - 14);
            g2.setColor(Theme.BACKGROUND);
            g2.fillOval(stub - 10, h - 10, 20, 20);
            g2.setStroke(new BasicStroke(1f));

            String[][] money = {
                {"FARE", Theme.money(b.getFare())},
                {"PAID", Theme.money(b.getPaid())},
                {b.getChange() >= 0 ? "CHANGE" : "BALANCE DUE", Theme.money(Math.abs(b.getChange()))}
            };
            for (int i = 0; i < money.length; i++) {
                int y = 124 + i * 56;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(Theme.MUTED);
                g2.drawString(money[i][0], stub + 24, y);
                g2.setFont(new Font("Segoe UI", Font.BOLD, i == 0 ? 22 : 16));
                g2.setColor(i == 0 ? Theme.PRIMARY : Theme.TEXT);
                g2.drawString(money[i][1], stub + 24, y + 24);
            }
            g2.setFont(Theme.SMALL);
            g2.setColor(Theme.MUTED);
            g2.drawString("Please arrive 20 minutes before departure.", 26, h - 18);
            g2.dispose();
        }

        private static String fit(Graphics2D g2, String text, int width) {
            if (g2.getFontMetrics().stringWidth(text) <= width) {
                return text;
            }
            String t = text;
            while (t.length() > 1 && g2.getFontMetrics().stringWidth(t + "...") > width) {
                t = t.substring(0, t.length() - 1);
            }
            return t + "...";
        }
    }
}
