package com.lauriewired.analyzer;

import java.io.File;

public class ApkAnalysisDetails {
    private File baseDir;
    private String fullQualifiedClassName;
    private int recognizedImports;

    public ApkAnalysisDetails(File baseDir, String fullQualifiedClassName, int recognizedImports) {
        this.baseDir = baseDir;
        this.fullQualifiedClassName = fullQualifiedClassName;
        this.recognizedImports = recognizedImports;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public String getFullyQualifiedClassName() {
        return fullQualifiedClassName;
    }
    
    public int getRecognizedImports() {
        return recognizedImports;
    }
}
