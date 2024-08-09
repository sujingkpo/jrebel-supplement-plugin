package com.czy.jrsupplement;

import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ForFastJson2 {
    private static final Logger log = LoggerFactory.getInstance();
    /**
     * fastjson2是否启用
     */
    private static boolean FAST_JSON_2_PLUGIN_ENABLE = false;

    private static Class JSONFactory;

    private static Method getDefaultObjectWriterProvider;
    private static Method getDefaultObjectReaderProvider;
    private static Method cleanupCache;
    private static Class BeanUtils;
    private static Method readerCleanUp;
    private static Method writerCleanUp;
    /**
     * fastjson2是否加载
     */
    private static int FAST_JSON_2_PLUGIN_LOADED_COUNT = 0;

    public static void init() {
        if (!FAST_JSON_2_PLUGIN_ENABLE && FAST_JSON_2_PLUGIN_LOADED_COUNT == 0) {
            FAST_JSON_2_PLUGIN_LOADED_COUNT++;
            log.infoEcho("Check Fast Json 2 Plugin...");
            try {
                System.setProperty("fastjson2.creator", "reflect");// 默认使用反射创建对象 禁用 asm
                BeanUtils = Class.forName("com.alibaba.fastjson2.util.BeanUtils");
                log.infoEcho("Init <<fastjson2.creator>>:" + System.getProperty("fastjson2.creator"));
                JSONFactory = Class.forName("com.alibaba.fastjson2.JSONFactory");
                Field get = Arrays.stream(JSONFactory.getDeclaredFields())
                                  .filter(m -> m.getName().equals("CREATOR"))
                                  .findAny()
                                  .orElse(null);
                if (null == get) {
                    FAST_JSON_2_PLUGIN_ENABLE = false;
                    return;
                }
                cleanupCache = BeanUtils.getDeclaredMethod("cleanupCache", Class.class);
                cleanupCache.setAccessible(true);
                FAST_JSON_2_PLUGIN_ENABLE = true;

            } catch (ClassNotFoundException | NoSuchMethodException e) {
                FAST_JSON_2_PLUGIN_ENABLE = false;
                return;
            } finally {
                log.infoEcho("For Fast Json 2 Plugin Enable:" + FAST_JSON_2_PLUGIN_ENABLE);
            }
        }
        if (FAST_JSON_2_PLUGIN_ENABLE) {
            Arrays.stream(JSONFactory.getDeclaredMethods()).forEach(m -> {

                if (m.getName().equals("getDefaultObjectWriterProvider")) {
                    getDefaultObjectWriterProvider = m;
                    log.infoEcho("Init Method Info :" + m.getName());
                }
                if (m.getName().equals("getDefaultObjectReaderProvider")) {
                    getDefaultObjectReaderProvider = m;
                    log.infoEcho("Init Method Info :" + m.getName());
                }
            });
        }
    }

    public static void reload(Class<?> aClass) {
        if (FAST_JSON_2_PLUGIN_ENABLE) {
            try {
                Object readerProvider = getDefaultObjectReaderProvider.invoke(null);
//                log.infoEcho("reader", readerProvider);

                Object writerProvider = getDefaultObjectWriterProvider.invoke(null);

//                log.infoEcho("writer", writerProvider);
                if (readerProvider != null) {
                    if (readerCleanUp == null) {

                        readerCleanUp = readerProvider.getClass().getDeclaredMethod("cleanup", Class.class);
                        readerCleanUp.setAccessible(true);
                    }
                    readerCleanUp.invoke(readerProvider, aClass);
//                    log.infoEcho("FastJSON2 ObjectReaderProvider CleanUp :"+ aClass.getName());
                }
                if (writerProvider != null) {
                    if (writerCleanUp == null) {
                        writerCleanUp = writerProvider.getClass().getDeclaredMethod("cleanup", Class.class);
                        writerCleanUp.setAccessible(true);
                    }
                    writerCleanUp.invoke(writerProvider, aClass);
//                    log.infoEcho("FastJSON2 ObjectWriterProvider CleanUp :"+ aClass.getName());
                }
                cleanupCache.invoke(null, aClass);
//                log.infoEcho("FastJSON2 BeanUtils CleanupCache :"+ aClass.getName());
            } catch (Exception e) {
                FAST_JSON_2_PLUGIN_ENABLE = false;
                log.errorEcho("For Fast Json 2 Plugin Error:" + e.getMessage());
            }
        }
    }
}
