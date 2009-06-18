/*
 * PriorDialog.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beauti.treespanel;

import dr.app.beauti.options.*;
import dr.gui.chart.Axis;
import dr.gui.chart.JChart;
import dr.gui.chart.LinearAxis;
import dr.gui.chart.PDFPlot;
import dr.math.*;
import dr.util.NumberFormatter;
import dr.evolution.datatype.*;
import org.virion.jam.components.RealNumberField;
import org.virion.jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: PriorDialog.java,v 1.4 2006/09/05 13:29:34 rambaut Exp $
 */
public class GenerateTreeDialog {

    private JFrame frame;

    public static enum MethodTypes {
            NJ,
            UPGMA
    }


    JTextField nameField;
    JComboBox partitionCombo;
    JComboBox methodCombo;

    OptionsPanel optionPanel;

    public GenerateTreeDialog(JFrame frame) {
        this.frame = frame;

        nameField = new JTextField();
        nameField.setColumns(20);

        methodCombo = new JComboBox(MethodTypes.values());
        partitionCombo = new JComboBox();

        optionPanel = new OptionsPanel(12, 12);
        optionPanel.addComponentWithLabel("Name:", nameField);
        optionPanel.addComponentWithLabel("Data Partition:", partitionCombo);
        optionPanel.addComponentWithLabel("Construction Method:", methodCombo);


    }

    public int showDialog(BeautiOptions options) {

        JOptionPane optionPane = new JOptionPane(optionPanel,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                null,
                null);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        partitionCombo.removeAllItems();
        for (DataPartition partition : options.dataPartitions) {
            partitionCombo.addItem(partition);
        }
        final JDialog dialog = optionPane.createDialog(frame, "Construct New Tree");
        dialog.pack();

        dialog.setVisible(true);

        int result = JOptionPane.CANCEL_OPTION;
        Integer value = (Integer) optionPane.getValue();
        if (value != null && value != -1) {
            result = value;
        }

        return result;
    }

    public String getName() {
        return nameField.getText();
    }

    public DataPartition getDataPartition() {
        return (DataPartition)partitionCombo.getSelectedItem();
    }
    public MethodTypes getMethodType() {
        return (MethodTypes)methodCombo.getSelectedItem();
    }
}