package com.lauriewired;

import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;

import com.lauriewired.ui.AnalysisWindow;

public class BadUnboxing {
    private static final Logger logger = LoggerFactory.getLogger(BadUnboxing.class);

    public static void main(String[] args) {
        // Set the Darklaf Look and Feel
        try {
            LafManager.install(new DarculaTheme());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(BadUnboxing::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("BadUnboxing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Increased size for better spacing
    
        // Set the window icon
        try {
            URL imageUrl = BadUnboxing.class.getClassLoader().getResource("icon.png");
            if (imageUrl != null) {
                ImageIcon imageIcon = new ImageIcon(imageUrl);
                frame.setIconImage(imageIcon.getImage());
            } else {
                logger.error("Icon resource not found");
            }
        } catch (Exception e) {
            logger.error("Failed to load window icon", e);
        }
    
        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel, frame);
    
        frame.setVisible(true);
    }
    
    private static void placeComponents(JPanel panel, JFrame frame) {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(15, 15, 15, 15);  // Larger margins around components
    
        /*
        // Label "APK File:"
        JLabel label = new JLabel("APK Unpacker");
        label.setFont(new Font("Verdana", Font.BOLD, 25));  // Set font style and size
        constraints.gridx = 0;  // Column 0
        constraints.gridy = 0;  // Row 0
        constraints.gridwidth = 4;  // Span across all columns
        constraints.anchor = GridBagConstraints.CENTER;  // Center alignment
        constraints.insets = new Insets(20, 15, 20, 15); // Add padding
        panel.add(label, constraints);
        */
    
        // Text Field for file path
        JTextField filePathText = new JTextField();
        filePathText.setFont(new Font("Verdana", Font.PLAIN, 16)); // Set font style and size
        filePathText.setEditable(false);
        filePathText.setPreferredSize(new Dimension(275, 30));  // Set preferred size for wider text field
        //filePathText.setBorder(BorderFactory.createEmptyBorder()); // Remove the white border
        constraints.gridx = 0;  // Column 0
        constraints.gridy = 1;  // Row 1
        constraints.gridwidth = 3;  // Takes three columns
        constraints.anchor = GridBagConstraints.CENTER;  // Center alignment
        constraints.insets = new Insets(10, 15, 10, 5); // Add padding
        panel.add(filePathText, constraints);
    
        // "Select File" button
        JButton fileButton = new JButton("Select File");
        constraints.gridx = 3;  // Column 3
        constraints.gridy = 1;  // Row 1
        constraints.gridwidth = 1;  // Takes one column
        constraints.anchor = GridBagConstraints.CENTER;  // Center alignment
        constraints.insets = new Insets(5, 5, 5, 15); // Add padding
        panel.add(fileButton, constraints);
    
        // Status label
        JLabel statusLabel = new JLabel("");
        statusLabel.setFont(new Font("Verdana", Font.ITALIC, 14));  // Set font style and size
        constraints.gridx = 0;  // Column 0
        constraints.gridy = 3;  // Row 3
        constraints.gridwidth = 4;  // Span across all columns
        constraints.anchor = GridBagConstraints.CENTER;  // Center alignment
        constraints.insets = new Insets(10, 15, 10, 15); // Add padding
        panel.add(statusLabel, constraints);
    
        // "Unpack" button
        JButton unpackButton = new JButton("Generate Unpacker");
        constraints.gridx = 0;  // Column 0
        constraints.gridy = 2;  // Row 2
        constraints.gridwidth = 4;  // Span across all columns
        constraints.anchor = GridBagConstraints.CENTER;  // Center alignment
        constraints.insets = new Insets(10, 15, 10, 15); // Add padding
        panel.add(unpackButton, constraints);
    
        // File and button listeners
        setupDragAndDrop(filePathText);
        setupFileButtonListener(fileButton, filePathText, panel);
        setupUnpackButtonListener(unpackButton, filePathText, statusLabel, frame);
    }
    
    private static void setupDragAndDrop(JTextField filePathText) {
        new DropTarget(filePathText, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        File file = droppedFiles.get(0);
                        filePathText.setText(file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    logger.error("Error during file drop", ex);
                }
            }
        });
    }
    
    private static void setupFileButtonListener(JButton fileButton, JTextField filePathText, JPanel panel) {
        fileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int option = fileChooser.showOpenDialog(panel);
            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathText.setText(selectedFile.getAbsolutePath());
            }
        });
    }

    private static void setupUnpackButtonListener(JButton unpackButton, JTextField filePathText, JLabel statusLabel, JFrame frame) {
        unpackButton.addActionListener(e -> {
            String apkFilePath = filePathText.getText();
            if (!apkFilePath.isEmpty()) {
                AnalysisWindow.show(frame, apkFilePath);  // Open Analysis Window
            } else {
                statusLabel.setText("Please enter a valid APK file path.");
            }
        });
    }
}
