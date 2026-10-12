package main;

import ui.modern.AppFrame;
import ui.modern.AppTheme;

import javax.swing.*;

public class LoginSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppTheme.install();
            new AppFrame().setVisible(true);
        });
    }
}
