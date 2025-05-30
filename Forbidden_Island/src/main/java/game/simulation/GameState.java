package game.simulation;

import game.panel.GameBoard;
import game.simulation.tiles.GameTile;
import game.simulation.board.TreasurePiece;
import game.simulation.board.WaterLevelMeter;
import game.simulation.board.TreasureType;
import game.simulation.card.Card;
import game.simulation.player.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameState {
    private GameBoard board;
    private int waterLevel;
    private TreasurePiece[] treasuresCollected;
    private int numPlayers;
    private final String[] allRoles;
    private ArrayList<Player> allPlayers;
    private int playerTurn = 0;
    private Player currentPlayer;
    private WaterLevelMeter meter;
    private ArrayList<Card> currentDeck;
    private static Stack<String> cardDeck;
    private static Stack<String> discardPile;
    private ArrayList<GameTile> moveableSpaces;

    // Flood mechanism components
    private Stack<String> floodDeck;
    private Stack<String> floodDiscardPile;

    public static final int BOARD_ROWS = 6;
    public static final int BOARD_COLS = 6;
    private GameTile[][] boardTiles;

    Iterator<Player> playerIterator;

    private static final boolean[][] TILE_LAYOUT_PATTERN = {
            {false, false, true, true, false, false},
            {false, true, true, true, true, false},
            {true, true, true, true, true, true},
            {true, true, true, true, true, true},
            {false, true, true, true, true, false},
            {false, false, true, true, false, false}
    };

    // Static map for tile names to their treasure types
    // IMPORTANT: User needs to verify these tileName-TreasureType mappings
    private static final Map<String, TreasureType> TILE_TO_TREASURE_MAP = new HashMap<>() {{
        put("TempleOfTheSun", TreasureType.EARTH_STONE);
        put("TempleOfTheMoon", TreasureType.EARTH_STONE);
        put("WhisperingGarden", TreasureType.STATUE_OF_THE_WIND);
        put("HowlingGarden", TreasureType.STATUE_OF_THE_WIND);
        put("CaveOfEmbers", TreasureType.CRYSTAL_OF_FIRE);
        put("CaveOfShadows", TreasureType.CRYSTAL_OF_FIRE);
        put("CoralPalace", TreasureType.OCEANS_CHALICE); 
        put("TidalPalace", TreasureType.OCEANS_CHALICE);
    }};

    private EnumMap<TreasureType, Boolean> treasuresCollectedMap;
    private Map<TreasureType, ArrayList<String>> treasureToTileNamesMap;

    public GameState(int difficulty, int numPlayers) throws IOException {
        this.numPlayers = numPlayers;
        this.allPlayers = new ArrayList<>();
        this.boardTiles = new GameTile[BOARD_ROWS][BOARD_COLS];
        this.floodDeck = new Stack<>();
        this.floodDiscardPile = new Stack<>();
        this.discardPile = new Stack<>(); // Initialize treasure discard pile

        this.treasuresCollectedMap = new EnumMap<>(TreasureType.class);
        for (TreasureType type : TreasureType.values()) {
            this.treasuresCollectedMap.put(type, false);
        }

        this.treasureToTileNamesMap = new EnumMap<>(TreasureType.class);
        TILE_TO_TREASURE_MAP.forEach((tileName, treasureType) -> 
            this.treasureToTileNamesMap.computeIfAbsent(treasureType, k -> new ArrayList<>()).add(tileName)
        );

        ArrayList<String> tileNames = new ArrayList<>(Arrays.asList(
                // Ensure all tiles from TILE_TO_TREASURE_MAP are in this list, plus others
                "MistyMarsh", "Observatory", "IronGate", "TidalPalace", "CrimsonForest",
                "BreakersBridge", "CaveOfEmbers", "TwilightHollow", "DunesOfDeception",
                "TempleOfTheMoon", "LostLagoon", "CaveOfShadows", "PhantomRock", "SilverGate",
                "Watchtower", "CopperGate", "CliffsOfAbandon", "WhisperingGarden", "TempleOfTheSun",
                "CoralPalace", "GoldGate", "FoolsLanding", "HowlingGarden", "BronzeGate"
        ));
        Collections.shuffle(tileNames);

        int tileNameIndex = 0;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                if (TILE_LAYOUT_PATTERN[r][c]) {
                    if (tileNameIndex < tileNames.size()) {
                        String originalTileName = tileNames.get(tileNameIndex);
                        BufferedImage img = loadImageForTile(originalTileName, false);
                        String displayName = originalTileName.replaceAll("([A-Z])", " $1").trim();
                        if (originalTileName.equals("FoolsLanding")) displayName = "Fools_ Landing";

                        boolean isStartingTile = originalTileName.equals("FoolsLanding") || 
                                                 originalTileName.equals("IronGate") || 
                                                 originalTileName.equals("BronzeGate") || 
                                                 originalTileName.equals("GoldGate") || 
                                                 originalTileName.equals("CopperGate");

                        // Determine treasure type and if it's Fools' Landing
                        boolean isFoolsLandingTile = originalTileName.equals("FoolsLanding");
                        TreasureType associatedTreasure = TILE_TO_TREASURE_MAP.get(originalTileName);

                        GameTile tile = new GameTile(displayName, img, new int[]{r, c}, originalTileName, 
                                                     isStartingTile, false, 
                                                     associatedTreasure, isFoolsLandingTile);
                        
                        this.boardTiles[r][c] = tile;
                        this.floodDeck.push(originalTileName);
                        tileNameIndex++;
                    }
                } else {
                    this.boardTiles[r][c] = null;
                }
            }
        }
        Collections.shuffle(this.floodDeck);

        // TEMPORARY: Manually flood some tiles for testing Shore Up
        // Make sure these coordinates correspond to actual tiles based on TILE_LAYOUT_PATTERN
        /* int[][] tilesToFloodForTest = {{0,2}, {1,2}, {2,2}, {2,3}}; // Example coordinates
        for (int[] coord : tilesToFloodForTest) {
            int rFlood = coord[0];
            int cFlood = coord[1];
            if (rFlood >= 0 && rFlood < BOARD_ROWS && cFlood >= 0 && cFlood < BOARD_COLS && this.boardTiles[rFlood][cFlood] != null) {
                this.boardTiles[rFlood][cFlood].setFlooded(true);
                System.out.println("DEBUG: Manually flooded tile: " + this.boardTiles[rFlood][cFlood].getName() + " at [" + rFlood + "," + cFlood + "]");
            } else {
                System.out.println("DEBUG: Could not manually flood tile at [" + rFlood + "," + cFlood + "] as it does not exist or is out of bounds.");
            }
        } */

        // Initialize WaterLevelMeter based on difficulty parameter
        // Assuming difficulty: 0=Novice, 1=Normal, 2=Elite, 3=Legendary maps to starting water levels
        // Standard starting levels: Novice (idx 0), Normal (idx 1), Elite (idx 2), Legendary (idx 3)
        // The WaterLevelMeter constructor now expects a 0-indexed level.
        int initialWaterLevelIndex = difficulty; // Or map difficulty (1-4) to index (0-3)
        // Let's map difficulty (typically 1-4) to an index for the meter.
        // Novice=1 (idx 0), Normal=2 (idx 1), Elite=3 (idx 2), Legendary=4 (idx 3)
        // If difficulty is passed as 1,2,3,4, then initialWaterLevelIndex = difficulty - 1;
        // If difficulty is passed as 0,1,2,3, then initialWaterLevelIndex = difficulty;
        // The `num` in `new WaterLevelMeter(num)` corresponds to the difficulty chosen in MenuPanel (0,1,2,3).
        // So `difficulty` here is already the 0-indexed starting level for the meter.
        meter = new WaterLevelMeter(difficulty);

        allRoles = new String[]{"Navigator", "Messenger", "Engineer", "Pilot", "Explorer", "Diver"};
        Random rnd = ThreadLocalRandom.current();
        for (int i = allRoles.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            String a = allRoles[index];
            allRoles[index] = allRoles[i];
            allRoles[i] = a;
        }

        // Initialize Treasure Card Deck (cardDeck)
        cardDeck = new Stack<>();
        // Standard deck: 5 of each treasure card, 3 Helicopter, 3 Sandbags, 2 Waters Rise (varies)
        // Assuming 4 treasure cards for each of the 4 treasures = 20 treasure cards
        // The previous code had 4 of each. Let's assume 5 of each treasure type for 4 treasures.
        for (TreasureType type : TreasureType.values()) {
            for (int i = 0; i < 5; i++) {
                cardDeck.push(type.name()); // Store as enum name string or a specific Card object type
            }
        }
        for(int i = 0; i < 3; i++) cardDeck.push("HelicopterLift");
        for(int i = 0; i < 2; i++) cardDeck.push("Sandbag");
        // Waters Rise cards are added after player hands are dealt.
        Collections.shuffle(cardDeck);

        ArrayList<GameTile> startingTiles = new ArrayList<>();
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                if (boardTiles[r][c] != null && boardTiles[r][c].isStarting()) {
                    startingTiles.add(boardTiles[r][c]);
                }
            }
        }
        Collections.shuffle(startingTiles);

        for(int i = 0; i < numPlayers; i++) {
            ArrayList<String> startingHand = new ArrayList<>(); // Player's hand of cards
            // Each player starts with 2 treasure cards
            if (cardDeck.size() >= 2) { // Ensure deck has enough cards
                startingHand.add(cardDeck.pop());
                startingHand.add(cardDeck.pop());
            } else {
                System.err.println("Warning: Not enough cards in treasure deck to deal initial hands.");
            }
            Player p = new Player(allRoles[i], startingHand);
            allPlayers.add(p);

            if (i < startingTiles.size()) {
                GameTile startTile = startingTiles.get(i);
                p.setPosition(startTile.getPosition());
            } else {
                System.err.println("Warning: Not enough unique starting tiles for all players. Player " + p.getRole() + " might share a start tile.");
                if (!startingTiles.isEmpty()) {
                    p.setPosition(startingTiles.get(0).getPosition());
                }
            }
        }
        // Add Waters Rise cards to the deck AFTER dealing initial hands
        for(int i = 0; i < 3; i++) cardDeck.push("WatersRise"); // Number of Waters Rise can vary (e.g., 2-5)
        Collections.shuffle(cardDeck);
        playerIterator = allPlayers.iterator();
    }

    private BufferedImage loadImageForTile(String tileName, boolean flooded) throws IOException {
        String baseName = tileName.replaceAll("([A-Z])", " $1").trim();
        if (tileName.equals("FoolsLanding")) baseName = "Fools_ Landing";

        String path = "Images/Tiles/" + baseName + (flooded ? "_flood" : "") + "@2x.png";
        try {
            return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)));
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading image for tile: " + tileName + " from path: " + path);
            throw new IOException("Failed to load image: " + path, e);
        }
    }

    public GameTile[][] getBoardTiles() {
        return boardTiles;
    }

    public ArrayList<Player> getAllPlayers() {
        return allPlayers;
    }

    public void shuffle(Stack<Card> pile) {
        Collections.shuffle(pile);
    }

    public void drawCard(Graphics g) {
    }

    public void setWater(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    public void raiseWater() {
        ++waterLevel;
    }

    public Player nextTurn() {
        if(allPlayers == null || allPlayers.isEmpty()) return null;
        if(playerIterator == null || !playerIterator.hasNext()) playerIterator = allPlayers.iterator();
        if(!playerIterator.hasNext()) return null;
        currentPlayer = playerIterator.next();
        if (currentPlayer != null) { // Ensure currentPlayer is not null before calling methods on it
            currentPlayer.resetActionPoints(); // Reset action points for the new current player
            // System.out.println("Current turn: " + currentPlayer.getRole() + " | AP: " + currentPlayer.getActionPoints()); // Debug line
        }
        return currentPlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public WaterLevelMeter getWaterLevelMeter() {
        return meter;
    }

    public int getFloodCardsToDrawThisTurn() {
        if (meter == null) return 0; // Should not happen
        return meter.getNumCards();
    }

    public void increaseWaterLevel() {
        if (meter != null) {
            meter.increaseLevel();
            // TODO: Check for game over condition here if (isGameOverByWaterLevel())
        }
    }

    public boolean isGameOverByWaterLevel() {
        if (meter == null) return false;
        return meter.isAtMaxLevelDanger();
    }

    // Method to draw and process flood cards at the end of a player's turn
    public ArrayList<GameTile> drawAndProcessFloodCards() {
        ArrayList<GameTile> affectedTilesForUI = new ArrayList<>();
        if (isGameOverByWaterLevel()) { // Or other game over conditions
            System.out.println("DEBUG: Game might be over, skipping flood card draw.");
            return affectedTilesForUI;
        }

        int numCardsToDraw = getFloodCardsToDrawThisTurn();
        System.out.println("DEBUG: Drawing " + numCardsToDraw + " flood card(s).");

        for (int i = 0; i < numCardsToDraw; i++) {
            if (floodDeck.isEmpty()) {
                // This case should ideally only be hit if Waters Rise! hasn't happened recently enough
                // or if the game is very long. Standard rule is Waters Rise! shuffles discard into deck.
                // If floodDeck is empty AND discard is also empty, something is wrong or all tiles sunk.
                if (floodDiscardPile.isEmpty()) {
                    System.out.println("DEBUG: Flood deck and discard pile are empty. No more tiles to flood or sink.");
                    break; 
                }
                System.out.println("DEBUG: Flood deck empty! Reshuffling flood discard pile into flood deck.");
                floodDeck.addAll(floodDiscardPile);
                floodDiscardPile.clear();
                Collections.shuffle(floodDeck);
            }
            if (floodDeck.isEmpty()) { // Still empty after potential reshuffle (all tiles sunk)
                 System.out.println("DEBUG: Flood deck is definitively empty. Cannot draw flood card.");
                 break;
            }

            String tileName = floodDeck.pop();
            System.out.println("DEBUG: Drew flood card for: " + tileName);
            boolean tileFoundAndProcessed = false;
            for (int r = 0; r < BOARD_ROWS; r++) {
                for (int c = 0; c < BOARD_COLS; c++) {
                    if (boardTiles[r][c] != null && boardTiles[r][c].getOriginalName().equals(tileName)) {
                        GameTile tile = boardTiles[r][c];
                        if (!tile.getFloodState()) {
                            tile.setFlooded(true);
                            System.out.println("DEBUG: Tile " + tile.getName() + " is now flooded.");
                            affectedTilesForUI.add(tile);
                        } else {
                            // Tile was already flooded, now it sinks
                            System.out.println("DEBUG: Tile " + tile.getName() + " was already flooded. It now sinks!");
                            boardTiles[r][c] = null; // Remove tile from board
                            // Sentinel tile for UI: Use all original properties, mark as flooded and use its original image or null
                            // GameTile(String name, BufferedImage image, int[] pos, String originalTileName, boolean isStarting, boolean isFlooded)
                            affectedTilesForUI.add(new GameTile(tile.getName(), null, tile.getPosition(), tile.getOriginalName(), tile.isStarting(), true, tile.getTreasureType(), tile.isFoolsLanding())); 
                            // TODO: Add checks for game over if critical tiles sink (e.g. FoolsLanding, Treasure tiles)
                        }
                        floodDiscardPile.push(tileName);
                        tileFoundAndProcessed = true;
                        break;
                    }
                }
                if (tileFoundAndProcessed) break;
            }
            if (!tileFoundAndProcessed) {
                // This means the card drawn was for a tile that has already sunk and is not on board.
                // This is normal. The card still goes to discard.
                System.out.println("DEBUG: Flood card " + tileName + " drawn for an already sunk/missing tile.");
                floodDiscardPile.push(tileName); 
            }
        }
        return affectedTilesForUI;
    }

    public void handleWatersRiseEffect() {
        System.out.println("WATERS RISE!");
        increaseWaterLevel();
        // The WatersRise card itself goes to the treasure discard pile, not back into the deck immediately.
        // It is handled by the calling method (in GameBoard) to place it in discard.
        System.out.println("DEBUG: Shuffling flood discard pile back into flood deck.");
        floodDeck.addAll(floodDiscardPile);
        floodDiscardPile.clear();
        Collections.shuffle(floodDeck);
        // TODO: Check for game over immediately after water level rise.
        // This check will now happen in GameBoard after this method call.
    }

    public void drawFlood(Graphics g) {
    }

    public boolean checkWinning() {
        return true;
    }

    public boolean checkLosing() {
        return false;
    }

    public ArrayList<GameTile> findMovable(Player player) {
        ArrayList<GameTile> movable = new ArrayList<>();
        if (player == null) return movable;

        int[] currentPos = player.getPos();
        int r = currentPos[0];
        int c = currentPos[1];
        String role = player.getRole();

        // Define potential moves (orthogonal and diagonal)
        int[][] moves = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1}, // Orthogonal
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonal
        };

        for (int i = 0; i < moves.length; i++) {
            int nr = r + moves[i][0];
            int nc = c + moves[i][1];

            boolean isDiagonal = i >= 4;

            // Boundary checks
            if (nr < 0 || nr >= BOARD_ROWS || nc < 0 || nc >= BOARD_COLS) {
                continue;
            }

            GameTile targetTile = boardTiles[nr][nc];
            if (targetTile != null && !targetTile.getFloodState()) { // Can only move to unflooded, existing tiles
                if (!isDiagonal) { // Orthogonal moves are always allowed for any player
                    movable.add(targetTile);
                } else if (role.equals("Explorer")) { // Diagonal moves only for Explorer
                    movable.add(targetTile);
                }
            }
        }
        
        // Pilot's special ability: fly to any non-flooded, non-sunk tile not currently on
        if (role.equals("Pilot") && player.canUseFlightAbility()) {
            for (int row = 0; row < BOARD_ROWS; row++) {
                for (int col = 0; col < BOARD_COLS; col++) {
                    if (boardTiles[row][col] != null && !boardTiles[row][col].getFloodState()) {
                        if (row != r || col != c) { // Not the current tile
                           if (!movable.contains(boardTiles[row][col])) { // Avoid duplicates
                                movable.add(boardTiles[row][col]);
                           }
                        }
                    }
                }
            }
        }

        // Diver's special ability: move through flooded/sunk tiles
        if (role.equals("Diver")) {
            // BFS to find reachable non-flooded tiles by passing through flooded/sunk tiles
            Queue<int[]> queue = new LinkedList<>();
            Set<String> visited = new HashSet<>(); // "r,c" to mark visited
            
            queue.add(currentPos);
            visited.add(r + "," + c);

            int[] dr = {-1, 1, 0, 0}; // Orthogonal directions
            int[] dc = {0, 0, -1, 1};

            while(!queue.isEmpty()){
                int[] curr = queue.poll();
                // Check neighbors
                for(int i=0; i<4; i++){
                    int nextR = curr[0] + dr[i];
                    int nextC = curr[1] + dc[i];
                    String nextPosStr = nextR + "," + nextC;

                    if(nextR >= 0 && nextR < BOARD_ROWS && nextC >=0 && nextC < BOARD_COLS && !visited.contains(nextPosStr)){
                        visited.add(nextPosStr);
                        GameTile nextTile = boardTiles[nextR][nextC];
                        
                        if(nextTile != null && !nextTile.getFloodState()){ // Reachable land tile
                            if (!movable.contains(nextTile)) { // Add if not already (e.g. by normal move)
                                movable.add(nextTile);
                            }
                            // Diver stops at the first land tile in a chain, so don't add this to queue
                        } else if (nextTile == null || nextTile.getFloodState()) { // Can pass through water/sunk
                            queue.add(new int[]{nextR, nextC});
                        }
                    }
                }
            }
        }
        return movable;
    }

    public Set<GameTile> findNavigatorCommandTargets(Player playerToMove) {
        Set<GameTile> targets = new HashSet<>();
        if (playerToMove == null) return targets;

        int[] currentPos = playerToMove.getPos();
        int r = currentPos[0];
        int c = currentPos[1];

        // Navigator can move another player 1 or 2 tiles orthogonally.
        // BFS approach for 1 or 2 steps
        Queue<int[]> queue = new LinkedList<>();
        Map<String, Integer> distance = new HashMap<>(); // "r,c" -> distance

        queue.add(currentPos);
        distance.put(r + "," + c, 0);

        int[] dr = {-1, 1, 0, 0}; // Orthogonal directions
        int[] dc = {0, 0, -1, 1};

        while(!queue.isEmpty()){
            int[] curr = queue.poll();
            int dist = distance.get(curr[0] + "," + curr[1]);

            if (dist >= 2) continue; // Max 2 steps

            for(int i=0; i<4; i++){
                int nr = curr[0] + dr[i];
                int nc = curr[1] + dc[i];
                String nextPosStr = nr + "," + nc;

                if(nr >= 0 && nr < BOARD_ROWS && nc >= 0 && nc < BOARD_COLS && !distance.containsKey(nextPosStr)){
                    GameTile targetTile = boardTiles[nr][nc];
                    if(targetTile != null && !targetTile.getFloodState()){
                        targets.add(targetTile);
                        distance.put(nextPosStr, dist + 1);
                        if (dist + 1 < 2) { // Only queue if we can take another step from here
                           queue.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        return targets;
    }

    public ArrayList<GameTile> findShoreUpTargets(Player player) {
        ArrayList<GameTile> targets = new ArrayList<>();
        if (player == null) return targets;
        int[] currentPos = player.getPos();
        int r = currentPos[0];
        int c = currentPos[1];

        // Check current tile
        if (boardTiles[r][c] != null && boardTiles[r][c].getFloodState()) {
            targets.add(boardTiles[r][c]);
        }

        // Check orthogonal neighbors
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < BOARD_ROWS && nc >= 0 && nc < BOARD_COLS &&
                boardTiles[nr][nc] != null && boardTiles[nr][nc].getFloodState()) {
                if (!targets.contains(boardTiles[nr][nc])) { // Avoid duplicates if current is also target
                    targets.add(boardTiles[nr][nc]);
                }
            }
        }
        return targets;
    }

    public void shoreUpTile(GameTile tile) {
        if (tile != null) {
            tile.setFlooded(false);
            // System.out.println(\"DEBUG: Tile \" + tile.getName() + \" shored up by GameState.\");
        }
    }

    public boolean isTreasureCollected(TreasureType type) {
        return treasuresCollectedMap.getOrDefault(type, false);
    }

    public boolean isFoolsLandingSunk() {
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                if (boardTiles[r][c] != null && boardTiles[r][c].isFoolsLanding()) {
                    return false; // Fools' Landing found and not sunk
                }
            }
        }
        return true; // Fools' Landing not found (i.e., it must have been sunk and set to null)
    }

    /**
     * Checks if both tiles for a given treasure type are sunk AND that treasure has not yet been collected.
     * This constitutes a game loss condition.
     * @param type The TreasureType to check.
     * @return true if the treasure is uncollected and both its tiles are sunk, false otherwise.
     */
    public boolean areTreasureTilesSunkAndUncollected(TreasureType type) {
        if (isTreasureCollected(type)) {
            return false; // Treasure already collected, so not a loss condition for this treasure
        }

        ArrayList<String> targetTileNames = treasureToTileNamesMap.get(type);
        if (targetTileNames == null || targetTileNames.size() < 2) {
            // This should not happen if TILE_TO_TREASURE_MAP and treasureToTileNamesMap are set up correctly
            System.err.println("Warning: Treasure type " + type + " does not have two associated tiles defined.");
            return false; 
        }

        int sunkCount = 0;
        for (String tileNameToFind : targetTileNames) {
            boolean foundAndNotSunk = false;
            for (int r = 0; r < BOARD_ROWS; r++) {
                for (int c = 0; c < BOARD_COLS; c++) {
                    if (boardTiles[r][c] != null && boardTiles[r][c].getOriginalName().equals(tileNameToFind)) {
                        foundAndNotSunk = true;
                break;
                    }
                }
                if (foundAndNotSunk) break;
            }
            if (!foundAndNotSunk) {
                sunkCount++;
            }
        }
        return sunkCount >= targetTileNames.size(); // True if all (typically 2) tiles for this treasure are sunk
    }

    /**
     * Checks all general loss conditions for the game.
     * @return true if any loss condition is met, false otherwise.
     */
    public boolean checkOverallLossCondition() {
        if (isGameOverByWaterLevel()) {
            System.out.println("GAME OVER: Water level reached maximum!");
            return true;
        }
        if (isFoolsLandingSunk()) {
            System.out.println("GAME OVER: Fools' Landing has sunk!");
            return true;
        }
        for (TreasureType type : TreasureType.values()) {
            if (areTreasureTilesSunkAndUncollected(type)) {
                System.out.println("GAME OVER: Treasure " + type.getDisplayName() + " is lost (tiles sunk before collection)!");
                return true;
            }
        }
        // TODO: Add check for all players being unable to move or act (more complex)
        // TODO: Add check if all players are on sunk tiles (part of pawn on sunk tile logic)
        return false;
    }

    // Placeholder for win condition check (to be implemented next)
    public boolean checkWinCondition() {
        // Condition 1: All four treasures must be collected.
        boolean allTreasuresCollected = true;
        for (Boolean collected : treasuresCollectedMap.values()) {
            if (!collected) {
                allTreasuresCollected = false;
                break;
            }
        }
        if (!allTreasuresCollected) {
            // System.out.println("Win check: Not all treasures collected.");
            return false;
        }

        // Condition 2: All players must be on the "FoolsLanding" tile.
        GameTile foolsLandingTile = null;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                if (boardTiles[r][c] != null && boardTiles[r][c].isFoolsLanding()) {
                    foolsLandingTile = boardTiles[r][c];
                    break;
                }
            }
            if (foolsLandingTile != null) break;
        }

        if (foolsLandingTile == null || foolsLandingTile.getFloodState()) { // Fools' Landing must exist and not be sunk
            // System.out.println("Win check: Fools' Landing is sunk or not found.");
            return false; 
        }

        for (Player player : allPlayers) {
            if (player.getPos() == null || player.getPos()[0] != foolsLandingTile.getPosition()[0] || player.getPos()[1] != foolsLandingTile.getPosition()[1]) {
                // System.out.println("Win check: Not all players on Fools' Landing. Player " + player.getRole() + " is at " + Arrays.toString(player.getPos()));
                return false; // All players must be on Fools' Landing.
            }
        }

        // Condition 3: At least one player must have a Helicopter Lift card in hand.
        boolean helicopterPresent = false;
        for (Player player : allPlayers) {
            if (player.getHand().contains("HelicopterLift")) {
                helicopterPresent = true;
                break;
            }
        }
        if (!helicopterPresent) {
            // System.out.println("Win check: No Helicopter Lift card among players.");
            return false;
        }

        System.out.println("WIN CONDITION MET!");
        return true; // All conditions met.
    }
    
    public boolean canPlayerCollectTreasure(Player player, GameTile tile) {
        if (player == null || tile == null) {
            return false;
        }
        // Condition 1: Tile must not be flooded
        if (tile.getFloodState()) {
            // System.out.println("canPlayerCollectTreasure: Tile " + tile.getName() + " is flooded.");
            return false;
        }
        // Condition 2: Tile must be a treasure tile
        TreasureType treasureOnTile = tile.getTreasureType();
        if (treasureOnTile == null) {
            // System.out.println("canPlayerCollectTreasure: Tile " + tile.getName() + " is not a treasure tile.");
            return false;
        }
        // Condition 3: This specific treasure must not have been collected yet
        if (treasuresCollectedMap.get(treasureOnTile)) {
            // System.out.println("canPlayerCollectTreasure: Treasure " + treasureOnTile + " already collected.");
            return false;
        }
        // Condition 4: Player must have 4 cards of that treasure type
        if (player.countTreasureCards(treasureOnTile) < 4) {
            // System.out.println("canPlayerCollectTreasure: Player " + player.getRole() + " has " + player.countTreasureCards(treasureOnTile) + " cards for " + treasureOnTile + ", needs 4.");
            return false;
        }
        return true;
    }

    public void collectTreasure(Player player, TreasureType treasureType) {
        if (player == null || treasureType == null) return;

        if (treasuresCollectedMap.get(treasureType)) {
            System.out.println("Treasure " + treasureType.getDisplayName() + " has already been collected.");
            return; // Already collected
        }

        // Player must discard 4 cards of this treasure type
        int cardsToDiscard = 4;
        ArrayList<String> playerCards = player.getHand();
        ArrayList<String> cardsToRemove = new ArrayList<>();
        for (String cardName : playerCards) {
            if (cardName.equals(treasureType.name())) { // Assuming treasure cards are stored by TreasureType enum name
                cardsToRemove.add(cardName);
                if (cardsToRemove.size() == cardsToDiscard) break;
            }
        }

        if (cardsToRemove.size() == cardsToDiscard) {
            for (String cardToRemove : cardsToRemove) {
                player.discardSpecificCardByName(cardToRemove); // This should also add to discardPile if implemented in Player or called here
                discardPile.push(cardToRemove); // Ensure cards go to the main treasure discard pile
            }
            treasuresCollectedMap.put(treasureType, true);
            System.out.println(player.getRole() + " collected " + treasureType.getDisplayName() + "!");

            // TEMPORARY MODIFICATION FOR EASIER WIN TESTING - REMOVING THIS
            /*
            System.out.println("TEMPORARY: Marking ALL treasures as collected for win testing.");
            for (TreasureType type : TreasureType.values()) {
                treasuresCollectedMap.put(type, true);
            }
            */

        } else {
            System.out.println(player.getRole() + " does not have enough " + treasureType.getDisplayName() + " cards to collect it.");
        }
    }

    public String drawTreasureCard() {
        if (cardDeck == null) { // Should not happen if initialized
            cardDeck = new Stack<>();
            // Potentially re-populate and shuffle if truly an error state, 
            // but for now, assume it's initialized.
            System.err.println("CRITICAL: Treasure card deck (cardDeck) was null in drawTreasureCard.");
            return null; // Or throw exception
        }
        if (discardPile == null) { // Should not happen
            discardPile = new Stack<>();
            System.err.println("CRITICAL: Treasure discard pile (discardPile) was null in drawTreasureCard.");
        }

        if (cardDeck.isEmpty()) {
            if (discardPile.isEmpty()) {
                System.out.println("Treasure deck and discard pile are empty. No cards to draw.");
                return null; // No cards left anywhere
            }
            System.out.println("Treasure deck empty! Reshuffling treasure discard pile into treasure deck.");
            cardDeck.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(cardDeck);
        }
        if (cardDeck.isEmpty()) { // Still empty means something is wrong or game is truly out of cards
             System.out.println("Treasure deck is definitively empty even after trying to reshuffle. Cannot draw treasure card.");
             return null;
        }
        return cardDeck.pop();
    }

    public void discardTreasureCardToPile(String cardName) {
        if (discardPile == null) {
            discardPile = new Stack<>(); // Defensive initialization
        }
        if (cardName != null) {
            discardPile.push(cardName);
        }
    }

    // Getter for the set of actually collected treasures
    public Set<TreasureType> getCollectedTreasures() {
        Set<TreasureType> collected = new HashSet<>();
        if (this.treasuresCollectedMap != null) {
            for (Map.Entry<TreasureType, Boolean> entry : this.treasuresCollectedMap.entrySet()) {
                if (entry.getValue()) { // If the boolean is true, the treasure is collected
                    collected.add(entry.getKey());
                }
            }
        }
        return collected;
    }
}
