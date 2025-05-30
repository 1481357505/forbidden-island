package game.panel;
import game.simulation.tiles.GameTile;
import game.simulation.GameState;
import game.simulation.player.Player;
import game.simulation.board.TreasureType;
import game.simulation.board.WaterLevelMeter; // Import WaterLevelMeter

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.EnumMap;

public class GameBoard extends JPanel implements MouseListener {
    private GameState gameState;
    private ParentPanel parentPanel;
    private JPanel[][] tilePanelHolder;
    private final Border defaultTileBorder = new LineBorder(Color.DARK_GRAY);
    private final Border highlightedTileBorder = new LineBorder(Color.GREEN, 3); // Thicker green border for highlight
    private final Border floodedTileBorder = new LineBorder(Color.BLUE, 2); // For flooded tiles

    // Navigator specific state
    private enum NavigatorActionState { NONE, SELECTING_PLAYER_TO_MOVE, SELECTING_TILE_FOR_OTHER }
    private NavigatorActionState navigatorActionState = NavigatorActionState.NONE;
    private Player playerBeingMovedByNavigator = null;
    private Set<GameTile> validNavigatorCommandTargetTiles = new HashSet<>(); // To store valid targets for Navigator's command
    // private JButton commandOtherPlayerButton; // Button will be added later if we choose a UI button approach

    // Shore Up specific state
    private boolean isInShoreUpMode = false;
    private JButton shoreUpButton;
    private final Border shoreUpHighlightBorder = new LineBorder(Color.ORANGE, 3); // For shoring up targets
    private int engineerShoredUpCountThisAction = 0; // For Engineer's ability

    private final Border sandbagHighlightBorder = new LineBorder(Color.CYAN, 3); // For Sandbag targets
    private boolean isUsingSandbagMode = false;
    private JButton useSandbagButton;

    // Helicopter Lift state variables
    private enum HelicopterPhase { NONE, SELECTING_PLAYERS, SELECTING_TARGET_TILE }
    private HelicopterPhase helicopterPhase = HelicopterPhase.NONE;
    private ArrayList<Player> playersToMoveWithHelicopter = new ArrayList<>();
    private final Border helicopterTargetTileHighlight = new LineBorder(Color.MAGENTA, 3);
    private final Border helicopterPlayerSelectHighlight = new LineBorder(Color.PINK, 3); // Distinct color for player selection
    private JButton useHelicopterButton;

    private JButton collectTreasureButton; // Added button
    private JButton giveCardButton; // Added button for giving cards

    // Panel and Labels for player information
    private JPanel playerInfoPanel;
    private JLabel currentPlayerLabel;
    private EnumMap<TreasureType, JLabel> treasureCardCountLabels;
    private JLabel helicopterCardLabel; // Added for Helicopter cards
    private JLabel sandbagCardLabel;    // Added for Sandbag cards
    private JButton endTurnButton; // Button to end turn manually
    // private JLabel waterLevelLabel;     // Removed, will use graphical display

    // UI for displaying collected treasures
    private JPanel collectedTreasuresPanel;
    private EnumMap<TreasureType, JLabel> collectedTreasureIconLabels;
    
    private Font playerInfoFont; // Define a common font for the info panel
    private WaterLevelDisplayPanel waterLevelDisplayPanel; // Graphical water level display

    // UI for displaying all players' card counts
    private JPanel allPlayersCardCountsPanel;
    private Map<String, JLabel> playerRoleLabelsForAllCardsPanel; // Key: Player Role
    private Map<String, EnumMap<TreasureType, JLabel>> allPlayersTreasureCardLabelsForAllCardsPanel; // Key: Player Role
    private Map<String, JLabel> allPlayersSandbagLabelsForAllCardsPanel; // Key: Player Role
    private Map<String, JLabel> allPlayersHelicopterLabelsForAllCardsPanel; // Key: Player Role

