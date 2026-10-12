package ui.modern;

import javax.swing.*;
import java.awt.*;

class SidebarSection extends JPanel {
    private final JButton headerButton;
    private final JPanel itemsPanel = Ui.panel(new GridLayout(0, 1, 0, 9));
    private final String title;
    private boolean expanded;

    SidebarSection(String title, boolean expanded, JButton... buttons) {
        this.title = title;
        this.expanded = expanded;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        headerButton = Ui.navButton("");
        headerButton.setHorizontalAlignment(SwingConstants.LEFT);
        headerButton.addActionListener(event -> toggle());

        itemsPanel.setOpaque(false);
        for (JButton button : buttons) {
            button.setHorizontalAlignment(SwingConstants.LEFT);
            itemsPanel.add(button);
        }

        add(headerButton, BorderLayout.NORTH);
        add(itemsPanel, BorderLayout.CENTER);
        updateState();
    }

    private void toggle() {
        expanded = !expanded;
        updateState();
        revalidate();
        repaint();
    }

    private void updateState() {
        headerButton.setText((expanded ? "v  " : ">  ") + title);
        itemsPanel.setVisible(expanded);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
