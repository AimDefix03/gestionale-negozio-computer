package ui.modern;

import model.AuditEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

class AuditTablePanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Data", "Utente", "Ruolo", "Azione", "Oggetto", "Dettagli"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    AuditTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 140, 110, 90, 150, 170, 260);
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setEvents(List<AuditEvent> events) {
        model.setRowCount(0);
        for (AuditEvent event : events) {
            model.addRow(new Object[]{
                    event.timestamp().format(FORMATTER),
                    event.actor(),
                    event.role(),
                    event.action(),
                    event.target(),
                    event.details()
            });
        }
    }
}
