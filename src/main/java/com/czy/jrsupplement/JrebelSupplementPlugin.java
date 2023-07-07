package com.czy.jrsupplement;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.override.MybatisMapperProxyFactory;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.session.Configuration;
import org.zeroturnaround.javarebel.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * <h1>JrebelFastJsonPlusPlugin</h1>
 *
 * @author czy
 * @since 2023/6/30 12:03
 */
public class JrebelSupplementPlugin implements Plugin {
    private static final Logger log = LoggerFactory.getInstance();
    private static DefaultSqlInjector defaultSqlInjector;
    private Boolean MYBATIS_PLUS_PLUGIN_ENABLE = false;
    private Boolean FAST_JSON_PLUGIN_ENABLE = false;
    private Integer MYBATIS_PLUS_PLUGIN_LOADED_COUNT = 0;
    private Integer FAST_JSON_PLUGIN_LOADED_COUNT = 0;

    private static void mapperProxyFactoryRemove(Class<?> aClass, Configuration configuration) {
        List<Class<?>> mappers = configuration.getMapperRegistry().getMappers().stream().filter(aClass1 -> {
            Class<?>[] classes = GenericTypeUtils.resolveTypeArguments(aClass1, BaseMapper.class);
            if (classes != null && classes.length > 0) {
                return Arrays.asList(classes).contains(aClass);
            }
            return false;
        }).collect(Collectors.toList());
        Field statements = Arrays.stream(configuration.getClass().getDeclaredFields())
                                 .filter(v -> v.getName().equals("mappedStatements"))
                                 .findFirst()
                                 .orElse(null);
        Field statements_supper = Arrays.stream(configuration.getClass().getSuperclass().getDeclaredFields())
                                        .filter(v -> v.getName().equals("mappedStatements"))
                                        .findFirst()
                                        .orElse(null);
        Map<String, MappedStatement> statementsMap = new HashMap<>();
        Map<String, MappedStatement> statementsSupperMap = new HashMap<>();
        if (statements != null) {
            statements.setAccessible(true);
            try {
                statementsMap = (Map<String, MappedStatement>) statements.get(configuration);
            } catch (IllegalAccessException e) {
                log.infoEcho("Get MappedStatements:[ERROR]:" + aClass.getName());
                log.error(e.getMessage());
            }
        }
        if (statements_supper != null) {
            statements_supper.setAccessible(true);
            try {
                statementsSupperMap = (Map<String, MappedStatement>) statements_supper.get(configuration);
            } catch (IllegalAccessException e) {
                log.infoEcho("Get SupperMappedStatements:[ERROR]:" + aClass.getName());
                log.error(e.getMessage());
            }
        }
        ((MybatisConfiguration) configuration).addNewMapper(aClass);
        if (mappers.size() > 0) {
            Map<String, MappedStatement> finalStatementsMap = statementsMap;
            Map<String, MappedStatement> finalStatementsSupperMap = statementsSupperMap;
            Arrays.stream(configuration.getMapperRegistry().getClass().getDeclaredFields())
                  .filter(field -> field.getName().equals("knownMappers"))
                  .findFirst()
                  .ifPresent(field -> {
                      field.setAccessible(true);
                      try {
                          Map<Class<?>, MybatisMapperProxyFactory<?>> proxyFactoryMap
                                  = (Map<Class<?>, MybatisMapperProxyFactory<?>>) field.get(
                                  configuration.getMapperRegistry());
                          if (proxyFactoryMap != null) {
                              mappers.forEach(m -> {
                                  Optional.ofNullable(proxyFactoryMap.get(m))
                                          .ifPresent(factory -> factory.getMethodCache().clear());
                                  log.infoEcho("Remove MapperProxyFactory Cache:" + m.getName());
                                  MapperBuilderAssistant mba = new MapperBuilderAssistant(configuration, m.toString());
                                  mba.setCurrentNamespace(m.toString());
                                  TableInfo tableInfo = reInitTableInfoCache(aClass, mba);
                                  List<AbstractMethod> methodList = defaultSqlInjector.getMethodList(m, tableInfo);
                                  Field methodName = Arrays.stream(AbstractMethod.class.getDeclaredFields())
                                                           .filter(v -> v.getName().equals("methodName"))
                                                           .findFirst()
                                                           .orElse(null);
                                  for (AbstractMethod method : methodList) {
                                      if (methodName != null) {
                                          try {
                                              methodName.setAccessible(true);
                                              String _methodName = (String) methodName.get(method);
                                              String _mn = m + "." + _methodName;
                                              String _mn2 = m.getName() + "." + _methodName;
                                              finalStatementsMap.remove(_mn);
                                              finalStatementsMap.remove(_mn2);
                                              finalStatementsSupperMap.remove(_mn);
                                              finalStatementsSupperMap.remove(_mn2);

                                              method.inject(mba, m, aClass, tableInfo);
                                              MappedStatement mappedStatement = configuration.getMappedStatement(_mn);
                                              if (mappedStatement != null) {
                                                  MappedStatement ms2 = getMappedStatement(_mn2, mappedStatement);
                                                  configuration.addMappedStatement(ms2);
                                                  log.infoEcho("ReInject method:" + _mn2);
                                              }
                                              log.infoEcho("ReInject method:" + _mn);
                                          } catch (IllegalAccessException e) {
                                              log.infoEcho("GetMethodName:[ERROR]:" + aClass.getName());
                                              log.error(e.getMessage());
                                          }
                                      }
                                  }
                              });
                          }
                      } catch (IllegalAccessException e) {

                          log.infoEcho("Remove MapperProxyFactory Cache:[ERROR]:" + aClass.getName());
                          log.error(e.getMessage());
                      }
                  });
        }

    }

