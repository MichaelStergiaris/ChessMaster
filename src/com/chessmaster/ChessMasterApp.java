package com.chessmaster;

import com.chessmaster.ui.MainFrame;
import com.chessmaster.ui.Theme;

import javax.swing.*;

/**
 * Entry point for ChessMaster Desktop Application.
 */
public final class ChessMasterApp {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            Theme.applyGlobalLookAndFeel();
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
