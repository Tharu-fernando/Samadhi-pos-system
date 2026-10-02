/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

package GUI;

import CODE.OrderDAO;
import java.math.BigDecimal;
import javax.swing.JOptionPane;
import CODE.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class POS_Billing extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(POS_Billing.class.getName());

    private final java.util.List<OrderDAO.CartItem> cart = new java.util.ArrayList<>();
    private final OrderDAO orderDAO = new OrderDAO();
    private String currentCategory = null;
    private Integer customerId;
    /**
     * Creates new form POS_Billing
     */

    public POS_Billing() {
        initComponents();
        productGridPanel.setLayout(new java.awt.GridLayout(0, 3, 20, 20));
        // Current Order panel: stack the cart rows from top to bottom
        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.Y_AXIS));
        jPanel10.setBackground(java.awt.Color.WHITE);
        refreshCartPanel();
        loadProductGrid(null, null);

        jButton3.addActionListener(evt -> { currentCategory = null; refreshGrid(); });
        jButton4.addActionListener(evt -> { currentCategory = "Incense"; refreshGrid(); });
        jButton5.addActionListener(evt -> { currentCategory = "Oil Lamps"; refreshGrid(); });
        jButton6.addActionListener(evt -> { currentCategory = "Statues"; refreshGrid(); });
        jButton7.addActionListener(evt -> { currentCategory = "Flowers & Garlands"; refreshGrid(); });
    }

    public POS_Billing(int customerId) {
        this(); // kept for any caller that already knows the customer (e.g. Customer_management's picker)
        this.customerId = customerId;
    }

    private void refreshGrid() {
        loadProductGrid(currentCategory, Searchbar.getText());
    }

    private void loadProductGrid(String categoryName, String keyword) {
        if (keyword != null && keyword.startsWith("Search ")) {
            keyword = null; // ignore the placeholder text still sitting in the search bar
        }

        productGridPanel.removeAll();

        java.util.List<OrderDAO.ProductRow> products = orderDAO.searchProducts(categoryName, keyword);
        for (OrderDAO.ProductRow product : products) {
            productGridPanel.add(createProductCard(product));
        }

        productGridPanel.revalidate();
        productGridPanel.repaint();
    }

    private javax.swing.JPanel createProductCard(OrderDAO.ProductRow product) {
        javax.swing.JPanel card = new javax.swing.JPanel();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.setBackground(java.awt.Color.WHITE);
        card.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 220, 220)));

        javax.swing.JLabel nameLabel = new javax.swing.JLabel(product.productName);
        nameLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        javax.swing.JLabel priceLabel = new javax.swing.JLabel(String.format("Rs. %.2f", product.unitPrice));
        priceLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        javax.swing.JButton addBtn = new javax.swing.JButton("Add To Order");
        addBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        addBtn.addActionListener(evt -> addProductToCart(product));

        card.add(javax.swing.Box.createVerticalStrut(10));
        card.add(nameLabel);
        card.add(priceLabel);
        card.add(addBtn);
        card.add(javax.swing.Box.createVerticalStrut(10));

        return card;
    }

    private void addProductToCart(OrderDAO.ProductRow product) {
        for (OrderDAO.CartItem existing : cart) {
                    if (java.util.Objects.equals(existing.productId, product.productId)) {
                        existing.quantity++;
                        refreshCartPanel();
                        return;
                    }
                }

                OrderDAO.CartItem item = new OrderDAO.CartItem();
                item.productId = product.productId;
                item.productName = product.productName;
                item.unitPrice = product.unitPrice;
                item.quantity = 1;
                cart.add(item);
                refreshCartPanel();
    }
    
    // Rebuild the "Current Order" list and the Subtotal / Discount / Total labels
    private void refreshCartPanel() {
        jPanel10.removeAll();

        if (cart.isEmpty()) {
            javax.swing.JLabel emptyLabel = new javax.swing.JLabel("No items added yet");
            emptyLabel.setForeground(java.awt.Color.GRAY);
            emptyLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            jPanel10.add(javax.swing.Box.createVerticalStrut(20));
            jPanel10.add(emptyLabel);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderDAO.CartItem item : cart) {
            jPanel10.add(createCartRow(item));
            subtotal = subtotal.add(item.lineTotal());
        }
        jPanel10.add(javax.swing.Box.createVerticalGlue()); // keeps the rows at the top

        jPanel10.revalidate();
        jPanel10.repaint();

        SumOfPrice.setText(String.format("Rs. %.2f", subtotal));
        DiscountPrice.setText("At checkout"); // loyalty discount is applied on Order Review
        TotalPrice.setText(String.format("Rs. %.2f", subtotal));
    }

    // One row in Current Order: name + (qty x price) on the left, + and - buttons on the right
    private javax.swing.JPanel createCartRow(OrderDAO.CartItem item) {
        javax.swing.JPanel row = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
        row.setBackground(java.awt.Color.WHITE);
        row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(230, 230, 230)),
                javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        // Left side: item name on top, "qty x price" below
        javax.swing.JLabel nameLabel = new javax.swing.JLabel(item.productName);
        nameLabel.setToolTipText(item.productName); // full name on hover if it's cut off
        javax.swing.JLabel priceLabel = new javax.swing.JLabel(
                item.quantity + " x " + String.format("Rs. %.2f", item.unitPrice));

        javax.swing.JPanel textPanel = new javax.swing.JPanel(new java.awt.GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(priceLabel);
        textPanel.setPreferredSize(new java.awt.Dimension(120, textPanel.getPreferredSize().height));

        // Right side: + and - buttons
        javax.swing.JButton plusBtn = new javax.swing.JButton("+");
        javax.swing.JButton minusBtn = new javax.swing.JButton("-");
        plusBtn.setPreferredSize(new java.awt.Dimension(42, 28));
        minusBtn.setPreferredSize(new java.awt.Dimension(42, 28));
        plusBtn.addActionListener(evt -> changeQuantity(item, 1));
        minusBtn.addActionListener(evt -> changeQuantity(item, -1));

        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 2));
        buttonPanel.setOpaque(false);
        buttonPanel.add(plusBtn);
        buttonPanel.add(minusBtn);

        row.add(textPanel, java.awt.BorderLayout.CENTER);
        row.add(buttonPanel, java.awt.BorderLayout.EAST);

        // Stop the row from stretching taller than it needs to
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // + adds 1, - removes 1. If quantity reaches 0, the item is removed from the order.
    private void changeQuantity(OrderDAO.CartItem item, int change) {
        item.quantity += change;
        if (item.quantity <= 0) {
            cart.remove(item);
        }
        refreshCartPanel();
    }    
    
    // Called by Order Review's "Edit Order" button.
    // The Dashboard shows Billing inside its own window, so we bring forward
    // the window that is really showing it, not this empty POS_Billing frame.
    public void returnToBilling() {
        java.awt.Window shownIn = javax.swing.SwingUtilities.getWindowAncestor(jPanel1);
        if (shownIn != null) {
            shownIn.toFront();
            shownIn.requestFocus();
        }
        jPanel1.revalidate();
        jPanel1.repaint();
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
        jButton2 = new javax.swing.JButton();
        Searchbar = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        SelectedItemPanel = new javax.swing.JScrollPane();
        jPanel10 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        SumOfPrice = new javax.swing.JLabel();
        DiscountPrice = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        TotalPrice = new javax.swing.JLabel();
        CheckoutBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        productGridPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(245, 243, 240));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Billing Terminal");

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton2.setText("Get Selling Report");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 328, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 18, Short.MAX_VALUE)))
                .addContainerGap())
        );

        Searchbar.setText("Search temple items, incense, oil lamps, brassware, statues...");
        Searchbar.addActionListener(this::SearchbarActionPerformed);

        jButton1.setBackground(new java.awt.Color(20, 92, 82));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Search");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Current Order");

        SelectedItemPanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        jLabel9.setText("Item name");

        jLabel10.setText("Price");

        jButton9.setText("+");
        jButton9.addActionListener(this::jButton9ActionPerformed);

        jButton10.setText("-");
        jButton10.addActionListener(this::jButton10ActionPerformed);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
                .addComponent(jButton9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton10)
                .addGap(30, 30, 30))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10))
                    .addComponent(jButton10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(281, Short.MAX_VALUE))
        );

        SelectedItemPanel.setViewportView(jPanel10);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Discount");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setText("Subtotal");

        SumOfPrice.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        SumOfPrice.setText("Price");

        DiscountPrice.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        DiscountPrice.setText("price");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Total Amount");

        TotalPrice.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        TotalPrice.setText("Total price");

        CheckoutBtn.setBackground(new java.awt.Color(20, 92, 82));
        CheckoutBtn.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        CheckoutBtn.setForeground(new java.awt.Color(255, 255, 255));
        CheckoutBtn.setText("Go to Checkout");
        CheckoutBtn.addActionListener(this::CheckoutBtnActionPerformed);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(SelectedItemPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(DiscountPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(TotalPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(SumOfPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(CheckoutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SelectedItemPanel)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(SumOfPrice))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(DiscountPrice))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(TotalPrice))
                .addGap(46, 46, 46)
                .addComponent(CheckoutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(41, 41, 41))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));

        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton3.setText("All Items");

        jButton4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton4.setText("Incense");

        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton5.setText("Oil Lamps");

        jButton6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton6.setText("Statues");

        jButton7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton7.setText("Flower & Garlands");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton7)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton4)
                    .addComponent(jButton5)
                    .addComponent(jButton6)
                    .addComponent(jButton7))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        productGridPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout productGridPanelLayout = new javax.swing.GroupLayout(productGridPanel);
        productGridPanel.setLayout(productGridPanelLayout);
        productGridPanelLayout.setHorizontalGroup(
            productGridPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 771, Short.MAX_VALUE)
        );
        productGridPanelLayout.setVerticalGroup(
            productGridPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(productGridPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(productGridPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jScrollPane1.setViewportView(jPanel5);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(Searchbar, javax.swing.GroupLayout.PREFERRED_SIZE, 690, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(Searchbar)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 11, Short.MAX_VALUE))
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
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void SearchbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchbarActionPerformed
        refreshGrid();
      
    }//GEN-LAST:event_SearchbarActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        refreshGrid();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void CheckoutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CheckoutBtnActionPerformed
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please add at least one item before checking out.",
                "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (customerId == null) {
            Integer resolvedId = resolveCustomerByPhone();
            if (resolvedId == null) {
                return; // cashier cancelled the phone lookup/creation
            }
            customerId = resolvedId;
        }

        OrderReview review = new OrderReview(cart, customerId);
        review.setBillingWindow(this);
        review.setLocationRelativeTo(this);
        review.setVisible(true);
    }//GEN-LAST:event_CheckoutBtnActionPerformed

    private Integer resolveCustomerByPhone() {
        String phone = JOptionPane.showInputDialog(this,
            "Enter customer's phone number:", "Customer Lookup", JOptionPane.PLAIN_MESSAGE);

        if (phone == null) {
            return null; // cashier cancelled
        }
        phone = phone.trim();
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Phone number cannot be empty.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String findSql = "SELECT customer_id FROM customers WHERE phone_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to look up customer by phone", e);
            JOptionPane.showMessageDialog(this, "Failed to look up customer: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        int confirmCreate = JOptionPane.showConfirmDialog(this,
            "No customer found with phone " + phone + ". Create a new customer?",
            "New Customer", JOptionPane.YES_NO_OPTION);
        if (confirmCreate != JOptionPane.YES_OPTION) {
            return null;
        }

        String name = JOptionPane.showInputDialog(this,
            "Customer's name (optional):", "New Customer", JOptionPane.PLAIN_MESSAGE);

        String insertSql = "INSERT INTO customers (phone_number, full_name) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, phone);
            if (name == null || name.trim().isEmpty()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, name.trim());
            }
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to create new customer", e);
            JOptionPane.showMessageDialog(this, "Failed to create customer: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }    
    
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton10ActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new POS_Billing().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CheckoutBtn;
    private javax.swing.JLabel DiscountPrice;
    private javax.swing.JTextField Searchbar;
    private javax.swing.JScrollPane SelectedItemPanel;
    private javax.swing.JLabel SumOfPrice;
    private javax.swing.JLabel TotalPrice;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel productGridPanel;
    // End of variables declaration//GEN-END:variables
}
