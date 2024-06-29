package com.lauriewired.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JProgressBar;

public class CustomProgressBar extends JProgressBar {
    private Color textColor;

    public CustomProgressBar() {
        super();
        this.textColor = Color.WHITE;  // Default text color
        setFont(getFont().deriveFont(Font.BOLD, getFont().getSize() + 1));
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();  // Repaint to apply the new color
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isStringPainted()) {
            String progressString = getString();
            FontMetrics fontMetrics = g.getFontMetrics();
            int stringWidth = fontMetrics.stringWidth(progressString);
            int stringHeight = fontMetrics.getAscent();
            int x = (getWidth() - stringWidth) / 2;
            int y = (getHeight() + stringHeight) / 2 - 3;
            g.setColor(textColor);
            g.drawString(progressString, x, y);
        }
    }
}