package com.mio.libpatcher.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ImGuiNativeTransformer implements BaseTransformer {
    private final String targetClass;
    private final String libraryProperty;

    public ImGuiNativeTransformer(int version) {
        this.targetClass = "imgui.moulberry" + version + ".ImGui";
        this.libraryProperty = "amethyst.imgui.moulberry" + version + ".path";
    }

    @Override
    public String getTargetClassName() {
        return targetClass;
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        CtConstructor initializer = clazz.getClassInitializer();
        if (initializer == null) {
            return;
        }

        // 지정한 외부 파일을 우선 로드하되 이후 JNI 초기화는 그대로 수행한다.
        initializer.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                String owner = call.getClassName();
                String method = call.getMethodName();
                if (owner.equals("java.lang.System")
                        && (method.equals("load") || method.equals("loadLibrary"))) {
                    // 공용 imgui.library.name이 다른 버전의 라이브러리를 가리킬 수 있다.
                    call.replace(
                            "{ String path = System.getProperty(\"" + libraryProperty + "\");"
                                    + " if (path != null) { java.lang.System.load(path); }"
                                    + " else { $proceed($$); } }"
                    );
                } else if (owner.equals(targetClass) && method.equals("tryLoadFromClasspath")) {
                    call.replace(
                            "{ String path = System.getProperty(\"" + libraryProperty + "\");"
                                    + " if (path != null) { $_ = path; }"
                                    + " else { $_ = $proceed($$); } }"
                    );
                }
            }
        });
    }
}