    private static MappedStatement getMappedStatement(String _mn2, MappedStatement mappedStatement) {
        MappedStatement.Builder builder = new MappedStatement.Builder(mappedStatement.getConfiguration(), _mn2,
                                                                      mappedStatement.getSqlSource(),
                                                                      mappedStatement.getSqlCommandType());
        builder.cache(mappedStatement.getCache());
        builder.databaseId(mappedStatement.getDatabaseId());
        builder.fetchSize(mappedStatement.getFetchSize());
        builder.flushCacheRequired(mappedStatement.isFlushCacheRequired());
        builder.keyGenerator(mappedStatement.getKeyGenerator());
        builder.lang(mappedStatement.getLang());
        builder.parameterMap(mappedStatement.getParameterMap());
        builder.resource(mappedStatement.getResource());
        builder.resultMaps(mappedStatement.getResultMaps());
        builder.resultOrdered(mappedStatement.isResultOrdered());
        builder.timeout(mappedStatement.getTimeout());
        builder.resultSetType(mappedStatement.getResultSetType());
        builder.statementType(mappedStatement.getStatementType());
        builder.useCache(mappedStatement.isUseCache());
        return builder.build();
    }

    private static void defaultReflectorRemove(Class<?> aClass, Configuration configuration) {
        DefaultReflectorFactory reflectorFactory = (DefaultReflectorFactory) configuration.getReflectorFactory();

        Arrays.stream(reflectorFactory.getClass().getDeclaredFields())
              .filter(field -> field.getName().equals("reflectorMap"))
              .findFirst()
              .ifPresent(field -> {
                  field.setAccessible(true);
                  try {
                      ConcurrentMap<Class<?>, Reflector> map = (ConcurrentMap<Class<?>, Reflector>) field.get(
                              reflectorFactory);
                      if (map != null) {
                          log.infoEcho("Remove DefaultReflector Cache:" + aClass.getName());
                          map.remove(aClass);
                      }
                  } catch (IllegalAccessException e) {
                      log.infoEcho("Remove DefaultReflector Cache:[ERROR]:" + aClass.getName());
                      log.error(e.getMessage());
                  }
              });
    }

    private static void removeTableInfoCache(Class<?> aClass, TableInfo tableInfo) {
        TableInfoHelper.remove(aClass);
        if (tableInfo != null) {
            Arrays.stream(TableInfoHelper.class.getDeclaredFields())
                  .filter(field -> field.getName().equals("TABLE_NAME_INFO_CACHE"))
                  .findFirst()
                  .ifPresent(field -> {
                      field.setAccessible(true);
                      try {
                          Map<String, TableInfo> tableInfoMap = (Map<String, TableInfo>) field.get(
                                  TableInfoHelper.class);
                          if (tableInfoMap != null) {
                              log.infoEcho("Remove Table Name Info Cache:" + aClass.getName());
                              tableInfoMap.remove(tableInfo.getTableName());
                          }
                      } catch (IllegalAccessException e) {
                          log.infoEcho("Remove Table Name Info Cache[ERROR]:" + aClass.getName());
                          log.error(e.getMessage());
                      }
                  });
        }

        Arrays.stream(ReflectionKit.class.getDeclaredFields())
              .filter(field -> field.getName().equals("CLASS_FIELD_CACHE"))
              .findFirst()
              .ifPresent(field -> {
                  field.setAccessible(true);
                  try {
                      Map<Class<?>, List<Field>> fields = (Map<Class<?>, List<Field>>) field.get(ReflectionKit.class);
                      if (fields != null) {
                          log.infoEcho("Remove Class Field Cache:" + aClass.getName());
                          fields.remove(aClass);
                      }
                  } catch (IllegalAccessException e) {
                      log.infoEcho("Remove Class Field Cache[ERROR]:" + aClass.getName());
                      log.error(e.getMessage());
                  }
              });
        log.infoEcho("Remove Table Info Cache:" + aClass.getName());
        if (aClass.getSuperclass() != null && !aClass.getSuperclass().equals(Object.class)) {
            removeTableInfoCache(aClass.getSuperclass(), null);
        }
    }

