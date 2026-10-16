package com.mio.libpatcher.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImGuiNativeTransformerTest {

    @Test
    void androidLibrarySkipsDesktopExtractionAndKeepsInitialization() throws Throwable {
        for (int version : new int[] {90, 92}) {
            String path = "/android/libimgui-moulberry" + version + ".so";
            verifySelection(version, path, path, 0);
        }
    }

    @Test
    void absentOverrideKeepsOriginalDesktopLoader() throws Throwable {
        for (int version : new int[] {90, 92}) {
            verifySelection(version, null, "/desktop/library.so", 1);
        }
    }

    @Test
    void versionsSelectIndependentPathsWhenBothPropertiesAreSet() throws Throwable {
        String property90 = "amethyst.imgui.moulberry90.path";
        String property92 = "amethyst.imgui.moulberry92.path";
        String original90 = System.getProperty(property90);
        String original92 = System.getProperty(property92);
        try {
            System.setProperty(property90, "/android/libimgui-moulberry90.so");
            System.setProperty(property92, "/android/libimgui-moulberry92.so");
            verifySelection(90, System.getProperty(property90),
                    "/android/libimgui-moulberry90.so", 0);
            verifySelection(92, System.getProperty(property92),
                    "/android/libimgui-moulberry92.so", 0);
        } finally {
            restoreProperty(property90, original90);
            restoreProperty(property92, original92);
        }
    }

    private void restoreProperty(String property, String original) {
        if (original == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, original);
        }
    }

    private void verifySelection(
            int version,
            String override,
            String expected,
            int extractions
    ) throws Throwable {
        String property = "amethyst.imgui.moulberry" + version + ".path";
        String className = "imgui.moulberry" + version + ".ImGui";
        String original = System.getProperty(property);
        ClassPool pool = new ClassPool(true);
        CtClass fixture = pool.makeClass(className);
        try {
            if (override == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, override);
            }
            fixture.addField(CtField.make("public static String selected;", fixture));
            fixture.addField(CtField.make("public static int extractions;", fixture));
            fixture.addField(CtField.make("public static boolean initialized;", fixture));
            fixture.addMethod(CtMethod.make(
                    "private static String tryLoadFromClasspath(String name) {"
                            + " extractions++; return \"/desktop/library.so\"; }",
                    fixture
            ));
            fixture.makeClassInitializer().setBody(
                    "{ selected = tryLoadFromClasspath(\"library\"); initialized = true; }"
            );
            new ImGuiNativeTransformer(version).transform(fixture);
            byte[] transformed = fixture.toBytecode();
            FixtureLoader loader = new FixtureLoader();
            Class<?> loaded = loader.define(className, transformed);
            assertEquals(expected, loaded
                    .getField("selected")
                    .get(null));
            assertEquals(extractions, loaded
                    .getField("extractions")
                    .getInt(null));
            assertEquals(true, loaded
                    .getField("initialized")
                    .getBoolean(null));
        } finally {
            fixture.detach();
            restoreProperty(property, original);
        }
    }

    private static class FixtureLoader extends ClassLoader {
        Class<?> define(String className, byte[] bytes) {
            return defineClass(className, bytes, 0, bytes.length);
        }
    }
}
