package com.lauriewired.analyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionRemover {
    private static final Logger logger = LoggerFactory.getLogger(ReflectionRemover.class);

    private static Stack<String> reflectiveValues = new Stack<>();
    private static final Set<String> analyzedValues = new HashSet<>();

    private static final Set<String> reflectiveKeywords = new HashSet<>(Arrays.asList(
        "getMethod",
        "invoke",
        "getDeclaredField",
        "WeakReference",
        "forName",
        "setAccessible",
        "getApplicationContext",
        "newInstance"
    ));

    public static void removeReflection(StringBuilder javaCode) {

        //System.out.println(javaCode);
        
        //commentOutMethodUsingReflection(javaCode, surroundWithRegex("forName"));

        /*
        commentOutLineUsingReflection(javaCode, surroundWithRegex("getDeclaredField"));

        for (String method : reflectiveValues) {
            System.out.println(method);
        }

        
        //commentOutMethodUsingReflection(javaCode, "[\\s\\.\\(]it[\\s\\.\\);]");
        //commentOutLineUsingReflection(javaCode, "[\\s\\.\\(]it[\\s\\.\\);]");

        //commentOutMethodUsingReflection(javaCode, "Class.forName");
        //commentOutLineUsingReflection(javaCode, "Class.forName");
        

        for (String method : reflectiveValues) {
            System.out.println(method);
        }*/

        //commentOutLineUsingReflection(javaCode, surroundWithRegex("NJdYmOqBdAmAiWnWkAzIiKhNbFqSfAkTtXyRwCmMbCfNqClHu"));
        //commentOutLineUsingReflection(javaCode, surroundWithRegex("bwyY_354421"));

        
        
        // Initial comment out of reflection
        for (String keyword : reflectiveKeywords) {
            commentOutMethodUsingReflection(javaCode, surroundWithRegex(keyword));
        }
        for (String keyword : reflectiveKeywords) {
            commentOutLineUsingReflection(javaCode, surroundWithRegex(keyword));
        }

        // Add while loop here to keep iterating through reflectiveMethods and reflectiveVariables while they have values
        while (!reflectiveValues.isEmpty()) {
            String value = reflectiveValues.pop();
            if (!analyzedValues.contains(value)) {
                commentOutMethodUsingReflection(javaCode, surroundWithRegex(value));
                commentOutLineUsingReflection(javaCode, surroundWithRegex(value));
                analyzedValues.add(value);
            }
        }
    }

    private static String surroundWithRegex(String value) {
        return "[\\[\\]\\s\\.\\(\\)]" + value + "[\\[\\]\\s\\.\\(\\),;]";
    }

    public static void commentOutLineUsingReflection(StringBuilder javaCode, String keywordRegex) {
        // Compile the keyword as a regex pattern
        Pattern keywordPattern = Pattern.compile(keywordRegex);

        // Split the code into lines
        String[] lines = javaCode.toString().split("\n");
        StringBuilder modifiedCode = new StringBuilder();

        // Pattern to match variable names
        Pattern variablePattern = Pattern.compile("([a-zA-Z0-9_]+)\\s*=(\\s*)");
        Pattern methodPattern = Pattern.compile(".*\\s(public|private|protected|static|void)+\\s.");

        // Iterate over each line
        for (String line : lines) {
            Matcher keywordMatcher = keywordPattern.matcher(line);
            Matcher methodMatch = methodPattern.matcher(line);

            if (keywordMatcher.find() && !methodMatch.find() && !line.trim().startsWith("//") && !line.trim().startsWith("import")) {
                // Check if the reflective call is on the right of an equal sign
                Matcher matcher = variablePattern.matcher(line);
                if (matcher.find()) {
                    if (!matcher.group(2).matches(".*" + keywordRegex + ".*")) {
                        // Parse out the variable name
                        String variableName = matcher.group(1);
                        reflectiveValues.push(variableName);

                        logger.info("Found reflective variable: " + variableName);
                    }
                }
                // Comment out the line if it contains the keyword and is not already commented out
                modifiedCode.append("// ").append(line).append(" // BadUnboxing: Line contains reflection and was commented out\n");

                // Don't be too noisy so print up to the first 30 chars of the line
                String logLine = line.replaceFirst("^\\s+", "");
                logger.info("Commented out reflective line starting with: " + logLine.substring(0, Math.min(logLine.length(), 30)));

                // If the line ends with an opening brace, add "if (true) {"
                if (line.trim().endsWith("{")) {
                    modifiedCode.append("if (true) {\n"); // We just need a dummy placeholder
                }
            } else {
                modifiedCode.append(line).append("\n");
            }
        }

        // Replace the original code with the modified code
        javaCode.setLength(0);
        javaCode.append(modifiedCode);
    }

    // omg it works. never change this
    public static void commentOutMethodUsingReflection(StringBuilder javaCode, String keywordRegex) {
        // Compile the keyword as a regex pattern
        Pattern keywordPattern = Pattern.compile(keywordRegex);

        // Split the code into lines
        String[] lines = javaCode.toString().split("\n");
        StringBuilder modifiedCode = new StringBuilder();

        boolean returnContainsReflection = false;
        StringBuilder currentMethod = new StringBuilder();
        String currentMethodName = null;
        int braceDepth = -1;

        for (String line : lines) {
            if (line.trim().matches(".*(public|protected|private|static|\\s)+\\s*\\S+\\s+(method\\S+|main|onCreate)\\(.*\\)\\s+[\\w\\s]*\\{") && !line.trim().startsWith("//")) {
                // New method start
                currentMethod.setLength(0);
                returnContainsReflection = false;
                currentMethodName = parseMethodName(line);
                currentMethod.append(line).append("\n");

                braceDepth = 1;
            } else if (braceDepth == 0) {
                // This means we've finished a method
                if (returnContainsReflection) {
                    commentOutMethod(currentMethod, modifiedCode);
                    reflectiveValues.push(currentMethodName);
                    logger.info("Removing reflective method: " + currentMethodName);
                } else {
                    modifiedCode.append(currentMethod).append("\n");
                }

                // Still need to handle the latest line
                modifiedCode.append(line);

                braceDepth = -1; // Reset for new method
            } else if (braceDepth > 0) {
                // Keep up with our braces to keep track of the method length
                for (int i = 0; i < line.length(); i++) {
                    if (line.charAt(i) == '{') {
                        braceDepth++;
                    } else if (line.charAt(i) == '}') {
                        braceDepth--;
                    }
                }

                // Check if the line is a return statement and contains the keyword
                Matcher keywordMatcher = keywordPattern.matcher(line);
                if (line.trim().startsWith("return") && keywordMatcher.find() && !line.trim().startsWith("//")) {
                    returnContainsReflection = true;
                }
                currentMethod.append(line).append("\n");
            } else {
                modifiedCode.append(line).append("\n");
            }
        }

        // Replace the original code with the modified code
        javaCode.setLength(0);
        javaCode.append(modifiedCode);
    }

    private static String parseMethodName(String line) {
        // Simple regex to extract the method name
        String methodSignature = line.trim().split("\\(")[0].trim();
        String[] parts = methodSignature.split("\\s+");
        return parts[parts.length - 1];
    }

    private static void commentOutMethod(StringBuilder methodCode, StringBuilder modifiedCode) {
        String[] lines = methodCode.toString().split("\n");
        for (String line : lines) {
            modifiedCode.append("// BadUnboxing ").append(line).append("\n");
        }
        modifiedCode.append("// BadUnboxing: Method contains reflection in return statement and was commented out\n\n");
    }
}