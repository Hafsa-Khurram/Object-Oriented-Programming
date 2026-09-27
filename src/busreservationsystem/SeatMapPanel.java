package busreservationsystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JPanel;

/**
 * Clickable bus seat map (2 + 2 layout). Booked seats are grey, Business seats have a
 * gold outline, and seats picked by the user turn green. The seat count comes from the
 * route, so the map changes size automatically for every bus.
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class SeatMapPanel extends JPanel {

    private static final int SEAT = 36;
    private static final int GAP = 7;
    private static final int AISLE = 30;
    private static final int PAD = 22;
    private static final int FRONT = 58;

    private Route route;
    private Set<Integer> booked = new TreeSet<>();
    private final Set<Integer> selected = new TreeSet<>();
    private int hoverSeat = -1;
    private final List<Runnable> listeners = new ArrayList<>();

    public SeatMapPanel() {
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int seat = seatAt(e.getX(), e.getY());
                if (seat > 0 && !booked.contains(seat)) {
                    if (!selected.remove(seat)) {
                        selected.add(seat);
                    }
                    repaint();
                    fireChanged();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int seat = seatAt(e.getX(), e.getY());
                if (seat != hoverSeat) {
                    hoverSeat = seat;
                    setCursor(java.awt.Cursor.getPredefinedCursor(seat > 0 && !booked.contains(seat)
                            ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                    setToolTipText(seat > 0 ? tooltip(seat) : null);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverSeat = -1;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** Shows the seats of a trip. Pass the seats to pre-select when editing a booking. */
    public void showTrip(Route route, Set<Integer> bookedSeats, Set<Integer> preselected) {
        this.route = route;
        this.booked = new TreeSet<>(bookedSeats);
        selected.clear();
        if (preselected != null) {
            for (int seat : preselected) {
                if (route != null && seat <= route.getTotalSeats() && !booked.contains(seat)) {
                    selected.add(seat);
                }
            }
        }
        revalidate();
        repaint();
        fireChanged();
    }

    public List<Integer> getSelectedSeats() {
        return Collections.unmodifiableList(new ArrayList<>(selected));
    }

    public int getAvailableCount() {
        return route == null ? 0 : route.getTotalSeats() - booked.size();
    }

    public void clearSelection() {
        selected.clear();
        repaint();
        fireChanged();
    }

    public void addSelectionListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private String tooltip(int seat) {
        String state = booked.contains(seat) ? "Booked" : selected.contains(seat) ? "Selected" : "Available";
        return "Seat " + seat + "  |  " + route.seatClass(seat) + "  |  " + Theme.money(route.seatFare(seat))
                + "  |  " + state;
    }

    private int rows() {
        return route == null ? 0 : (route.getTotalSeats() + 3) / 4;
    }

    private int busWidth() {
        return PAD * 2 + SEAT * 4 + GAP * 2 + AISLE;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(busWidth() + 4, FRONT + rows() * (SEAT + GAP) + PAD + 6);
    }

    private int originX() {
        return Math.max(0, (getWidth() - busWidth()) / 2);
    }

    /** Screen rectangle of a seat (seat numbers start at 1, four per row). */
    private Rectangle seatBounds(int seat) {
        int index = seat - 1;
        int row = index / 4;
        int col = index % 4;
        int x = originX() + PAD + col * (SEAT + GAP) + (col >= 2 ? AISLE - GAP : 0);
        int y = FRONT + row * (SEAT + GAP);
        return new Rectangle(x, y, SEAT, SEAT);
    }

    private int seatAt(int x, int y) {
        if (route == null) {
            return -1;
        }
        for (int seat = 1; seat <= route.getTotalSeats(); seat++) {
            if (seatBounds(seat).contains(x, y)) {
                return seat;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (route == null) {
            g2.setFont(Theme.BODY);
            g2.setColor(Theme.MUTED);
            String msg = "Choose a route, date and time to see the seats.";
            g2.drawString(msg, (getWidth() - g2.getFontMetrics().stringWidth(msg)) / 2, 60);
            g2.dispose();
            return;
        }

        // Bus body
        int x0 = originX();
        int height = FRONT + rows() * (SEAT + GAP) + PAD - GAP;
        g2.setColor(new Color(250, 250, 255));
        g2.fillRoundRect(x0, 0, busWidth(), height, 40, 40);
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x0, 0, busWidth(), height, 40, 40);

        // Driver's steering wheel and door
        int wx = x0 + busWidth() - PAD - SEAT + 4;
        g2.setColor(Theme.MUTED);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(wx, 14, 28, 28);
        g2.fillOval(wx + 11, 25, 6, 6);
        g2.setFont(Theme.SMALL);
        g2.drawString("FRONT", x0 + PAD, 32);

        for (int seat = 1; seat <= route.getTotalSeats(); seat++) {
            drawSeat(g2, seat);
        }
        g2.dispose();
    }

    private void drawSeat(Graphics2D g2, int seat) {
        Rectangle r = seatBounds(seat);
        boolean isBooked = booked.contains(seat);
        boolean isSelected = selected.contains(seat);
        boolean business = route.isBusinessSeat(seat);

        Color fill;
        Color text;
        if (isBooked) {
            fill = Theme.SEAT_BOOKED;
            text = Color.WHITE;
        } else if (isSelected) {
            fill = Theme.SUCCESS;
            text = Color.WHITE;
        } else {
            fill = business ? Theme.ACCENT_LIGHT : Theme.PRIMARY_LIGHT;
            text = business ? new Color(146, 64, 14) : Theme.PRIMARY;
        }
        if (seat == hoverSeat && !isBooked && !isSelected) {
            fill = fill.darker();
        }

        g2.setColor(fill);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        // Backrest
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(r.x + 4, r.y + r.height - 7, r.width - 8, 4, 4, 4);
        if (business && !isBooked) {
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        }

        g2.setColor(text);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String label = String.valueOf(seat);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + (r.height + fm.getAscent()) / 2 - 3);
        if (isBooked) {
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(r.x + 6, r.y + 6, r.x + r.width - 6, r.y + r.height - 10);
        }
    }

    /** Small colour legend shown under the seat map. */
    public static JPanel legend() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 0));
        panel.setOpaque(false);
        panel.add(legendItem(Theme.PRIMARY_LIGHT, null, "Economy"));
        panel.add(legendItem(Theme.ACCENT_LIGHT, Theme.ACCENT, "Business"));
        panel.add(legendItem(Theme.SUCCESS, null, "Selected"));
        panel.add(legendItem(Theme.SEAT_BOOKED, null, "Booked"));
        return panel;
    }

    private static javax.swing.JLabel legendItem(Color fill, Color outline, String text) {
        javax.swing.JLabel label = Theme.label(text, Theme.SMALL, Theme.MUTED);
        label.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(x, y, 16, 16, 5, 5);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.drawRoundRect(x, y, 15, 15, 5, 5);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        });
        return label;
    }
}
