/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.ReceiptDAO;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import CODE.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
/**
 *
 * @author User
 */
public class ReceiptInvoicePrintout extends javax.swing.JFrame {
    
        private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ReceiptInvoicePrintout.class.getName());

    private int orderId = -1;
    private final ReceiptDAO receiptDAO = new ReceiptDAO();

    /**
     * Creates new form ReceiptInvoicePrintout
     */
    public ReceiptInvoicePrintout() {
        initComponents();
    }

    /**
     * Opens the receipt for a specific order and loads its data.
     */
    public ReceiptInvoicePrintout(int orderId) {
        this();
        this.orderId = orderId;
        loadReceipt();
    }

    private void loadReceipt() {
        if (orderId == -1) {
            return;
        }

        ReceiptDAO.ReceiptHeader header = receiptDAO.readReceiptHeader(orderId);
        if (header == null) {
            logger.log(java.util.logging.Level.WARNING, "No receipt data found for order {0}", orderId);
            JOptionPane.showMessageDialog(this, "Could not load receipt details for this order.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ReciptId.setText("#ORD-" + orderId);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a");
        String formattedDate = header.orderDate != null ? sdf.format(header.orderDate) : "-";
        Date.setText(formattedDate);
        DateAndTime.setText(formattedDate);

        Cashier.setText(header.cashierName);
        Subtotal.setText("Rs. " + String.format("%,.2f", header.subtotal));
        Discount.setText("Rs. " + String.format("%,.2f", header.discount));
        Total.setText("Rs. " + String.format("%,.2f", header.total));
        PaymentMethod.setText(header.paymentMethod);
        PaidinfullLabel.setText("Paid in full");

        if ("Cash".equals(header.paymentMethod)) {
            java.math.BigDecimal change = header.amountPaid.subtract(header.total);
            ShowBalance.setText("Cash received · Rs. " + String.format("%,.2f", header.amountPaid)
                    + " · Change Rs. " + String.format("%,.2f", change));
        } else {
            ShowBalance.setText("Paid by Card · Rs. " + String.format("%,.2f", header.amountPaid));
        }

        Object[][] items = receiptDAO.readReceiptItems(orderId);
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (Object[] row : items) {
            model.addRow(row);
        }
        showRefundStatus();     
    }
    

    // If this sale was already refunded, show it and turn off the Refund button
    private void showRefundStatus() {
        String sql = "SELECT payment_status FROM payments WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next() && "Refunded".equals(rs.getString("payment_status"))) {
                    PaidinfullLabel.setText("REFUNDED");
                    RefundBtn.setEnabled(false);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.WARNING, "Could not check refund status for order " + orderId, e);
        }
    }    

    // UPDATE - refunds this sale. In one transaction:
    // payment -> Refunded, order -> Cancelled, stock is put back, loyalty points are taken back.
    // Either all 4 changes are saved, or none of them are.
    private void refundSale() {
        if (orderId == -1) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Refund order #ORD-" + orderId + "?\nThe stock will be put back and the order will be cancelled.",
                "Refund Sale", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Payment -> Refunded (only if it is still Success)
            int rows;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE payments SET payment_status = 'Refunded' "
                  + "WHERE order_id = ? AND payment_status = 'Success'")) {
                ps.setInt(1, orderId);
                rows = ps.executeUpdate();
            }
            if (rows == 0) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "This sale has already been refunded.",
                        "Refund Sale", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Order -> Refunded
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE orders SET order_status = 'Refunded' WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // 3. Put the sold items back into stock
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE inventory i "
                  + "JOIN order_items oi ON oi.product_id = i.product_id "
                  + "SET i.quantity_on_hand = i.quantity_on_hand + oi.quantity "
                  + "WHERE oi.order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // 4. Take back the loyalty points (1 point per Rs. 100), never below 0
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE customers c "
                  + "JOIN orders o ON o.customer_id = c.customer_id "
                  + "SET c.loyalty_points = GREATEST(c.loyalty_points - FLOOR(o.total_amount / 100), 0) "
                  + "WHERE o.order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            conn.commit();

            PaidinfullLabel.setText("REFUNDED");
            RefundBtn.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Sale refunded. The stock has been put back.",
                    "Refund Sale", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            logger.log(java.util.logging.Level.SEVERE, "Failed to refund order " + orderId, e);
            JOptionPane.showMessageDialog(this, "Refund failed. Nothing was changed.\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
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
        ReceiptInvoiceLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        SaleCompleteLabel = new javax.swing.JLabel();
        SuccessfullLabel = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        ISSUEDlabel = new javax.swing.JLabel();
        DateAndTime = new javax.swing.JLabel();
        ShowBalance = new javax.swing.JLabel();
        PaidinfullLabel = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        RefundBtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        ReceiptLabel = new javax.swing.JLabel();
        ReciptId = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        Date = new javax.swing.JLabel();
        CashierLabel = new javax.swing.JLabel();
        Cashier = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        SubtotalLabel = new javax.swing.JLabel();
        Subtotal = new javax.swing.JLabel();
        DiscountLabel = new javax.swing.JLabel();
        Discount = new javax.swing.JLabel();
        TotalLabel = new javax.swing.JLabel();
        Total = new javax.swing.JLabel();
        PaymentMethodLabel = new javax.swing.JLabel();
        PaymentMethod = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(244, 246, 245));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        ReceiptInvoiceLabel.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        ReceiptInvoiceLabel.setText("Receipt Invoice");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(ReceiptInvoiceLabel)
                .addContainerGap(978, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(ReceiptInvoiceLabel)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        SaleCompleteLabel.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        SaleCompleteLabel.setText("Sale complete");

        SuccessfullLabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        SuccessfullLabel.setText("Payment was recorded successfull");
        SuccessfullLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jPanel4.setBackground(new java.awt.Color(244, 246, 245));

        ISSUEDlabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        ISSUEDlabel.setText("ISSUED");

        DateAndTime.setText("Date and time");

        ShowBalance.setText("Cash received · Rs. 9,000.00 · Change Rs. 250.00");

        PaidinfullLabel.setText("Paid in full");

        jButton1.setBackground(new java.awt.Color(20, 92, 82));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Print receipt");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        RefundBtn.setText("Refund Sale");
        RefundBtn.addActionListener(this::RefundBtnActionPerformed);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PaidinfullLabel)
                    .addComponent(ShowBalance)
                    .addComponent(ISSUEDlabel)
                    .addComponent(DateAndTime, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(RefundBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(130, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(ISSUEDlabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(DateAndTime, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(PaidinfullLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ShowBalance)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(RefundBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE))
                .addContainerGap(77, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SuccessfullLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SaleCompleteLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 557, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(SaleCompleteLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SuccessfullLabel)
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(61, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("SAMADHI POOJA BANDA");

        ReceiptLabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        ReceiptLabel.setText("Receipt");

        ReciptId.setText("ID");

        jLabel1.setText("Date");

        Date.setText("Date and time");

        CashierLabel.setText("Cashier");

        Cashier.setText("Cashier");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "ITEM", "QTY", "AMOUNT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Float.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        SubtotalLabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        SubtotalLabel.setText("Subtotal");

        Subtotal.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        Subtotal.setText("Subtotal");

        DiscountLabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        DiscountLabel.setText("Discount ");

        Discount.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        Discount.setText("Discount");

        TotalLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        TotalLabel.setText("Total");

        Total.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        Total.setText("Total");

        PaymentMethodLabel.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        PaymentMethodLabel.setText("Payment method");

        PaymentMethod.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        PaymentMethod.setText("Payment method");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(101, 101, 101))
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ReceiptLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(CashierLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(Date, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Cashier)
                    .addComponent(ReciptId, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(71, 71, 71))
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(PaymentMethod)
                                .addGroup(jPanel6Layout.createSequentialGroup()
                                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(DiscountLabel)
                                        .addComponent(TotalLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(198, 198, 198)
                                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(Total, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(Discount)
                                            .addComponent(Subtotal))))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(PaymentMethodLabel)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(SubtotalLabel))
                        .addContainerGap(24, Short.MAX_VALUE))))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ReceiptLabel)
                    .addComponent(ReciptId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(Date))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(CashierLabel)
                    .addComponent(Cashier))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SubtotalLabel)
                    .addComponent(Subtotal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DiscountLabel)
                    .addComponent(Discount))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(TotalLabel)
                    .addComponent(Total))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PaymentMethodLabel)
                    .addComponent(PaymentMethod))
                .addContainerGap(43, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
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

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            boolean complete = jTable1.print();
            if (!complete) {
                JOptionPane.showMessageDialog(this, "Printing was cancelled.",
                        "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (java.awt.print.PrinterException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to print receipt", e);
            JOptionPane.showMessageDialog(this, "Could not print receipt: " + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void RefundBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RefundBtnActionPerformed
        refundSale();
    }//GEN-LAST:event_RefundBtnActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new ReceiptInvoicePrintout().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Cashier;
    private javax.swing.JLabel CashierLabel;
    private javax.swing.JLabel Date;
    private javax.swing.JLabel DateAndTime;
    private javax.swing.JLabel Discount;
    private javax.swing.JLabel DiscountLabel;
    private javax.swing.JLabel ISSUEDlabel;
    private javax.swing.JLabel PaidinfullLabel;
    private javax.swing.JLabel PaymentMethod;
    private javax.swing.JLabel PaymentMethodLabel;
    private javax.swing.JLabel ReceiptInvoiceLabel;
    private javax.swing.JLabel ReceiptLabel;
    private javax.swing.JLabel ReciptId;
    private javax.swing.JButton RefundBtn;
    private javax.swing.JLabel SaleCompleteLabel;
    private javax.swing.JLabel ShowBalance;
    private javax.swing.JLabel Subtotal;
    private javax.swing.JLabel SubtotalLabel;
    private javax.swing.JLabel SuccessfullLabel;
    private javax.swing.JLabel Total;
    private javax.swing.JLabel TotalLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
