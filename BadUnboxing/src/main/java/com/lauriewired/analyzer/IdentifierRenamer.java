package com.lauriewired.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.core.dex.nodes.FieldNode;

public class IdentifierRenamer {
    private static final Logger logger = LoggerFactory.getLogger(IdentifierRenamer.class);
    private static final int POSTFIX_LENGTH = 8;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    public static void renameMethodsAndFields(JavaClass javaClass, JadxDecompiler jadx, Set<String> existingNames) {
        // Via JADX API
        renameMethods(javaClass, jadx, existingNames);
        renameFields(javaClass, jadx, existingNames);
    }

    public static StringBuilder renameArgsAndVars(JavaClass javaClass, JadxDecompiler jadx, Set<String> existingNames) {
        javaClass.reload(); // Make sure we got the renamed methods and fields

        // Via regular expressions
        // Can't find a great way to rename these via the JADX API
        StringBuilder javaCode = new StringBuilder();
        javaCode.append(javaClass.getCode());

        renameMethodArguments(javaCode, existingNames);
        renameLocalVariables(javaCode, existingNames);

        return javaCode;
    }

    private static void renameMethodArguments(StringBuilder javaCode, Set<String> existingNames) {
        // Pattern to match methods with their arguments
        Pattern methodPattern = Pattern.compile("(public|protected|private|static)+\\s+[\\w\\[\\]<>]+\\s+(\\w+)\\s*\\(([^)]*)\\)[\\w\\s]*\\{");

        // Find the last match and work backwards
        Matcher methodMatcher = methodPattern.matcher(javaCode);
        List<Integer> matchPositions = new ArrayList<>();

        while (methodMatcher.find()) {
            matchPositions.add(methodMatcher.start());
        }

        // Process matches in reverse order
        for (int i = matchPositions.size() - 1; i >= 0; i--) {
            int matchStart = matchPositions.get(i);
            methodMatcher = methodPattern.matcher(javaCode);
            methodMatcher.find(matchStart);

            String methodSignature = methodMatcher.group();
            String arguments = methodMatcher.group(3); // Capture group 3 for arguments
            String methodBody = extractMethodBody(javaCode, methodMatcher.end());
            String modifiedMethodBody = methodBody;

            if (!arguments.isEmpty()) {
                String[] args = arguments.split(",");
                for (String arg : args) {
                    arg = arg.trim();
                    String[] parts = arg.split("\\s+");
                    String argName = parts[parts.length - 1];
                    if (!argName.startsWith("var_") && !argName.startsWith("method_")) {
                        String uniqueArgName = generateUniqueName(existingNames, argName, "arg_");

                        logger.info("Renaming method argument {} to {}", argName, uniqueArgName);

                        // Replace all references to this argument within the method body
                        modifiedMethodBody = modifiedMethodBody.replaceAll("\\b" + argName + "\\b", uniqueArgName);

                        // Update the method signature with the new argument name
                        methodSignature = methodSignature.replaceAll("\\b" + argName + "\\b", uniqueArgName);
                    }
                }
            }

            // Replace the method body in the original code
            String fullMethod = methodSignature + modifiedMethodBody;
            javaCode.replace(methodMatcher.start(), methodMatcher.end() + methodBody.length(), fullMethod);
        }
    }

    private static void renameLocalVariables(StringBuilder javaCode, Set<String> existingNames) {
        // Pattern to match methods with their bodies
        Pattern methodPattern = Pattern.compile("(public|protected|private|static)+\\s+[\\w\\[\\]<>]+\\s+(\\w+)\\s*\\(([^)]*)\\)[\\w\\s]*\\{");

        // Find the last match and work backwards
        Matcher methodMatcher = methodPattern.matcher(javaCode);
        List<Integer> matchPositions = new ArrayList<>();

        while (methodMatcher.find()) {
            matchPositions.add(methodMatcher.start());
        }

        // Process matches in reverse order
        for (int i = matchPositions.size() - 1; i >= 0; i--) {
            int matchStart = matchPositions.get(i);
            methodMatcher = methodPattern.matcher(javaCode);
            methodMatcher.find(matchStart);

            String methodSignature = methodMatcher.group();
            String methodBody = extractMethodBody(javaCode, methodMatcher.end());
            String modifiedMethodBody = methodBody;

            // Pattern to match local variable declarations (including array types)
            Pattern localVarPattern = Pattern.compile("(\\b\\w+[\\[\\]\\<\\?\\>]*)\\s+(\\b\\w+\\b)\\s*(=|;)");
            Matcher localVarMatcher = localVarPattern.matcher(methodBody);

            while (localVarMatcher.find()) {
                String varType = localVarMatcher.group(1);
                String varName = localVarMatcher.group(2);
                if (!varName.matches("\\d+") && !varName.equals("null") && 
                    !varName.equals("true") && !varName.equals("false") &&
                    !varName.startsWith("var_")) {

                    String uniqueVarName = generateUniqueName(existingNames, varName, "var_");

                    // Replace all references to this variable within the method body
                    if (varName.startsWith("method_")) {
                        modifiedMethodBody = replaceVariableNamesWithoutMethod(modifiedMethodBody, varName, uniqueVarName);
                    } else {
                        modifiedMethodBody = replaceVariableNames(modifiedMethodBody, varName, uniqueVarName);
                    }

                    logger.info("Renaming local variable {} to {}", varName, uniqueVarName);
                }
            }

            // Replace the method body in the original code
            String fullMethod = methodSignature + modifiedMethodBody;
            javaCode.replace(methodMatcher.start(), methodMatcher.end() + methodBody.length(), fullMethod);
        }
    }

