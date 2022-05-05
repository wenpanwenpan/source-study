package com.wp.base.hashmap;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  hashmap源码学习
 *
 * @author wenpan 2021/04/20 21:39
 */
public class HashMapSourceStudy {

    public static void main(String[] args) {
        My my = new My();
        Class<?> aClass = comparableClassFor(my);
        System.out.println(aClass);
    }

    static class My implements Comparable<My>,Cloneable, Serializable {

        @Override
        public int compareTo(@NotNull HashMapSourceStudy.My o) {
            return 0;
        }
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     * 参考hashmap的comparableClassFor
     * 判断当前的x是否实现了Comparable接口，如果是则返回他的class，反之则返回null
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            // ParameterizedType 表示复合类型
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType)t).getRawType() == Comparable.class)){
                        if((as = p.getActualTypeArguments()) != null){
                            if(as.length == 1 && as[0] == c){
                                return c;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
