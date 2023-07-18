package com.czy.jrsupplement.bean;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.internal.LoadingCache;
import org.springframework.stereotype.Component;
import org.zeroturnaround.javarebel.Logger;
import org.zeroturnaround.javarebel.LoggerFactory;
import org.zeroturnaround.javarebel.integration.util.ReloaderUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

@Component
public class BeanCopierReload {
    private static final Logger log = LoggerFactory.getInstance();
    private static final String BEAN_COPIER_KEY = "org.springframework.cglib.beans.BeanCopier$BeanCopierKey";
    private static final String CLASS_LOADER_DATA
            = "org.springframework.cglib.core.AbstractClassGenerator$ClassLoaderData";

    private static Field CACHE_FIELD;
    private static Field GENERATED_CLASSES_FIELD;
    private static Field LOADING_CACHE_MAP_FIELD;
    private static Field FIELD_0;
    private static Field FIELD_1;
    private static ClassLoader CLASS_LOADER;

    static {

        initField();
    }

    public static void initField() {
        try {
            Field cacheField = Arrays.stream(AbstractClassGenerator.class.getDeclaredFields())
                                     .filter(v -> v.getName().equals("CACHE"))
                                     .findFirst()
                                     .orElse(null);
            if (cacheField != null) {
                cacheField.setAccessible(true);
                CACHE_FIELD = cacheField;
            }

            Field generatedClasses = Arrays.stream(Class.forName(CLASS_LOADER_DATA).getDeclaredFields())
                                           .filter(v -> v.getName().equals("generatedClasses"))
                                           .findFirst()
                                           .orElse(null);
            if (generatedClasses != null) {
                generatedClasses.setAccessible(true);
                GENERATED_CLASSES_FIELD = generatedClasses;
            }

            Field loadingCacheMapField = Arrays.stream(LoadingCache.class.getDeclaredFields())
                                               .filter(v -> v.getName().equals("map"))
                                               .findFirst()
                                               .orElse(null);

            if (loadingCacheMapField != null) {
                loadingCacheMapField.setAccessible(true);
                LOADING_CACHE_MAP_FIELD = loadingCacheMapField;
            }

            BeanCopier.Generator generator = new BeanCopier.Generator();
            generator.setSource(BeanCopier.class);
            CLASS_LOADER = generator.getClassLoader();

        } catch (Exception ignored) {

        }

    }

    public void initFieldKeyFactoryField(Object o) {
        try {

            Field[] declaredFields = o.getClass().getDeclaredFields();
            Field field0 = Arrays.stream(declaredFields)
                                 .filter(v -> v.getName().equals("FIELD_0"))
                                 .findFirst()
                                 .orElse(null);
            Field field1 = Arrays.stream(declaredFields)
                                 .filter(v -> v.getName().equals("FIELD_1"))
                                 .findFirst()
                                 .orElse(null);

            if (field0 != null) {
                field0.setAccessible(true);
                FIELD_0 = field0;
            }
            if (field1 != null) {
                field1.setAccessible(true);
                FIELD_1 = field1;
            }

        } catch (Exception ignored) {

        }
    }

    public void reload(Class<?> aClass) {
        try {
            Map<ClassLoader, Object> cache = (Map<ClassLoader, Object>) CACHE_FIELD.get(null);
            Object cacheData = cache.get(CLASS_LOADER);
            LoadingCache<?, Object, Object> cached = (LoadingCache<?, Object, Object>) GENERATED_CLASSES_FIELD.get(
                    cacheData);

            Map<?, Object> map = (Map<?, Object>) LOADING_CACHE_MAP_FIELD.get(cached);
            map.keySet().forEach(o1 -> {
                if (o1.getClass().getName().contains(BEAN_COPIER_KEY)) {
//                    map.remove(o1);
                    Object o = map.get(o1);
                    try {
                        initFieldKeyFactoryField(o1);
                        if (FIELD_0 != null) {
                            String className = (String) FIELD_0.get(o1);
                            if (className.equals(aClass.getName())) {
                               ReloaderUtil.getReloadableClassHierarchies(Arrays.asList(aClass, o.getClass())).forEach(v->{
                                    log.infoEcho(v.getName());
                                });
                            }

                        }
                        if (FIELD_1 != null) {
                            String className = (String) FIELD_1.get(o1);
                            if (className.equals(aClass.getName())) {
                                ReloaderUtil.getReloadableClassHierarchies(Arrays.asList(aClass, o.getClass())).forEach(v->{
                                    log.infoEcho(v.getName());
                                });



                            }

                        }
                    } catch (Exception ignored) {

                    }

                }
            });
        } catch (Exception ignored) {

        }
    }

}
