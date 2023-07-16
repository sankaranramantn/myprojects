package com.sankyman;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

class BooleanRenderer extends JCheckBox implements TableCellRenderer
{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        
            Boolean isValueSelected = Boolean.valueOf(String.valueOf(value));

            super.setSelected(isValueSelected);
            super.setText(isValueSelected.toString());

            return this;
        }

}

class FfmpegCheckbox extends JCheckBox implements ItemListener
{
    public FfmpegCheckbox(boolean isSelected)
    {
        super(String.valueOf(isSelected), isSelected);
        addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
        
        setText(String.valueOf(isSelected));
    }
}

class FfmpegChoicesRenderer extends JLabel implements TableCellRenderer
{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        
            FfmpegChoices ffmpegChoices = (FfmpegChoices)value;

            super.setText(ffmpegChoices.getSelectedItem());

            return this;
        }

}

class FfmpegCellEditor extends AbstractCellEditor implements TableCellEditor
{
    Component editorComponent;

    public FfmpegCellEditor()
    {
    }


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        if(value instanceof Boolean)
        {
            Boolean boolValue = (Boolean)value;
            FfmpegCheckbox ffmpegCheckbox = new FfmpegCheckbox(boolValue);
            editorComponent = ffmpegCheckbox;

            ffmpegCheckbox.addActionListener((e) -> {
                fireEditingStopped();
            });

            
        }
        else if(value instanceof FfmpegChoices)
        {
            FfmpegChoices choices = (FfmpegChoices)value;
            JComboBox<String> comboBoxEditor = new JComboBox<String>(choices.getItems());
            comboBoxEditor.setSelectedItem(choices.getSelectedItem());

            editorComponent = comboBoxEditor;

            comboBoxEditor.addActionListener((e) -> {
                fireEditingStopped();
            });

        }
        else
        {
            JTextField textField = new JTextField(String.valueOf(value));

            editorComponent = textField;

            textField.addActionListener((e) -> {
                fireEditingStopped();
            });

        }
        
        return editorComponent;
    }

    @Override
    public Object getCellEditorValue() {

        if(editorComponent instanceof JCheckBox)
        {
            JCheckBox checkBox = (JCheckBox)editorComponent;

            return Boolean.valueOf(checkBox.isSelected());
        }
        else if(editorComponent instanceof JComboBox)
        {
            JComboBox<String> comboBox = (JComboBox<String>)editorComponent;

            return String.valueOf(comboBox.getSelectedItem());

        }
        else
        {
            JTextField textField = (JTextField)editorComponent;

            return textField.getText();
        }
    }
}

public class FfmpegOptionsTable extends JTable {
    public FfmpegOptionsTable() {
        super(new FfpmegOptions());

        //create our own editor tailored to Ffmpeg options
        getColumnModel().getColumn(1).setCellEditor(new FfmpegCellEditor());
    }

    //override this to provide our own renderer, checkbox in case of boolean value and text box otherwise
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {

        if(column == 1) {
            TableModel optionsModel = getModel();


            Object value = optionsModel.getValueAt(row, column);

            if(value instanceof Boolean)
            {
                return new BooleanRenderer();
            }
            else if(value instanceof FfmpegChoices)
            {
                return new FfmpegChoicesRenderer();
            }
        }

        return super.getCellRenderer(row, column);

    }    
}
