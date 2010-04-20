/*
 * CorrelationPanel.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.tracer.traces;

import dr.gui.chart.*;
import dr.inference.trace.Trace;
import dr.inference.trace.TraceDistribution;
import dr.inference.trace.TraceList;
import dr.stats.Variate;
import org.virion.jam.framework.Exportable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A panel that displays correlation plots of 2 traces
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: CorrelationPanel.java,v 1.1.1.2 2006/04/25 23:00:09 rambaut Exp $
 */
public class CorrelationPanel extends JPanel implements Exportable {

    private ChartSetupDialog chartSetupDialog = null;

    private JChart correlationChart = new JChart(new LinearAxis(), new LinearAxis());
    private JChartPanel chartPanel = new JChartPanel(correlationChart, null, "", "");
    private JLabel messageLabel = new JLabel("No data loaded");

    private JCheckBox sampleCheckBox = new JCheckBox("Sample only");
    private JCheckBox pointsCheckBox = new JCheckBox("Draw as points");
    private JCheckBox translucencyCheckBox = new JCheckBox("Use translucency");

    private TraceList tl1 = null;
    private TraceList tl2 = null;
    private int traceIndex1 = -1;
    private int traceIndex2 = -1;

    private String name1;
    private String name2;

    /**
     * Creates new CorrelationPanel
     */
    public CorrelationPanel(final JFrame frame) {

        setOpaque(false);
        setMinimumSize(new Dimension(300, 150));
        setLayout(new BorderLayout());

        add(messageLabel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setOpaque(false);
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolBar.setFloatable(false);

        JButton chartSetupButton = new JButton("Axes...");
        chartSetupButton.putClientProperty(
                "Quaqua.Button.style", "placard"
        );
        chartSetupButton.setFont(UIManager.getFont("SmallSystemFont"));
        toolBar.add(chartSetupButton);

        sampleCheckBox.setOpaque(false);
        sampleCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
        sampleCheckBox.setSelected(true);
        toolBar.add(sampleCheckBox);

        pointsCheckBox.setOpaque(false);
        pointsCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
        toolBar.add(pointsCheckBox);

        translucencyCheckBox.setOpaque(false);
        translucencyCheckBox.setFont(UIManager.getFont("SmallSystemFont"));
        toolBar.add(translucencyCheckBox);

        toolBar.add(new JToolBar.Separator(new Dimension(8, 8)));

        add(messageLabel, BorderLayout.NORTH);
        add(toolBar, BorderLayout.SOUTH);
        add(chartPanel, BorderLayout.CENTER);

        chartSetupButton.addActionListener(
                new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (chartSetupDialog == null) {
                            chartSetupDialog = new ChartSetupDialog(frame, true, true,
                                    Axis.AT_MAJOR_TICK, Axis.AT_MAJOR_TICK, Axis.AT_MAJOR_TICK, Axis.AT_MAJOR_TICK);
                        }

                        chartSetupDialog.showDialog(correlationChart);
                        validate();
                        repaint();
                    }
                }
        );