    private static TableInfo reInitTableInfoCache(Class<?> aClass, MapperBuilderAssistant mba) {
        log.infoEcho("ReInit TableInfoCache:" + aClass.getName());
        return TableInfoHelper.initTableInfo(mba, aClass);
    }

    private void forFastJson(Class<?> aClass) {
        if (FAST_JSON_PLUGIN_ENABLE) {
            if (SerializeConfig.getGlobalInstance().isAsmEnable()) {
                SerializeConfig.getGlobalInstance().setAsmEnable(false);
                log.infoEcho("Reset FastJson AsmEnable:" + SerializeConfig.globalInstance.isAsmEnable());
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

    @Override
    public void preinit() {
        log.infoEcho("Ready config JRebel Supplement plugin...");
        fastJsonInit();
        ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener() {
            @Override
            public void onClassEvent(int i, Class<?> aClass) {
                if (i == 1 || i == 2) {
                    forFastJson(aClass);
                    forMybatisPlus(aClass);
                }
            }

            @Override
            public int priority() {
                return 0;
            }
        });
    }

    private void fastJsonInit() {
        if (!FAST_JSON_PLUGIN_ENABLE && FAST_JSON_PLUGIN_LOADED_COUNT == 0) {
            FAST_JSON_PLUGIN_LOADED_COUNT++;
            log.infoEcho("Check Fast Json Plugin...");
            try {
                Class.forName("com.alibaba.fastjson.serializer.SerializeConfig");
                FAST_JSON_PLUGIN_ENABLE = true;
            } catch (ClassNotFoundException e) {
                FAST_JSON_PLUGIN_ENABLE = false;
            }
            log.infoEcho("Fast Json Plugin Enable:" + FAST_JSON_PLUGIN_ENABLE);
        }
        if (FAST_JSON_PLUGIN_ENABLE) {
            IntegrationFactory.getInstance().addAfterMainCallback(() -> {
                SerializeConfig.getGlobalInstance().setAsmEnable(false);
                log.infoEcho("Init FastJson AsmEnable:" + SerializeConfig.getGlobalInstance().isAsmEnable());
            });
        }
    }

    private void forMybatisPlus(Class<?> aClass) {
        if (!MYBATIS_PLUS_PLUGIN_ENABLE && MYBATIS_PLUS_PLUGIN_LOADED_COUNT == 0) {
            MYBATIS_PLUS_PLUGIN_LOADED_COUNT++;
            log.infoEcho("Check Mybatis Plus Plugin...");
            try {
                Class.forName("com.baomidou.mybatisplus.core.MybatisConfiguration");
                MYBATIS_PLUS_PLUGIN_ENABLE = true;
                defaultSqlInjector = new DefaultSqlInjector();
            } catch (ClassNotFoundException e) {
                MYBATIS_PLUS_PLUGIN_ENABLE = false;
            }
            log.infoEcho("Mybatis Plus Plugin Enable:" + MYBATIS_PLUS_PLUGIN_ENABLE);
        }
        if (MYBATIS_PLUS_PLUGIN_ENABLE) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(aClass);
            if (!Objects.isNull(tableInfo)) {
                Configuration configuration = tableInfo.getConfiguration();
                removeTableInfoCache(aClass, tableInfo);
                defaultReflectorRemove(aClass, configuration);
                mapperProxyFactoryRemove(aClass, configuration);
            }
        }
    }

    @Override
    public boolean checkDependencies(ClassLoader classLoader, ClassResourceSource classResourceSource) {
        return classResourceSource.getClassResource("com.alibaba.fastjson.serializer.SerializeConfig") != null ||
               classResourceSource.getClassResource("com.baomidou.mybatisplus.core.MybatisConfiguration") != null;
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
