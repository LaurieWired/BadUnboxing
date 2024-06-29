package com.lauriewired.analyzer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeReplacerUtils {
    private static final Logger logger = LoggerFactory.getLogger(CodeReplacerUtils.class);

    // Dummy class defintions
    private static final String DUMMY_CONTEXT_CLASS =
        "class Context {\n" +
        "    // Dummy Context class implementation\n" +
        "}\n";

    private static final String DUMMY_APPLICATION_CLASS =
        "class Application {\n" +
        "    // Dummy Application class implementation\n" +
        "}\n";

    //TODO: update variable patterns to look like this Pattern variablePattern = Pattern.compile("([a-zA-Z0-9_]+)\\s*=\\s*");

    public static String insertImport(String newImport, String classCode) {
        // Prefix the new import with 'import ' and suffix with ';'
        String formattedImport = "import " + newImport + ";";
        
        // Check if the formatted import already exists in classCode
        if (classCode.contains(formattedImport)) {
            // Import already exists, do nothing
            return classCode;
        }
    
        // Find the first occurrence of an import statement
        int importIndex = classCode.indexOf("import ");
        
        if (importIndex != -1) {
            // Insert the new import one line before the first import statement
            int insertIndex = classCode.lastIndexOf("\n", importIndex) + 1;
            String modifiedClassCode = classCode.substring(0, insertIndex) + formattedImport + "\n" + classCode.substring(insertIndex);
            logger.info("Inserted import " + newImport);
            return modifiedClassCode;
        } else {
            // No import statements found, add the new import at the beginning
            String modifiedClassCode = formattedImport + "\n" + classCode;
            logger.info("Inserted import " + newImport);
            return modifiedClassCode;
        }
    }

    public static StringBuilder processClassImports(StringBuilder javaCode, String newClassCode) {
        // Extract imports from existing code
        Set<String> existingImports = new HashSet<>();
        Matcher importMatcher = Pattern.compile("(?m)^import\\s+.*?;").matcher(javaCode);
        while (importMatcher.find()) {
            existingImports.add(importMatcher.group());
        }
    
        StringBuilder imports = new StringBuilder();
        importMatcher = Pattern.compile("(?m)^import\\s+.*?;").matcher(newClassCode);
        while (importMatcher.find()) {
            String importStatement = importMatcher.group();
            if (!existingImports.contains(importStatement)) {
                imports.append(importStatement).append("\n");
                existingImports.add(importStatement);
            }
        }
    
        return imports;
    }

    /*
     * Modifying methods from dalvik.system.DexClassLoader
     */
    public static String processDexClassLoaderMethods(String classCode) {
        // Pattern to find calls to new DexClassLoader with arguments
        Pattern dexClassLoaderPattern = Pattern.compile(".*new\\s+DexClassLoader\\(([^,]+),.*");
    
        Matcher matcher = dexClassLoaderPattern.matcher(classCode);
        StringBuffer modifiedCode = new StringBuffer();
    
        // Replace all occurrences with System.out.println(firstArgument)
        while (matcher.find()) {
            String firstArgument = matcher.group(1).trim();
            
            // Replace the call to DexClassLoader with System.out.println(firstArgument)
            String replacement = "System.out.println(" + firstArgument + "); // BadUnboxing: Replacing DexClassLoader call with directory print";
            
            matcher.appendReplacement(modifiedCode, replacement);
    
            logger.info("Replacing call to DexClassLoader with printing target directory to console");
        }
        matcher.appendTail(modifiedCode);
    
        return modifiedCode.toString();
    }

    public static void modifyAssetManager() {
        // Change asset manager to instead input a file from a folder with the dumped assets
    }
    
    /*
    * Modifying methods from android.util.ArrayMap
    */
    public static String processArrayMapMethods(String classCode, StringBuilder imports) {
        classCode = insertImport("java.util.HashMap", classCode);

        // Pattern to find lines containing ArrayMap
        Pattern arrayMapPattern = Pattern.compile("^(?!import).*\\bArrayMap\\b.*", Pattern.MULTILINE);

        Matcher matcher = arrayMapPattern.matcher(classCode);
        StringBuffer modifiedCode = new StringBuffer();

        // Replace all occurrences of ArrayMap with HashMap and add a comment at the end of the line
        while (matcher.find()) {
            String line = matcher.group();
            String modifiedLine = line.replaceAll("\\bArrayMap\\b", "HashMap") + " // BadUnboxing: Replacing ArrayMap with HashMap";

            logger.info("Replacing call to ArrayMap references with HashMap");
            
            matcher.appendReplacement(modifiedCode, modifiedLine);
        }
        matcher.appendTail(modifiedCode);

        return modifiedCode.toString();
    }

    /*
     * Modifying methods from android.content.pm.ApplicationInfo
     */
    public static String processApplicationInfoMethods(String classCode, String apkPath) {
        classCode = modifySourceDir(classCode, apkPath);
        //classCode = modifyNativeLibraryDir(classCode); //TODO

        return classCode;
    }

    public static String modifySourceDir(String classCode, String apkPath) {
        // Pattern to find lines containing Build.VERSION.SDK_INT
        Pattern sdkIntPattern = Pattern.compile(".*\\.sourceDir.*", Pattern.MULTILINE);
    
        Matcher matcher = sdkIntPattern.matcher(classCode);
        StringBuffer modifiedCode = new StringBuffer();
    
        // Replace all occurrences with the hardcoded value 30 and add a comment at the end of the line
        while (matcher.find()) {
            String line = matcher.group();
            String replacement = "\"" + apkPath.replace("\\", "\\\\\\\\\\\\\\\\") + "\"";
            String modifiedLine = line.replaceAll("(sourceDir)|(([a-zA-Z]+\\.)sourceDir|[a-zA-Z]+\\(\\)\\.sourceDir)", replacement);
            // Add the comment at the end of the line
            modifiedLine += " // BadUnboxing: Replacing sourceDir with path to APK";
            
            matcher.appendReplacement(modifiedCode, modifiedLine);
            logger.info("Replacing call to sourceDir with path to APK");
        }
        matcher.appendTail(modifiedCode);
    
        return modifiedCode.toString();
    }

    /*
     * Modifying methods from android.os.Build
     */
    public static String processBuildMethods(String classCode) {
        classCode = modifyBuildSdkInt(classCode);

        return classCode;
    }

    public static String modifyBuildSdkInt(String classCode) {
        // Pattern to find lines containing Build.VERSION.SDK_INT
        Pattern sdkIntPattern = Pattern.compile(".*SDK_INT.*", Pattern.MULTILINE);
    
        Matcher matcher = sdkIntPattern.matcher(classCode);
        StringBuffer modifiedCode = new StringBuffer();
    
        // Replace all occurrences with the hardcoded value 30 and add a comment at the end of the line
        while (matcher.find()) {
            String line = matcher.group();
            // Replace SDK_INT with 30
            String modifiedLine = line.replaceAll("(SDK_INT)|(([a-zA-Z]+\\.)+SDK_INT)", "30");
            // Add the comment at the end of the line
            modifiedLine += " // BadUnboxing: Hardcode build SDK_INT";
            
            matcher.appendReplacement(modifiedCode, modifiedLine);
            logger.info("Replacing call to SDK_INT with constant value 30 in line");
        }
        matcher.appendTail(modifiedCode);
    
        return modifiedCode.toString();
    }

    /*
     * Modifying methods from android.app.Application
     */
    public static void processApplicationMethods(StringBuilder javaCode) {
        insertDummyApplicationClass(javaCode);
    }

    public static void insertDummyApplicationClass(StringBuilder javaCode) {
        // Find the end of the import section
        Matcher importMatcher = Pattern.compile("(?m)^import\\s+.*?;").matcher(javaCode);
        int lastImportIndex = 0;
        while (importMatcher.find()) {
            lastImportIndex = importMatcher.end();
        }
        
        // Find the index to insert the dummy class after the last import
        int insertIndex = javaCode.indexOf("\n", lastImportIndex) + 1;
        javaCode.insert(insertIndex, "\n" + DUMMY_APPLICATION_CLASS + "\n");
    
        logger.info("Inserted dummy Application class");
    }
    
    /*
     * Modifying methods from android.content.Context 
     */

    public static void processContextMethods(StringBuilder javaCode, String className, String packageName) {
        insertDummyContextClass(javaCode);
        modifyGetDirMethod(javaCode, className);
        modifyGetPackageName(javaCode, packageName);
        modifyGetFileStreamPath(javaCode, className);
    }

    public static void insertDummyContextClass(StringBuilder javaCode) {
        // Find the end of the import section
        Matcher importMatcher = Pattern.compile("(?m)^import\\s+.*?;").matcher(javaCode);
        int lastImportIndex = 0;
        while (importMatcher.find()) {
            lastImportIndex = importMatcher.end();
        }
        
        // Find the index to insert the dummy class after the last import
        int insertIndex = javaCode.indexOf("\n", lastImportIndex) + 1;
        javaCode.insert(insertIndex, "\n" + DUMMY_CONTEXT_CLASS + "\n");

        logger.info("Inserted dummy Context class");
    }

    // TODO we might be able to combine this method and the getDir method. Lots of repeated code except regex
    private static void modifyGetFileStreamPath(StringBuilder javaCode, String className) {
        // Pattern to find getFileStreamPath method calls in both variable assignment and return statement contexts
        Pattern getFileStreamPathPattern = Pattern.compile("(.*)getFileStreamPath\\((.*)\\)");
    
        Matcher matcher = getFileStreamPathPattern.matcher(javaCode);
        StringBuffer modifiedCode = new StringBuffer();
    
        while (matcher.find()) {
            String prefix = matcher.group(1); // This captures 'var = ' or 'return ', if present
            String fileName = matcher.group(2).trim(); // The file name argument to getFileStreamPath
    
            // Construct the replacement code using standard Java File operations
            String replacement = "new File(System.getProperty(\"user.dir\") + \"/" + className + "_dynamic\", " + fileName + ")";
            if (prefix != null && prefix.contains("=")) {
                // Case for variable assignment
                String varName = prefix.split("\\s*=\\s*")[0].trim();

                replacement = varName + " = " + replacement;
                
                // Make sure we don't include type if it was included
                if (varName.split("\\s").length > 1) {
                    varName = varName.split("\\s")[1].trim();
                }

                replacement += ";\nif (!" + varName + ".exists()) { " + varName + ".mkdirs(); }";
            } else {
                // Case for return statement
                String newFileReplacement = replacement;
                String varName = "var_tmp_" + IdentifierRenamer.generateRandomPostfix();
                replacement = "File " + varName + " = " + newFileReplacement + ";\n";
                replacement += "if (!" + varName + ".exists()) { " + varName + ".mkdirs(); }";
                replacement += "\nreturn " + varName + ";";
            }
    
            replacement += " // BadUnboxing: Redirect to dynamic directory";
    
            // Replace the getFileStreamPath call in the original line
            matcher.appendReplacement(modifiedCode, replacement);
            logger.info("Replacing call to getFileStreamPath with dynamic directory path");
        }
        matcher.appendTail(modifiedCode);
    
        // Replace the original code with the modified code
        javaCode.setLength(0);
        javaCode.append(modifiedCode);
    }

    private static void modifyGetPackageName(StringBuilder javaCode, String packageName) {
        // Pattern to find lines containing getPackageName() calls
        Pattern getPackageNamePattern = Pattern.compile(".*getPackageName\\(\\)(\\s*;)");
        Matcher lineMatcher = getPackageNamePattern.matcher(javaCode);
        StringBuffer modifiedCode = new StringBuffer();
        
        while (lineMatcher.find()) {
            String line = lineMatcher.group();
            // Replace getPackageName() or any prefix with it using the new regex
            String modifiedLine = line.replaceAll("getPackageName\\(\\)|[\\w+\\.]+getPackageName\\(\\)", "\"" + packageName + "\"");
            modifiedLine += " // BadUnboxing: Hardcode package name";
            lineMatcher.appendReplacement(modifiedCode, modifiedLine);
            logger.info("Replacing call to getPackageName with string literal '{}'", packageName);
        }
        lineMatcher.appendTail(modifiedCode);
        
        // Replace the original code with the modified code
        javaCode.setLength(0);
        javaCode.append(modifiedCode);
    }
    
    private static void modifyGetDirMethod(StringBuilder javaCode, String className) {
        // Pattern to find getDir method calls in both variable assignment and return statement contexts
        Pattern getDirPattern = Pattern.compile("(.*)getDir\\(([^,]+),\\s*\\d+\\s*\\)");
    
        Matcher matcher = getDirPattern.matcher(javaCode);
        StringBuffer modifiedCode = new StringBuffer();
    
        while (matcher.find()) {
            String prefix = matcher.group(1); // This captures 'var = ' or 'return ', if present
            String dirName = matcher.group(2).trim(); // The directory name argument to getDir
    
            // Construct the replacement code using standard Java File operations
            String replacement = "new File(System.getProperty(\"user.dir\") + \"/" + className + "_dynamic\", " + dirName + ")";
            if (prefix != null && prefix.contains("=")) {
                // Case for variable assignment
                String varName = prefix.split("\\s*=\\s*")[0].trim();
                replacement = varName + " = " + replacement;

                // Make sure we don't include type if it was included
                if (varName.split("\\s").length > 1) {
                    varName = varName.split("\\s")[1].trim();
                }

                replacement += ";\nif (!" + varName + ".exists()) { " + varName + ".mkdirs(); }";
            } else {
                // Case for return statement
                String newFileReplacement = replacement;
                String varName = "var_tmp_" + IdentifierRenamer.generateRandomPostfix();
                replacement = "File " + varName + " = " + newFileReplacement + ";\n";
                replacement += "if (!" + varName + ".exists()) { " + varName + ".mkdirs(); }";
                replacement += "\nreturn " + varName + ";";
            }
    
            replacement += " // BadUnboxing: Change to current directory";
    
            // Replace the getDir call in the original line
            matcher.appendReplacement(modifiedCode, replacement);
            logger.info("Replacing call to getDir with path to dynamic directory based on context");
        }
        matcher.appendTail(modifiedCode);
    
        // Replace the original code with the modified code
        javaCode.setLength(0);
        javaCode.append(modifiedCode);
    }
}