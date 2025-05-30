package game;

// import com.formdev.flatlaf.intellijthemes.FlatNordIJTheme;
import game.panel.ParentPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException; // Keep for potential future use, though not directly used by this structure now

public class GameRunner {

    public static void main(String[] args) {
        // It's good practice to set up Look and Feel before creating any Swing components
//        FlatNordIJTheme.setup();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame mainFrame = new JFrame("Forbidden Island");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                // Create the ParentPanel which will manage MenuPanel and GameBoard
                ParentPanel parentPanel = new ParentPanel();
                mainFrame.setContentPane(parentPanel);

                // Pack the frame to fit the preferred sizes of its components (ParentPanel and its children)
                mainFrame.pack(); 
                // Center the frame on the screen
                mainFrame.setLocationRelativeTo(null);
                // Make the frame visible
                mainFrame.setVisible(true);
            }
        });
    }
}
