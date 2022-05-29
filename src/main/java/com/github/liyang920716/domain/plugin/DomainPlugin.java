package com.github.liyang920716.domain.plugin;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.liyang920716.domain.annotation.Domain;
import com.github.liyang920716.domain.annotation.DomainField;
import com.github.liyang920716.domain.annotation.DomainParam;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author liyang
 * @description
 * @date 2022-04-07 13:49:50
 */
@Intercepts({@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
), @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}
), @Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
), @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
)})
public class DomainPlugin implements Interceptor {

    private String domain;
    private static final ConcurrentHashMap<String, Field[]> classField = new ConcurrentHashMap<>();
    private static String REGEX_BASE_URL = "\\$\\{baseUrl}";
    private static String REGEX_VAL = "(.*?)=#\\{ew.[a-zA-Z]+.(.*?)}";

    public DomainPlugin(String domain) {
        this.domain = domain;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof Executor) {
            MappedStatement ms = (MappedStatement) args[0];
            if (ms.getSqlCommandType() == SqlCommandType.SELECT) {
                Object proceed = invocation.proceed();
                select(proceed);
                return proceed;
            } else if (ms.getSqlCommandType() == SqlCommandType.INSERT) {
                Object o = args[1];
                insert(o, ms);
                return invocation.proceed();
            } else if (ms.getSqlCommandType() == SqlCommandType.UPDATE) {
                Object o = args[1];
                update(o, ms);
                return invocation.proceed();
            } else if (ms.getSqlCommandType() == SqlCommandType.DELETE) {

            } else {
                return invocation.proceed();
            }
        }
        return invocation.proceed();
    }

    /**
     * 查询处理
     *
     * @param proceed
     * @throws Exception
     */
    private void select(Object proceed) throws Exception {
        if (!(proceed instanceof ArrayList)) {
            return;
        }
        List<Object> list = (List<Object>) proceed;
        if (list.isEmpty()) {
            return;
        }
        Object o = list.get(0);
        if (o == null) {
            return;
        }
        Class<?> aClass = o.getClass();
        if (!aClass.isAnnotationPresent(Domain.class)) {
            return;
        }
        Field[] declaredFields = null;
        if (classField.containsKey(aClass.getName())) {
            declaredFields = classField.get(aClass.getName());
        } else {
            declaredFields = FieldUtils.getAllFields(aClass);
            classField.put(aClass.getName(), declaredFields);
        }
        Field[] resultFields = new Field[]{};
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(DomainField.class)) {
                resultFields = Arrays.copyOf(resultFields, resultFields.length + 1);
                resultFields[resultFields.length - 1] = declaredField;
            }
        }
        if (resultFields.length == 0) {
            return;
        }
        for (Object index : list) {
            for (Field declaredField : resultFields) {
                declaredField.setAccessible(true);
                Object o1 = declaredField.get(index);
                if (o1 != null && o1 instanceof String) {
                    declaredField.set(index, o1.toString().replaceAll(REGEX_BASE_URL, domain));
                }
            }
        }
    }

    /**
     * 插入处理
     *
     * @param o
     * @param ms
     * @throws Exception
     */
    private void insert(Object o, MappedStatement ms) throws Exception {
        if (o == null) {
            return;
        }
        //对非实体类的处理
        if (o instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap paramMap = (MapperMethod.ParamMap) o;
            dealNoEntity(ms, paramMap);
            return;
        }
        //对实体类处理
        Class<?> aClass = o.getClass();
        if (!aClass.isAnnotationPresent(Domain.class)) {
            return;
        }
        Field[] declaredFields = null;
        if (classField.containsKey(aClass.getName())) {
            declaredFields = classField.get(aClass.getName());
        } else {
            declaredFields = FieldUtils.getAllFields(aClass);
            classField.put(aClass.getName(), declaredFields);
        }
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(DomainField.class)) {
                declaredField.setAccessible(true);
                Object o1 = declaredField.get(o);
                if (o1 != null && o1 instanceof String) {
                    declaredField.set(o, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                }
            }
        }
    }

    /**
     * 修改处理
     *
     * @param o
     * @param ms
     * @throws Exception
     */
    private void update(Object o, MappedStatement ms) throws Exception {
        if (o instanceof MapperMethod.ParamMap) {
            //对mybatis-plus处理
            MapperMethod.ParamMap paramMap = (MapperMethod.ParamMap) o;
            if (paramMap.containsKey("et") && !paramMap.containsKey("ew")) {
                Object et = paramMap.get("et");
                Class<?> aClass = et.getClass();
                if (!aClass.isAnnotationPresent(Domain.class)) {
                    return;
                }
                Field[] declaredFields = null;
                if (classField.containsKey(aClass.getName())) {
                    declaredFields = classField.get(aClass.getName());
                } else {
                    declaredFields = FieldUtils.getAllFields(aClass);
                    classField.put(aClass.getName(), declaredFields);
                }
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(DomainField.class)) {
                        declaredField.setAccessible(true);
                        Object o1 = declaredField.get(et);
                        if (o1 != null && o1 instanceof String) {
                            declaredField.set(et, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                        }
                    }
                }
                return;
            } else if (paramMap.containsKey("et") && paramMap.containsKey("ew")) {
                Object ew = paramMap.get("ew");
                Object et = paramMap.get("et");
                if (ew != null && et == null) {
                    LambdaUpdateWrapper<?> lambdaUpdateWrapper = (LambdaUpdateWrapper<?>) ew;
                    Class<?> entityClass = lambdaUpdateWrapper.getEntityClass();
                    if (entityClass == null) {
                        return;
                    }
                    if (!entityClass.isAnnotationPresent(Domain.class)) {
                        return;
                    }
                    Map<String, String> valMap = new HashMap<>();
                    String[] split = lambdaUpdateWrapper.getSqlSet().split(",");
                    for (String s1 : split) {
                        valMap.put(Pattern.compile(REGEX_VAL).matcher(s1).group(1), Pattern.compile(REGEX_VAL).matcher(s1).group(2));
                    }
                    Field[] declaredFields = null;
                    if (classField.containsKey(entityClass.getName())) {
                        declaredFields = classField.get(entityClass.getName());
                    } else {
                        declaredFields = FieldUtils.getAllFields(entityClass);
                        classField.put(entityClass.getName(), declaredFields);
                    }
                    Map<String, Object> paramNameValuePairs = lambdaUpdateWrapper.getParamNameValuePairs();
                    for (Field declaredField : declaredFields) {
                        if (declaredField.isAnnotationPresent(DomainField.class)) {
                            String key = valMap.get(declaredField.getName());
                            Object o1 = paramNameValuePairs.get(key);
                            if (o1 instanceof String) {
                                paramNameValuePairs.put(key, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                            }
                        }
                    }
                }
                if (ew == null && et != null) {
                    Class<?> aClass = et.getClass();
                    if (!aClass.isAnnotationPresent(Domain.class)) {
                        return;
                    }
                    Field[] declaredFields = null;
                    if (classField.containsKey(aClass.getName())) {
                        declaredFields = classField.get(aClass.getName());
                    } else {
                        declaredFields = FieldUtils.getAllFields(aClass);
                        classField.put(aClass.getName(), declaredFields);
                    }
                    for (Field declaredField : declaredFields) {
                        if (declaredField.isAnnotationPresent(DomainField.class)) {
                            declaredField.setAccessible(true);
                            Object o1 = declaredField.get(et);
                            if (o1 != null && o1 instanceof String) {
                                declaredField.set(et, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                            }
                        }
                    }
                }
                if (ew != null && et != null) {
                    Class<?> aClass = et.getClass();
                    if (!aClass.isAnnotationPresent(Domain.class)) {
                        return;
                    }
                    Field[] declaredFields = null;
                    if (classField.containsKey(aClass.getName())) {
                        declaredFields = classField.get(aClass.getName());
                    } else {
                        declaredFields = FieldUtils.getAllFields(aClass);
                        classField.put(aClass.getName(), declaredFields);
                    }
                    for (Field declaredField : declaredFields) {
                        if (declaredField.isAnnotationPresent(DomainField.class)) {
                            declaredField.setAccessible(true);
                            Object o1 = declaredField.get(et);
                            if (o1 != null && o1 instanceof String) {
                                declaredField.set(et, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                            }
                        }
                    }
                }
                return;
            } else {
                //对非实体类处理
                dealNoEntity(ms, paramMap);
                return;
            }
        } else {
            if (o != null) {
                Class<?> aClass = o.getClass();
                if (!aClass.isAnnotationPresent(Domain.class)) {
                    return;
                }
                Field[] declaredFields = null;
                if (classField.containsKey(aClass.getName())) {
                    declaredFields = classField.get(aClass.getName());
                } else {
                    declaredFields = FieldUtils.getAllFields(aClass);
                    classField.put(aClass.getName(), declaredFields);
                }
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(DomainField.class)) {
                        declaredField.setAccessible(true);
                        Object o1 = declaredField.get(o);
                        if (o1 != null && o1 instanceof String) {
                            declaredField.set(o, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                        }
                    }
                }
                return;
            }
        }
    }

    /**
     * 对非实体类处理
     *
     * @param ms
     * @param paramMap
     * @throws Exception
     */
    private void dealNoEntity(MappedStatement ms, MapperMethod.ParamMap paramMap) throws Exception {
        if (paramMap.size() > 0) {
            String id = ms.getId();
            String classPath = id.substring(0, id.lastIndexOf("."));
            String classMethod = id.substring(id.lastIndexOf(".") + 1);
            Class<?> aClass = Class.forName(classPath);
            Method[] methods = aClass.getMethods();
            List<String> params = new ArrayList<>();
            for (Method method : methods) {
                if (method.getName().equals(classMethod)) {
                    Parameter[] parameters = method.getParameters();
                    for (Parameter parameter : parameters) {
                        if (parameter.getType().getSimpleName().equals("String") && parameter.isAnnotationPresent(DomainParam.class)) {
                            params.add(parameter.getName());
                        }
                    }
                    break;
                }
            }
            if (!params.isEmpty()) {
                for (String param : params) {
                    if (paramMap.containsKey(param)) {
                        Object o1 = paramMap.get(param);
                        if (o1 != null && o1 instanceof String) {
                            paramMap.put(param, o1.toString().replaceAll(domain, REGEX_BASE_URL));
                        }
                    }
                }
            }
        }
    }
}
