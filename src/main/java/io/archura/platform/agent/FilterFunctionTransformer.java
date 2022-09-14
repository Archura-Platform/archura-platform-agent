package io.archura.platform.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

public class FilterFunctionTransformer implements ClassFileTransformer {

    private final Logger log = Logger.getLogger(FilterFunctionTransformer.class.getSimpleName());
    private final String executorClassName;

    public FilterFunctionTransformer(final String className) {
        this.executorClassName = className;
    }

    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;
        byteCode = handleThreadClass(className, classfileBuffer, byteCode);
        byteCode = handleSocketClass(className, classfileBuffer, byteCode);
        return byteCode;
    }

    private byte[] handleThreadClass(String className, byte[] classfileBuffer, byte[] byteCode) {
        if (className.contains("java/lang/Thread")) {
            try {
                ClassPool classPool = ClassPool.getDefault();
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                CtMethod[] methods = ctClass.getDeclaredMethods();
                for (CtMethod method : methods) {
                    if (method.getName().contains("start")
                            || method.getName().contains("next")) {
                        try {
                            method.insertBefore(getAllowCheckCode());
                        } catch (CannotCompileException error) {
                            if ("no method body".equalsIgnoreCase(error.getMessage())) {
                                log.warning("Could not compile and insert code, 'no method body' in the method: " + method.getLongName());
                            } else {
                                log.severe("Could not compile and insert code, method: " + method.getLongName() + " error: " + error.getMessage());
                            }
                        } catch (Throwable throwable) {
                            log.severe("Error occurred while compiling and inserting code, method: " + method.getLongName() + " error: " + throwable.getMessage());
                            throwable.printStackTrace();
                        }
                    }
                }
                byteCode = ctClass.toBytecode();
                ctClass.detach();
            } catch (Throwable throwable) {
                System.out.println("throwable.getMessage() = " + throwable.getMessage());
            }
        }
        return byteCode;
    }

    private byte[] handleSocketClass(String className, byte[] classfileBuffer, byte[] byteCode) {
        if ((className.startsWith("java/net/") && className.contains("Socket"))
                || className.contains("java/util/concurrent/AbstractExecutorService")
                || className.contains("java/lang/Thread")
        ) {
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
                "                        final String className = Thread.currentThread().getStackTrace()[i].getClassName();\n" +
                "                        if(className.contains(\"jdk.internal.net.http.HttpClientImpl\")) {\n" +
                "                            break;\n" +
                "                        }\n" +
                "                        if(className.contains(\"" + executorClassName + "\")){\n" +
                "                            throw new RuntimeException(\"Filters/Functions are not allowed to create Sockets/Threads.\");\n" +
                "                        }\n" +
                "                    };";
    }

}
