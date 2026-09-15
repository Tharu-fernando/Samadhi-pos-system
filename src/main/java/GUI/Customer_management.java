/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.CustomerActionRenderer;
import CODE.CustomerActionEditor;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import CODE.DBConnection;
/**
 *
 * @author tharu
 */
public class Customer_management extends javax.swing.JFrame {

    
    private boolean selectionMode;
    private final java.util.List<Integer> currentCustomerIds = new java.util.ArrayList<>();
    private java.util.function.Consumer<Integer> onCustomerSelected;
    /**
     * Creates new form Customer_management
     */
    public Customer_management() {
        this(false, null);
    }

    public Customer_management(boolean selectionMode) {
        this(selectionMode, null);
    }

    public Customer_management(boolean selectionMode, java.util.function.Consumer<Integer> onCustomerSelected) {
        initComponents();
        this.selectionMode = selectionMode;
        this.onCustomerSelected = onCustomerSelected;
        jTable1.setRowHeight(40);
        btnadd.addActionListener(evt -> openNewCustomerDialog());
        jButton1.addActionListener(evt -> searchCustomers());
        jTextField1.addActionListener(evt -> searchCustomers());

        jTable1.getColumn("Action").setCellRenderer(new CustomerActionRenderer());
        jTable1.getColumn("Action").setCellEditor(new CustomerActionEditor(new CustomerActionEditor.CustomerActions() {
            @Override
            public void onEdit(int modelRow) {
                // TODO: open New_customer dialog pre-filled with this row's data
            }
            @Override
            public void onDelete(int modelRow) {
                int confirm = JOptionPane.showConfirmDialog(Customer_management.this,
                        "Delete this customer?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
                    model.removeRow(modelRow);
                }
            }
        }));

        btnSelectCustomer.setVisible(selectionMode);
        btnSelectCustomer.addActionListener(evt -> {
            int row = jTable1.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the table first.",
                        "No Customer Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int customerId = currentCustomerIds.get(row);
            openBillingForCustomer(customerId);
        });

        if (selectionMode) {
            jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() != 2) {
                        return; // require double-click so a normal single click still just selects the row
                    }
                    int row = jTable1.rowAtPoint(evt.getPoint());
                    int col = jTable1.columnAtPoint(evt.getPoint());
                    if (row < 0 || col == 4) {
                        return; // column 4 is "Action" — let its own Edit/Delete buttons handle that click
                    }
                    int customerId = currentCustomerIds.get(row);
                    openBillingForCustomer(customerId);
                }
            });
        }

        loadCustomers();
    }
    
        public void loadCustomers() {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            currentCustomerIds.clear();

            String sql = "SELECT customer_id, full_name, phone_number, loyalty_tier, loyalty_points FROM customers";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String name = rs.getString("full_name");
                    model.addRow(new Object[]{
                        (name == null || name.isEmpty()) ? "-" : name,
                        rs.getString("phone_number"),
                        rs.getString("loyalty_tier"),
                        rs.getInt("loyalty_points"),
                        ""
                    });
                    currentCustomerIds.add(rs.getInt("customer_id"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to load customers: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        
        private void searchCustomers() {
            String term = jTextField1.getText().trim();
            if (term.isEmpty()) {
                loadCustomers();
                return;
            }

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            currentCustomerIds.clear();

            String sql = "SELECT customer_id, full_name, phone_number, loyalty_tier, loyalty_points "
                       + "FROM customers WHERE phone_number LIKE ? OR full_name LIKE ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String likeTerm = "%" + term + "%";
                pstmt.setString(1, likeTerm);
                pstmt.setString(2, likeTerm);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("full_name");
                        model.addRow(new Object[]{
                            (name == null || name.isEmpty()) ? "-" : name,
                            rs.getString("phone_number"),
                            rs.getString("loyalty_tier"),
                            rs.getInt("loyalty_points"),
                            ""
                        });
                        currentCustomerIds.add(rs.getInt("customer_id"));
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Search failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void openBillingForCustomer(int customerId) {
            if (onCustomerSelected != null) {
                onCustomerSelected.accept(customerId);
            } else {
                new POS_Billing(customerId).setVisible(true);
                this.dispose();
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
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnadd = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        btnSelectCustomer = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(246, 245, 242));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("CUSTOMER MANAGEMENT");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Customer Directory");

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "NAME", "PHONE", "LOYALTY", "POINTS", "Action"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        btnadd.setBackground(new java.awt.Color(11, 107, 109));
        btnadd.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnadd.setForeground(new java.awt.Color(255, 255, 255));
        btnadd.setText("+   New Customer");

        jButton1.setText("Search");

        btnSelectCustomer.setText("Select Customer");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnadd)
                        .addGap(78, 78, 78))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnSelectCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 952, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addContainerGap(56, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(btnadd, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)))
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField1)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 436, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnSelectCustomer, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
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

     private void openNewCustomerDialog() {
        New_customer dialog = new New_customer(this, true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Customer_management.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Customer_management.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Customer_management.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Customer_management.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Customer_management().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSelectCustomer;
    private javax.swing.JButton btnadd;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
