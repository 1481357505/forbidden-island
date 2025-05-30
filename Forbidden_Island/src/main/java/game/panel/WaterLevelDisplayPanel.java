package game.panel;

import game.simulation.board.WaterLevelMeter;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class WaterLevelDisplayPanel extends JPanel {
    private WaterLevelMeter waterLevelMeter;
    private static final int NUM_DISPLAY_LEVELS = 10; // Displaying 10 segments (0-9)
    private static final int SEGMENT_HEIGHT = 30;
    private static final int SEGMENT_WIDTH = 100;
    private static final int TEXT_OFFSET_Y = 20;
    private Image backgroundImage;

    public WaterLevelDisplayPanel(WaterLevelMeter meter) {
        this.waterLevelMeter = meter;
        setPreferredSize(new Dimension(SEGMENT_WIDTH + 20, NUM_DISPLAY_LEVELS * SEGMENT_HEIGHT + 40));
        setBorder(BorderFactory.createTitledBorder("Water Level"));
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/Images/FloodMeter/Water_Meter_Water.png"));
        } catch (IOException e) {
            e.printStackTrace();
            backgroundImage = null; // Set to null if loading fails
        }
    }

    public void updateMeter(WaterLevelMeter meter) {
        this.waterLevelMeter = meter;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        if (waterLevelMeter == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int currentActualLevelIndex = waterLevelMeter.getCurrentLevelIndex(); // This is 0-9

        // Loop through the 10 display segments (0 to 9)
        for (int displayIndex = 0; displayIndex < NUM_DISPLAY_LEVELS; displayIndex++) {
            // displayIndex maps directly to the actual level index (0-9) from WaterLevelMeter
            int actualLevelIndex = displayIndex;
            
            int y = getHeight() - (displayIndex + 1) * SEGMENT_HEIGHT - 20; // Draw from bottom up

            // Segment Background Color
            if (actualLevelIndex <= currentActualLevelIndex) {
                g2d.setColor(new Color(30, 144, 255)); // Dodger Blue for filled
            } else {
                g2d.setColor(Color.LIGHT_GRAY); // Light gray for empty
            }
            g2d.fillRect(10, y, SEGMENT_WIDTH, SEGMENT_HEIGHT);

            // Segment Border
            if (waterLevelMeter.isDangerLevel(actualLevelIndex)) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
            } else {
                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawRect(10, y, SEGMENT_WIDTH, SEGMENT_HEIGHT);
            g2d.setStroke(new BasicStroke(1)); // Reset stroke

            // Text (Number of flood cards or Skull)
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String text;
            if (actualLevelIndex == WaterLevelMeter.MAX_WATER_LEVEL) { // MAX_WATER_LEVEL is 9 (the highest index)
                text = "SKULL";
                 if (actualLevelIndex <= currentActualLevelIndex) g2d.setColor(Color.WHITE);
            } else {
                text = String.valueOf(waterLevelMeter.getFloodCardsAtLevel(actualLevelIndex)) + " cards";
                 if (actualLevelIndex <= currentActualLevelIndex) g2d.setColor(Color.WHITE);
            }
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text, 10 + (SEGMENT_WIDTH - textWidth) / 2, y + TEXT_OFFSET_Y);
        }
    }
} 