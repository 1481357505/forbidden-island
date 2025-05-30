package game.simulation.player;
//import game.simulation.GameState;
import game.simulation.board.GameTile;
import game.simulation.card.Card;
import game.simulation.card.TreasureCard;
import game.simulation.board.TreasureType;

//import java.lang.reflect.Array;
import java.util.ArrayList;

public class Player
{
    private ArrayList<String> playerDeck;
    private boolean deckFilled, hasSunk;
    private String role;
    private int[] position;
    public static final int DEFAULT_ACTION_POINTS = 3;
    private int actionPoints;
    private boolean hasUsedFlightAbilityThisTurn;
    private boolean hasUsedCommandAbilityThisTurn;

    public Player(String role, ArrayList<String> startingDeck)
    {
        playerDeck = startingDeck;
        deckFilled = false;
        hasSunk = false;
        this.role = role;
        position = new int[2];
        this.actionPoints = DEFAULT_ACTION_POINTS;
        if (this.role.equals("Pilot")) {
            this.hasUsedFlightAbilityThisTurn = false;
        }
        if (this.role.equals("Navigator")) {
            this.hasUsedCommandAbilityThisTurn = false;
        }
    }

    public void drawCard(String c)
    {
        if (c != null) {
            playerDeck.add(c);
            System.out.println(getRole() + " added " + c + " to hand. Hand size: " + playerDeck.size());
        }
    }

    public void updatePosition(int[] newPos)
    {
        position = newPos;
    }

    public void disposeTreasure(String treasure)
    {
        ArrayList<String> player = this.playerDeck;

        for(String card: player)
        {
            if(card.equals(treasure))
            {
                player.remove(card);
            }
        }
    }

    public void setDeckFilled(boolean deckFilled) {
        this.deckFilled = deckFilled;
    }

    public void disposeCard()
    {
        ArrayList<String> player = this.playerDeck;
        if(deckFilled)
        {
           player.remove(player.size()-1);
        }
    }

    public ArrayList<String> getDeck()
    {
        return playerDeck;
    }


    public void giveTreasure(String treasure, Player send, Player receive)
    {
        ArrayList<String> rec = receive.getDeck();
        ArrayList<String> sen = send.getDeck();

        for(String card: sen)
        {
            if(card.equals(treasure))
            {
                sen.remove(card);
                rec.add(card);
                break;
            }
        }
    }

    public void shoreUp(GameTile tile)
    {
        tile.setFlooded(false);
    }

    public void movePawn(int[] pos)
    {
        position = pos;
    }

    public int[] getPos()
    {
        return position;
    }

    public String getRole() {
        return role;
    }

    public void setPosition(int[] pos) {
        position = pos;
    }

    public int getActionPoints() {
        return actionPoints;
    }

    public boolean hasActionPoints() {
        return actionPoints > 0;
    }

    public void useActionPoint() {
        if (actionPoints > 0) {
            actionPoints--;
        }
    }

    public void resetActionPoints() {
        this.actionPoints = DEFAULT_ACTION_POINTS;
        if (this.role.equals("Pilot")) {
            this.hasUsedFlightAbilityThisTurn = false;
        }
        if (this.role.equals("Navigator")) {
            this.hasUsedCommandAbilityThisTurn = false;
        }
    }

    public boolean canUseFlightAbility() {
        return this.role.equals("Pilot") && !this.hasUsedFlightAbilityThisTurn;
    }

    public void markFlightAbilityUsed() {
        if (this.role.equals("Pilot")) {
            this.hasUsedFlightAbilityThisTurn = true;
        }
    }

    public boolean canUseCommandAbility() {
        return this.role.equals("Navigator") && !this.hasUsedCommandAbilityThisTurn;
    }

    public void markCommandAbilityUsed() {
        if (this.role.equals("Navigator")) {
            this.hasUsedCommandAbilityThisTurn = true;
        }
    }

    public int countTreasureCards(TreasureType treasureType) {
        if (playerDeck == null || treasureType == null) {
            return 0;
        }
        int count = 0;
        for (String cardName : playerDeck) {
            // Assuming cardName for treasure cards is simply the TreasureType's name()
            if (cardName.equals(treasureType.name())) {
                count++;
            }
        }
        return count;
    }

    public boolean discardTreasureCards(TreasureType treasureType, int count) {
        if (playerDeck == null || treasureType == null || count <= 0) {
            return false;
        }
        if (countTreasureCards(treasureType) < count) {
            return false; // Not enough cards to discard
        }

        int cardsDiscarded = 0;
        ArrayList<String> cardsToRemove = new ArrayList<>();
        for (String cardName : playerDeck) {
            if (cardName.equals(treasureType.name()) && cardsDiscarded < count) {
                cardsToRemove.add(cardName);
                cardsDiscarded++;
            }
        }
        playerDeck.removeAll(cardsToRemove);
        return cardsDiscarded == count;
    }

    public int getHandSize() {
        return playerDeck != null ? playerDeck.size() : 0;
    }

    // Discards a card by its reference/name from the hand. Returns true if successful.
    public boolean discardSpecificCardByName(String cardNameToDiscard) {
        if (playerDeck == null || cardNameToDiscard == null) {
            return false;
        }
        return playerDeck.remove(cardNameToDiscard);
    }

    // Discards a card by its index. Returns the name of the discarded card, or null if index is invalid.
    public String discardCardByIndex(int index) {
        if (playerDeck == null || index < 0 || index >= playerDeck.size()) {
            return null;
        }
        return playerDeck.remove(index);
    }

    public ArrayList<String> getHand() { // Added getter for the player's hand content
        return new ArrayList<>(playerDeck); // Return a copy to prevent direct external modification
    }

    public ArrayList<String> getGiveableTreasureCards() {
        ArrayList<String> giveableCards = new ArrayList<>();
        if (playerDeck == null) {
            return giveableCards;
        }
        for (String cardName : playerDeck) {
            // Assuming TreasureType.name() are the treasure cards.
            // Special cards like "HelicopterLift", "Sandbag" should not be given.
            // "WatersRise" should not be in hand anyway.
            if (!cardName.equals("HelicopterLift") && !cardName.equals("Sandbag") && !cardName.equals("WatersRise")) {
                 // A more robust way would be to check if cardName is a value in TreasureType. GSON for enum check?
                 // For now, direct string comparison against known special cards is okay.
                try {
                    TreasureType.valueOf(cardName); // This will throw IllegalArgumentException if cardName is not a TreasureType
                    giveableCards.add(cardName);
                } catch (IllegalArgumentException e) {
                    // It's not a standard TreasureType name, so it might be a special card we haven't excluded
                    // or an unexpected card. For now, we assume only TreasureType names are giveable.
                    // System.out.println("Card '" + cardName + "' is not a giveable treasure card.");
                }
            }
        }
        return giveableCards;
    }
}
