package io.esastack.cabin.agent;


import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class CabinInjectClassFileTransformer implements ClassFileTransformer {

    private static final String[] SKIP_PREFIXES = new String[]{
            "java/", "javax/", "io/esastack/cabin", "org/springframework"};

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) {
                return classfileBuffer;
            }
        }
        className = className.replace("/", ".");
        final ClassPool classPool = new ClassPool();
        classPool.insertClassPath(new LoaderClassPath(loader));
        CtClass clazz = null;
        try {
            clazz = classPool.get(className);
            final CtClass stringClazz = ClassPool.getDefault().get("java.lang.String[]");
            CtMethod convertToAbbr = clazz.getDeclaredMethod("main", new CtClass[]{stringClazz});
            String methodBody = "{io.esastack.cabin.support.bootstrap.CabinAppBootstrap.run(args);}";
            convertToAbbr.insertBefore(methodBody);
            return clazz.toBytecode();
        } catch (Throwable ex) {
            //ignore
        } finally {
            clazz.detach();
        }
        return classfileBuffer;
    }
}
