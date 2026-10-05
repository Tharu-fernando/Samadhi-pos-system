/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
/**
 *
 * @author User
 */
public class ManageStaff extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManageStaff.class.getName());

    /**
     * Creates new form ManageStaff
     */
    public ManageStaff() {
        initComponents();
        CreateAccountBtn.addActionListener(evt -> openCreateAccount());
        setupActionColumn();
        setupSearch();
        loadUsers();
    }

    private void openCreateAccount() {
        Create_user dialog = new Create_user(this);   // pass this list so it refreshes after saving
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void loadUsers() {
        
        if (Usertable.isEditing()) {
            Usertable.getCellEditor().cancelCellEditing();
        }        
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) Usertable.getModel();
        model.setRowCount(0);
//READ
        String sql = "SELECT user_id, full_name, username, role, status, created_at "
                   + "FROM users ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String createdDate = rs.getTimestamp("created_at").toLocalDateTime().toLocalDate().toString();
                model.addRow(new Object[]{
                    String.valueOf(rs.getInt("user_id")),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("role"),
                    createdDate,
                    rs.getString("status"),
                    ""
                });
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to load staff list", e);
            JOptionPane.showMessageDialog(this, "Failed to load staff: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    // Puts the edit / Del buttons into the "Actions" column
    private void setupActionColumn() {
        Usertable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{"Staff ID", "Full Name", "Username", "Role", "Created Date", "Status", "Actions"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;   // only the Actions column can be clicked
            }
        });

        Usertable.setRowHeight(32);
        Usertable.getColumnModel().getColumn(6).setPreferredWidth(140);
        Usertable.getColumnModel().getColumn(6).setCellRenderer(new CODE.ProductActionRenderer());
        Usertable.getColumnModel().getColumn(6).setCellEditor(
            new CODE.ProductActionEditor(new CODE.ProductActionEditor.ProductActions() {
                @Override
                public void onEdit(int modelRow) {
                    editUser(modelRow);
                }

                @Override
                public void onDelete(int modelRow) {
                    deleteUser(modelRow);
                }
            }));
    }
    

    private static final String SEARCH_HINT = "Search by name, username, role or status";

    // Connects the search box to the table: rows are filtered while you type
    private void setupSearch() {
        sorter = new javax.swing.table.TableRowSorter<>(
                (javax.swing.table.DefaultTableModel) Usertable.getModel());
        Usertable.setRowSorter(sorter);
        sorter.setSortable(6, false);   // don't sort by the Actions column

        // Grey hint text that disappears when you click in the box
        SearchUser.setText(SEARCH_HINT);
        SearchUser.setForeground(java.awt.Color.GRAY);
        SearchUser.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (SearchUser.getText().equals(SEARCH_HINT)) {
                    SearchUser.setText("");
                    SearchUser.setForeground(java.awt.Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (SearchUser.getText().isEmpty()) {
                    SearchUser.setForeground(java.awt.Color.GRAY);
                    SearchUser.setText(SEARCH_HINT);
                }
            }
        });

        // Filter again every time the text changes
        SearchUser.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterUsers(); }
        });

        SearchUser.addActionListener(evt -> filterUsers());   // pressing Enter also searches
    }

    private void filterUsers() {
        String text = SearchUser.getText().trim();

        if (text.isEmpty() || text.equals(SEARCH_HINT)) {
            sorter.setRowFilter(null);   // show everyone
            return;
        }

        // Case-insensitive match in: Staff ID (0), Full Name (1), Username (2), Role (3), Status (5)
        sorter.setRowFilter(javax.swing.RowFilter.regexFilter(
                "(?i)" + java.util.regex.Pattern.quote(text), 0, 1, 2, 3, 5));
    }    

    // UPDATE - opens the Create Account form in edit mode
    private void editUser(int modelRow) {
        int userId = Integer.parseInt(String.valueOf(Usertable.getModel().getValueAt(modelRow, 0)));

        Create_user form = new Create_user(this, userId);
        form.setLocationRelativeTo(this);
        form.setVisible(true);
    }

    // DELETE - removes a staff account.
    // If the person has already processed orders, MySQL blocks the delete
    // (orders.user_id needs them), so we offer to make the account Inactive instead.
    private void deleteUser(int modelRow) {
        int userId = Integer.parseInt(String.valueOf(Usertable.getModel().getValueAt(modelRow, 0)));
        String name = String.valueOf(Usertable.getModel().getValueAt(modelRow, 1));

        if (userId == CODE.Session.getCurrentUserId()) {
            JOptionPane.showMessageDialog(this, "You can't delete the account you are signed in with.",
                    "Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete the staff account of \"" + name + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Staff account deleted.");
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            int deactivate = JOptionPane.showConfirmDialog(this,
                    name + " has processed orders, so the account can't be deleted.\n"
                  + "Make the account Inactive instead? (They won't be able to sign in.)",
                    "Cannot Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (deactivate == JOptionPane.YES_OPTION) {
                deactivateUser(userId);
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to delete staff account", e);
            JOptionPane.showMessageDialog(this, "Failed to delete: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        loadUsers();   // refresh the table
    }

    // Soft delete - the account stays (for the sales records) but can't sign in
    private void deactivateUser(int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE users SET status = 'Inactive' WHERE user_id = ?")) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "The account is now Inactive.");
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to deactivate staff account", e);
            JOptionPane.showMessageDialog(this, "Failed to deactivate: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        CreateAccountBtn = new javax.swing.JButton();
        SearchUser = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        Usertable = new javax.swing.JTable();
        btnSearch = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(244, 246, 245));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Manage Staff");

        CreateAccountBtn.setBackground(new java.awt.Color(20, 92, 82));
        CreateAccountBtn.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        CreateAccountBtn.setForeground(new java.awt.Color(255, 255, 255));
        CreateAccountBtn.setText("+ Create Account");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(CreateAccountBtn)
                .addGap(46, 46, 46))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(29, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(CreateAccountBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35))
        );

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setForeground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        Usertable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Staff ID", "Full Name", "Username", "Role", "Created Date", "Status", "Actions"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(Usertable);

        btnSearch.setText("Search");
        btnSearch.addActionListener(this::btnSearchActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(SearchUser, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(47, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(SearchUser, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                    .addComponent(btnSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 469, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 39, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        filterUsers();
    }//GEN-LAST:event_btnSearchActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ManageStaff().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CreateAccountBtn;
    private javax.swing.JTextField SearchUser;
    private javax.swing.JTable Usertable;
    private javax.swing.JButton btnSearch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    private javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> sorter;
}
