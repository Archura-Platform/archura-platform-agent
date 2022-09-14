package io.archura.platform.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class FilterFunctionTransformer implements ClassFileTransformer {

    private final String executorClassName;

    public FilterFunctionTransformer(final String className) {
        this.executorClassName = className;
    }

    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        byteCode = handleSocketClass(className, classfileBuffer, byteCode);
        return byteCode;
    }

    private byte[] handleSocketClass(String className, byte[] classfileBuffer, byte[] byteCode) {
        if (className.startsWith("java/net/") && className.contains("Socket")) {
            try {
                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                final CtConstructor[] constructors = ctClass.getConstructors();
                for (CtConstructor constructor : constructors) {
                    constructor.insertBefore(getAllowCheckCode());
                }
                byteCode = ctClass.toBytecode();
                ctClass.detach();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return byteCode;
    }

    private String getAllowCheckCode() {
        return "                     for (int i=0; i< Thread.currentThread().getStackTrace().length; i++) {\n" +
                "                        StackTraceElement element = Thread.currentThread().getStackTrace()[i];\n" +
                "                        final String className = element.getClassName();\n" +
                "                        final String methodName = element.getMethodName();\n" +
                "                        if(className.contains(\"jdk.internal.net.http.HttpClientImpl\")) {\n" +
                "                            //System.out.println(\">>>>> EXPLICITLY ALLOWED className = \" + className + \" \" + Thread.currentThread().getName() + \" \" + methodName);\n" +
                "                            break;\n" +
                "                        }\n" +
                "                        if(className.contains(\"" + executorClassName + "\")){\n" +
                "                            //System.out.println(\">>>>> DENIED className = \" + className + \" \" + Thread.currentThread().getName() + \" \" + methodName);\n" +
                "                            throw new RuntimeException(\"Filters/Functions are not allowed to create Sockets.\");\n" +
                "                        }\n" +
                "                        //System.out.println(\">>>>> IMPLICITLY ALLOWED className = \" + className + \":\" + methodName + \" [\" + Thread.currentThread().getName() + \"]  this.getClass().getName(): \" + this.getClass().getName());\n" +
                "                    };";
    }

}
