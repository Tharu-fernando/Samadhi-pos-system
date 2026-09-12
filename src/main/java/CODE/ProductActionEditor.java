package CODE;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author HP
 */
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class ProductActionEditor extends AbstractCellEditor implements TableCellEditor {
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
    private int currentRow;

    public ProductActionEditor(ProductActions listener) {
        JButton editBtn = new JButton("edit");
        JButton delBtn = new JButton("Del");
        editBtn.setBackground(new Color(147, 197, 253));
        delBtn.setBackground(new Color(220, 38, 38));
        delBtn.setForeground(Color.WHITE);
        panel.add(editBtn);
        panel.add(delBtn);

        editBtn.addActionListener(e -> { fireEditingStopped(); listener.onEdit(currentRow); });
        delBtn.addActionListener(e -> { fireEditingStopped(); listener.onDelete(currentRow); });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        currentRow = table.convertRowIndexToModel(row);
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    public interface ProductActions {
        void onEdit(int modelRow);
        void onDelete(int modelRow);
    }
}
