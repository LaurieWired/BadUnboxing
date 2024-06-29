package com.lauriewired.ui;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;

public class DirectoryTreeModel {

    public static DefaultTreeModel buildTreeModel(File root) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileNode(root));
        buildTreeNodes(rootNode, root);
        return new DefaultTreeModel(rootNode);
    }

    private static void buildTreeNodes(DefaultMutableTreeNode parentNode, File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(child));
                if (child.isDirectory()) {
                    parentNode.add(childNode);
                    buildTreeNodes(childNode, child);
                } else {
                    parentNode.add(new DefaultMutableTreeNode(new FileNode(child)));
                }
            }
        } else {
            parentNode.add(new DefaultMutableTreeNode(new FileNode(file)));
        }
    }
}

class FileNode {
    private final File file;

    public FileNode(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getName(); // Display only the name
    }
}