    public GameBoard(GameState gameState, ParentPanel parentPanel) throws IOException {
        // super("Forbidden Island"); // Removed, JPanel doesn't take title
        this.gameState = gameState;
        this.parentPanel = parentPanel;
        this.tilePanelHolder = new JPanel[GameState.BOARD_ROWS][GameState.BOARD_COLS];

        setOpaque(false); // Make GameBoard transparent

        // Initialize currentPlayerLabel FIRST
        currentPlayerLabel = new JLabel("Current Player: "); 

        // Define a slightly larger font for the player info panel
        // Now it's safe to get font from currentPlayerLabel as it's initialized
        playerInfoFont = currentPlayerLabel.getFont().deriveFont(currentPlayerLabel.getFont().getSize() + 2f); // Increase by 2 points
        
        // GameBoard itself is now a JPanel, so we set its layout directly
        setLayout(new BorderLayout()); 

        // Initialize Player Info Panel
        playerInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        playerInfoPanel.setOpaque(false); // Make playerInfoPanel transparent
        currentPlayerLabel.setFont(playerInfoFont);
        playerInfoPanel.add(currentPlayerLabel);

        treasureCardCountLabels = new EnumMap<>(TreasureType.class);
        for (TreasureType type : TreasureType.values()) {
            String imageFileName = "";
            switch (type) {
                case STATUE_OF_THE_WIND: imageFileName = "Captured_Treasures_Statue_of_the_Wind@2x.png"; break;
                case OCEANS_CHALICE: imageFileName = "Captured_Treasures_Oceans_Chalice@2x.png"; break;
                case CRYSTAL_OF_FIRE: imageFileName = "Captured_Treasures_Crystal_of_Fire@2x.png"; break;
                case EARTH_STONE: imageFileName = "Captured_Treasures_Earth_Stone@2x.png"; break;
            }

            if (!imageFileName.isEmpty()) {
                String imagePath = "Images/Treasures/" + imageFileName;
                try {
                    BufferedImage treasureImage = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
                    Image scaledTreasureImage = treasureImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    JLabel iconLabel = new JLabel(new ImageIcon(scaledTreasureImage));
                    playerInfoPanel.add(iconLabel); // Add icon directly to playerInfoPanel
                } catch (IOException | NullPointerException ex) {
                    System.err.println("Failed to load treasure icon: " + imagePath + " for " + type.getDisplayName() + ". Error: " + ex.getMessage());
                    JLabel textIconFallback = new JLabel("[" + type.getDisplayName().charAt(0) + "]"); 
                    textIconFallback.setFont(playerInfoFont);
                    playerInfoPanel.add(textIconFallback); // Add fallback icon directly
                }
            } else {
                JLabel textIconFallback = new JLabel("[" + type.getDisplayName().charAt(0) + "]");
                textIconFallback.setFont(playerInfoFont);
                playerInfoPanel.add(textIconFallback); // Add fallback icon directly
            }

            JLabel countLabel = new JLabel(" 0");
            countLabel.setFont(playerInfoFont);
            treasureCardCountLabels.put(type, countLabel);
            playerInfoPanel.add(countLabel); // Add count label directly to playerInfoPanel

            playerInfoPanel.add(new JSeparator(SwingConstants.VERTICAL));
        }

        // Add labels for special cards and water level
        helicopterCardLabel = new JLabel("Helicopter: 0");
        helicopterCardLabel.setFont(playerInfoFont);
        playerInfoPanel.add(helicopterCardLabel);
        playerInfoPanel.add(new JSeparator(SwingConstants.VERTICAL));

        sandbagCardLabel = new JLabel("Sandbags: 0");
        sandbagCardLabel.setFont(playerInfoFont);
        playerInfoPanel.add(sandbagCardLabel);
        // playerInfoPanel.add(new JSeparator(SwingConstants.VERTICAL)); // Remove separator before water level if it was last
        
        // waterLevelLabel = new JLabel("Water Level: 0"); // Removed
        // waterLevelLabel.setFont(playerInfoFont); // Removed
        // playerInfoPanel.add(waterLevelLabel); // Removed

        add(playerInfoPanel, BorderLayout.NORTH);

        // Initialize Collected Treasures Panel
        collectedTreasuresPanel = new JPanel(new GridLayout(0, 1, 5, 10)); // 0 rows, 1 col, with vertical spacing
        collectedTreasuresPanel.setBorder(BorderFactory.createTitledBorder("Collected Treasures"));
        collectedTreasuresPanel.setOpaque(false); // Make collectedTreasuresPanel transparent
        collectedTreasureIconLabels = new EnumMap<>(TreasureType.class);

        for (TreasureType type : TreasureType.values()) {
            JLabel treasureStatusLabel = new JLabel("[" + type.getDisplayName() + ": Not collected]");
            treasureStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            // If treasureStatusLabel itself needs transparency, add: treasureStatusLabel.setOpaque(false);
            treasureStatusLabel.setForeground(Color.WHITE); // Ensure text is white
            collectedTreasureIconLabels.put(type, treasureStatusLabel);
            collectedTreasuresPanel.add(treasureStatusLabel);
        }
        // add(collectedTreasuresPanel, BorderLayout.EAST); // Will be added to eastRegionPanel instead

        // Initialize All Players Card Counts Panel
        allPlayersCardCountsPanel = new JPanel();
        allPlayersCardCountsPanel.setLayout(new BoxLayout(allPlayersCardCountsPanel, BoxLayout.Y_AXIS));
        allPlayersCardCountsPanel.setBorder(BorderFactory.createTitledBorder("All Player Hands"));
        allPlayersCardCountsPanel.setOpaque(false);
        
        playerRoleLabelsForAllCardsPanel = new HashMap<>();
        allPlayersTreasureCardLabelsForAllCardsPanel = new HashMap<>();
        allPlayersSandbagLabelsForAllCardsPanel = new HashMap<>();
        allPlayersHelicopterLabelsForAllCardsPanel = new HashMap<>();

        if (gameState.getAllPlayers() != null) {
            for (Player player : gameState.getAllPlayers()) {
                String playerRole = player.getRole();

                JPanel playerSpecificCardPanel = new JPanel();
                playerSpecificCardPanel.setLayout(new BoxLayout(playerSpecificCardPanel, BoxLayout.Y_AXIS));
                playerSpecificCardPanel.setOpaque(false);
                playerSpecificCardPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                JLabel roleLabel = new JLabel(playerRole + ":");
                roleLabel.setForeground(Color.WHITE);
                roleLabel.setFont(playerInfoFont.deriveFont(Font.BOLD));
                playerSpecificCardPanel.add(roleLabel);
                playerRoleLabelsForAllCardsPanel.put(playerRole, roleLabel); // Though role itself won't change

                EnumMap<TreasureType, JLabel> treasureLabels = new EnumMap<>(TreasureType.class);
                for (TreasureType type : TreasureType.values()) {
                    JLabel label = new JLabel(type.getDisplayName() + ": 0");
                    label.setForeground(Color.WHITE);
                    label.setFont(playerInfoFont);
                    playerSpecificCardPanel.add(label);
                    treasureLabels.put(type, label);
                }
                allPlayersTreasureCardLabelsForAllCardsPanel.put(playerRole, treasureLabels);

                JLabel sandbagLabel = new JLabel("Sandbags: 0");
                sandbagLabel.setForeground(Color.WHITE);
                sandbagLabel.setFont(playerInfoFont);
                playerSpecificCardPanel.add(sandbagLabel);
                allPlayersSandbagLabelsForAllCardsPanel.put(playerRole, sandbagLabel);

                JLabel helicopterLabel = new JLabel("Helicopter Lifts: 0");
                helicopterLabel.setForeground(Color.WHITE);
                helicopterLabel.setFont(playerInfoFont);
                playerSpecificCardPanel.add(helicopterLabel);
                allPlayersHelicopterLabelsForAllCardsPanel.put(playerRole, helicopterLabel);
                
                allPlayersCardCountsPanel.add(playerSpecificCardPanel);
                if (gameState.getAllPlayers().indexOf(player) < gameState.getAllPlayers().size() -1) {
                     allPlayersCardCountsPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
                }
            }
        }


        // Create a wrapper panel for the EAST region
        JPanel eastRegionPanel = new JPanel();
        eastRegionPanel.setLayout(new BoxLayout(eastRegionPanel, BoxLayout.Y_AXIS));
        eastRegionPanel.setOpaque(false);
        eastRegionPanel.add(collectedTreasuresPanel);
        eastRegionPanel.add(Box.createVerticalStrut(10)); // Add some spacing
        eastRegionPanel.add(allPlayersCardCountsPanel);
        
        add(eastRegionPanel, BorderLayout.EAST);

        // Initialize Water Level Display Panel
        if (gameState.getWaterLevelMeter() != null) {
            waterLevelDisplayPanel = new WaterLevelDisplayPanel(gameState.getWaterLevelMeter());
            waterLevelDisplayPanel.setOpaque(false); // Make waterLevelDisplayPanel transparent
            add(waterLevelDisplayPanel, BorderLayout.WEST);
        } else {
            System.err.println("GameBoard Constructor: gameState.getWaterLevelMeter() is null. Cannot create WaterLevelDisplayPanel.");
            // Optionally add a placeholder or leave the WEST empty
        }

        // Panel for the game board tiles (grid)
        JPanel gameGridPanel = new JPanel(new GridLayout(GameState.BOARD_ROWS, GameState.BOARD_COLS, 2, 2));
        gameGridPanel.setOpaque(false); // Make gameGridPanel transparent

        GameTile[][] boardTiles = gameState.getBoardTiles();
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                GameTile currentTile = boardTiles[r][c];
                if (currentTile != null) {
                    JPanel tilePanel = new JPanel(new BorderLayout(5,5));
                    tilePanel.setOpaque(false); // Make tilePanel transparent
                    tilePanel.setBorder(defaultTileBorder); 
                    tilePanel.putClientProperty("gameTile", currentTile);
                    tilePanel.addMouseListener(this);

                    JLabel nameLabel = new JLabel(currentTile.getName(), SwingConstants.CENTER);
                    nameLabel.setOpaque(false); // Make nameLabel transparent
                    // nameLabel.setBackground(Color.LIGHT_GRAY); // Remove background color
                    tilePanel.add(nameLabel, BorderLayout.NORTH);

                    if (currentTile.getImage() != null) {
                        Image scaledImg = currentTile.getImage().getScaledInstance(100, 80, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(scaledImg));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        // Make imageLabel transparent if it has a default opaque background
                        imageLabel.setOpaque(false); 
                        tilePanel.add(imageLabel, BorderLayout.CENTER);
                    } else {
                        JLabel noImageLabel = new JLabel("No Image", SwingConstants.CENTER);
                        noImageLabel.setOpaque(false); // Make noImageLabel transparent
                        tilePanel.add(noImageLabel, BorderLayout.CENTER);
                    }
                    // Visual update for flooded state handled in refreshTileAppearance
                    gameGridPanel.add(tilePanel);
                    tilePanelHolder[r][c] = tilePanel;
                } else {
                    JPanel emptyPanel = new JPanel();
                    emptyPanel.setOpaque(false); // Make emptyPanel transparent
                    gameGridPanel.add(emptyPanel);
                    tilePanelHolder[r][c] = emptyPanel; 
                }
            }
        }
        add(gameGridPanel, BorderLayout.CENTER); // Add gameGridPanel to this GameBoard JPanel

        // Control panel for buttons
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false); // Make controlPanel transparent
        shoreUpButton = new JButton("Shore Up (1 AP)");
        shoreUpButton.addActionListener(e -> enterShoreUpMode());
        controlPanel.add(shoreUpButton);

        collectTreasureButton = new JButton("Collect Treasure (1 AP)"); // Initialize button
        collectTreasureButton.addActionListener(e -> collectTreasureAction());
        controlPanel.add(collectTreasureButton); // Add to control panel

        giveCardButton = new JButton("Give Card (1 AP)"); // Initialize Give Card button
        giveCardButton.addActionListener(e -> giveCardActionSetup());
        controlPanel.add(giveCardButton); // Add to control panel

        useSandbagButton = new JButton("Use Sandbag (0 AP)");
        useSandbagButton.addActionListener(e -> useSandbagAction());
        controlPanel.add(useSandbagButton);

        useHelicopterButton = new JButton("Use Helicopter (1 AP)");
        useHelicopterButton.addActionListener(e -> useHelicopterAction());
        controlPanel.add(useHelicopterButton);

        // TODO: Add Navigator command button here later if desired
        endTurnButton = new JButton("End Turn");
        endTurnButton.addActionListener(e -> {
            Player currentPlayer = gameState.getCurrentPlayer();
            if (currentPlayer != null) {
                System.out.println(currentPlayer.getRole() + " chose to end turn manually.");
                handleTurnEnd();
            }
        });
        controlPanel.add(endTurnButton);

        add(controlPanel, BorderLayout.SOUTH); // Add controlPanel to this GameBoard JPanel

        refreshAllTileAppearances(); // Initial appearance refresh
        refreshPawnDisplay();
        updatePlayerInfoDisplay(); // Initial player info display
        updateCollectedTreasuresDisplay(); // Initial display of collected treasures
        updateWaterLevelMeterDisplay(); // Initial display of water level meter
        updateAllPlayersCardCountsDisplay(); // Initial display for the new panel
        
        if (gameState.getCurrentPlayer() != null) { // Ensure first player is set before highlighting
            highlightMovableTiles(); 
            updateActionButtonsState(); // Update button visibility/enabled state
        } else {
            System.err.println("GameBoard Constructor: currentPlayer is null after GameState initialization. Ensure gameState.nextTurn() is called before creating GameBoard or at the start of GameState constructor.");
        }

        // Estimate control panel height (e.g., a button's preferred height + some padding)
        int controlPanelHeight = shoreUpButton.getPreferredSize().height + 10; // Add some padding
        int playerInfoPanelHeight = playerInfoPanel.getPreferredSize().height + 5;
        int sidePanelWidth = 0;
        if (waterLevelDisplayPanel != null) sidePanelWidth += waterLevelDisplayPanel.getPreferredSize().width;
        if (collectedTreasuresPanel != null) sidePanelWidth += collectedTreasuresPanel.getPreferredSize().width;

        setPreferredSize(new Dimension(GameState.BOARD_COLS * 130 + sidePanelWidth, GameState.BOARD_ROWS * 165 + controlPanelHeight + playerInfoPanelHeight));
    }

    private void clearAllHighlights() {
        // GameTile[][] boardTiles = gameState.getBoardTiles(); // Not needed if using getClientProperty
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (tilePanelHolder[r][c] != null) {
                    GameTile tile = (GameTile) tilePanelHolder[r][c].getClientProperty("gameTile");
                    if (tile != null) { // It's an active, non-sunk tile
                        refreshTileAppearance(tile, tilePanelHolder[r][c]);
                    }
                    // Sunk tiles (gameTile property is null) and empty layout panels are not affected by this general clear,
                    // as they have their own specific appearance or no border to clear.
                }
            }
        }
    }

    private void highlightMovableTiles() {
        clearAllHighlights();

        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) return;

        ArrayList<GameTile> movableTiles = gameState.findMovable(currentPlayer);
        if (movableTiles == null) return;

        for (GameTile tile : movableTiles) {
            if (tile != null && tile.getPosition() != null) {
                int r = tile.getPosition()[0];
                int c = tile.getPosition()[1];
                if (r >= 0 && r < GameState.BOARD_ROWS && c >= 0 && c < GameState.BOARD_COLS) {
                    if (tilePanelHolder[r][c] != null) {
                        tilePanelHolder[r][c].setBorder(highlightedTileBorder);
                    }
                }
            }
        }
    }

    private BufferedImage loadPawnImage(String role) throws IOException {
        String imageName = role + "_Adventurer_Icon@2x.png";
        String path = "Images/Pawns/" + imageName;
        try {
            return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)));
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading pawn image for role '" + role + "' from path: " + path + ". Pawn will not be displayed.");
            return null;
        }
    }

    private void refreshAllTileAppearances() {
        GameTile[][] boardTiles = gameState.getBoardTiles();
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (boardTiles[r][c] != null && tilePanelHolder[r][c] != null) {
                    refreshTileAppearance(boardTiles[r][c], tilePanelHolder[r][c]);
                }
            }
        }
    }

    private void refreshTileAppearance(GameTile tile, JPanel panel) {
        if (tile == null || panel == null) return;
        
        JLabel nameLabel = null;
        // Find the nameLabel (assuming it's the NORTH component)
        if (panel.getLayout() instanceof BorderLayout) {
            Component northComp = ((BorderLayout)panel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (northComp instanceof JLabel) {
                nameLabel = (JLabel) northComp;
            }
        }

        if (tile.getFloodState()) {
            panel.setBorder(floodedTileBorder);
            panel.setBackground(new Color(0, 0, 150, 80)); // Semi-transparent darker blue
            if (nameLabel != null) {
                nameLabel.setForeground(Color.WHITE);
                nameLabel.setBackground(new Color(0,0,0,0)); // Fully transparent background for label
            }
        } else {
            panel.setBorder(defaultTileBorder);
            panel.setBackground(new Color(220, 220, 220, 70)); // Semi-transparent light gray
            if (nameLabel != null) {
                nameLabel.setForeground(Color.BLACK);
                nameLabel.setBackground(new Color(0,0,0,0)); // Fully transparent background for label
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    private void enterShoreUpMode() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (isInShoreUpMode) {
            if (currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1) {
                // Engineer has shored up one tile and clicks "Finish Shoring Up" button
                System.out.println("Engineer finishes shoring up after 1 tile.");
                currentPlayer.useActionPoint(); // Consume 1 AP for the completed action of shoring 1 tile
                System.out.println(currentPlayer.getRole() + " used 1 AP and shored up " + engineerShoredUpCountThisAction + " tile(s).");
                engineerShoredUpCountThisAction = 0;
                isInShoreUpMode = false;
                clearAllHighlights();
                updatePlayerInfoDisplay(); // Update AP display
                if (currentPlayer.hasActionPoints()) {
                    highlightMovableTiles();
                } else {
                    System.out.println(currentPlayer.getRole() + "'s turn ends after shoring up.");
                    handleTurnEnd();
                }
            } else {
                // General cancel for any player, or Engineer cancelling before first tile
                System.out.println("Shore Up mode deactivated/cancelled.");
                engineerShoredUpCountThisAction = 0; 
                isInShoreUpMode = false;
                clearAllHighlights();
                highlightMovableTiles(); 
            }
        } else {
            // Entering Shore Up mode
            if (currentPlayer.hasActionPoints()) {
                isUsingSandbagMode = false; // Ensure other modes are off
                navigatorActionState = NavigatorActionState.NONE;
                isInShoreUpMode = true;
                engineerShoredUpCountThisAction = 0; // Reset for new shore up sequence
                System.out.println(currentPlayer.getRole() + " entering Shore Up mode.");
                highlightShoreUpTargets(currentPlayer);
            } else {
                System.out.println("Cannot enter Shore Up mode: No action points.");
            }
        }
        updateActionButtonsState(); // This will also update shoreUpButton text
    }

    private Set<GameTile> getShoreUpTargetTiles(Player player) {
        Set<GameTile> targets = new HashSet<>();
        if (player == null || player.getPos() == null) return targets;
        int[] playerPos = player.getPos();
        GameTile playerCurrentTile = gameState.getBoardTiles()[playerPos[0]][playerPos[1]];

        // Player's own tile
        if (playerCurrentTile != null && playerCurrentTile.getFloodState()) {
            targets.add(playerCurrentTile);
        }

        // Adjacent tiles
        int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] dir : DIRS) {
            int newX = playerPos[0] + dir[0];
            int newY = playerPos[1] + dir[1];
            if (newX >= 0 && newX < GameState.BOARD_ROWS && newY >= 0 && newY < GameState.BOARD_COLS) {
                GameTile adjacentTile = gameState.getBoardTiles()[newX][newY];
                if (adjacentTile != null && adjacentTile.getFloodState()) {
                    targets.add(adjacentTile);
                }
            }
        }
        return targets;
    }

    private void highlightShoreUpTargets(Player player) {
        clearAllHighlights();
        Set<GameTile> shoreUpTargets = getShoreUpTargetTiles(player);
        
        boolean isEngineerAndShoredOne = player.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1;

        for (GameTile target : shoreUpTargets) {
            // If Engineer has shored one, and this target is that one, don't highlight it again as a primary target for the *second* shore up.
            // This requires knowing which tile was the first. For now, assume getShoreUpTargetTiles() is fresh.
            tilePanelHolder[target.getPosition()[0]][target.getPosition()[1]].setBorder(shoreUpHighlightBorder);
        }

        if (shoreUpTargets.isEmpty()) {
            if (isEngineerAndShoredOne) {
                System.out.println("Engineer has shored up 1 tile. No more valid targets nearby. Click 'Finish Shoring Up' button.");
                // Button text update will be handled by updateActionButtonsState which is called next
            } else { // No targets from the start (or not engineer who shored one)
                System.out.println("No valid tiles to Shore Up nearby.");
                isInShoreUpMode = false; 
                engineerShoredUpCountThisAction = 0;
                highlightMovableTiles(); 
            }
        } 
        updateActionButtonsState(); // This will update the shoreUpButton text based on new state
    }

    private void updateCollectTreasureButtonState() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null || collectTreasureButton == null) {
            if (collectTreasureButton != null) collectTreasureButton.setEnabled(false);
            return;
        }

        int[] playerPos = currentPlayer.getPos();
        GameTile currentTile = gameState.getBoardTiles()[playerPos[0]][playerPos[1]];

        boolean canCollect = gameState.canPlayerCollectTreasure(currentPlayer, currentTile) && currentPlayer.hasActionPoints();
        collectTreasureButton.setEnabled(canCollect);
    }

    private void updateGiveCardButtonState() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (giveCardButton == null || currentPlayer == null) {
            if (giveCardButton != null) giveCardButton.setEnabled(false);
            return;
        }

        if (!currentPlayer.hasActionPoints()) {
            giveCardButton.setEnabled(false);
            return;
        }

        // Check if there are other players on the same tile
        boolean otherPlayerPresent = false;
        int[] playerPos = currentPlayer.getPos();
        for (Player p : gameState.getAllPlayers()) {
            if (p != currentPlayer && Arrays.equals(p.getPos(), playerPos)) {
                otherPlayerPresent = true;
                break;
            }
        }
        if (!otherPlayerPresent) {
            giveCardButton.setEnabled(false);
            return;
        }

        // Check if the player has any giveable treasure cards
        giveCardButton.setEnabled(!currentPlayer.getGiveableTreasureCards().isEmpty());
    }

    private void updateUseSandbagButtonState() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (useSandbagButton == null || currentPlayer == null) {
            if (useSandbagButton != null) useSandbagButton.setEnabled(false);
            return;
        }
        boolean hasSandbag = false;
        for (String card : currentPlayer.getHand()) {
            if ("Sandbag".equals(card)) {
                hasSandbag = true;
                break;
            }
        }
        useSandbagButton.setEnabled(hasSandbag);
        useSandbagButton.setText(isUsingSandbagMode ? "Cancel Sandbag" : "Use Sandbag (0 AP)");
    }

    private void updateUseHelicopterButtonState() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (useHelicopterButton == null) return;

        boolean hasHelicopterAndAP = false;
        if (currentPlayer != null && currentPlayer.getHand().contains("HelicopterLift") && currentPlayer.hasActionPoints()) {
            hasHelicopterAndAP = true;
        }

        switch (helicopterPhase) {
            case SELECTING_PLAYERS:
                useHelicopterButton.setText("Confirm Players (" + playersToMoveWithHelicopter.size() + ")");
                useHelicopterButton.setEnabled(true); // Always enabled to confirm or to cancel via button logic
                break;
            case SELECTING_TARGET_TILE:
                useHelicopterButton.setText("Cancel Flight");
                useHelicopterButton.setEnabled(true); // Always enabled to cancel
                break;
            case NONE:
            default:
                useHelicopterButton.setText("Use Helicopter (1 AP)");
                useHelicopterButton.setEnabled(hasHelicopterAndAP);
                break;
        }
    }
    
    // Helper pseudo-method, actual logic will be more complex for phase detection
    // private boolean areAllTargetsHighlightedForHelicopter() { // No longer needed
    //     return false; 
    // }

    private void useHelicopterAction() { // Button ActionListener for Helicopter
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) return;

        switch (helicopterPhase) {
            case NONE: // === ENTERING HELICOPTER MODE (SELECTING_PLAYERS phase) ===
                if (!currentPlayer.getHand().contains("HelicopterLift") || !currentPlayer.hasActionPoints()) {
                    JOptionPane.showMessageDialog(this, "Cannot use Helicopter: No Helicopter card or no AP.", "Action Denied", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                helicopterPhase = HelicopterPhase.SELECTING_PLAYERS;
                playersToMoveWithHelicopter.clear();
                playersToMoveWithHelicopter.add(currentPlayer); // Current player is always part of the group
                // Reset other modes
                isInShoreUpMode = false; isUsingSandbagMode = false; navigatorActionState = NavigatorActionState.NONE;
                System.out.println(currentPlayer.getRole() + " initiated Helicopter Lift. Phase: SELECTING_PLAYERS.");
                JOptionPane.showMessageDialog(this,
                    "HELICOPTER - Step 1: Select Players & Destination (Optional)" +
                    "\n1. Click other players on your current tile (pink border) to add/remove them." +
                    "\n2. Selected players will have a green border." +
                    "\n3. Click the button (now \"Confirm Players (#)\") to finalize player selection and proceed to select only the destination." +
                    "\n4. OR, you can directly click a valid destination tile (purple border) at any time to fly immediately with currently selected players.",
                    "Helicopter Lift", JOptionPane.INFORMATION_MESSAGE);
                refreshHelicopterHighlightsAndDestinations();
                break;

            case SELECTING_PLAYERS: // === User clicked "Confirm Players (#)" button ===
                helicopterPhase = HelicopterPhase.SELECTING_TARGET_TILE;
                System.out.println("Players confirmed for Helicopter: " + playersToMoveWithHelicopter.size() + ". Phase: SELECTING_TARGET_TILE.");
                JOptionPane.showMessageDialog(this,
                    "HELICOPTER - Step 2: Select Destination" +
                    "\nPlayers to move: " + playersToMoveWithHelicopter.size() + "." +
                    "\nClick a purple highlighted tile to fly there. Click \"Cancel Flight\" to abort.",
                    "Helicopter Lift", JOptionPane.INFORMATION_MESSAGE);
                refreshHelicopterHighlightsAndDestinations(); // Now only destinations will effectively be interactive targets
                break;

            case SELECTING_TARGET_TILE: // === User clicked "Cancel Flight" button ===
                System.out.println("Helicopter Lift cancelled by button.");
                helicopterPhase = HelicopterPhase.NONE;
                playersToMoveWithHelicopter.clear();
                clearAllHighlights();
                highlightMovableTiles();
                break;
        }
        updateActionButtonsState();
    }

    private void refreshHelicopterHighlightsAndDestinations() {
        clearAllHighlights();
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null || helicopterPhase == HelicopterPhase.NONE) return;

        int[] initiatorPos = currentPlayer.getPos();

        // Highlight players on the same tile for selection OR confirm visual
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                JPanel panel = tilePanelHolder[r][c];
                if (panel == null) continue;

                GameTile tile = gameState.getBoardTiles()[r][c];
                boolean isInitiatorTile = (r == initiatorPos[0] && c == initiatorPos[1]);

                if (isInitiatorTile && helicopterPhase == HelicopterPhase.SELECTING_PLAYERS) {
                    // Special handling for the tile where player selection happens
                    // Check all players on this tile
                    boolean multiplePlayersOnThisTile = false;
                    for(Player p : gameState.getAllPlayers()) {
                        if (Arrays.equals(p.getPos(), initiatorPos)) {
                            multiplePlayersOnThisTile = true; // Simplified, pawn specific highlight would be better
                            if (playersToMoveWithHelicopter.contains(p)) {
                                panel.setBorder(new LineBorder(Color.GREEN, 3)); // Selected for flight
                            } else if (p != currentPlayer) {
                                panel.setBorder(helicopterPlayerSelectHighlight); // Pink: available to select
                            } else { // current player (initiator)
                                panel.setBorder(new LineBorder(Color.BLUE, 2)); // Blue: initiator, also selected
                            }
                        }
                    }
                } else if (tile != null) { // For all other tiles (potential destinations)
                    panel.setBorder(helicopterTargetTileHighlight); // Magenta: destination
                } else { // Empty panel spot
                     panel.setBorder(defaultTileBorder);
                }
                 // Ensure Sunk tiles are not highlighted as destinations
                if (tile == null && panel.getBorder() == helicopterTargetTileHighlight) {
                    panel.setBorder(defaultTileBorder);
                }
            }
        }
        // Overwrite border for tiles that are confirmed to be part of the flight group in selection phase
        if (helicopterPhase == HelicopterPhase.SELECTING_TARGET_TILE) {
             for(Player pInFlight : playersToMoveWithHelicopter){
                 if(tilePanelHolder[pInFlight.getPos()[0]][pInFlight.getPos()[1]] != null) {
                    tilePanelHolder[pInFlight.getPos()[0]][pInFlight.getPos()[1]].setBorder(new LineBorder(Color.GREEN, 2)); // Confirmed
                 }
             }
        }
    }

    private void useSandbagAction() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (!isUsingSandbagMode) { // Entering Sandbag mode
            boolean hasSandbag = false;
            for (String card : currentPlayer.getHand()) {
                if ("Sandbag".equals(card)) {
                    hasSandbag = true;
                    break;
                }
            }
            if (!hasSandbag) {
                JOptionPane.showMessageDialog(this, "You do not have a Sandbag card.", "Action Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }

            isUsingSandbagMode = true;
            // Ensure other modes are off
            isInShoreUpMode = false;
            navigatorActionState = NavigatorActionState.NONE;
            helicopterPhase = HelicopterPhase.NONE; // Reset helicopter mode
            updateActionButtonsState(); 
            
            clearAllHighlights();
            boolean foundTarget = false;
            for (int r = 0; r < GameState.BOARD_ROWS; r++) {
                for (int c = 0; c < GameState.BOARD_COLS; c++) {
                    GameTile tile = gameState.getBoardTiles()[r][c];
                    if (tile != null && tile.getFloodState() && tilePanelHolder[r][c] != null) {
                        tilePanelHolder[r][c].setBorder(sandbagHighlightBorder);
                        foundTarget = true;
                    }
                }
            }
            if (!foundTarget) {
                JOptionPane.showMessageDialog(this, "No flooded tiles to use Sandbag on.", "Sandbag", JOptionPane.INFORMATION_MESSAGE);
                isUsingSandbagMode = false; // Exit mode if no targets
                updateActionButtonsState();
                highlightMovableTiles();
            }
        } else { // Cancelling Sandbag mode
            isUsingSandbagMode = false;
            isInShoreUpMode = false;
            navigatorActionState = NavigatorActionState.NONE;
            helicopterPhase = HelicopterPhase.NONE; // Reset helicopter mode
            updateActionButtonsState(); 
            clearAllHighlights();
            highlightMovableTiles();
        }
    }

    private void collectTreasureAction() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.hasActionPoints()) {
            return;
        }

        int[] playerPos = currentPlayer.getPos();
        GameTile currentTile = gameState.getBoardTiles()[playerPos[0]][playerPos[1]];

        if (gameState.canPlayerCollectTreasure(currentPlayer, currentTile)) {
            TreasureType treasureToCollect = currentTile.getTreasureType();
            if (treasureToCollect == null) {
                 System.err.println("Error: Current tile has no treasure type for collection.");
                 return;
            }
            
            gameState.collectTreasure(currentPlayer, treasureToCollect);
            currentPlayer.useActionPoint();

            // Update UI and game state
            refreshAllTileAppearances(); // Though treasure collection itself doesn't change tile appearance
            updateActionButtonsState();   // Refresh button states (AP used)
            updatePlayerInfoDisplay(); // Refresh player hand info
            updateCollectedTreasuresDisplay(); // Refresh collected treasures display
            updateAllPlayersCardCountsDisplay(); // Cards might have been used to collect

            // Check for win condition
            if (gameState.checkWinCondition()) {
                // Ensure Helicopter Lift card is also considered if it's part of win
                // For now, simplified win: all treasures + all on Fools' Landing
                String winMessage = "Congratulations! All treasures collected and team assembled at Fools' Landing!\nYou have escaped the Forbidden Island!";
                JOptionPane.showMessageDialog(this, winMessage, "Victory!", JOptionPane.INFORMATION_MESSAGE);
                // Consider disabling further actions or signaling game over to ParentPanel
                // For now, just show message. Further turn progression might be blocked by AP or other logic.
                 parentPanel.showMenuPanel(); // Corrected method call
                 return; // Stop further processing for this turn.
            }
            
            System.out.println(currentPlayer.getRole() + " collected " + treasureToCollect.getDisplayName() + ". AP left: " + currentPlayer.getActionPoints());


            if (!currentPlayer.hasActionPoints()) {
                System.out.println("No action points left for " + currentPlayer.getRole() + ". Ending turn.");
                handleTurnEnd();
            } else {
                // Player still has actions, refresh highlights for current player
                highlightMovableTiles(); // Or other relevant highlights if other actions are primary
                updatePlayerInfoDisplay(); // Refresh AP display
            }
        } else {
            System.out.println("Cannot collect treasure here or conditions not met.");
        }
    }

    private void giveCardActionSetup() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null || !currentPlayer.hasActionPoints()) {
             JOptionPane.showMessageDialog(this, "Cannot give card: No action points or no current player.", "Action Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<Player> otherPlayersOnTile = new ArrayList<>();
        int[] playerPos = currentPlayer.getPos();
        for (Player p : gameState.getAllPlayers()) {
            if (p != currentPlayer && Arrays.equals(p.getPos(), playerPos)) {
                otherPlayersOnTile.add(p);
            }
        }

        if (otherPlayersOnTile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No other players on your tile to give a card to.", "Action Denied", JOptionPane.INFORMATION_MESSAGE);
            updateGiveCardButtonState();
            return;
        }

        Player playerToGiveTo = null;
        if (otherPlayersOnTile.size() == 1) {
            playerToGiveTo = otherPlayersOnTile.get(0);
        } else {
            // Multiple players on the tile, let current player choose
            String[] playerNames = otherPlayersOnTile.stream().map(Player::getRole).toArray(String[]::new);
            String chosenPlayerName = (String) JOptionPane.showInputDialog(this,
                    "Choose player to give card to:",
                    "Give Card - Select Player",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    playerNames,
                    playerNames[0]);

            if (chosenPlayerName == null) return; // User cancelled

            for (Player p : otherPlayersOnTile) {
                if (p.getRole().equals(chosenPlayerName)) {
                    playerToGiveTo = p;
                    break;
                }
            }
        }

        if (playerToGiveTo == null) {
            System.err.println("Error: Could not determine player to give card to.");
            return;
        }

        ArrayList<String> giveableCards = currentPlayer.getGiveableTreasureCards();
        if (giveableCards.isEmpty()) {
            JOptionPane.showMessageDialog(this, currentPlayer.getRole() + " has no treasure cards to give.", "Action Denied", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] cardNamesArray = giveableCards.toArray(new String[0]);
        String cardToGive = (String) JOptionPane.showInputDialog(this,
                "Choose card to give to " + playerToGiveTo.getRole() + ":",
                "Give Card - Select Card",
                JOptionPane.PLAIN_MESSAGE,
                null,
                cardNamesArray,
                cardNamesArray[0]);

        if (cardToGive == null) return; // User cancelled

        // --- Execute Give Card --- 
        boolean success = currentPlayer.discardSpecificCardByName(cardToGive);
        if (success) {
            playerToGiveTo.drawCard(cardToGive); 
            currentPlayer.useActionPoint();
            isUsingSandbagMode = false; 
            helicopterPhase = HelicopterPhase.NONE; // Explicitly reset helicopter phase

            System.out.println(currentPlayer.getRole() + " gave " + cardToGive + " to " + playerToGiveTo.getRole() + ". AP left: " + currentPlayer.getActionPoints());

            updatePlayerInfoDisplay(); // Update current player's info panel
            updateAllPlayersCardCountsDisplay(); // Both players' hands changed

            handleHandLimit(playerToGiveTo); // Check hand limit for the receiver

            if (!currentPlayer.hasActionPoints()) {
                System.out.println(currentPlayer.getRole() + " has no more action points. Ending turn.");
                handleTurnEnd();
            } else {
                updateActionButtonsState(); // Refresh button states (AP used)
                highlightMovableTiles(); // Refresh highlights for current player
            }
        } else {
            JOptionPane.showMessageDialog(this, "Error: Could not find card '" + cardToGive + "' in " + currentPlayer.getRole() + "'s hand to give.", "Error", JOptionPane.ERROR_MESSAGE);
             System.err.println("Error: discardSpecificCardByName failed for " + cardToGive);
        }
    }

    private void handleTurnEnd() {
        Player playerWhoseTurnEnded = gameState.getCurrentPlayer(); 
        if (playerWhoseTurnEnded == null) {
            System.err.println("handleTurnEnd called but no current player in gameState.");
            return;
        }
        System.out.println(playerWhoseTurnEnded.getRole() + " ends their turn actions.");

        // --- DRAW TREASURE CARDS --- (Added Phase)
        System.out.println("DEBUG: Starting treasure card draw phase for " + playerWhoseTurnEnded.getRole());
        boolean watersRiseDrawnThisPhase = false;
        for (int i = 0; i < 2; i++) {
            if (watersRiseDrawnThisPhase) break; // Stop drawing if Waters Rise was drawn

            String drawnCard = gameState.drawTreasureCard();
            System.out.println(playerWhoseTurnEnded.getRole() + " drew treasure card: " + drawnCard);

            if (drawnCard == null) {
                System.out.println("No more treasure cards to draw, even after reshuffle.");
                break; // Stop if no cards left
            }

            if (drawnCard.equals("WatersRise")) {
                watersRiseDrawnThisPhase = true;
                gameState.handleWatersRiseEffect();
                gameState.discardTreasureCardToPile(drawnCard); // Add WatersRise to treasure discard
                updatePlayerInfoDisplay(); // Update AP and card counts if water level affects it (it doesn't directly, but good practice)
                System.out.println("Waters Rise! card drawn and processed. Water level is now: " + gameState.getWaterLevelMeter().getLevelForDisplay());
                if (gameState.isGameOverByWaterLevel()) {
                    JOptionPane.showMessageDialog(this,
                        "The island has been swallowed by the abyss! (Waters Rise led to max water level)\nGame Over.", 
                        "Game Over", 
                        JOptionPane.ERROR_MESSAGE);
                    parentPanel.showMenuPanel();
                    return; // Game Over
                }
            } else {
                playerWhoseTurnEnded.drawCard(drawnCard); // Adds card to player's hand
            }
        }
        updatePlayerInfoDisplay(); // Update player info after drawing all cards
        // TODO: Implement hand limit and discard phase here if player hand > 5
        handleHandLimit(playerWhoseTurnEnded);
        updateAllPlayersCardCountsDisplay(); // Update after treasure card draws and hand limit

        // Reset turn-specific states for the GameBoard UI (moved after treasure draw)
        isInShoreUpMode = false; 
        engineerShoredUpCountThisAction = 0;
        navigatorActionState = NavigatorActionState.NONE; 
        playerBeingMovedByNavigator = null;
        if (validNavigatorCommandTargetTiles != null) { 
            validNavigatorCommandTargetTiles.clear();
        } else {
            validNavigatorCommandTargetTiles = new HashSet<>(); 
        }

        // --- FLOOD CARD DRAWING AND PROCESSING ---
        System.out.println("DEBUG: Starting flood card draw phase.");
        ArrayList<game.simulation.tiles.GameTile> affectedTiles = gameState.drawAndProcessFloodCards();
        for (game.simulation.tiles.GameTile affectedMarkerTile : affectedTiles) {
            int[] pos = affectedMarkerTile.getPosition();
            if (pos != null && pos.length == 2) {
                game.simulation.tiles.GameTile currentTileStateOnBoard = gameState.getBoardTiles()[pos[0]][pos[1]];
                if (currentTileStateOnBoard == null) { 
                    tileSunk(pos[0], pos[1]);
                } else { 
                    refreshTileAppearance(currentTileStateOnBoard, tilePanelHolder[pos[0]][pos[1]]);
                }
            }
        }
        refreshPawnDisplay(); 

        // --- CHECK FOR GAME OVER CONDITIONS ---
        if (gameState.checkOverallLossCondition()) { 
            // Messages like "GAME OVER: Fools' Landing has sunk!" are printed from GameState methods.
            // We can show a general game over dialog here or let GameState methods handle specifics if they use JOptionPane directly.
            // For now, GameState prints to console, and we show a panel switch.
            JOptionPane.showMessageDialog(this, 
                "The island could not withstand the perils! Game Over.", 
                "Game Over", 
                JOptionPane.ERROR_MESSAGE);
            parentPanel.showMenuPanel(); 
            return; // Stop further turn processing, game is over
        }

        // --- ADVANCE TO NEXT PLAYER --- 
        Player nextPlayer = gameState.nextTurn(); 
        
        updatePlayerInfoDisplay(); // Update for new player
        updateAllPlayersCardCountsDisplay(); // Update for new turn starting
        
        clearAllHighlights();
        if (nextPlayer != null) { 
            highlightMovableTiles(); 
            System.out.println("Next turn: " + nextPlayer.getRole() + " with " + nextPlayer.getActionPoints() + " AP.");
        } else {
            // If nextPlayer is null, and checkOverallLossCondition was false, this might be an unexpected state
            // or a win condition met that also results in no next player (e.g. successful escape)
            // For now, we assume loss conditions are primary. Win conditions need to be checked too.
            System.out.println("Game has ended or no next player available (and not a loss per checkOverallLossCondition).");
            // Potentially check for win condition here if not a loss
            if (gameState.checkWinCondition()) { // Assuming checkWinCondition exists and is implemented
                 JOptionPane.showMessageDialog(this, "Congratulations! You have escaped the Forbidden Island!", "Victory!", JOptionPane.INFORMATION_MESSAGE);
            } else {
                 // Added a more specific check for the null player case if not a win or already handled loss
                 if (nextPlayer == null && !gameState.checkOverallLossCondition()){ 
                    JOptionPane.showMessageDialog(this, "Game Over! Critical error or unexpected end state.", "Game Over", JOptionPane.WARNING_MESSAGE);
                 } else if (nextPlayer == null) { // Already handled by loss condition, but as a fallback
                    JOptionPane.showMessageDialog(this, "Game Over! The island's fate is sealed.", "Game Over", JOptionPane.WARNING_MESSAGE);
                 }
            }
            parentPanel.showMenuPanel();
            return; 
        }
        updateActionButtonsState(); 
        updatePlayerInfoDisplay(); // Ensure display is accurate after all turn end processing
    }

    private void handleHandLimit(Player player) {
        if (player == null) return;

        while (player.getHandSize() > 5) {
            ArrayList<String> currentHand = player.getHand(); // Get a copy of the hand
            StringBuilder handString = new StringBuilder("Your hand (" + player.getHandSize() + " cards, need to discard to 5):\n");
            for (int i = 0; i < currentHand.size(); i++) {
                handString.append((i + 1)).append(". ").append(currentHand.get(i)).append("\n");
            }
            handString.append("Enter the number of the card to discard:");

            String input = JOptionPane.showInputDialog(this,
                    handString.toString(),
                    "Discard Card - " + player.getRole(),
                    JOptionPane.PLAIN_MESSAGE);

            if (input == null) { // Player cancelled or closed dialog
                // In a real game, cancelling might not be an option if over limit.
                // For now, we'll just keep prompting. Or we could auto-discard the last card.
                JOptionPane.showMessageDialog(this, "You must discard cards until you have 5 or fewer.", "Discard Required", JOptionPane.WARNING_MESSAGE);
                continue; // Re-loop to show dialog again
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice > 0 && choice <= currentHand.size()) {
                    String discardedCard = player.discardCardByIndex(choice - 1); // Adjust to 0-based index
                    if (discardedCard != null) {
                        gameState.discardTreasureCardToPile(discardedCard);
                        System.out.println(player.getRole() + " discarded " + discardedCard);
                        updatePlayerInfoDisplay(); // Refresh hand display
                        updateAllPlayersCardCountsDisplay(); // Also update the all players display
                    } else {
                        JOptionPane.showMessageDialog(this, "Error discarding card. Index out of bounds or card not found (should not happen with valid input).", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid selection. Please enter a number from the list.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter a number.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void tileSunk(int r, int c) {
        JPanel panel = tilePanelHolder[r][c];
        if (panel != null) {
            panel.removeAll(); 
            panel.setBackground(new Color(0, 0, 128)); // Dark blue for sunk
            panel.setBorder(new LineBorder(Color.BLUE.darker()));
            JLabel sunkLabel = new JLabel("SUNK", SwingConstants.CENTER);
            sunkLabel.setForeground(Color.WHITE);
            sunkLabel.setFont(new Font("Arial", Font.BOLD, 16));
            panel.add(sunkLabel, BorderLayout.CENTER);
            panel.putClientProperty("gameTile", null); // Important: dissociate GameTile
            panel.revalidate();
            panel.repaint();
        }
        System.out.println("UI: Tile at [" + r + "," + c + "] visually marked as SUNK.");
        // TODO: Implement logic for what happens to players on a sunk tile.
        // For now, refreshPawnDisplay might clear them if their tile is gone.
    }

    private void updatePlayerInfoDisplay() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) {
            currentPlayerLabel.setText("Current Player: None");
            for (TreasureType type : TreasureType.values()) {
                if (treasureCardCountLabels.get(type) != null) {
                    treasureCardCountLabels.get(type).setText(" 0");
                }
            }
            helicopterCardLabel.setText("Helicopter: 0");
            sandbagCardLabel.setText("Sandbags: 0");
            // Water level display is handled by its own panel + update method
            // If waterLevelDisplayPanel exists, ensure it reflects no data or a default state
            if (waterLevelDisplayPanel != null) {
                 waterLevelDisplayPanel.updateMeter(null); // Pass null if no current meter
            }
            return;
        }

        currentPlayerLabel.setText("Current Player: " + currentPlayer.getRole() + " (AP: " + currentPlayer.getActionPoints() + ")");
        for (TreasureType type : TreasureType.values()) {
            int count = currentPlayer.countTreasureCards(type);
            if (treasureCardCountLabels.get(type) != null) {
                treasureCardCountLabels.get(type).setText(" " + count);
            }
        }
        int helicopterCount = 0;
        int sandbagCount = 0;
        for (String card : currentPlayer.getHand()) {
            if ("HelicopterLift".equals(card)) helicopterCount++;
            else if ("Sandbag".equals(card)) sandbagCount++;
        }
        helicopterCardLabel.setText("Helicopter: " + helicopterCount);
        sandbagCardLabel.setText("Sandbags: " + sandbagCount);
        playerInfoPanel.revalidate();
        playerInfoPanel.repaint();
    }

    private void updateCollectedTreasuresDisplay() {
        if (gameState == null || collectedTreasureIconLabels == null) return;
        Set<TreasureType> treasuresActuallyCollected = gameState.getCollectedTreasures();
        for (Map.Entry<TreasureType, JLabel> entry : collectedTreasureIconLabels.entrySet()) {
            TreasureType type = entry.getKey();
            JLabel label = entry.getValue();
            label.setIcon(null);
            // Default to not collected with white text
            label.setText("[" + type.getDisplayName() + ": Not collected]");
            label.setForeground(Color.WHITE); // Changed from Color.GRAY

            if (treasuresActuallyCollected.contains(type)) {
                String imageFileName = "";
                switch (type) {
                    case STATUE_OF_THE_WIND: imageFileName = "AIR.png"; break;
                    case OCEANS_CHALICE: imageFileName = "EAU.png"; break;
                    case CRYSTAL_OF_FIRE: imageFileName = "FEU.png"; break;
                    case EARTH_STONE: imageFileName = "TERRE.png"; break;
                }
                if (!imageFileName.isEmpty()) {
                    String imagePath = "Images/Treasures/" + imageFileName;
                    try {
                        BufferedImage treasureImage = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
                        Image scaledTreasureImage = treasureImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                        label.setIcon(new ImageIcon(scaledTreasureImage));
                        label.setText(type.getDisplayName() + " (Collected!)");
                        label.setForeground(Color.WHITE); // Changed from Color.BLACK
                        label.setHorizontalTextPosition(JLabel.CENTER);
                        label.setVerticalTextPosition(JLabel.BOTTOM);
                    } catch (IOException | NullPointerException ex) {
                        label.setText(type.getDisplayName() + " (Collected - Image Error)");
                         label.setForeground(Color.WHITE); // Changed from Color.RED
                    }
                } else {
                    label.setText(type.getDisplayName() + " (Collected - No Image Mapping)");
                     label.setForeground(Color.WHITE); // Changed from Color.ORANGE
                }
            }
        }
        if (collectedTreasuresPanel != null) {
            collectedTreasuresPanel.revalidate();
            collectedTreasuresPanel.repaint();
        }
    }
    
    private void updateWaterLevelMeterDisplay() {
        if (waterLevelDisplayPanel != null && gameState != null && gameState.getWaterLevelMeter() != null) {
            waterLevelDisplayPanel.updateMeter(gameState.getWaterLevelMeter());
        }
    }

    private void updateAllPlayersCardCountsDisplay() {
        if (gameState == null || gameState.getAllPlayers() == null) return;

        for (Player player : gameState.getAllPlayers()) {
            String playerRole = player.getRole();

            EnumMap<TreasureType, JLabel> treasureLabels = allPlayersTreasureCardLabelsForAllCardsPanel.get(playerRole);
            if (treasureLabels != null) {
                for (TreasureType type : TreasureType.values()) {
                    JLabel label = treasureLabels.get(type);
                    if (label != null) {
                        label.setText(type.getDisplayName() + ": " + player.countTreasureCards(type));
                    }
                }
            }

            JLabel sandbagLabel = allPlayersSandbagLabelsForAllCardsPanel.get(playerRole);
            if (sandbagLabel != null) {
                int sandbagCount = 0;
                for (String card : player.getHand()) {
                    if ("Sandbag".equals(card)) sandbagCount++;
                }
                sandbagLabel.setText("Sandbags: " + sandbagCount);
            }

            JLabel helicopterLabel = allPlayersHelicopterLabelsForAllCardsPanel.get(playerRole);
            if (helicopterLabel != null) {
                int helicopterCount = 0;
                for (String card : player.getHand()) {
                    if ("HelicopterLift".equals(card)) helicopterCount++;
                }
                helicopterLabel.setText("Helicopter Lifts: " + helicopterCount);
            }
        }
        if (allPlayersCardCountsPanel != null) {
            allPlayersCardCountsPanel.revalidate();
            allPlayersCardCountsPanel.repaint();
        }
    }

    private void refreshPawnDisplay() {
        // 1. Clear all existing master pawn display areas from all tile panels
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (tilePanelHolder[r][c] != null && tilePanelHolder[r][c].getLayout() instanceof BorderLayout) {
                    BorderLayout layout = (BorderLayout) tilePanelHolder[r][c].getLayout();
                    Component southComponent = layout.getLayoutComponent(BorderLayout.SOUTH);
                    // Use a new name for the master container to avoid confusion with old logic
                    if (southComponent instanceof JPanel && "master_pawn_display_area".equals(southComponent.getName())) {
                        tilePanelHolder[r][c].remove(southComponent);
                    } else if (southComponent != null && "pawn_container".equals(southComponent.getName())) {
                        // Also clear out old single pawn containers if they exist from previous logic
                        tilePanelHolder[r][c].remove(southComponent);
                    }
                }
            }
        }

        // 2. Add pawns to their current positions
        if (gameState.getAllPlayers() != null) {
            for (Player player : gameState.getAllPlayers()) {
                if (player != null && player.getPos() != null) {
                    int pRow = player.getPos()[0];
                    int pCol = player.getPos()[1];

                    if (pRow >= 0 && pRow < GameState.BOARD_ROWS && pCol >= 0 && pCol < GameState.BOARD_COLS) {
                        JPanel targetTilePanel = tilePanelHolder[pRow][pCol];
                        if (targetTilePanel != null && targetTilePanel.getClientProperty("gameTile") != null) {
                            
                            // Find or create the master pawn display area for this tile
                            JPanel masterPawnDisplayAreaOnTile;
                            BorderLayout tileLayout = (BorderLayout) targetTilePanel.getLayout();
                            Component southComponent = tileLayout.getLayoutComponent(BorderLayout.SOUTH);

                            if (southComponent instanceof JPanel && "master_pawn_display_area".equals(southComponent.getName())) {
                                masterPawnDisplayAreaOnTile = (JPanel) southComponent;
                            } else {
                                if (southComponent != null) { // If something else was there
                                    targetTilePanel.remove(southComponent);
                                }
                                masterPawnDisplayAreaOnTile = new JPanel();
                                masterPawnDisplayAreaOnTile.setName("master_pawn_display_area");
                                masterPawnDisplayAreaOnTile.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 0)); // Arrange player representations side-by-side
                                masterPawnDisplayAreaOnTile.setOpaque(false);
                                targetTilePanel.add(masterPawnDisplayAreaOnTile, BorderLayout.SOUTH);
                            }

                            // Create the individual representation for the current player (icon + name vertically)
                            JPanel individualPlayerRepresentationPanel = new JPanel();
                            individualPlayerRepresentationPanel.setLayout(new BoxLayout(individualPlayerRepresentationPanel, BoxLayout.Y_AXIS));
                            individualPlayerRepresentationPanel.setOpaque(false);
                            // individualPlayerRepresentationPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Alignment for BoxLayout children is usually on the child itself

                            try {
                                BufferedImage pawnImg = loadPawnImage(player.getRole());
                                if (pawnImg != null) {
                                    Image scaledPawn = pawnImg.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
                                    JLabel pawnImageLabel = new JLabel(new ImageIcon(scaledPawn));
                                    pawnImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                                    individualPlayerRepresentationPanel.add(pawnImageLabel);
                                } else {
                                    JLabel noPawnImageLabel = new JLabel("[P]");
                                    noPawnImageLabel.setFont(new Font("Arial", Font.BOLD, 14));
                                    noPawnImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                                    individualPlayerRepresentationPanel.add(noPawnImageLabel);
                                }

                                JLabel pawnNameLabel = new JLabel(player.getRole());
                                pawnNameLabel.setFont(new Font("Arial", Font.BOLD, 10));
                                pawnNameLabel.setForeground(Color.BLACK);
                                pawnNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                                individualPlayerRepresentationPanel.add(pawnNameLabel);
                                
                                masterPawnDisplayAreaOnTile.add(individualPlayerRepresentationPanel);

                            } catch (IOException e) {
                                System.err.println("Could not load pawn image for " + player.getRole() + " during refresh: " + e.getMessage());
                                // Fallback: Add a simple representation to the individual panel
                                JLabel errorNameLabel = new JLabel(player.getRole().substring(0, Math.min(player.getRole().length(), 3))); // e.g., "Pil"
                                errorNameLabel.setFont(new Font("Arial", Font.BOLD, 10));
                                errorNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                                individualPlayerRepresentationPanel.add(errorNameLabel);
                                masterPawnDisplayAreaOnTile.add(individualPlayerRepresentationPanel);
                            }
                        }
                    }
                }
            }
        }
        // 3. After processing all players, revalidate and repaint all tile panels
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (tilePanelHolder[r][c] != null) {
                    tilePanelHolder[r][c].revalidate();
                    tilePanelHolder[r][c].repaint();
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JPanel clickedPanel = (JPanel) e.getSource();
        GameTile clickedGameTile = (GameTile) clickedPanel.getClientProperty("gameTile");
        Player currentPlayer = gameState.getCurrentPlayer();

        if (currentPlayer == null) { return; }

        // Helicopter Logic
        if (helicopterPhase != HelicopterPhase.NONE) {
            if (clickedGameTile == null && helicopterPhase == HelicopterPhase.SELECTING_TARGET_TILE) {
                JOptionPane.showMessageDialog(this, "Helicopter: Invalid destination. Click a valid (purple) tile or Cancel Flight.", "Helicopter Action", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (helicopterPhase == HelicopterPhase.SELECTING_PLAYERS) {
                boolean interactionOnCurrentTile = clickedGameTile != null && Arrays.equals(clickedGameTile.getPosition(), currentPlayer.getPos());
                Player selectedPlayerOnTile = null;
                if(interactionOnCurrentTile){
                    for (Player p : gameState.getAllPlayers()) {
                        if (p != currentPlayer && Arrays.equals(p.getPos(), clickedGameTile.getPosition())) {
                            selectedPlayerOnTile = p; 
                            break;
                        }
                    }
                }
                if (selectedPlayerOnTile != null) { 
                    if (playersToMoveWithHelicopter.contains(selectedPlayerOnTile)) {
                        playersToMoveWithHelicopter.remove(selectedPlayerOnTile);
                    } else {
                        playersToMoveWithHelicopter.add(selectedPlayerOnTile);
                    }
                    refreshHelicopterHighlightsAndDestinations();
                    updateActionButtonsState(); 
                    return;
                } else if (clickedGameTile != null && gameState.getBoardTiles()[clickedGameTile.getPosition()[0]][clickedGameTile.getPosition()[1]] != null) { 
                    // Destination click (fast-track)
                } else { 
                     JOptionPane.showMessageDialog(this, "Helicopter: Click a player on your tile to select, a valid destination, or \"Confirm Players\".", "Helicopter Action", JOptionPane.WARNING_MESSAGE);
                     return;
                }
            }
            if (clickedGameTile != null && gameState.getBoardTiles()[clickedGameTile.getPosition()[0]][clickedGameTile.getPosition()[1]] != null) {
                if (playersToMoveWithHelicopter.isEmpty()) playersToMoveWithHelicopter.add(currentPlayer);
                for (Player pToMove : playersToMoveWithHelicopter) {
                    pToMove.setPosition(clickedGameTile.getPosition());
                }
                currentPlayer.useActionPoint();
                if(currentPlayer.discardSpecificCardByName("HelicopterLift")) gameState.discardTreasureCardToPile("HelicopterLift");
                helicopterPhase = HelicopterPhase.NONE;
                playersToMoveWithHelicopter.clear();
                refreshPawnDisplay(); updatePlayerInfoDisplay(); clearAllHighlights(); updateActionButtonsState();
                updateAllPlayersCardCountsDisplay(); // Player used a helicopter card
                if (currentPlayer.hasActionPoints()) {
                    highlightMovableTiles();
                } else {
                    handleTurnEnd();
                }
            } else if (clickedGameTile != null && helicopterPhase == HelicopterPhase.SELECTING_TARGET_TILE){
                 JOptionPane.showMessageDialog(this, "Invalid destination for Helicopter.", "Helicopter Action", JOptionPane.WARNING_MESSAGE);
                 return;
            }
        }

        // Sandbag Logic
        if (isUsingSandbagMode) {
            if (clickedGameTile != null && clickedGameTile.getFloodState()) {
                gameState.shoreUpTile(clickedGameTile);
                if (currentPlayer.discardSpecificCardByName("Sandbag")) gameState.discardTreasureCardToPile("Sandbag");
                refreshTileAppearance(clickedGameTile, clickedPanel); updatePlayerInfoDisplay();
                isUsingSandbagMode = false; updateActionButtonsState(); clearAllHighlights(); highlightMovableTiles();
                updateAllPlayersCardCountsDisplay(); // Player used a sandbag
            } else {
                JOptionPane.showMessageDialog(this, "Invalid target for Sandbag. Click a flooded (cyan bordered) tile or Cancel.", "Sandbag", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        // Shore Up Logic
        if (isInShoreUpMode) {
            if (clickedGameTile != null && clickedGameTile.getFloodState() && getShoreUpTargetTiles(currentPlayer).contains(clickedGameTile)) {
                gameState.shoreUpTile(clickedGameTile);
                refreshTileAppearance(clickedGameTile, clickedPanel);
                if (currentPlayer.getRole().equals("Engineer")) {
                    engineerShoredUpCountThisAction++;
                    if (engineerShoredUpCountThisAction == 1) {
                        highlightShoreUpTargets(currentPlayer);
                    } else if (engineerShoredUpCountThisAction == 2) {
                        currentPlayer.useActionPoint(); 
                        engineerShoredUpCountThisAction = 0; isInShoreUpMode = false; clearAllHighlights(); updatePlayerInfoDisplay();
                        if (currentPlayer.hasActionPoints()) { highlightMovableTiles(); updateActionButtonsState(); }
                        else handleTurnEnd();
                    }
                } else {
                    currentPlayer.useActionPoint();
                    isInShoreUpMode = false; clearAllHighlights(); updatePlayerInfoDisplay();
                    if (currentPlayer.hasActionPoints()) { highlightMovableTiles(); updateActionButtonsState(); }
                    else handleTurnEnd();
                }
            } else {
                String msg = "Click on a valid (flooded, orange bordered) tile to shore up.";
                if (currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1) msg += " Or click 'Finish Shoring Up' button.";
                else msg += " Or click 'Cancel Shore Up' button.";
                System.out.println(msg);
            }
            return; 
        }

        // Navigator Command Logic (Simplified for brevity, full logic was more extensive)
        if (navigatorActionState == NavigatorActionState.SELECTING_PLAYER_TO_MOVE) {
            Player selectedPlayer = null;
            if (clickedGameTile != null){
                for (Player p : gameState.getAllPlayers()) {
                    if (p != currentPlayer && Arrays.equals(p.getPos(), clickedGameTile.getPosition())) {
                        selectedPlayer = p;
                        break;
                    }
                }
            }
            if (selectedPlayer != null) {
                playerBeingMovedByNavigator = selectedPlayer;
                navigatorActionState = NavigatorActionState.SELECTING_TILE_FOR_OTHER;
                validNavigatorCommandTargetTiles = highlightTilesForNavigatorCommand();
            } else {
                 JOptionPane.showMessageDialog(this, "Navigator: No other player on that tile. Click another player or cancel.", "Navigator", JOptionPane.INFORMATION_MESSAGE);
            }
            return; 
        } else if (navigatorActionState == NavigatorActionState.SELECTING_TILE_FOR_OTHER) {
            if (playerBeingMovedByNavigator == null) { navigatorActionState = NavigatorActionState.NONE; clearAllHighlights(); highlightMovableTiles(); return; }
            if (clickedGameTile != null && validNavigatorCommandTargetTiles.contains(clickedGameTile)) {
                playerBeingMovedByNavigator.setPosition(clickedGameTile.getPosition());
                currentPlayer.useActionPoint(); 
                navigatorActionState = NavigatorActionState.NONE; playerBeingMovedByNavigator = null; validNavigatorCommandTargetTiles.clear();
                refreshPawnDisplay();
                if (currentPlayer.hasActionPoints()) {
                    highlightMovableTiles(); 
                    updateNavigatorButtonState();
                } else {
                    handleTurnEnd();
                }
            } else if (clickedGameTile != null && Arrays.equals(clickedGameTile.getPosition(), playerBeingMovedByNavigator.getPos())){
                navigatorActionState = NavigatorActionState.SELECTING_PLAYER_TO_MOVE;
                playerBeingMovedByNavigator = null;
                validNavigatorCommandTargetTiles.clear();
                clearAllHighlights();
                JOptionPane.showMessageDialog(this, "Navigator: Player command target reset. Select a player to move.", "Navigator", JOptionPane.INFORMATION_MESSAGE);
            }else {
                JOptionPane.showMessageDialog(this, "Navigator: Invalid target. Select a highlighted tile or click the commanded player to cancel.", "Navigator", JOptionPane.INFORMATION_MESSAGE);
            }
            return; 
        }

        if (currentPlayer.getRole().equals("Navigator") && currentPlayer.canUseCommandAbility() &&
            clickedGameTile != null && Arrays.equals(currentPlayer.getPos(), clickedGameTile.getPosition()) &&
            navigatorActionState == NavigatorActionState.NONE) {
            navigatorActionState = NavigatorActionState.SELECTING_PLAYER_TO_MOVE;
            clearAllHighlights();
            JOptionPane.showMessageDialog(this, "Navigator: Command ability activated. Click another player to move them, or click yourself again to cancel.", "Navigator", JOptionPane.INFORMATION_MESSAGE);
            for(Player p : gameState.getAllPlayers()){
                if(p != currentPlayer && p.getPos() != null){
                    tilePanelHolder[p.getPos()[0]][p.getPos()[1]].setBorder(helicopterPlayerSelectHighlight);
                }
            }
            return;
        }
        
        if (clickedGameTile == null) return;
        ArrayList<GameTile> movableTiles = gameState.findMovable(currentPlayer);
        if (movableTiles.stream().anyMatch(t -> Arrays.equals(t.getPosition(), clickedGameTile.getPosition()))) {
            int[] originalPlayerPos = Arrays.copyOf(currentPlayer.getPos(), currentPlayer.getPos().length);
            currentPlayer.setPosition(clickedGameTile.getPosition());
            currentPlayer.useActionPoint(); 
            if (currentPlayer.getRole().equals("Pilot") && currentPlayer.canUseFlightAbility()) {
                boolean standardMoveWasPossible = false;
                int dx = Math.abs(originalPlayerPos[0] - clickedGameTile.getPosition()[0]);
                int dy = Math.abs(originalPlayerPos[1] - clickedGameTile.getPosition()[1]);
                if ((dx == 1 && dy == 0) || (dx == 0 && dy == 1)) standardMoveWasPossible = true;
                if (!standardMoveWasPossible) currentPlayer.markFlightAbilityUsed();
            }
            refreshPawnDisplay(); 
            updatePlayerInfoDisplay(); // This updates the AP display

            if (currentPlayer.hasActionPoints()) {
                if (navigatorActionState == NavigatorActionState.NONE) highlightMovableTiles(); 
                // updateNavigatorButtonState(); // This is specific, we need the general one
                updateActionButtonsState(); // Ensure all buttons are updated after a move
            } else {
                handleTurnEnd();
            }
        }
    }

    private Set<GameTile> highlightTilesForNavigatorCommand() {
        clearAllHighlights(); 
        Set<GameTile> commandMoveTiles = new HashSet<>();
        if (playerBeingMovedByNavigator == null || playerBeingMovedByNavigator.getPos() == null) return commandMoveTiles;
        
        tilePanelHolder[playerBeingMovedByNavigator.getPos()[0]][playerBeingMovedByNavigator.getPos()[1]].setBorder(new LineBorder(Color.BLUE, 3));

        commandMoveTiles = gameState.findNavigatorCommandTargets(playerBeingMovedByNavigator);

        for (GameTile tile : commandMoveTiles) {
            if (tile != null && tile.getPosition() != null) {
                tilePanelHolder[tile.getPosition()[0]][tile.getPosition()[1]].setBorder(highlightedTileBorder);
            }
        }
        return commandMoveTiles;
    }

    private void updateNavigatorButtonState() {
        if (gameState.getCurrentPlayer() != null && gameState.getCurrentPlayer().getRole().equals("Navigator") && 
            gameState.getCurrentPlayer().hasActionPoints() && gameState.getCurrentPlayer().canUseCommandAbility()) {
            // System.out.println("Navigator has command ability available this turn."); 
        }
    }

    @Override public void mousePressed(MouseEvent e) { /* Not used */ }
    @Override public void mouseReleased(MouseEvent e) { /* Not used */ }
    @Override public void mouseEntered(MouseEvent e) { /* Not used */ }
    @Override public void mouseExited(MouseEvent e) { /* Not used */ }

    private void updateActionButtonsState() {
        Player currentPlayer = gameState.getCurrentPlayer();
        if (currentPlayer == null) {
            // Disable all buttons if no current player
            if (shoreUpButton != null) shoreUpButton.setEnabled(false);
            if (collectTreasureButton != null) collectTreasureButton.setEnabled(false);
            if (giveCardButton != null) giveCardButton.setEnabled(false);
            if (useSandbagButton != null) useSandbagButton.setEnabled(false);
            if (useHelicopterButton != null) useHelicopterButton.setEnabled(false);
            if (endTurnButton != null) endTurnButton.setEnabled(false);
            // if (commandOtherPlayerButton != null) commandOtherPlayerButton.setEnabled(false); // If you add it
            return;
        }

        // Update individual button states
        // Shore Up Button
        if (shoreUpButton != null) {
            boolean canShoreUp = currentPlayer.hasActionPoints() || 
                                (isInShoreUpMode && currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1);
            shoreUpButton.setEnabled(canShoreUp);
            if (isInShoreUpMode) {
                if (currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1) {
                    shoreUpButton.setText("Finish Shoring Up (1 AP)");
                } else {
                    shoreUpButton.setText("Cancel Shore Up");
                }
            } else {
                shoreUpButton.setText("Shore Up (1 AP)");
            }
        }

        updateCollectTreasureButtonState();
        updateGiveCardButtonState();
        updateUseSandbagButtonState();
        updateUseHelicopterButtonState();
        updateNavigatorButtonState(); // For Navigator's command ability
        if (endTurnButton != null) {
            endTurnButton.setEnabled(currentPlayer.hasActionPoints()); // Only allow ending turn if AP > 0, or always allow?
                                                                    // Let's always allow ending turn, even with 0 AP, as a manual override to proceed.
            endTurnButton.setEnabled(true);
        }
    }
}

