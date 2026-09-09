package dev.velocity;

import javax.swing.*;
import java.awt.event.*;

public final class Standalone {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Games");
            GamesPanel panel = new GamesPanel();
            frame.setContentPane(panel); frame.setSize(1200, 800); frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { panel.dispose(); System.exit(0); } });
            frame.setVisible(true);
        });
    }
}
