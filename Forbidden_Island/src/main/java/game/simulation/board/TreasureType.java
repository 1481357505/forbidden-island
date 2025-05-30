package game.simulation.board;

public enum TreasureType {
    EARTH_STONE("The Earth Stone"),
    STATUE_OF_THE_WIND("The Statue of the Wind"),
    CRYSTAL_OF_FIRE("The Crystal of Fire"),
    OCEANS_CHALICE("The Ocean's Chalice");

    private final String displayName;

    TreasureType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 