package ui.modern;

import javax.swing.*;
import java.awt.*;

public final class AppTheme {
    private AppTheme() {
    }

    public static void install() {
        installLookAndFeel();
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 999);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.selectionBackground", new Color(210, 229, 255));
        UIManager.put("Table.selectionForeground", Ui.TEXT);
        UIManager.put("Panel.background", Ui.BACKGROUND);
        UIManager.put("defaultFont", Ui.font(Font.PLAIN, 13));
        UIManager.put("Button.font", Ui.font(Font.BOLD, 13));
        UIManager.put("Label.font", Ui.font(Font.PLAIN, 13));
        UIManager.put("ComboBox.font", Ui.font(Font.PLAIN, 13));
        UIManager.put("TextField.font", Ui.font(Font.PLAIN, 13));
        UIManager.put("PasswordField.font", Ui.font(Font.PLAIN, 13));
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("ProgressBar.foreground", Ui.ACCENT);
        UIManager.put("ProgressBar.background", Ui.SURFACE_LIGHT);
    }

    private static void installLookAndFeel() {
        try {
            Class<?> flatLightLaf = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLightLaf.getMethod("setup").invoke(null);
        } catch (ReflectiveOperationException e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                UIManager.put("Panel.background", Ui.BACKGROUND);
            }
        }
    }
}
