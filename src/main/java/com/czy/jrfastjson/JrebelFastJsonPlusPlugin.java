package com.czy.jrfastjson;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.zeroturnaround.javarebel.*;

/**
 * <h1>JrebelFastJsonPlusPlugin</h1>
 *
 * @author czy
 * @since 2023/6/30 12:03
 */
public class JrebelFastJsonPlusPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getInstance();

    @Override
    public void preinit() {
        log.infoEcho("Ready config JRebel FastJson plugin...");

        IntegrationFactory.getInstance().addAfterMainCallback(() -> {
            SerializeConfig.getGlobalInstance().setAsmEnable(false);
            log.infoEcho("Init FastJson AsmEnable:" + SerializeConfig.getGlobalInstance().isAsmEnable());
        });

        ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener() {
            @Override
            public void onClassEvent(int i, Class<?> aClass) {
                if (i == 1 || i == 2) {
                    if (SerializeConfig.globalInstance.isAsmEnable()) {
                        SerializeConfig.getGlobalInstance().setAsmEnable(false);
                        log.infoEcho("Reset FastJson AsmEnable:" + SerializeConfig.globalInstance.isAsmEnable());
                    }
                    if (SerializeConfig.getGlobalInstance().get(aClass) != null) {
                        ObjectSerializer javaBeanSerializer = SerializeConfig.getGlobalInstance()
                                                                             .createJavaBeanSerializer(aClass);
                        SerializeConfig.getGlobalInstance().put(aClass, javaBeanSerializer);
                        log.infoEcho("Reload FastJson Serializer:" + aClass.getName());
                    }
                    if (ParserConfig.getGlobalInstance().getDeserializer(aClass) != null) {
                        ObjectDeserializer javaBeanDeserializer = ParserConfig.getGlobalInstance()
                                                                              .createJavaBeanDeserializer(aClass,
                                                                                                          aClass);
                        ParserConfig.getGlobalInstance().putDeserializer(aClass, javaBeanDeserializer);
                        log.infoEcho("Reload FastJson Deserializer:" + aClass.toString());
                    }
                }
            }

            @Override
            public int priority() {
                return 0;
            }
        });
    }

    @Override
    public boolean checkDependencies(ClassLoader classLoader, ClassResourceSource classResourceSource) {
        return classResourceSource.getClassResource("com.alibaba.fastjson.serializer.SerializeConfig") != null;
    }

    @Override
    public String getId() {
        return "jrebelFastJsonPlusPlugin";
    }

    @Override
    public String getName() {
        return "jrebelFastJsonPlusPlugin";
    }

    @Override
    public String getDescription() {
        return "<li>A hook plugin for Support fast_json that reloads.</li>";
    }

    @Override
    public String getAuthor() {
        return "czy";
    }

    @Override
    public String getWebsite() {
        return "https://github.com/sujingkpo";
    }

    @Override
    public String getSupportedVersions() {
        return null;
    }

    @Override
    public String getTestedVersions() {
        return null;
    }
}
