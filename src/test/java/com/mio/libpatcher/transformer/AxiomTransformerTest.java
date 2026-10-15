package com.mio.libpatcher.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AxiomTransformerTest {
    private static final String PROPERTY = "amethyst.imgui.moulberry92.path";

    @Test
    void androidLibrarySkipsDesktopExtractionAndKeepsInitialization() throws Throwable {
        verifySelection("/android/libimgui-moulberry92.so", "/android/libimgui-moulberry92.so", 0);
    }

    @Test
    void absentOverrideKeepsOriginalDesktopLoader() throws Throwable {
        verifySelection(null, "/desktop/library.so", 1);
    }

    private void verifySelection(String override, String expected, int extractions) throws Throwable {
        String original = System.getProperty(PROPERTY);
        ClassPool pool = new ClassPool(true);
        CtClass fixture = pool.makeClass("imgui.moulberry92.ImGui");
        try {
            if (override == null) {
                System.clearProperty(PROPERTY);
            } else {
                System.setProperty(PROPERTY, override);
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
            new AxiomTransformer().transform(fixture);
            Class<?> loaded = new FixtureLoader().define(fixture.toBytecode());
            assertEquals(expected, loaded.getField("selected").get(null));
            assertEquals(extractions, loaded.getField("extractions").getInt(null));
            assertEquals(true, loaded.getField("initialized").getBoolean(null));
        } finally {
            fixture.detach();
            if (original == null) {
                System.clearProperty(PROPERTY);
            } else {
                System.setProperty(PROPERTY, original);
            }
        }
    }

    private static class FixtureLoader extends ClassLoader {
        Class<?> define(byte[] bytes) {
            return defineClass("imgui.moulberry92.ImGui", bytes, 0, bytes.length);
        }
    }
}
