package com.lauriewired.analyzer;

import java.io.File;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.ResourceFile;

public class JadxUtils {
    private static final Logger logger = LoggerFactory.getLogger(JadxUtils.class);

    public static JadxDecompiler loadJadx(String apkFilePath) {
        File apkFile = new File(apkFilePath);
        File outputDir = new File(apkFile.getParent(), "output_temp");

        JadxArgs jadxArgs = new JadxArgs();
        jadxArgs.setInputFile(apkFile);
        jadxArgs.setOutDir(outputDir);
        jadxArgs.setShowInconsistentCode(true);
        jadxArgs.setSkipResources(true);
        jadxArgs.setDeobfuscationOn(true);
        jadxArgs.setUseSourceNameAsClassAlias(true);

        JadxDecompiler jadx = new JadxDecompiler(jadxArgs);
        try {
            jadx.load();
            //jadx.save();
        } catch (Exception e) {
            logger.error("Error loading APK", e);
        }

        return jadx;
    }

    public static JavaClass getJavaClassByName(JadxDecompiler jadx, String className) {
        for (JavaClass javaClass : jadx.getClasses()) {
            if (javaClass.getFullName().equals(className)) {
                return javaClass;
            }
        }
        return null; // Class not found
    }

    public static JavaClass findApplicationSubclass(JadxDecompiler jadx) {
        for (JavaClass javaClass : jadx.getClasses()) {
            if (javaClass.getClassNode().getSuperClass().toString().equals("android.app.Application")) {
                logger.info("Found Application subclass: {}", javaClass.getFullName());
                return javaClass;
            }
        }
        return null;
    }

    public static Set<String> getManifestClasses(String apkFilePath, JadxDecompiler jadx) throws Exception {
        Set<String> classNames = new HashSet<>();

        String manifestContent = "";
        for (ResourceFile resource : jadx.getResources()) {
            if (resource.getOriginalName().equals("AndroidManifest.xml")) {
                logger.info("Found AndroidManifest.xml");
                manifestContent = resource.loadContent().getText().getCodeStr();
                break;
            }
        }

        if (!manifestContent.equals("")) {
            // Parse the AndroidManifest.xml
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(manifestContent)); // Use StringReader here

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("activity".equals(name) || "service".equals(name) || "receiver".equals(name) || "provider".equals(name)) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if ("android:name".equals(parser.getAttributeName(i))) {
                                String className = parser.getAttributeValue(i);
                                logger.info("Manifest class found: {}", className);
                                classNames.add(className.replace(".", "/") + ".class");
                            }
                        }
                    }
                }
                eventType = parser.next();
            }
        }

        return classNames;
    }

    public static Set<String> getDexClasses(String apkFilePath, JadxDecompiler jadx) throws Exception {
        Set<String> classNames = new HashSet<>();

        for (JavaClass cls : jadx.getClasses()) {
            logger.info("Dex class found: {}", cls.getFullName());
            classNames.add(cls.getRawName().replace('.', '/') + ".class");
        }

        return classNames;
    }
}
