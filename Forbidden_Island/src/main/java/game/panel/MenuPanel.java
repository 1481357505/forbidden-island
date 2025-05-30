package game.panel;

import game.simulation.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Objects;


public class MenuPanel extends JPanel {
    private Font Kurale, PNBold, PNRegular;
    private HelpPanel helpPanel = null;
    private ParentPanel parentPanel;

    public MenuPanel(ParentPanel p) {
        this.parentPanel = p;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1010, 535));

        try {
            Kurale = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("Fonts/Kurale-Regular.ttf"))).deriveFont(12f);
            PNBold = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("Fonts/Proxima Nova Bold.otf"))).deriveFont(15f);
            PNRegular = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("Fonts/ProximaNova-Regular.otf"))).deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Kurale);
        } catch (FontFormatException | IOException e) {
            System.out.println("Error loading fonts: " + e.getMessage());
        }

        JLabel background = new JLabel();
        background.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("Images/TitleScreen.png"))));
        add(background, BorderLayout.CENTER);
        background.setLayout(null);
        
        JLabel difficultyLabel = new JLabel("Difficulty:");
        String[] difficultyStrings = {"--Select--", "Novice", "Normal", "Elite", "Legendary"};
        JComboBox<String> difficultyDropdown = new JComboBox<>(difficultyStrings);
        difficultyLabel.setBounds(161, 270, 149, 25);
        difficultyLabel.setFont(PNBold);
        difficultyLabel.setBackground(new Color(191, 105, 86));
        difficultyLabel.setForeground(new Color(255, 255, 255));
        difficultyDropdown.setBounds(160, 295, 150, 25);
        background.add(difficultyLabel);
        background.add(difficultyDropdown);

        JLabel playerLabel = new JLabel("Number of Players:");
        JSpinner playerSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 4, 1));
        playerSpinner.setBounds(690, 295, 150, 25);
        playerLabel.setBounds(691, 270, 148, 25);
        playerLabel.setBackground(new Color(191, 105, 86));
        playerLabel.setFont(PNBold);
        playerLabel.setForeground(new Color(255, 255, 255));
        background.add(playerLabel);
        background.add(playerSpinner);

        JButton help = new JButton("How to Play");
        help.setBounds(423, 375, 150, 45);
        help.setFont(PNBold);
        background.add(help);

        JButton play = new JButton("Play");
        play.setBounds(423, 425, 150, 45);
        play.setFont(PNBold);
        background.add(play);

        help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentPanel.toggleHelpPanel();
            }
        });

        play.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int difficulty = difficultyDropdown.getSelectedIndex();
                int numPlayers = (int) playerSpinner.getValue();
                System.out.println("Number of Players: " + numPlayers + "\nDifficulty: " + difficulty);
                if (difficulty == 0) {
                    JOptionPane.showMessageDialog(MenuPanel.this, "You did not select a difficulty! \nPlease go back and select one!", "Missing Arguments!", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    GameState gameState = new GameState(difficulty, numPlayers);
                    gameState.nextTurn();
                    parentPanel.startGame(gameState);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MenuPanel.this, "Error starting game: " + ex.getMessage(), "Game Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
