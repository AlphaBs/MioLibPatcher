package com.mio.libpatcher.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class AxiomTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "imgui.moulberry92.ImGui";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        CtConstructor initializer = clazz.getClassInitializer();
        if (initializer == null) {
            return;
        }

        // 데스크톱 네이티브 추출을 건너뛰되 이후 JNI 초기화는 그대로 수행한다.
        initializer.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(getTargetClassName())
                        && call.getMethodName().equals("tryLoadFromClasspath")) {
                    call.replace(
                            "{ String path = System.getProperty(\"amethyst.imgui.moulberry92.path\");"
                                    + " if (path != null) { $_ = path; }"
                                    + " else { $_ = $proceed($$); } }"
                    );
                }
            }
        });
    }
}
