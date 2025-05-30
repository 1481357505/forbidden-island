package game.panel;

import game.simulation.GameState;
import game.simulation.GameBoard;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
// import javax.swing.CardLayout; // No longer using CardLayout

public class ParentPanel extends JPanel {
    private MenuPanel menuPanel = null;
    private HelpPanel helpPanel = null;
    private SettingsPanel settingsPanel = null;
    private game.panel.GameBoard gameBoard = null;
    // private CardLayout cardLayout; // Removed
    private Image backgroundImage;

    public ParentPanel() {
        setLayout(new BorderLayout()); // Set BorderLayout for ParentPanel
        menuPanel = new MenuPanel(this);
        add(menuPanel, BorderLayout.CENTER); // Add menuPanel initially to the center
        // gameBoard will be created and added in startGame

        // Load the background image
        try {
            // Old way:
            // backgroundImage = ImageIO.read(new File("Forbidden_Island/src/main/resources/Images/FloodMeter/Water_Meter_Water.png"));
            // New way:
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/Images/FloodMeter/Water_Meter_Water.png"));
        } catch (IOException e) {
            e.printStackTrace();
            backgroundImage = null; // Handle error: image won't be drawn
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public void startGame(GameState gameState) {
        if (menuPanel != null) {
            remove(menuPanel); // Remove menuPanel
            menuPanel.setVisible(false); // Also set invisible just in case
        }
        if (gameBoard != null) {
            remove(gameBoard); // Remove old game board if any
        }

        try {
            // It seems GameBoard's constructor in your setup might throw IOException
            // And it also might require gameState to be processed (e.g., gameState.nextTurn())
            if (gameState != null) {
                 // gameState.nextTurn(); // This was in ParentPanel.startGame before, ensure it's called if needed by GameBoard or GameState construction logic for the first turn.
                                     // My reconstructed GameBoard doesn't call it, it expects GameState to provide current player.
                                     // ParentPanel previously called gameState.nextTurn() *before* new GameBoard().
                                     // Let's assume GameState constructor or a call before startGame handles initial turn setup.
            }
            gameBoard = new game.panel.GameBoard(gameState, this); // Create new game board
            add(gameBoard, BorderLayout.CENTER); // Add gameBoard to the center
            gameBoard.setVisible(true);
            gameBoard.requestFocusInWindow();
        } catch (Exception e) { // Catching general Exception if GameBoard throws IOException
            e.printStackTrace();
            // Optionally, show an error message and revert to menu
            showMenuPanelOnError();
            return;
        }
        
        revalidate();
        repaint();
    }

    public void showMenuPanel() {
        if (gameBoard != null) {
            remove(gameBoard);
            gameBoard.setVisible(false);
        }
        if (menuPanel == null) { // If menu was somehow disposed or not created
            menuPanel = new MenuPanel(this);
        }
        // Ensure menuPanel is not already added, though remove/add strategy should handle it.
        if (!isAncestorOf(menuPanel)) {
             add(menuPanel, BorderLayout.CENTER); // Add menuPanel back to the center
        }
        menuPanel.setVisible(true);
        
        revalidate();
        repaint();
    }
    
    private void showMenuPanelOnError() {
        // Simplified version for error case, ensures menu is shown
        if (gameBoard != null) {
            remove(gameBoard);
        }
        if (menuPanel == null) {
            menuPanel = new MenuPanel(this);
        }
        if (!isAncestorOf(menuPanel)) { // Check if menuPanel is already added
            add(menuPanel, BorderLayout.CENTER);
        }
        menuPanel.setVisible(true);
        revalidate();
        repaint();
    }

    public void toggleHelpPanel() {
        if (helpPanel == null) {
            try {
                helpPanel = new HelpPanel();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        } else if (!helpPanel.isVisible()) {
            helpPanel.setVisible(true);
        } else {
            helpPanel.setVisible(false);
        }
    }

    public void hideMenuPanel() {
        if (menuPanel != null) {
            // No dispose, and remove is handled by startGame or showMenuPanel
            // menuPanel.setVisible(false); // This would be an alternative to remove/add strategy
        }
    }
}
