package com.czy.jrsupplement;

import org.zeroturnaround.javarebel.*;

/**
 * <h1>JrebelFastJsonPlusPlugin</h1>
 *
 * @author czy
 * @since 2023/6/30 12:03
 */
public class JrebelSupplementPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getInstance();

    @Override
    public void preinit() {
        log.infoEcho("Ready config JRebel Supplement plugin...");
         ForFastJson2.init();
         ForFastJson.init();
         ForMybatisPlus.init();
        ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener() {
            @Override
            public void onClassEvent(int i, Class<?> aClass) {
                if (i == 1 || i == 2) {
                    ForFastJson.reload(aClass);
                    ForMybatisPlus.reload(aClass);
                    ForFastJson2.reload(aClass);
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
        return classResourceSource.getClassResource("com.alibaba.fastjson.serializer.SerializeConfig") != null ||
               classResourceSource.getClassResource("com.baomidou.mybatisplus.core.MybatisConfiguration") != null ||
               classResourceSource.getClassResource("org.springframework.cglib.beans.BeanCopier") != null;
    }

    @Override
    public String getId() {
        return "jrebel-supplement-plugin";
    }

    @Override
    public String getName() {
        return "Jrebel Supplement Plugin";
    }

    @Override
    public String getDescription() {
        return "<li>A hook plugin for Support fast_json, mybatis-plus that reloads.</li>";
    }

    @Override
    public String getAuthor() {
        return "czy";
    }

    @Override
    public String getWebsite() {
        return "https://github.com/sujingkpo/jrebel-supplement-plugin";
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
