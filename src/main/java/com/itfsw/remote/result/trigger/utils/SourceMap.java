package com.itfsw.remote.result.trigger.utils;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Map;

/**
 * SourceMap 工具
 */
public class SourceMap {
    /**
     * 类型转换器
     */
    private static ConversionService conversionService = new DefaultConversionService();
    /**
     * SourceMap
     */
    private Map<String, Object> source;

    /**
     * 构造函数
     *
     * @param source ES SourceMap
     */
    public SourceMap(Map<String, Object> source) {
        this.source = source;
    }

    /**
     * 静态获取方式
     *
     * @param source
     * @return SourceMap工具
     */
    public static SourceMap of(Map<String, Object> source) {
        return new SourceMap(source);
    }

    /**
     * 获取值
     *
     * @param key  字段名
     * @param type 类型
     * @param <T>
     * @return 值
     */
    public <T> T value(String key, Class<T> type) {
        Object value = source.get(key);
        if (value != null) {
            // 类型相同直接返回
            if (type.isAssignableFrom(value.getClass())) {
                return (T) value;
            } else if (conversionService.canConvert(value.getClass(), type)) {
                // 使用类型转换器
                return conversionService.convert(value, type);
            } else {
                // 强制转换
                return (T) value;
            }
        }
        return null;
    }

    /**
     * boolean 值
     *
     * @param key 字段名
     * @return 值
     */
    public Boolean booleanValue(String key) {
        return value(key, Boolean.class);
    }

    /**
     * byte 值
     *
     * @param key 字段名
     * @return 值
     */
    public Byte byteValue(String key) {
        return value(key, Byte.class);
    }

    /**
     * short 值
     *
     * @param key 字段名
     * @return 值
     */
    public Short shortValue(String key) {
        return value(key, Short.class);
    }

    /**
     * Integer 值
     *
     * @param key 字段名
     * @return 值
     */
    public Integer integerValue(String key) {
        return value(key, Integer.class);
    }

    /**
     * Float 值
     *
     * @param key 字段名
     * @return 值
     */
    public Float floatValue(String key) {
        return value(key, Float.class);
    }

    /**
     * Double 值
     *
     * @param key 字段名
     * @return 值
     */
    public Double doubleValue(String key) {
        return value(key, Double.class);
    }

    /**
     * String 值
     *
     * @param key 字段名
     * @return 值
     */
    public String stringValue(String key) {
        return value(key, String.class);
    }

    /**
     * Map 值
     *
     * @param key 字段名
     * @return 值
     */
    public Map<String, Object> mapValue(String key) {
        return value(key, Map.class);
    }

    /**
     * List 值
     *
     * @param key 字段名
     * @return 值
     */
    public List<Object> listValue(String key) {
        return value(key, List.class);
    }

    /**
     * List 值
     *
     * @param key          字段名
     * @param elementClass List 对象类型
     * @param <T>
     * @return 值
     */
    public <T> List<T> listValue(String key, Class<T> elementClass) {
        Object value = source.get(key);
        if (value != null) {
            TypeDescriptor source = TypeDescriptor.forObject(value);
            TypeDescriptor target = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(elementClass));
            if (conversionService.canConvert(source, target)) {
                // 使用类型转换器
                return (List<T>) conversionService.convert(value, source, target);
            }
        }
        return null;
    }

    /**
     * 下一级获取器
     *
     * @param key 字段名
     * @return SourceMap工具
     */
    public SourceMap sourceMap(String key) {
        Map<String, Object> value = mapValue(key);
        if (value != null) {
            return SourceMap.of(value);
        }
        return null;
    }
}
