/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import CODE.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author User
 */
public class ReceiptDAO {

    // Simple holder for the receipt header fields
    public static class ReceiptHeader {
        public Timestamp orderDate;
        public String cashierName;
        public String paymentMethod;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;
        public BigDecimal amountPaid;
    }

    // READ #1 - populates ReceiptLabel/Date/Cashier/Subtotal/Discount/Total/PaymentMethod/ShowBalance
    public ReceiptHeader readReceiptHeader(int orderId) {
        String sql = "SELECT o.order_date, u.full_name AS cashier_name, " +
                     "o.subtotal_amount, o.discount_amount, o.total_amount, " +
                     "p.payment_method, p.amount_paid " +
                     "FROM orders o " +
                     "JOIN users u ON o.user_id = u.user_id " +
                     "JOIN payments p ON p.order_id = o.order_id " +
                     "WHERE o.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ReceiptHeader h = new ReceiptHeader();
                    h.orderDate = rs.getTimestamp("order_date");
                    h.cashierName = rs.getString("cashier_name");
                    h.paymentMethod = rs.getString("payment_method");
                    h.subtotal = rs.getBigDecimal("subtotal_amount");
                    h.discount = rs.getBigDecimal("discount_amount");
                    h.total = rs.getBigDecimal("total_amount");
                    h.amountPaid = rs.getBigDecimal("amount_paid");
                    return h;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // TEMPORARY
        }
        return null;
    }

    // READ #2 - populates jTable1 (ITEM, QTY, AMOUNT)
    public Object[][] readReceiptItems(int orderId) {
        String sql = "SELECT p.product_name, oi.quantity, oi.line_total " +
                     "FROM order_items oi " +
                     "JOIN products p ON oi.product_id = p.product_id " +
                     "WHERE oi.order_id = ?";

        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getFloat("line_total")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // TEMPORARY
        }

        return rows.toArray(new Object[0][]);
    }
}