    private static String replaceVariableNamesWithoutMethod(String body, String varName, String uniqueVarName) {
        StringBuilder result = new StringBuilder();
        // This regex looks for the variable name as a whole word, ensuring it isn't part of a longer string or variable name
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(varName) + "\\b(?![\\(])");  // Negative lookahead to avoid matches followed by '('
        Matcher matcher = pattern.matcher(body);
    
        int lastIndex = 0;
        while (matcher.find()) {
            // Append the part of the body before the match, then append the new unique variable name
            result.append(body, lastIndex, matcher.start())
                  .append(uniqueVarName);
            lastIndex = matcher.end();
        }
        // Append the rest of the body after the last match
        result.append(body.substring(lastIndex));
        return result.toString();
    }

    private static String replaceVariableNames(String body, String varName, String uniqueVarName) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("\\b" + varName + "\\b");
        Matcher matcher = pattern.matcher(body);
    
        int lastIndex = 0;
        while (matcher.find()) {
            // Check the preceding character
            if (matcher.start() == 0 || body.charAt(matcher.start() - 1) != '.') {
                result.append(body, lastIndex, matcher.start())
                      .append(uniqueVarName);
            } else {
                result.append(body, lastIndex, matcher.end());
            }
            lastIndex = matcher.end();
        }
        result.append(body.substring(lastIndex));
        return result.toString();
    }

    private static String extractMethodBody(StringBuilder javaCode, int startIndex) {
        int openBraces = 1;
        int currentIndex = startIndex;
        while (openBraces > 0 && currentIndex < javaCode.length()) {
            char currentChar = javaCode.charAt(currentIndex);
            if (currentChar == '{') {
                openBraces++;
            } else if (currentChar == '}') {
                openBraces--;
            }
            currentIndex++;
        }
        return javaCode.substring(startIndex, currentIndex);
    }

    private static void renameMethods(JavaClass javaClass, JadxDecompiler jadx, Set<String> existingNames) {
        for (JavaMethod method : javaClass.getMethods()) {
            if (!method.getName().startsWith("method_") && !method.getName().equals("attachBaseContext") &&
                !method.getName().equals("onCreate") && !method.getName().equals("<init>")) {
                    
                String uniqueMethodName = generateUniqueName(existingNames, method.getName(), "method_");
                logger.info("Renaming method {} to {}", method.getName(), uniqueMethodName);
                method.getMethodNode().rename(uniqueMethodName);
            }
        }
    }

    private static void renameFields(JavaClass javaClass, JadxDecompiler jadx, Set<String> existingNames) {
        for (JavaField field : javaClass.getFields()) {
            FieldNode fieldNode = field.getFieldNode();
            if (!field.getName().startsWith("method_") && !field.getName().startsWith("field_")) {
                String uniqueFieldName = generateUniqueName(existingNames, field.getName(), "field_");
                logger.info("Renaming field {} to {}", field.getName(), uniqueFieldName);
                fieldNode.rename(uniqueFieldName);
            }
        }
    }

    private static String generateUniqueName(Set<String> existingNames, String originalName, String prefix) {
        String uniqueName;
        do {
            uniqueName = prefix + originalName + "_" + generateRandomPostfix();
        } while (existingNames.contains(uniqueName));
        existingNames.add(uniqueName);
        return uniqueName;
    }

    public static String generateRandomPostfix() {
        StringBuilder sb = new StringBuilder(POSTFIX_LENGTH);
        for (int i = 0; i < POSTFIX_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}