        ActionListener listener = new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                setupChart();
            }
        };
        sampleCheckBox.addActionListener(listener);
        pointsCheckBox.addActionListener(listener);
        translucencyCheckBox.addActionListener(listener);
    }

    public void setCombinedTraces() {
        chartPanel.setXAxisTitle("");
        chartPanel.setYAxisTitle("");
        messageLabel.setText("Can't show correlation of combined traces");
    }

    public void setTraces(TraceList[] traceLists, java.util.List<String> traceNames) {

        correlationChart.removeAllPlots();

        if (traceLists != null && traceNames != null && traceLists.length == 2 && traceNames.size() == 1) {
            tl1 = traceLists[0];
            name1 = tl1.getName();
            tl2 = traceLists[1];
            name2 = tl2.getName();
            traceIndex1 = tl1.getTraceIndex(traceNames.get(0));
            traceIndex2 = tl2.getTraceIndex(traceNames.get(0));
            name1 = name1 + " - " + tl1.getTraceName(traceIndex1);
            name2 = name2 + " - " + tl2.getTraceName(traceIndex2);
        } else if (traceLists != null && traceNames != null && traceLists.length == 1 && traceNames.size() == 2) {
            tl1 = traceLists[0];
            tl2 = traceLists[0];
            traceIndex1 = tl1.getTraceIndex(traceNames.get(0));
            traceIndex2 = tl2.getTraceIndex(traceNames.get(1));
            name1 = tl1.getTraceName(traceIndex1);
            name2 = tl2.getTraceName(traceIndex2);
        } else {
            tl1 = null;
            tl2 = null;
        }

        setupChart();
    }

    private void setupChart() {

        correlationChart.removeAllPlots();

        if (tl1 == null || tl2 == null) {
            chartPanel.setXAxisTitle("");
            chartPanel.setYAxisTitle("");
            messageLabel.setText("Select two statistics or traces from the table to view their correlation");
            return;
        }

        TraceDistribution td1 = tl1.getDistributionStatistics(traceIndex1);
        TraceDistribution td2 = tl2.getDistributionStatistics(traceIndex2);
        if (td1 == null || td2 == null) {
            chartPanel.setXAxisTitle("");
            chartPanel.setYAxisTitle("");
            messageLabel.setText("Waiting for analysis to complete");
            return;
        }

        messageLabel.setText("");

        int maxCount = Math.max(tl1.getStateCount(), tl2.getStateCount());
        int minCount = Math.min(tl1.getStateCount(), tl2.getStateCount());

        int sampleSize = minCount;

        if (sampleCheckBox.isSelected()) {
            if (td1.getESS() < td2.getESS()) {
                sampleSize = (int) td1.getESS();
            } else {
                sampleSize = (int) td2.getESS();
            }
            if (sampleSize < 20) {
                sampleSize = 20;
                messageLabel.setText("One of the traces has an ESS < 20 so a sample size of 20 will be used");
            }
            if (sampleSize > 500) {
                messageLabel.setText("This plot has been sampled down to 500 points");
                sampleSize = 500;
            }
        }

        double values[] = new double[maxCount];

        tl1.getValues(traceIndex1, Trace.arrayCopy(values));

        double samples1[] = new double[sampleSize];
        int k = 0;
        for (int i = 0; i < sampleSize; i++) {
            samples1[i] = values[k];
            k += minCount / sampleSize;
        }

        tl2.getValues(traceIndex2, Trace.arrayCopy(values));

        double samples2[] = new double[sampleSize];
        k = 0;
        for (int i = 0; i < sampleSize; i++) {
            samples2[i] = values[k];
            k += minCount / sampleSize;
        }

        ScatterPlot plot = new ScatterPlot(samples1, samples2);
        plot.setMarkStyle(pointsCheckBox.isSelected() ? Plot.POINT_MARK : Plot.CIRCLE_MARK, pointsCheckBox.isSelected() ? 1.0 : 3.0,
                new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER),
                new Color(16, 16, 64, translucencyCheckBox.isSelected() ? 32 : 255), new Color(16, 16, 64, translucencyCheckBox.isSelected() ? 32 : 255));
        correlationChart.addPlot(plot);

        chartPanel.setXAxisTitle(name1);
        chartPanel.setYAxisTitle(name2);

        validate();
        repaint();
    }

    public JComponent getExportableComponent() {
        return chartPanel;
    }

    public String toString() {
        if (correlationChart.getPlotCount() == 0) {
            return "no plot available";
        }

        StringBuffer buffer = new StringBuffer();

        Plot plot = correlationChart.getPlot(0);
        Variate xData = plot.getXData();
        Variate yData = plot.getYData();

        buffer.append(chartPanel.getXAxisTitle());
        buffer.append("\t");
        buffer.append(chartPanel.getYAxisTitle());
        buffer.append("\n");

        for (int i = 0; i < xData.getCount(); i++) {
            buffer.append(String.valueOf(xData.get(i)));
            buffer.append("\t");
            buffer.append(String.valueOf(yData.get(i)));
            buffer.append("\n");
        }

        return buffer.toString();
    }
}
