/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import java.math.BigDecimal;
import java.sql.*;


/**
 *
 * @author User
 */
public class PaymentDAO {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PaymentDAO.class.getName());
    // READ - populates AmountDueLable / TotalePriceLable when PayementScreen opens
    public BigDecimal readAmountDue(int orderId) {
        String sql = "SELECT total_amount FROM orders WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total_amount");
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error reading amount due for order " + orderId, e);
        }
        return BigDecimal.ZERO;
    }

    // CREATE - called from ConfirmBtn once it's wired up
    // Why the last createPayment() failed (shown to the cashier on the payment screen)
    private String lastError = null;

    public String getLastError() {
        return lastError;
    }

    // CREATE - records the payment, deducts stock and completes the order (all or nothing)
    public boolean createPayment(int orderId, String paymentMethod, BigDecimal amountPaid) {
        lastError = null;

        String insertPayment = "INSERT INTO payments (order_id, payment_method, amount_paid, payment_status) "
                              + "VALUES (?, ?, ?, 'Success')";
        String selectItems = "SELECT oi.product_id, oi.quantity, p.product_name "
                           + "FROM order_items oi JOIN products p ON p.product_id = oi.product_id "
                           + "WHERE oi.order_id = ?";
        // Only deducts when there is enough stock, so stock can never go below zero
        String updateInventory = "UPDATE inventory SET quantity_on_hand = quantity_on_hand - ? "
                               + "WHERE product_id = ? AND quantity_on_hand >= ?";
        String updateOrderStatus = "UPDATE orders SET order_status = 'Completed' WHERE order_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Record the payment
            try (PreparedStatement ps = conn.prepareStatement(insertPayment)) {
                ps.setInt(1, orderId);
                ps.setString(2, paymentMethod);
                ps.setBigDecimal(3, amountPaid);
                ps.executeUpdate();
            }

            // 2. Deduct stock for every item in this order
            try (PreparedStatement selectPs = conn.prepareStatement(selectItems)) {
                selectPs.setInt(1, orderId);
                try (ResultSet rs = selectPs.executeQuery()) {
                    while (rs.next()) {
                        int productId = rs.getInt("product_id");
                        int quantity = rs.getInt("quantity");
                        String productName = rs.getString("product_name");

                        try (PreparedStatement updatePs = conn.prepareStatement(updateInventory)) {
                            updatePs.setInt(1, quantity);
                            updatePs.setInt(2, productId);
                            updatePs.setInt(3, quantity);
                            int updated = updatePs.executeUpdate();

                            if (updated == 0) {
                                // Not enough stock: stop here, and the catch block below undoes everything
                                throw new SQLException("Not enough stock for \"" + productName
                                        + "\" (trying to sell " + quantity + ").");
                            }
                        }
                    }
                }
            }

            // 3. Mark the order completed
            try (PreparedStatement ps = conn.prepareStatement(updateOrderStatus)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            lastError = e.getMessage();
            logger.log(java.util.logging.Level.SEVERE, "Error creating payment for order " + orderId, e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
