package game.simulation;

import game.panel.ParentPanel;
import game.simulation.player.*;
import game.simulation.tiles.GameTile;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GameBoard extends JPanel implements MouseListener {

    private GameState gameState;
    private ParentPanel parentPanel;

    private JPanel[][] tilePanelHolder;
    private static final int TILE_SIZE = 100; // Example size, adjust as needed
    private static final int PAWN_ICON_SIZE = 30; // Example size

    private Border defaultTileBorder = BorderFactory.createLineBorder(Color.GRAY, 1);
    private Border highlightedTileBorder = BorderFactory.createLineBorder(Color.GREEN, 3);
    private Border navigatorSelectedPlayerBorder = BorderFactory.createLineBorder(Color.BLUE, 3);
    private Border shoreUpHighlightBorder = BorderFactory.createLineBorder(Color.ORANGE, 3);


    // Game State related UI
    private Player currentPlayer; // convenience reference
    private boolean isInShoreUpMode = false;
    private int engineerShoredUpCountThisAction = 0;

    private enum NavigatorActionState {NONE, SELECTING_PLAYER_TO_MOVE, SELECTING_TILE_FOR_OTHER}
    private NavigatorActionState navigatorActionState = NavigatorActionState.NONE;
    private Player playerBeingMovedByNavigator = null;
    private Set<GameTile> validNavigatorCommandTargetTiles = null;

    // Controls
    private JPanel controlPanel;
    private JButton shoreUpButton;
    private JButton specialActionButton; // For Pilot flight / Navigator command start
    private JButton endTurnButton;


    public GameBoard(GameState gameState, ParentPanel parentPanel) {
        this.gameState = gameState;
        this.parentPanel = parentPanel;
        this.currentPlayer = gameState.getCurrentPlayer(); // Initial current player

        setLayout(new BorderLayout()); // Main layout for game board area + control panel

        JPanel boardPanel = new JPanel(new GridLayout(GameState.BOARD_ROWS, GameState.BOARD_COLS));
        this.tilePanelHolder = new JPanel[GameState.BOARD_ROWS][GameState.BOARD_COLS];

        game.simulation.tiles.GameTile[][] tiles = gameState.getBoardTiles();
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                JPanel p = new JPanel(new BorderLayout());
                p.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
                p.setBorder(defaultTileBorder);
                p.addMouseListener(this); // Add mouse listener to each tile panel

                game.simulation.tiles.GameTile currentTile = tiles[r][c];
                if (currentTile != null) {
                    p.putClientProperty("gameTile", currentTile); // Associate GameTile with panel
                    refreshTileAppearance(currentTile, p);
                } else {
                    p.setBackground(new Color(0, 105, 148)); // Water color for initially empty spots
                }
                tilePanelHolder[r][c] = p;
                boardPanel.add(p);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        setupControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        refreshPawnDisplay();
        if (this.currentPlayer != null) { // Check if currentPlayer is initialized
           highlightMovableTiles();
           updateActionButtonsState();
        } else {
            System.err.println("GameBoard Constructor: currentPlayer is null after GameState initialization.");
            // This might happen if gameState.nextTurn() isn't called before creating GameBoard
            // Or if there are no players.
        }
        
        setPreferredSize(new Dimension(GameState.BOARD_COLS * TILE_SIZE, GameState.BOARD_ROWS * TILE_SIZE + 100)); // +100 for control panel
    }
    
    private void setupControlPanel() {
        controlPanel = new JPanel(new FlowLayout());

        shoreUpButton = new JButton("Shore Up");
        shoreUpButton.addActionListener(e -> enterShoreUpMode());
        controlPanel.add(shoreUpButton);

        specialActionButton = new JButton("Special Action"); // Text will be updated
        specialActionButton.addActionListener(e -> handleSpecialAction());
        controlPanel.add(specialActionButton);
        
        endTurnButton = new JButton("End Turn");
        endTurnButton.addActionListener(e -> handleTurnEnd());
        controlPanel.add(endTurnButton);

        updateActionButtonsState(); // Initial state
    }

    private void handleSpecialAction() {
        if (currentPlayer == null) return;

        if (currentPlayer.canUseCommandAbility() && navigatorActionState == NavigatorActionState.NONE) { 
            System.out.println("Navigator: Entering SELECTING_PLAYER_TO_MOVE state.");
            navigatorActionState = NavigatorActionState.SELECTING_PLAYER_TO_MOVE;
            clearAllHighlights();
            JOptionPane.showMessageDialog(this, "Navigator: Select another player to move (1 AP). Click your own tile again to cancel.", "Navigator Command", JOptionPane.INFORMATION_MESSAGE);
        } else if (currentPlayer.canUseFlightAbility()) { 
            JOptionPane.showMessageDialog(this, "Pilot: Your flight ability allows you to move to any un-sunk tile once per turn (1 AP).", "Pilot Flight", JOptionPane.INFORMATION_MESSAGE);
        }
        updateActionButtonsState();
    }


    private BufferedImage loadPawnImage(String role) {
        if (role == null) return null;
        String resourcePath = "Images/Pawns/" + role + "_Adventurer_Icon@2x.png";
        try {
            return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resourcePath)));
        } catch (IOException | NullPointerException e) {
            System.err.println("Error loading pawn image for: " + role + " from path: " + resourcePath);
            return null;
        }
    }

    public void refreshPawnDisplay() {
        if (gameState == null || tilePanelHolder == null) return;

        // Clear all existing pawns
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (tilePanelHolder[r][c] != null) {
                    Component[] components = tilePanelHolder[r][c].getComponents();
                    for (Component comp : components) {
                        if (comp instanceof JPanel && "pawn_container".equals(comp.getName())) {
                            tilePanelHolder[r][c].remove(comp);
                        }
                    }
                    tilePanelHolder[r][c].revalidate();
                    tilePanelHolder[r][c].repaint();
                }
            }
        }

        // Draw current pawns
        List<Player> players = gameState.getAllPlayers();
        if (players == null) return;

        for (Player p : players) {
            int[] playerPos = p.getPos();
            if (playerPos != null && playerPos.length == 2) {
                int r = playerPos[0];
                int c = playerPos[1];
                if (r >= 0 && r < GameState.BOARD_ROWS && c >= 0 && c < GameState.BOARD_COLS && tilePanelHolder[r][c] != null) {
                    JPanel targetTilePanel = tilePanelHolder[r][c];

                    JPanel pawnContainerPanel = new JPanel();
                    pawnContainerPanel.setName("pawn_container");
                    pawnContainerPanel.setLayout(new BoxLayout(pawnContainerPanel, BoxLayout.Y_AXIS)); // Stack vertically
                    pawnContainerPanel.setOpaque(false); // Make it transparent

                    BufferedImage pawnImg = loadPawnImage(p.getRole());
                    JLabel pawnLabel;
                    if (pawnImg != null) {
                        ImageIcon icon = new ImageIcon(pawnImg.getScaledInstance(PAWN_ICON_SIZE, PAWN_ICON_SIZE, Image.SCALE_SMOOTH));
                        pawnLabel = new JLabel(icon);
                    } else {
                        pawnLabel = new JLabel("[P]"); // Placeholder if image fails
                        pawnLabel.setForeground(Color.BLACK);
                    }
                    pawnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    JLabel nameLabel = new JLabel(p.getRole().substring(0, Math.min(p.getRole().length(), 4))); // Short name
                    nameLabel.setForeground(Color.BLACK);
                    nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
                    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    
                    pawnContainerPanel.add(pawnLabel);
                    pawnContainerPanel.add(nameLabel);

                    targetTilePanel.add(pawnContainerPanel, BorderLayout.SOUTH);
                    targetTilePanel.revalidate();
                    targetTilePanel.repaint();
                }
            }
        }
    }
    
    public void refreshTileAppearance(GameTile tile, JPanel panel) {
        if (tile == null || panel == null) return;

        panel.removeAll(); // Clear previous components (like old name label or sunk label)
        panel.setLayout(new BorderLayout()); // Ensure layout

        // Tile Name
        JLabel nameLabel = new JLabel(tile.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
        panel.add(nameLabel, BorderLayout.NORTH);

        // Tile Image
        BufferedImage tileImg = tile.getImage();
        if (tileImg != null) {
            ImageIcon icon = new ImageIcon(tileImg.getScaledInstance(TILE_SIZE - 20, TILE_SIZE - 20, Image.SCALE_SMOOTH));
            JLabel imgLabel = new JLabel(icon);
            panel.add(imgLabel, BorderLayout.CENTER);
        }

        // Background based on flood state
        if (tile.getFloodState()) {
            panel.setBackground(new Color(173, 216, 230)); // Light blue for flooded
        } else {
            panel.setBackground(new Color(210, 180, 140)); // Tan for unflooded
        }
        
        // Keep border (will be managed by highlight methods)
        if (panel.getBorder() == null || panel.getBorder() == defaultTileBorder || panel.getBorder() == highlightedTileBorder || panel.getBorder() == shoreUpHighlightBorder || panel.getBorder() == navigatorSelectedPlayerBorder) {
            // If it's one of our standard borders, let highlightMovableTiles or clearAllHighlights manage it.
            // If it was a special border (like a sunk border), this refresh might override it if not handled.
            // For now, assume highlight methods will fix it.
        }


        panel.revalidate();
        panel.repaint();
    }
    
    public void refreshAllTileAppearances() {
        game.simulation.tiles.GameTile[][] tiles = gameState.getBoardTiles();
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                game.simulation.tiles.GameTile tile = tiles[r][c];
                JPanel panel = tilePanelHolder[r][c];
                if (tile != null) {
                    refreshTileAppearance(tile, panel);
                } else if (panel != null) { // Tile is sunk or was never there
                    tileSunk(r,c); // Visually mark as sunk
                }
            }
        }
        refreshPawnDisplay(); // Pawns might need to be redrawn over new backgrounds
    }

    private void clearAllHighlights() {
        for (int r = 0; r < GameState.BOARD_ROWS; r++) {
            for (int c = 0; c < GameState.BOARD_COLS; c++) {
                if (tilePanelHolder[r][c] != null) {
                    tilePanelHolder[r][c].setBorder(defaultTileBorder);
                }
            }
        }
    }

    private void highlightMovableTiles() {
        clearAllHighlights(); // Clear previous highlights first
        if (currentPlayer == null || !currentPlayer.hasActionPoints() || isInShoreUpMode) {
            return;
        }

        ArrayList<game.simulation.tiles.GameTile> movableTiles = gameState.findMovable(currentPlayer);
        for (game.simulation.tiles.GameTile tile : movableTiles) {
            int[] pos = tile.getPosition();
            tilePanelHolder[pos[0]][pos[1]].setBorder(highlightedTileBorder);
        }
    }
    
    private void highlightShoreUpTargets() {
        clearAllHighlights();
        if (currentPlayer == null || !isInShoreUpMode) return;

        ArrayList<game.simulation.tiles.GameTile> shoreUpTargets = getShoreUpTargetTiles(currentPlayer);
        for (game.simulation.tiles.GameTile tile : shoreUpTargets) {
            int[] pos = tile.getPosition();
            if (tilePanelHolder[pos[0]][pos[1]] != null) {
                 tilePanelHolder[pos[0]][pos[1]].setBorder(shoreUpHighlightBorder);
            }
        }
    }
    
    private Set<game.simulation.tiles.GameTile> highlightTilesForNavigatorCommand(Player playerToMove) {
        clearAllHighlights();
        if (playerToMove == null) return null;

        int[] playerPos = playerToMove.getPos();
        if (playerPos != null) { // Highlight the player being moved
            tilePanelHolder[playerPos[0]][playerPos[1]].setBorder(navigatorSelectedPlayerBorder);
        }
        
        // Get valid move locations for the chosen player (1 or 2 orthogonal)
        Set<game.simulation.tiles.GameTile> targetTiles = gameState.findNavigatorCommandTargets(playerToMove);
        for (game.simulation.tiles.GameTile tile : targetTiles) {
            int[] pos = tile.getPosition();
            if (tilePanelHolder[pos[0]][pos[1]] != null) {
                 tilePanelHolder[pos[0]][pos[1]].setBorder(highlightedTileBorder); // Green for valid targets
            }
        }
        return targetTiles;
    }


    private void updateActionButtonsState() {
        if (currentPlayer == null) {
            shoreUpButton.setEnabled(false);
            specialActionButton.setEnabled(false);
            endTurnButton.setEnabled(false);
            return;
        }

        boolean canAct = currentPlayer.hasActionPoints() && !isInShoreUpMode;
        
        // Shore Up Button
        if (isInShoreUpMode) {
            if (currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1) {
                shoreUpButton.setText("Finish Shoring (1 AP Total)");
                shoreUpButton.setEnabled(true);
            } else {
                shoreUpButton.setText("Cancel Shore Up");
                shoreUpButton.setEnabled(true);
            }
        } else {
            shoreUpButton.setText("Shore Up (1 AP)");
            ArrayList<game.simulation.tiles.GameTile> shoreUpTargets = getShoreUpTargetTiles(currentPlayer);
            shoreUpButton.setEnabled(currentPlayer.hasActionPoints() && shoreUpTargets != null && !shoreUpTargets.isEmpty());
        }

        // Special Action Button (Pilot Flight / Navigator Command)
        specialActionButton.setVisible(false); // Default to hidden
        if (currentPlayer.getRole().equals("Pilot") && currentPlayer.canUseFlightAbility() && !isInShoreUpMode) {
            specialActionButton.setText("Pilot Flight (1 AP)");
            specialActionButton.setVisible(true);
            specialActionButton.setEnabled(canAct);
        } else if (currentPlayer.getRole().equals("Navigator") && currentPlayer.canUseCommandAbility() && !isInShoreUpMode) {
            if (navigatorActionState == NavigatorActionState.NONE) {
                 specialActionButton.setText("Command Player (1 AP)");
                 specialActionButton.setVisible(true);
                 specialActionButton.setEnabled(canAct);
            } else if (navigatorActionState == NavigatorActionState.SELECTING_PLAYER_TO_MOVE || navigatorActionState == NavigatorActionState.SELECTING_TILE_FOR_OTHER) {
                 specialActionButton.setText("Cancel Command");
                 specialActionButton.setVisible(true);
                 specialActionButton.setEnabled(true); // Can always cancel
            }
        }
        
        // End Turn Button
        endTurnButton.setEnabled(true); // Can always choose to end turn
        endTurnButton.setText("End Turn (" + currentPlayer.getActionPoints() + " AP left)");
    }

    private void enterShoreUpMode() {
        if (currentPlayer == null) return;

        if (isInShoreUpMode) { // Currently in shore up mode, button means "Finish/Cancel"
            if (currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction == 1) {
                // Engineer used button to finish after 1 shore up
                System.out.println("Engineer finished shoring up with 1 tile.");
                currentPlayer.useActionPoint(); // Consume the single AP for the whole action
            }
            // For non-engineers, or engineer finishing/cancelling, just exit mode
            isInShoreUpMode = false;
            engineerShoredUpCountThisAction = 0;
            clearAllHighlights();
            highlightMovableTiles(); // Back to move highlights
            if (!currentPlayer.hasActionPoints()) {
                 handleTurnEnd();
            }
        } else { // Not in shore up mode, button means "Enter shore up mode"
            if (!currentPlayer.hasActionPoints()) {
                JOptionPane.showMessageDialog(this, "No action points left to shore up!", "Shore Up", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ArrayList<game.simulation.tiles.GameTile> shoreUpTargets = getShoreUpTargetTiles(currentPlayer);
            if (shoreUpTargets == null || shoreUpTargets.isEmpty()) {
                 JOptionPane.showMessageDialog(this, "No adjacent or current flooded tiles to shore up!", "Shore Up", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            isInShoreUpMode = true;
            engineerShoredUpCountThisAction = 0;
            highlightShoreUpTargets();
        }
        updateActionButtonsState();
    }

    private ArrayList<game.simulation.tiles.GameTile> getShoreUpTargetTiles(Player player) {
        if (player == null) return new ArrayList<>();
        return gameState.findShoreUpTargets(player);
    }
    
    // This is where the flood mechanism changes will be integrated
    private void handleTurnEnd() {
        if (currentPlayer == null) return;
        System.out.println(currentPlayer.getRole() + " ends their turn.");

        isInShoreUpMode = false; // Reset shore up mode
        engineerShoredUpCountThisAction = 0;
        navigatorActionState = NavigatorActionState.NONE; // Reset navigator state
        playerBeingMovedByNavigator = null;
        validNavigatorCommandTargetTiles = null;

        // Player-specific turn end resets (AP, abilities) are handled in gameState.nextTurn() or player.resetActionPoints()
        // currentPlayer.resetActionPoints(); // This is now in gameState.nextTurn()
        // Reset specific abilities if they are turn-based
        // if (currentPlayer instanceof Pilot) { // Not needed
        //    ((Pilot) currentPlayer).resetFlightAbility();
        // }
        // if (currentPlayer instanceof Navigator) { // Not needed
        //    ((Navigator) currentPlayer).resetCommandAbility();
        // }
        // resetActionPoints in Player.java already handles resetting these flags.
        // So, no specific calls to resetFlightAbility or resetCommandAbility are needed here
        // as player.resetActionPoints() is called by gameState.nextTurn().

        // --- FLOOD CARD DRAWING AND PROCESSING --- 
        System.out.println("DEBUG: Starting flood card draw phase.");
        ArrayList<game.simulation.tiles.GameTile> affectedTiles = gameState.drawAndProcessFloodCards();
        for (game.simulation.tiles.GameTile affectedMarkerTile : affectedTiles) {
            int[] pos = affectedMarkerTile.getPosition();
            if (pos != null && pos.length == 2) {
                // The affectedMarkerTile could be a sentinel for a sunk tile.
                // Check the actual state of the tile on the board from gameState.
                game.simulation.tiles.GameTile currentTileStateOnBoard = gameState.getBoardTiles()[pos[0]][pos[1]];
                if (currentTileStateOnBoard == null) { // Tile has actually sunk in gameState
                    System.out.println("DEBUG GameBoard: Tile at [" + pos[0] + "," + pos[1] + "] (" + affectedMarkerTile.getName() + ") has sunk. Updating UI.");
                    tileSunk(pos[0], pos[1]);
                } else { // Tile was flooded (but not sunk)
                     System.out.println("DEBUG GameBoard: Tile " + currentTileStateOnBoard.getName() + " at [" + pos[0] + "," + pos[1] + "] is now flooded. Refreshing UI.");
                    refreshTileAppearance(currentTileStateOnBoard, tilePanelHolder[pos[0]][pos[1]]);
                }
            }
        }
        refreshPawnDisplay(); // Pawns might need to be redrawn if their tile appearance changed (e.g. flooded background)

        // Check for game over by water level (or other conditions like critical tiles sunk)
        if (gameState.isGameOverByWaterLevel()) {
            JOptionPane.showMessageDialog(this, "Game Over! The island has succumbed to the waters! (Water Level Maxxed Out)", "Game Over", JOptionPane.ERROR_MESSAGE);
            // TODO: Disable further actions, show main menu, etc.
            parentPanel.showMenuPanel(); // Example action
            return; // Stop further turn processing, game is over
        }
        // TODO: Add other game over checks (Fools Landing sunk, both tiles for a treasure sunk before capture, etc.)

        // Advance to the next player
        Player nextPlayer = gameState.nextTurn();
        if (nextPlayer == null && !gameState.isGameOverByWaterLevel()) { // Should not happen unless all players removed or game ends another way
            JOptionPane.showMessageDialog(this, "Error: Could not get next player.", "Turn Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.currentPlayer = nextPlayer; // Update current player reference


        // UI updates for new turn
        clearAllHighlights();
        if (this.currentPlayer != null && !gameState.isGameOverByWaterLevel()){ // Check if game is not over
            highlightMovableTiles();
             System.out.println("Next turn: " + this.currentPlayer.getRole() + " with " + this.currentPlayer.getActionPoints() + " AP.");
        } else if (gameState.isGameOverByWaterLevel()) {
             System.out.println("Game is over due to water level. No next turn highlights.");
        }
        updateActionButtonsState();
        refreshPawnDisplay(); // Pawns might need refresh if players moved or for turn indication

    }
    
    // Placeholder for where a sunk tile's panel is updated
    private void tileSunk(int r, int c) {
        JPanel panel = tilePanelHolder[r][c];
        if (panel != null) {
            panel.removeAll(); 
            panel.setBackground(new Color(0, 50, 100)); // Dark blue for sunk
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK)); 
            JLabel sunkLabel = new JLabel("SUNK", SwingConstants.CENTER);
            sunkLabel.setForeground(Color.WHITE);
            panel.add(sunkLabel, BorderLayout.CENTER);
            
            for (MouseListener ml : panel.getMouseListeners()) { // Remove its own listener
                panel.removeMouseListener(ml);
            }
            panel.putClientProperty("gameTile", null); // Disassociate tile
            panel.revalidate();
            panel.repaint();
        }
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        if (currentPlayer == null || !currentPlayer.hasActionPoints() && !(isInShoreUpMode && currentPlayer.getRole().equals("Engineer") && engineerShoredUpCountThisAction ==1) && navigatorActionState == NavigatorActionState.NONE) {
            // No AP and not in a special state that allows action without AP (like Engineer's 2nd shore up click, or Nav cancel)
            System.out.println("Mouse click ignored: No AP or invalid state.");
            return;
        }

        JPanel clickedPanel = (JPanel) e.getSource();
        game.simulation.tiles.GameTile clickedTile = (game.simulation.tiles.GameTile) clickedPanel.getClientProperty("gameTile");

        if (clickedTile == null && navigatorActionState != NavigatorActionState.SELECTING_PLAYER_TO_MOVE) {
            // Clicked on a sunk or empty tile, and not in navigator's player selection phase (where they might click a panel with a player but no underlying 'GameTile' if it sunk under them)
            System.out.println("Clicked on an empty/sunk tile spot.");
            return;
        }

        // Handle Navigator's multi-step command action
        if (currentPlayer.getRole().equals("Navigator") && currentPlayer.canUseCommandAbility() ) { 
            if (navigatorActionState == NavigatorActionState.SELECTING_PLAYER_TO_MOVE) {
                if (clickedPanel.getComponentCount() > 0 && clickedTile != null) { 
                     Player targetPlayer = null;
                     // Find which player is on this tile (if any), excluding the Navigator themselves
                     for(Player p : gameState.getAllPlayers()){
                         if(p != currentPlayer && p.getPos()[0] == clickedTile.getPosition()[0] && p.getPos()[1] == clickedTile.getPosition()[1]){
                             targetPlayer = p;
                             break;
                         }
                     }

                    if (targetPlayer != null) {
                        playerBeingMovedByNavigator = targetPlayer;
                        navigatorActionState = NavigatorActionState.SELECTING_TILE_FOR_OTHER;
                        validNavigatorCommandTargetTiles = highlightTilesForNavigatorCommand(playerBeingMovedByNavigator);
                         if (validNavigatorCommandTargetTiles == null || validNavigatorCommandTargetTiles.isEmpty()) {
                            JOptionPane.showMessageDialog(this, playerBeingMovedByNavigator.getRole() + " has no valid moves for Navigator command.", "Navigator Command", JOptionPane.INFORMATION_MESSAGE);
                            navigatorActionState = NavigatorActionState.NONE; // Reset
                            playerBeingMovedByNavigator = null;
                            highlightMovableTiles(); // Back to navigator's own moves
                        } else {
                             JOptionPane.showMessageDialog(this, "Navigator: Selected " + playerBeingMovedByNavigator.getRole() + ". Now select a highlighted tile for them to move to.", "Navigator Command", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else if (clickedTile.getPosition()[0] == currentPlayer.getPos()[0] && clickedTile.getPosition()[1] == currentPlayer.getPos()[1]) {
                        // Navigator clicked their own tile again while in SELECTING_PLAYER_TO_MOVE mode: Cancel
                        navigatorActionState = NavigatorActionState.NONE;
                        playerBeingMovedByNavigator = null;
                        clearAllHighlights();
                        highlightMovableTiles();
                        JOptionPane.showMessageDialog(this, "Navigator command cancelled.", "Navigator Command", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                         JOptionPane.showMessageDialog(this, "Navigator: Please click on a tile occupied by another player.", "Navigator Command", JOptionPane.WARNING_MESSAGE);
                    }
                }
                updateActionButtonsState();
                return; // End click processing for this state
            } else if (navigatorActionState == NavigatorActionState.SELECTING_TILE_FOR_OTHER) {
                if (playerBeingMovedByNavigator != null && validNavigatorCommandTargetTiles != null && clickedTile != null && validNavigatorCommandTargetTiles.contains(clickedTile)) {
                    playerBeingMovedByNavigator.setPosition(clickedTile.getPosition());
                    currentPlayer.useActionPoint();
                    currentPlayer.markCommandAbilityUsed(); 
                    System.out.println("Navigator commanded " + playerBeingMovedByNavigator.getRole() + " to " + clickedTile.getName());
                    
                    navigatorActionState = NavigatorActionState.NONE;
                    playerBeingMovedByNavigator = null;
                    validNavigatorCommandTargetTiles = null;
                    
                    refreshPawnDisplay();
                    clearAllHighlights();
                    if (currentPlayer.hasActionPoints()) {
                        highlightMovableTiles();
                    } else {
                        handleTurnEnd();
                    }
                } else if (playerBeingMovedByNavigator != null && clickedTile != null && clickedTile.getPosition()[0] == playerBeingMovedByNavigator.getPos()[0] && clickedTile.getPosition()[1] == playerBeingMovedByNavigator.getPos()[1]){
                    // Clicked the selected player's tile again: cancel selection of tile for other, go back to selecting player
                    // Or more simply, cancel the whole command action
                    navigatorActionState = NavigatorActionState.NONE;
                    playerBeingMovedByNavigator = null;
                    validNavigatorCommandTargetTiles = null;
                    clearAllHighlights();
                    highlightMovableTiles();
                     JOptionPane.showMessageDialog(this, "Navigator command to move player cancelled.", "Navigator Command", JOptionPane.INFORMATION_MESSAGE);
                } else {
                     JOptionPane.showMessageDialog(this, "Navigator: Please click a valid highlighted (green) tile for " + playerBeingMovedByNavigator.getRole() + ", or click their current (blue) tile to cancel selection.", "Navigator Command", JOptionPane.WARNING_MESSAGE);
                }
                updateActionButtonsState();
                return; // End click processing for this state
            }
        }


        // Handle Shore Up Mode
        if (isInShoreUpMode) {
            if (clickedTile != null && clickedTile.getFloodState() && getShoreUpTargetTiles(currentPlayer).contains(clickedTile)) {
                gameState.shoreUpTile(clickedTile); // GameState handles setFlooded(false)
                refreshTileAppearance(clickedTile, (JPanel)e.getSource());
                System.out.println(currentPlayer.getRole() + " shored up " + clickedTile.getName());

                if (currentPlayer.getRole().equals("Engineer")) {
                    engineerShoredUpCountThisAction++;
                    if (engineerShoredUpCountThisAction < 2) { // First shore up for Engineer
                        // Engineer does not use AP yet, can shore up another or finish
                        JOptionPane.showMessageDialog(this, "Engineer shored up 1 tile. Select another flooded tile or click 'Finish Shoring Up'.", "Engineer Shore Up", JOptionPane.INFORMATION_MESSAGE);
                        highlightShoreUpTargets(); // Re-highlight remaining targets
                    } else { // Second shore up for Engineer or if they choose to finish early
                        currentPlayer.useActionPoint(); // One AP for two shoring ups
                        System.out.println("Engineer finished shoring up with 2 tiles for 1 AP.");
                        isInShoreUpMode = false;
                        engineerShoredUpCountThisAction = 0;
                        if (currentPlayer.hasActionPoints()) {
                            clearAllHighlights();
                            highlightMovableTiles();
                        } else {
                            handleTurnEnd();
                        }
                    }
                } else { // Non-Engineer player
                    currentPlayer.useActionPoint();
                    isInShoreUpMode = false; // Exit shore up mode after one action
                    if (currentPlayer.hasActionPoints()) {
                        clearAllHighlights();
                        highlightMovableTiles();
                    } else {
                        handleTurnEnd();
                    }
                }
            } else {
                 JOptionPane.showMessageDialog(this, "Invalid target for shoring up. Select a highlighted (orange) flooded tile.", "Shore Up", JOptionPane.WARNING_MESSAGE);
            }
            updateActionButtonsState();
            return; // End click processing for shore up mode
        }


        // Handle Player Movement
        if (clickedTile != null && gameState.findMovable(currentPlayer).contains(clickedTile)) {
            int[] oldPos = currentPlayer.getPos();
            currentPlayer.setPosition(clickedTile.getPosition());
            currentPlayer.useActionPoint();
            System.out.println(currentPlayer.getRole() + " moved to " + clickedTile.getName() + ". AP left: " + currentPlayer.getActionPoints());

            // Check for Pilot's special flight usage
            if (currentPlayer.getRole().equals("Pilot") && currentPlayer.canUseFlightAbility()) { 
                boolean standardMove = false; // Check if it was a normal adjacent/diagonal move
                int rOld = oldPos[0]; int cOld = oldPos[1];
                int rNew = clickedTile.getPosition()[0]; int cNew = clickedTile.getPosition()[1];
                if (Math.abs(rNew - rOld) <= 1 && Math.abs(cNew - cOld) <= 1) { // Adjacent or diagonal
                    if (! (Math.abs(rNew - rOld) == 1 && Math.abs(cNew - cOld) == 1 && !currentPlayer.getRole().equals("Explorer"))) { // not diagonal unless explorer
                       standardMove = true;
                    } else if (currentPlayer.getRole().equals("Explorer")) {
                        standardMove = true; // Explorer can move diagonally
                    }
                }
                if (!standardMove) { 
                    currentPlayer.markFlightAbilityUsed(); 
                    System.out.println("Pilot used their special flight ability.");
                }
            }
            
            refreshPawnDisplay();

            if (currentPlayer.hasActionPoints()) {
                highlightMovableTiles();
            } else {
                // No AP left, automatically end turn
                handleTurnEnd();
            }
        } else if (clickedTile != null) {
            System.out.println("Clicked on a non-movable tile: " + clickedTile.getName());
        }
        updateActionButtonsState();
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
} 