package game.simulation.board;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WaterLevelMeter extends JPanel {
//    private BufferedImage frame = ImageIO.read(getClass().getClassLoader().getResource("Images/FloodMeter/Water_Meter_Frame.png"));
    private int height = 800;
    private int width = 300;
    private BufferedImage levelImg;
    private int waterLevel;
    private int[] waterLevelTracker = {2,2,3,3,3,4,4,4,4,6};
    public static final int MAX_WATER_LEVEL = 9; // Index for waterLevelTracker (0-9 for 10 levels)

    public WaterLevelMeter(int num) throws IOException {
        // waterLevel is 0-indexed for the array, but often referred to as 1-10 by players.
        // Let's assume 'num' is the starting level (e.g. 1, 2, 3, 4), so convert to 0-indexed.
        // Difficulty: Novice (1), Normal (2), Elite (3), Legendary (4)
        // These usually map to starting indices in waterLevelTracker.
        // For simplicity, if num is 0,1,2,3 it can be direct index. Let's assume num is already 0-indexed for tracker.
        this.waterLevel = Math.max(0, Math.min(num, MAX_WATER_LEVEL)); // Ensure it's within bounds
        // paint(this.getGraphics()); // Painting should be handled by Swing normally, not in constructor directly
    }

    public int getCurrentLevelIndex(){
        // Returns 0-indexed water level
        return waterLevel;
    }

    public int getLevelForDisplay(){
        // Returns 1-indexed water level for player display
        return waterLevel + 1;
    }

    public int getNumCards(){
        // Ensure waterLevel is a valid index
        if (waterLevel >= 0 && waterLevel < waterLevelTracker.length) {
            return waterLevelTracker[waterLevel];
        }
        return waterLevelTracker[waterLevelTracker.length - 1]; // Return max cards if out of bounds (should not happen)
    }

    public void increaseLevel() {
        if (waterLevel < MAX_WATER_LEVEL) {
            waterLevel++;
            System.out.println("DEBUG: Water level increased to " + getLevelForDisplay());
        } else {
            System.out.println("DEBUG: Water level is already at maximum (" + getLevelForDisplay() + ").");
        }
    }

    public boolean isAtMaxLevelDanger() {
        // Game over if water level marker reaches skull and crossbones.
        // The waterLevelTracker has 10 values (indices 0-9).
        // If level 5 (index 4) is dangerous, and level 10 (index 9) is game over.
        // The last value in waterLevelTracker (index 9) often means 5 cards + game over if it reaches here.
        // Let's say index 9 (representing level 10) is game over.
        return waterLevel == MAX_WATER_LEVEL;
    }

    // New method for WaterLevelDisplayPanel
    public boolean isDangerLevel(int levelIndex) {
        // Danger levels (0-indexed): 4 (Level 5), 6 (Level 7), 8 (Level 9), 9 (Level 10/Skull)
        return levelIndex == 4 || levelIndex == 6 || levelIndex == 8 || levelIndex == 9;
    }

    // New method for WaterLevelDisplayPanel
    public int getFloodCardsAtLevel(int levelIndex) {
        if (levelIndex >= 0 && levelIndex < waterLevelTracker.length) {
            return waterLevelTracker[levelIndex];
        }
        // Should not happen if levelIndex is within 0 to MAX_LEVELS-1 in display panel
        return waterLevelTracker[waterLevelTracker.length - 1]; 
    }
}
