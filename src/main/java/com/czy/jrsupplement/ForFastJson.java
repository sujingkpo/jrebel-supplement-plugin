package com.czy.jrsupplement;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.zeroturnaround.javarebel.IntegrationFactory;
import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ForFastJson {
    private static final Logger log = LoggerFactory.getInstance();
    // FastJsonPlus插件加载次数
    private static Integer FAST_JSON_PLUGIN_LOADED_COUNT = 0;

    // 是否启用FastJsonPlus插件
    private static Boolean FAST_JSON_PLUGIN_ENABLE = false;

    public static void init() {
        if (!FAST_JSON_PLUGIN_ENABLE && FAST_JSON_PLUGIN_LOADED_COUNT == 0) {
            FAST_JSON_PLUGIN_LOADED_COUNT++;
            log.infoEcho("Check Fast Json Plugin...");
            try {
                Class<?> aClass = Class.forName("com.alibaba.fastjson.serializer.SerializeConfig");
                Field get = Arrays.stream(aClass.getDeclaredFields())
                                  .filter(m -> m.getName().equals("asm"))
                                  .findAny()
                                  .orElse(null);
                if (null == get) {
                    FAST_JSON_PLUGIN_ENABLE = false;
                    return;
                }
                FAST_JSON_PLUGIN_ENABLE = true;

            } catch (ClassNotFoundException e) {
                FAST_JSON_PLUGIN_ENABLE = false;
                return;
            } finally {
                log.infoEcho("For Fast Json Plugin:" + FAST_JSON_PLUGIN_ENABLE);
            }
        }
        if (FAST_JSON_PLUGIN_ENABLE) {
            IntegrationFactory.getInstance().addAfterMainCallback(() -> {
                SerializeConfig.getGlobalInstance().setAsmEnable(false);
                ParserConfig.getGlobalInstance().setAsmEnable(false);
                log.infoEcho("Init FastJson AsmEnable:" + SerializeConfig.getGlobalInstance().isAsmEnable());
            });
        }
    }

    public static void reload(Class<?> aClass) {
        if (FAST_JSON_PLUGIN_ENABLE) {
            if (SerializeConfig.getGlobalInstance().isAsmEnable()) {
                SerializeConfig.getGlobalInstance().setAsmEnable(false);
                log.infoEcho("Reset FastJson AsmEnable:" + SerializeConfig.getGlobalInstance().isAsmEnable());
            }
            if (ParserConfig.getGlobalInstance().isAsmEnable()) {
                ParserConfig.getGlobalInstance().setAsmEnable(false);
                log.infoEcho("Reset FastJson AsmEnable:" + ParserConfig.getGlobalInstance().isAsmEnable());
            }
            if (null != SerializeConfig.getGlobalInstance().get(aClass)) {
                ObjectSerializer javaBeanSerializer = SerializeConfig.getGlobalInstance()
                                                                     .createJavaBeanSerializer(aClass);
                SerializeConfig.getGlobalInstance().put(aClass, javaBeanSerializer);
                log.infoEcho("Reload FastJson Serializer:" + aClass.getName());
            }
            if (null != ParserConfig.getGlobalInstance().getDeserializers().get(aClass)) {
                ObjectDeserializer javaBeanDeserializer = ParserConfig.getGlobalInstance()
                                                                      .createJavaBeanDeserializer(aClass, aClass);
                ParserConfig.getGlobalInstance().putDeserializer(aClass, javaBeanDeserializer);
                log.infoEcho("Reload FastJson Deserializer:" + aClass.toString());
            }
        }

    }
}
