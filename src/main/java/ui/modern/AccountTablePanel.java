package ui.modern;

import model.UserAccount;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class AccountTablePanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Username", "Ruolo"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<UserAccount> accounts = new ArrayList<>();

    AccountTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 180, 120);
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setAccounts(List<UserAccount> accounts) {
        this.accounts = new ArrayList<>(accounts);
        model.setRowCount(0);
        for (UserAccount account : accounts) {
            model.addRow(new Object[]{account.username(), account.role()});
        }
        table.clearSelection();
    }

    List<UserAccount> getSelectedAccounts() {
        int[] selectedRows = table.getSelectedRows();
        List<UserAccount> selectedAccounts = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            selectedAccounts.add(accounts.get(table.convertRowIndexToModel(selectedRow)));
        }
        return selectedAccounts;
    }
}
