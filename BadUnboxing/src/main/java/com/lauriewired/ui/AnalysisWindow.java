package com.lauriewired.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.lauriewired.analyzer.ApkAnalysisDetails;
import com.lauriewired.analyzer.DynamicDexLoaderDetection;
import com.lauriewired.analyzer.JadxUtils;
import com.lauriewired.analyzer.UnpackerGenerator;
import jadx.api.JadxDecompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalysisWindow {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisWindow.class);
    private static JTree directoryTree;
    private static JTree apkDetailsTree;
    private static DefaultMutableTreeNode rootNode;
    private static String currentFilePath;
    private static RSyntaxTextArea rightPanelEditorPane;
    private static JScrollPane apkDetailsScrollPane;
    private static CustomProgressBar progressBar;
    private static String pathToUnpacker;
    private static ApkAnalysisDetails apkAnalysisDetails;

    public static void show(JFrame frame, String apkFilePath) {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);  // Maximize the window
        frame.setVisible(true);
    
        // Create the root node for the APK details tree
        DefaultMutableTreeNode apkDetailsRoot = new DefaultMutableTreeNode("APK Summary");

        // Add nodes for different sections
        DefaultMutableTreeNode fileNameNode = new DefaultMutableTreeNode("File Name: " + new File(apkFilePath).getName());
        DefaultMutableTreeNode fileSizeNode = new DefaultMutableTreeNode("Size: " + new File(apkFilePath).length() / 1024 + " KB");

        apkDetailsRoot.add(fileNameNode);
        apkDetailsRoot.add(fileSizeNode);

        apkDetailsTree = new JTree(apkDetailsRoot);
        apkDetailsTree.setCellRenderer(new NoIconTreeCell());
        apkDetailsTree.setFont(new Font("Verdana", Font.PLAIN, 18));

        apkDetailsScrollPane = new JScrollPane(apkDetailsTree);

        // Initialize the directory tree with the APK name as the root node
        File apkFile = new File(apkFilePath);
        rootNode = new DefaultMutableTreeNode(new FileNode(apkFile));
        directoryTree = new JTree(rootNode);
        directoryTree.setFont(new Font("Verdana", Font.PLAIN, 18));

        JScrollPane treeScrollPane = new JScrollPane(directoryTree);

        // Initialize the progress bar
        progressBar = new CustomProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(0, 20));
        progressBar.setString("Processing");
        progressBar.setStringPainted(true);
        progressBar.setTextColor(Color.ORANGE);

        // Create a panel for the progress bar
        JPanel progressBarPanel = new JPanel(new BorderLayout());
        progressBarPanel.add(progressBar, BorderLayout.CENTER);
        progressBarPanel.setPreferredSize(new Dimension(0, 20));

        // Create a split pane to combine the directory tree and APK details tree
        JSplitPane directoryAndDetailsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, apkDetailsScrollPane);
        directoryAndDetailsSplitPane.setDividerLocation(200);
        directoryAndDetailsSplitPane.setResizeWeight(0.5);  // Ratio between top and bottom panels

        // Create the main left split pane to include the directory/details split pane and the progress bar
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, directoryAndDetailsSplitPane, progressBarPanel);
        leftSplitPane.setResizeWeight(0.97);  // Ratio between directory/details and progress bar

        // Make the divider invisible
        leftSplitPane.setDividerSize(2);

        // Create a RSyntaxTextArea for versatile content display with syntax highlighting
        rightPanelEditorPane = new RSyntaxTextArea();
        SyntaxUtility.applyCustomTheme(rightPanelEditorPane);
        rightPanelEditorPane.setEditable(true); // Make it editable
        rightPanelEditorPane.setFont(new Font("Monospaced", Font.PLAIN, 18));
        rightPanelEditorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);  // Default to no syntax highlighting

    
        // Set darker background color
        rightPanelEditorPane.setBackground(new Color(30, 30, 30));
        rightPanelEditorPane.setForeground(new Color(230, 230, 230));
    
        RTextScrollPane rightPanelScrollPane = new RTextScrollPane(rightPanelEditorPane);
    
        // Create a panel for the buttons and add them to the top right of the right panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    
        // Create a font that supports Unicode characters
        Font arrowFont = new Font("Arial", Font.PLAIN, 12);
    
        // Create buttons with Unicode arrow symbols
        JButton upButton = new JButton("\u25B2");  // Unicode for black up-pointing triangle
        JButton downButton = new JButton("\u25BC");  // Unicode for black down-pointing triangle
    
        // Set the font for the buttons
        upButton.setFont(arrowFont);
        downButton.setFont(arrowFont);
    
        // Set tooltips for the buttons
        upButton.setToolTipText("Go to previous BadUnboxing code mod");
        downButton.setToolTipText("Go to next BadUnboxing code mod");
    
        // Set preferred sizes to keep the buttons small
        Dimension buttonSize = new Dimension(45, 40);
        upButton.setPreferredSize(buttonSize);
        downButton.setPreferredSize(buttonSize);
    
        // Add the buttons to the panel
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
    
        // Set the maximum size of the button panel to match the width of the buttons
        buttonPanel.setMaximumSize(new Dimension(buttonSize.width, Integer.MAX_VALUE));
    
        // Create a panel to hold the button panel and the right panel editor
        JPanel rightPanelContainer = new JPanel(new BorderLayout());
        rightPanelContainer.add(buttonPanel, BorderLayout.EAST);
        rightPanelContainer.add(rightPanelScrollPane, BorderLayout.CENTER);
    
        // Create a text area to display the console output
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    
        // Set darker background color
        textArea.setBackground(new Color(30, 30, 30));
        textArea.setForeground(new Color(230, 230, 230));
    
        // Redirect the console output to the text area
        PrintStream printStream = new PrintStream(new TextAreaOutputStream(textArea));
        System.setOut(printStream);
        System.setErr(printStream);
    
        // Create a split pane for the right side
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightPanelContainer, new JScrollPane(textArea));
        rightSplitPane.setDividerLocation(800);
        rightSplitPane.setResizeWeight(0.7);  // Ratio between top and bottom panels
    
        // Create a split pane for the left and right sections
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, rightSplitPane);
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setResizeWeight(0.3);  // Ratio between left and right panels
    
        frame.getContentPane().removeAll();  // Remove previous components
        frame.setLayout(new BorderLayout());
        frame.add(mainSplitPane, BorderLayout.CENTER);
    
        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
    
        // Create the "Save" menu item
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(e -> saveFile());
        saveMenuItem.setMargin(new Insets(5, 10, 5, 10));
        fileMenu.add(saveMenuItem);
    
        JMenu runMenu = new JMenu("Run");
        // Create the "Execute" menu item
        JMenuItem executeMenuItem = new JMenuItem("Execute");
        executeMenuItem.setMargin(new Insets(5, 10, 5, 10));
        executeMenuItem.addActionListener(e -> {
            int response = JOptionPane.showOptionDialog(
                frame,
                "Are you sure? This should only be executed in a secure malware analysis environment.",
                "Confirm Execution",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Continue", "Cancel"},
                "Cancel"
            );
            if (response == JOptionPane.YES_OPTION) {
                executeCode(rootNode, textArea);
            }
        });
        runMenu.add(executeMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(runMenu);
        frame.setJMenuBar(menuBar);
    
        frame.revalidate();
        frame.repaint();
    
        // Call the APK analysis method
        analyzeApk(apkFilePath, apkDetailsRoot);
    
        addDirectoryTreeSelectionListener();
        addRightPanelEditorPaneKeyListener();
        upButton.addActionListener(e -> findPreviousOccurrence("BadUnboxing"));
        downButton.addActionListener(e -> findNextOccurrence("BadUnboxing"));
    }

    private static void addDirectoryTreeSelectionListener() {
        directoryTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) directoryTree.getLastSelectedPathComponent();
                if (selectedNode == null) return;
    
                String filePath = getFilePath(selectedNode);
                if (filePath != null) {
                    currentFilePath = filePath;
                    try {
                        Path path = Paths.get(filePath);
                        if (Files.isRegularFile(path)) {
                            String content = new String(Files.readAllBytes(path));
                            if (filePath.endsWith(".java")) {
                                rightPanelEditorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                            } else if (filePath.endsWith(".json")) {
                                rightPanelEditorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                            } else {
                                rightPanelEditorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                            }
                            rightPanelEditorPane.setText(content);
                            rightPanelEditorPane.setCaretPosition(0);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        rightPanelEditorPane.setText("Error loading file: " + ex.getMessage());
                    }
                }
            }
        });
    }
    
    private static void addRightPanelEditorPaneKeyListener() {
        rightPanelEditorPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                    saveFile();
                }
            }
        });
    }
    
    private static void executeCode(DefaultMutableTreeNode rootNode, JTextArea textArea) {
        try {
            // Directory containing the generated Java files (set after APK analysis)
            File compilationDir = new File(pathToUnpacker).getParentFile();
            File sourceDir = compilationDir.getParentFile();

            // Get all java files in the directory
            File[] javaFiles = compilationDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (javaFiles == null || javaFiles.length == 0) {
                textArea.append("No Java files found to compile.\n");
                return;
            }
            
            // Convert the file paths to a format suitable for the javac command
            List<String> filePaths = Arrays.stream(javaFiles)
                                        .map(File::getAbsolutePath)
                                        .collect(Collectors.toList());

            // Prepare command list
            List<String> command = new ArrayList<>();
            command.add("javac");
            command.addAll(filePaths);

            // Execute the javac command
            ProcessBuilder compileProcessBuilder = new ProcessBuilder(command);
            compileProcessBuilder.directory(compilationDir);
            Process compileProcess = compileProcessBuilder.start();

            // Redirect compilation output
            redirectProcessOutput(compileProcess, textArea);
            
            compileProcess.waitFor();

            if (compileProcess.exitValue() != 0) {
                textArea.append("Compilation failed.\n");
                return;
            }

            // Execute the main unpacker class
            String mainClass = apkAnalysisDetails.getFullyQualifiedClassName();
            ProcessBuilder runProcessBuilder = new ProcessBuilder(
                    "java", "-cp", sourceDir.getAbsolutePath(), mainClass);
            runProcessBuilder.directory(sourceDir);

            // Log the command being executed
            String commandString = String.join(" ", runProcessBuilder.command());
            logger.info("Running execution command: " + commandString);
            textArea.append("Running execution command: " + commandString + "\n");
            progressBar.setString("Executing");
            progressBar.setTextColor(Color.ORANGE);

            Process runProcess = runProcessBuilder.start();

            // Redirect execution output
            redirectProcessOutput(runProcess, textArea);

            runProcess.waitFor();

            if (runProcess.exitValue() != 0) {
                logger.error("Execution failed");
                progressBar.setString("Error");
                progressBar.setTextColor(Color.RED);
            } else {
                logger.info("Completed execution");
                progressBar.setString("Complete");
                progressBar.setTextColor(Color.WHITE);
                updateDirectoryTree(apkAnalysisDetails.getBaseDir());
                ((DefaultTreeModel) directoryTree.getModel()).reload(rootNode);
                displayUnpackerFile();
                logger.info("File tree updated with dynamic artifacts directory");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error executing code: " + e.getMessage());
            textArea.append("Error executing code: " + e.getMessage() + "\n");
            progressBar.setString("Error");
            progressBar.setTextColor(Color.RED);
        }
    }

    private static void redirectProcessOutput(Process process, JTextArea textArea) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    textArea.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    textArea.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private static void findNextOccurrence(String searchString) {
        String content = rightPanelEditorPane.getText();
        int currentPosition = rightPanelEditorPane.getCaretPosition();
        int nextPosition = content.indexOf(searchString, currentPosition);
    
        if (nextPosition != -1) {
            rightPanelEditorPane.setCaretPosition(nextPosition);
            rightPanelEditorPane.select(nextPosition, nextPosition + searchString.length());
        } else {
            // If no more occurrences, optionally wrap around to the start
            nextPosition = content.indexOf(searchString);
            if (nextPosition != -1) {
                rightPanelEditorPane.setCaretPosition(nextPosition);
                rightPanelEditorPane.select(nextPosition, nextPosition + searchString.length());
            }
        }
    }
    
    private static void findPreviousOccurrence(String searchString) {
        String content = rightPanelEditorPane.getText();
        int currentPosition = rightPanelEditorPane.getCaretPosition();
        int previousPosition = content.lastIndexOf(searchString, currentPosition - searchString.length() - 1);
    
        if (previousPosition != -1) {
            rightPanelEditorPane.setCaretPosition(previousPosition);
            rightPanelEditorPane.select(previousPosition, previousPosition + searchString.length());
        } else {
            // If no more occurrences, optionally wrap around to the end
            previousPosition = content.lastIndexOf(searchString);
            if (previousPosition != -1) {
                rightPanelEditorPane.setCaretPosition(previousPosition);
                rightPanelEditorPane.select(previousPosition, previousPosition + searchString.length());
            }
        }
    }
    
    private static void saveFile() {
        if (currentFilePath != null) {
            try {
                Files.write(Paths.get(currentFilePath), rightPanelEditorPane.getText().getBytes());
                logger.info("File saved: " + currentFilePath);
            } catch (IOException e) {
                logger.error("Error saving file: " + currentFilePath, e);
            }
        } else {
            logger.warn("No file selected to save.");
        }
    }


    private static String getFilePath(DefaultMutableTreeNode node) {
        FileNode fileNode = (FileNode) node.getUserObject();
        return fileNode.getFile().getAbsolutePath();
    }

    private static List<String> isPacked(String apkFilePath, JadxDecompiler jadx) {
        List<String> packedClasses = new ArrayList<>();
        try {
            // Extract and parse AndroidManifest.xml
            Set<String> manifestClasses = JadxUtils.getManifestClasses(apkFilePath, jadx);

            // Get classes from dex files
            Set<String> dexClasses = JadxUtils.getDexClasses(apkFilePath, jadx);

            // Check if there are any classes in the manifest that are not in the dex files
            for (String className : manifestClasses) {
                if (!dexClasses.contains(className)) {
                    logger.info("Class {} found in manifest but not in dex files", className);
                    packedClasses.add(className);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking if APK is packed", e);
        }
        return packedClasses; // Return the list of packed classes
    }

    // Modify the analyzeApk method to update the progress bar
    private static void analyzeApk(String apkFilePath, DefaultMutableTreeNode apkDetailsRoot) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                logger.info("Loading APK");
                JadxDecompiler jadx = JadxUtils.loadJadx(apkFilePath);
        
                List<String> packedClasses = isPacked(apkFilePath, jadx);
                if (!packedClasses.isEmpty()) {
                    logger.info("APK is packed");

                    DefaultMutableTreeNode missingClassesNode = new DefaultMutableTreeNode("Missing Classes");
                    apkDetailsRoot.add(missingClassesNode);
                    for (String className : packedClasses) {
                        missingClassesNode.add(new DefaultMutableTreeNode(className));
                    }
        
                    List<String> dexLoadingDetails = DynamicDexLoaderDetection.getJavaDexLoadingDetails(jadx);
                    if (!dexLoadingDetails.isEmpty()) {
                        logger.info("Generating Java unpacker stub");
                        DefaultMutableTreeNode classLoaderNode = new DefaultMutableTreeNode("Code Loader Details");
                        classLoaderNode.add(new DefaultMutableTreeNode("Type: Java"));
                        
                        for (String detail : dexLoadingDetails) {
                            classLoaderNode.add(new DefaultMutableTreeNode(detail));
                        }
                        
                        apkDetailsRoot.add(classLoaderNode);
                    
                        apkAnalysisDetails = UnpackerGenerator.generateJava(jadx, apkFilePath);
                        if (apkAnalysisDetails.getBaseDir() != null) {
                            SwingUtilities.invokeLater(() -> updateDirectoryTree(apkAnalysisDetails.getBaseDir()));
                        } else {
                            logger.error("Error generating Java unpacker code.");
                        }
                    } else {
                        logger.info("Could not find code loader in Java. Probable native packer detected.");
                        DefaultMutableTreeNode classLoaderNode = new DefaultMutableTreeNode("Code Loader Details");
                        classLoaderNode.add(new DefaultMutableTreeNode("Type: Native"));
                        apkDetailsRoot.add(classLoaderNode);
                    }
                } else {
                    logger.info("APK is not packed");
                    DefaultMutableTreeNode packerNode = new DefaultMutableTreeNode("Packer");
                    packerNode.add(new DefaultMutableTreeNode("Not Packed"));
                    apkDetailsRoot.add(packerNode);
                }

                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    ((DefaultTreeModel) apkDetailsTree.getModel()).reload(apkDetailsRoot);
                    displayUnpackerFile();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Complete");
                    progressBar.setTextColor(Color.WHITE);
                });
            }
        };
        worker.execute();
    }

    private static void displayUnpackerFile() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) directoryTree.getModel().getRoot();
        DefaultMutableTreeNode targetNode = findNode(root, "Unpacker_", ".java");

        if (targetNode != null) {
            String filePath = getFilePath(targetNode);
            if (filePath != null) {
                currentFilePath = filePath;
                try {
                    // We'll use this path for other things as well
                    pathToUnpacker = filePath;

                    Path path = Paths.get(filePath);
                    if (Files.isRegularFile(path)) {
                        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                        rightPanelEditorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                        rightPanelEditorPane.setText(content);
                        rightPanelEditorPane.setCaretPosition(0);

                        // Expand the path to the target node
                        TreePath treePath = new TreePath(targetNode.getPath());
                        directoryTree.scrollPathToVisible(treePath);
                        directoryTree.setSelectionPath(treePath);
                        directoryTree.expandPath(treePath);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    rightPanelEditorPane.setText("Error loading file: " + ex.getMessage());
                }
            }
        } else {
            rightPanelEditorPane.setText("Entry file not found.");
        }
    }

    private static DefaultMutableTreeNode findNode(DefaultMutableTreeNode root, String prefix, String suffix) {
        Enumeration<?> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject() instanceof FileNode) {
                FileNode fileNode = (FileNode) node.getUserObject();
                String fileName = fileNode.getFile().getName();
                if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static void updateDirectoryTree(File baseDir) {
        DefaultTreeModel treeModel = DirectoryTreeModel.buildTreeModel(baseDir);
        directoryTree.setModel(treeModel);
    }
}
