package busreservationsystem;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Simple horizontal bar chart (label on the left, bar, value on the right).
 *
 * @author Hafsa Khurram, Zainab Asif
 */
public class BarChart extends JPanel {

    private static final int ROW = 34;

    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private final List<String> valueTexts = new ArrayList<>();
    private String emptyMessage = "No data yet.";

    public BarChart() {
        setOpaque(false);
    }

    public void setEmptyMessage(String message) {
        emptyMessage = message;
    }

    public void setData(List<String> newLabels, List<Double> newValues, List<String> newValueTexts) {
        labels.clear();
        values.clear();
        valueTexts.clear();
        labels.addAll(newLabels);
        values.addAll(newValues);
        valueTexts.addAll(newValueTexts);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, Math.max(80, labels.size() * ROW + 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.SMALL);
        FontMetrics fm = g2.getFontMetrics();

        if (labels.isEmpty()) {
            g2.setColor(Theme.MUTED);
            g2.drawString(emptyMessage, 4, 30);
            g2.dispose();
            return;
        }

        int labelWidth = 0;
        int valueWidth = 0;
        double max = 0;
        for (int i = 0; i < labels.size(); i++) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(labels.get(i)));
            valueWidth = Math.max(valueWidth, fm.stringWidth(valueTexts.get(i)));
            max = Math.max(max, values.get(i));
        }
        labelWidth = Math.min(labelWidth, 190);
        int barX = labelWidth + 14;
        int barMax = Math.max(40, getWidth() - barX - valueWidth - 16);

        for (int i = 0; i < labels.size(); i++) {
            int y = i * ROW + 6;
            g2.setColor(Theme.TEXT);
            g2.drawString(labels.get(i), 0, y + 17);
            g2.setColor(new Color(238, 239, 248));
            g2.fillRoundRect(barX, y + 5, barMax, 16, 16, 16);
            int width = max <= 0 ? 0 : (int) Math.round(barMax * values.get(i) / max);
            g2.setColor(i == 0 ? Theme.ACCENT : Theme.PRIMARY);
            g2.fillRoundRect(barX, y + 5, Math.max(width, 6), 16, 16, 16);
            g2.setColor(Theme.MUTED);
            g2.drawString(valueTexts.get(i), barX + barMax + 10, y + 17);
        }
        g2.dispose();
    }
}
