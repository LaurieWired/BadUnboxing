package com.lauriewired.ui;

import java.awt.Color;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;

public class SyntaxUtility {
    public static void applyCustomTheme(RSyntaxTextArea textArea) {
        SyntaxScheme scheme = textArea.getSyntaxScheme();

        // Common color for keywords, return statements, and boolean literals
        Color keywordColor = Color.decode("#569CD6");

        // Define a color for operators (braces, parentheses, brackets)
        Color operatorColor = Color.WHITE; // Set operators to white

        // Define a more suitable shade of green for comments
        Color commentColor = Color.decode("#57A64A");

        scheme.getStyle(Token.RESERVED_WORD).foreground = keywordColor;
        scheme.getStyle(Token.DATA_TYPE).foreground = Color.decode("#4EC9B0");
        scheme.getStyle(Token.FUNCTION).foreground = Color.decode("#DCDCAA");
        scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = Color.decode("#B5CEA8");
        scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.decode("#CE9178");
        scheme.getStyle(Token.COMMENT_MULTILINE).foreground = commentColor;
        scheme.getStyle(Token.COMMENT_DOCUMENTATION).foreground = commentColor;
        scheme.getStyle(Token.COMMENT_EOL).foreground = commentColor;  // Single-line comments
        
        scheme.getStyle(Token.OPERATOR).foreground = operatorColor;  // Operators to white
        scheme.getStyle(Token.SEPARATOR).foreground = operatorColor;
        scheme.getStyle(Token.RESERVED_WORD_2).foreground = keywordColor;
        scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = keywordColor;  // Boolean literals

        scheme.getStyle(Token.IDENTIFIER).foreground = Color.decode("#9CDCFE");

        // Set the background color of the text area itself
        textArea.setBackground(Color.decode("#1E1E1E"));
        textArea.setCurrentLineHighlightColor(Color.decode("#264F78"));
        textArea.setFadeCurrentLineHighlight(true);

        // Apply the scheme to the text area
        textArea.revalidate();
        textArea.repaint();
    }
}
