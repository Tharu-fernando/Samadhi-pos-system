/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author tharu
 */
public class OrderDAO {
 
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(OrderDAO.class.getName());
 
    // Simple holder for a product row shown in the billing screen
    public static class ProductRow {
        public int productId;
        public String productName;
        public BigDecimal unitPrice;
        public int quantityOnHand;
    }
 
    // Simple holder for one line the cashier has added to the cart
    public static class CartItem {
        public int productId;
        public String productName;
        public BigDecimal unitPrice;
        public int quantity;
 
        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
 
    // ================= READ =================
 
    /** All active products, optionally filtered by category name and/or search keyword. */
    public List<ProductRow> searchProducts(String categoryName, String keyword) {
        List<ProductRow> rows = new ArrayList<>();
 
        StringBuilder sql = new StringBuilder(
                "SELECT p.product_id, p.product_name, p.unit_price, i.quantity_on_hand " +
                "FROM products p " +
                "JOIN inventory i ON p.product_id = i.product_id " +
                "LEFT JOIN product_categories c ON p.category_id = c.category_id " +
                "WHERE p.status = 'Active'");
 
        List<Object> params = new ArrayList<>();
 
        if (categoryName != null && !categoryName.equalsIgnoreCase("All Items")) {
            sql.append(" AND c.category_name = ?");
            params.add(categoryName);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND p.product_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
 
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
 
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductRow row = new ProductRow();
                    row.productId = rs.getInt("product_id");
                    row.productName = rs.getString("product_name");
                    row.unitPrice = rs.getBigDecimal("unit_price");
                    row.quantityOnHand = rs.getInt("quantity_on_hand");
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Error searching products", e);
        }
 
        return rows;
    }
 
    // ================= CREATE =================
 
    /**
     * Creates a Pending order with its line items in a single transaction.
     * Stock is NOT deducted here — per project design, that happens only after
     * payment is confirmed (see PaymentDAO.createPayment).
     *
     * @return the new order_id, or -1 on failure
     */
    public int createOrder(int customerId, int userId, List<CartItem> items,
                            BigDecimal discountAmount, String fulfillmentMode) {
 
        if (items == null || items.isEmpty()) {
            logger.log(java.util.logging.Level.WARNING, "Attempted to create an order with no items.");
            return -1;
        }
 
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotal = subtotal.add(item.lineTotal());
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        BigDecimal total = subtotal.subtract(discountAmount);
 
        String insertOrder = "INSERT INTO orders " +
                "(customer_id, user_id, order_status, subtotal_amount, discount_amount, total_amount) " +
                "VALUES (?, ?, 'Pending', ?, ?, ?)";
        String insertItem = "INSERT INTO order_items " +
                "(order_id, product_id, quantity, unit_price_at_sale, line_total) " +
                "VALUES (?, ?, ?, ?, ?)";
 
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
 
            int newOrderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setInt(2, userId);
                ps.setBigDecimal(3, subtotal);
                ps.setBigDecimal(4, discountAmount);
                ps.setBigDecimal(5, total);
                ps.executeUpdate();
 
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newOrderId = keys.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated order ID.");
                    }
                }
            }
 
            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (CartItem item : items) {
                    ps.setInt(1, newOrderId);
                    ps.setInt(2, item.productId);
                    ps.setInt(3, item.quantity);
                    ps.setBigDecimal(4, item.unitPrice);
                    ps.setBigDecimal(5, item.lineTotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
 
            conn.commit();
            return newOrderId;
 
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(java.util.logging.Level.SEVERE, "Error creating order", e);
            return -1;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}

