/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.ProductActionRenderer;
import CODE.ProductActionEditor;
import javax.swing.JOptionPane;

/**
 *
 * @author HP
 */
public class Inventory_and_product_management extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Inventory_and_product_management.class.getName());
    private final java.util.List<Integer> currentProductIds = new java.util.ArrayList<>(); // product_id for each table row
    /**
     * Creates new form Inventory_and_product_management
     */
    public Inventory_and_product_management() {
        initComponents();
            jTable1.setRowHeight(40);
            jTable1.getColumn("ACTION").setCellRenderer(new ProductActionRenderer());
            jTable1.getColumn("ACTION").setCellEditor(new ProductActionEditor(new ProductActionEditor.ProductActions() {
            @Override
            public void onEdit(int modelRow) {
                stopTableEditing();
                int productId = currentProductIds.get(modelRow);
                Add_Product dialog = new Add_Product(Inventory_and_product_management.this, true, productId);
                dialog.setLocationRelativeTo(Inventory_and_product_management.this);
                dialog.setVisible(true); // the table refreshes itself after a successful update
            }
            @Override
            public void onDelete(int modelRow) {
                stopTableEditing();
                int productId = currentProductIds.get(modelRow);
                String productName = String.valueOf(jTable1.getModel().getValueAt(modelRow, 0));
                int confirm = JOptionPane.showConfirmDialog(Inventory_and_product_management.this,
                        "Remove \"" + productName + "\" from the product list?\n"
                      + "It will no longer appear in Billing, but past sales records are kept.",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    discontinueProduct(productId, productName);
                }
            }
        }));
        loadProducts();
    }
 //READ   
    public void loadProducts() {
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        currentProductIds.clear();

        String sql = "SELECT p.product_id, p.product_name, p.sku, i.quantity_on_hand, i.reorder_level, p.unit_price "
                   + "FROM products p "
                   + "JOIN inventory i ON p.product_id = i.product_id "
                   + "WHERE p.status = 'Active'";

        try (java.sql.Connection conn = CODE.DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("product_name"),
                    rs.getString("sku"),
                    rs.getInt("quantity_on_hand"),
                    rs.getInt("reorder_level"),
                    rs.getDouble("unit_price"),
                    ""
                });
                currentProductIds.add(rs.getInt("product_id")); // same position as the table row
            }
        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to load products", e);
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Makes sure the Edit/Delete button cell is no longer "being edited" before the table reloads
    private void stopTableEditing() {
        if (jTable1.isEditing()) {
            jTable1.getCellEditor().stopCellEditing();
        }
    }

    // DELETE (soft delete): marks the product as Discontinued instead of removing it,
    // because past orders (order_items) still point to it
    private void discontinueProduct(int productId, String productName) {
        String sql = "UPDATE products SET status = 'Discontinued' WHERE product_id = ?";

        try (java.sql.Connection conn = CODE.DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();

            loadProducts(); // it disappears because the list only shows Active products
            JOptionPane.showMessageDialog(this,
                    "\"" + productName + "\" was removed from the product list.",
                    "Product Removed", JOptionPane.INFORMATION_MESSAGE);

        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to discontinue product", e);
            JOptionPane.showMessageDialog(this, "Failed to remove product: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
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

        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btnAdd = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        btnRestock = new javax.swing.JButton();

        jLabel17.setText("20");

        jLabel18.setText("20");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(246, 245, 242));

        btnAdd.setBackground(new java.awt.Color(11, 107, 109));
        btnAdd.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnAdd.setForeground(new java.awt.Color(255, 255, 255));
        btnAdd.setText("+   Add Product");
        btnAdd.addActionListener(this::btnAddActionPerformed);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel2.setText("Product Catelogue");

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "PRODUCT", "SKU", "STOCK", "REORDER LEVEL ", "PRICE", "ACTION"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("INVENTORY & PRODUCT MANAGEMENT ");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 475, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );

        btnRestock.setBackground(new java.awt.Color(255, 153, 102));
        btnRestock.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRestock.setForeground(new java.awt.Color(255, 255, 255));
        btnRestock.setText("Restock");
        btnRestock.addActionListener(this::btnRestockActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRestock, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1124, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(27, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(61, 61, 61))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnRestock, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(114, Short.MAX_VALUE))
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

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
       // Create an instance of the Add_Product JDialog
        Add_Product addProductDialog = new Add_Product(this, true);
        
        // Center the dialog relative to the main window
        addProductDialog.setLocationRelativeTo(this);
        
        // Display the dialog window
        addProductDialog.setVisible(true);
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnRestockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestockActionPerformed
        // Make sure the Edit/Delete cell isn't still active before the table reloads
        if (jTable1.isEditing()) {
            jTable1.getCellEditor().stopCellEditing();
        }

        // 1. A product must be selected
        int viewRow = jTable1.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please click a product in the table first.",
                    "No Product Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = jTable1.convertRowIndexToModel(viewRow);
        int productId = currentProductIds.get(modelRow);
        String productName = String.valueOf(jTable1.getModel().getValueAt(modelRow, 0));
        Object currentStock = jTable1.getModel().getValueAt(modelRow, 2);

        // 2. Ask how many units arrived
        String input = JOptionPane.showInputDialog(this,
                "Product: " + productName
                + "\nCurrent stock: " + currentStock
                + "\n\nHow many units are you adding?",
                "Restock Product", JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            return; // cancelled
        }

        int qty;
        try {
            qty = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a whole number (e.g. 25).",
                    "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be more than 0.",
                    "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. UPDATE: add to the existing stock (never creates a duplicate product)
        String sql = "UPDATE inventory "
                   + "SET quantity_on_hand = quantity_on_hand + ?, last_restocked_at = NOW() "
                   + "WHERE product_id = ?";

        try (java.sql.Connection conn = CODE.DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, qty);
            pstmt.setInt(2, productId);
            int updated = pstmt.executeUpdate();

            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "No inventory record was found for this product.",
                        "Restock Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            loadProducts(); // refresh the STOCK column
            JOptionPane.showMessageDialog(this,
                    qty + " units added to " + productName + ".",
                    "Restocked", JOptionPane.INFORMATION_MESSAGE);

        } catch (java.sql.SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to restock product", e);
            JOptionPane.showMessageDialog(this, "Failed to restock: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnRestockActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Inventory_and_product_management().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnRestock;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
