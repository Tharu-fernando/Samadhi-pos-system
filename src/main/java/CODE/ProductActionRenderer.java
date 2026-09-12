/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import javax.swing.*;
import java.awt.*;

public class ProductActionRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
    public ProductActionRenderer() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 4, 0));
        JButton editBtn = new JButton("edit");
        JButton delBtn = new JButton("Del");
        editBtn.setBackground(new Color(147, 197, 253));
        delBtn.setBackground(new Color(220, 38, 38));
        delBtn.setForeground(Color.WHITE);
        add(editBtn);
        add(delBtn);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}


