package game.simulation.tiles;

import game.simulation.board.TreasureType; // Import TreasureType
import java.awt.image.BufferedImage;

public class GameTile {
    private String name;
    private BufferedImage image;
    private int[] position; // {row, col}
    private boolean isFlooded;
    private boolean isStarting;
    private String originalTileName; // CamelCase name used for resource loading and identification
    private TreasureType treasureType; // Which treasure this tile is associated with (can be null)
    private boolean isFoolsLanding;    // Is this the Fools' Landing tile?

    public GameTile(String name, BufferedImage image, int[] pos, String originalTileName, 
                    boolean isStarting, boolean isFlooded, 
                    TreasureType treasureType, boolean isFoolsLanding) {
        this.name = name; // Display name
        this.image = image;
        this.position = pos;
        this.originalTileName = originalTileName;
        this.isStarting = isStarting;
        this.isFlooded = isFlooded;
        this.treasureType = treasureType;
        this.isFoolsLanding = isFoolsLanding;
    }

    // Getters
    public String getName() {
        return name;
    }

    public BufferedImage getImage() {
        return image;
    }

    public int[] getPosition() {
        return position;
    }

    public boolean getFloodState() {
        return isFlooded;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public String getOriginalName() {
        return originalTileName;
    }

    public TreasureType getTreasureType() {
        return treasureType;
    }

    public boolean isFoolsLanding() {
        return isFoolsLanding;
    }

    public boolean isFlooded() {
        return isFlooded;
    }

    public boolean isSunk() {
        return isFlooded; // Assuming sunk is equivalent to flooded for this check
    }

    // Setters
    public void setPosition(int[] pos) {
        this.position = pos;
    }

    public void setFlooded(boolean flooded) {
        this.isFlooded = flooded;
    }

    public void setIsStarting(boolean isStarting) {
        this.isStarting = isStarting;
    }

    // For UI rendering, a tile might need to signal it has sunk (image becomes null)
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    // It might be useful to set treasure type or fools landing status after construction too,
    // but for now, constructor initialization is primary.
    public void setTreasureType(TreasureType type) {
        this.treasureType = type;
    }

    public void setIsFoolsLanding(boolean isFoolsLanding) {
        this.isFoolsLanding = isFoolsLanding;
    }
} 