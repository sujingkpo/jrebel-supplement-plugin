package com.czy.jrsupplement.bean;

import org.zeroturnaround.bundled.javassist.ClassPool;
import org.zeroturnaround.bundled.javassist.CtClass;
import org.zeroturnaround.bundled.javassist.CtMethod;
import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.ReloaderFactory;
import org.zeroturnaround.javarebel.integration.support.JavassistClassBytecodeProcessor;

public class BeanCopierCPB extends JavassistClassBytecodeProcessor {

    private static final Logger log = LoggerFactory.getInstance();
    @Override
    public void process(ClassPool cp, ClassLoader cl, CtClass ctClass) throws Exception {

        CtMethod newInstanceMethod = ctClass.getDeclaredMethod("generateClass");
        cp.importPackage("org.zeroturnaround.javarebel.IntegrationFactory");
        cp.importPackage("org.springframework.cglib.core.DebuggingClassWriter");
        cp.importPackage("org.zeroturnaround.javarebel.ReloaderFactory");
//        ReloaderFactory.getInstance().reinitOnReload();
//        newInstanceMethod.insertAfter(
//                                  "IntegrationFactory.getInstance().defineReloadableClass(getDefaultClassLoader(), getClassName(),((DebuggingClassWriter)v).toByteArray(), getProtectionDomain());",
//                                  true);

        newInstanceMethod.insertAfter("ReloaderFactory.getInstance().reinitOnReload(getClassName());");
        log.infoEcho("org.springframework.cglib.beans.BeanCopier.Generator");
    }
}
