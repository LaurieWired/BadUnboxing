package com.lauriewired.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class DynamicDexLoaderDetection  {
    private static final Logger logger = LoggerFactory.getLogger(DynamicDexLoaderDetection.class);

    private static final Set<String> dynamicDexLoadingKeywords = new HashSet<>(Arrays.asList(
        "DexClassLoader", "PathClassLoader", "InMemoryDexClassLoader", "BaseDexClassLoader", "loadDex", "OpenMemory"
    ));

    public static List<String> getJavaDexLoadingDetails(JadxDecompiler jadx) {
        List<String> details = new ArrayList<>();
        for (JavaClass cls : jadx.getClasses()) {
            String classCode = cls.getCode();
            for (String keyword : dynamicDexLoadingKeywords) {
                if (classCode.contains(keyword)) {
                    String detail = String.format("Found keyword '%s' in class '%s'", keyword, cls.getFullName());
                    logger.info(detail);
                    details.add(detail);
                }
            }
        }
        return details;
    }

    public static boolean hasNativeDexLoading() {
        // TODO

        // Save the jadx output directory and find the native libs (files ending in .so)
        
        return false;
    }
}
