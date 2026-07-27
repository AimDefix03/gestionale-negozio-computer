package ui.modern;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

final class Ui {
    static final Color BACKGROUND = new Color(244, 248, 255);
    static final Color SIDEBAR = new Color(255, 255, 255);
    static final Color SURFACE = new Color(255, 255, 255);
    static final Color SURFACE_LIGHT = new Color(232, 241, 255);
    static final Color SURFACE_SOFT = new Color(247, 250, 255);
    static final Color BORDER = new Color(213, 225, 244);
    static final Color BORDER_STRONG = new Color(24, 119, 242);
    static final Color TEXT = new Color(23, 32, 51);
    static final Color MUTED = new Color(95, 111, 135);
    static final Color ACCENT = new Color(24, 119, 242);
    static final Color DANGER = new Color(220, 38, 38);
    static final Color WARNING = new Color(217, 119, 6);
    static final Color SUCCESS = new Color(22, 163, 74);

    private Ui() {
    }

    static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(BACKGROUND);
        return panel;
    }

    static JPanel card(LayoutManager layout) {
        JPanel panel = new RoundedPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    static JPanel compactCard(LayoutManager layout) {
        JPanel panel = card(layout);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    static JLabel title(String text, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(font(Font.BOLD, size));
        return label;
    }

    static JLabel text(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(font(Font.PLAIN, 13));
        return label;
    }

    static JLabel eyebrow(String text) {
        JLabel label = text(text);
        label.setForeground(ACCENT);
        label.setFont(font(Font.BOLD, 12));
        return label;
    }

    static JLabel sectionLabel(String text) {
        JLabel label = text(text.toUpperCase());
        label.setForeground(MUTED);
        label.setFont(font(Font.BOLD, 11));
        label.setBorder(new EmptyBorder(4, 6, 2, 6));
        return label;
    }

    static JButton primaryButton(String text) {
        return button(text, ACCENT, Color.WHITE, new Color(12, 101, 228), new Color(12, 101, 228));
    }

    static JButton secondaryButton(String text) {
        return button(text, SURFACE, TEXT, SURFACE_LIGHT, BORDER);
    }

    static JButton dangerButton(String text) {
        return button(text, new Color(255, 241, 242), new Color(185, 28, 28), new Color(254, 226, 226), new Color(252, 165, 165));
    }

    static JButton ghostButton(String text) {
        return button(text, new Color(245, 249, 255), MUTED, SURFACE_LIGHT, BORDER);
    }

    static JButton navButton(String text) {
        JButton button = ghostButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return button;
    }

    static void setButtonActive(JButton button, boolean active) {
        button.setSelected(active);
        button.setForeground(active ? ACCENT : MUTED);
        button.repaint();
    }

    static JTextField textField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleTextField(field);
        return field;
    }

    static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(SURFACE_SOFT);
        comboBox.setForeground(TEXT);
        comboBox.setFont(font(Font.PLAIN, 13));
        comboBox.setBorder(new EmptyBorder(7, 10, 7, 10));
        comboBox.setFocusable(false);
    }

    static JScrollPane scrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBackground(SURFACE);
        return scrollPane;
    }

    static void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(226, 235, 249));
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setFont(font(Font.PLAIN, 13));
        table.setSelectionBackground(new Color(210, 229, 255));
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setDefaultRenderer(Object.class, new PaddedTableCellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        JTableHeader header = table.getTableHeader();
        header.setBackground(SURFACE_SOFT);
        header.setForeground(MUTED);
        header.setFont(font(Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
    }

    static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    static Font font(int style, int size) {
        return new Font("Inter", style, size);
    }

    private static JButton button(String text, Color background, Color foreground, Color hover, Color border) {
        JButton button = new RoundedButton(text, background, hover, border);
        button.setForeground(foreground);
        button.setFont(font(Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(9, 18, 9, 18));
        button.setMinimumSize(new Dimension(0, 36));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 36));
        return button;
    }

    private static void styleTextField(JTextField field) {
        field.setBackground(SURFACE_SOFT);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(font(Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private static final class RoundedPanel extends JPanel {
        private static final int ARC = 24;

        private RoundedPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            graphics2D.setColor(BORDER);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class PaddedTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (isSelected) {
                component.setBackground(table.getSelectionBackground());
                component.setForeground(table.getSelectionForeground());
                return component;
            }
            component.setBackground(row % 2 == 0 ? SURFACE : SURFACE_SOFT);
            component.setForeground(TEXT);
            return component;
        }
    }

    private static final class RoundedButton extends JButton {
        private static final int ARC = 36;
        private final Color normalBackground;
        private final Color hoverBackground;
        private final Color borderColor;

        private RoundedButton(String text, Color normalBackground, Color hoverBackground, Color borderColor) {
            super(text);
            this.normalBackground = normalBackground;
            this.hoverBackground = hoverBackground;
            this.borderColor = borderColor;
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fillColor = getModel().isSelected() ? SURFACE_LIGHT : getModel().isRollover() ? hoverBackground : normalBackground;
            Color strokeColor = getModel().isSelected() ? ACCENT : borderColor;
            graphics2D.setColor(fillColor);
            graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            graphics2D.setColor(strokeColor);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }
    }
}
