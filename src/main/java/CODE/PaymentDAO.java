/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import CODE.DBConnection;
import java.math.BigDecimal;
import java.sql.*;


/**
 *
 * @author User
 */
public class PaymentDAO {

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
            e.printStackTrace(); // TEMPORARY
        }
        return BigDecimal.ZERO;
    }

    // CREATE - called from ConfirmBtn once it's wired up
    public boolean createPayment(int orderId, String paymentMethod, BigDecimal amountPaid) {
        String sql = "INSERT INTO payments (order_id, payment_method, amount_paid) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setString(2, paymentMethod); // "Cash" or "Card" — matches setCashPaymentMode(boolean)
            ps.setBigDecimal(3, amountPaid);
            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace(); // TEMPORARY
            return false;
        }
    }
}
