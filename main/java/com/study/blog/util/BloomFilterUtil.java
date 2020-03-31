package com.study.blog.util;

import java.util.BitSet;
import java.util.Objects;

/**
 * @author 10652
 * @date
 */
public class BloomFilterUtil {
    /**
     * 容量：大概在 1.6*10^7 个数
     */
    private static final int DEFAULT_SIZE = 2 << 24;
    /**
     * 通过种子数组创建6个不同的哈希函数
     */
    private static final int[] SEED = new int[]{3, 13, 71, 97, 133};
    /**
     * 位数组：java.util 中封装的 BitSet
     */
    private static BitSet bitSet = new BitSet(DEFAULT_SIZE);

    /**
     * 存放 hash 函数的实例对象
     */
    private static SimpleHashFunction[] hashFunction = new SimpleHashFunction[SEED.length];

    static {
        for (int i = 0; i < SEED.length; ++i) {
            hashFunction[i] = new SimpleHashFunction(SEED[i], DEFAULT_SIZE);
        }
    }

    /**
     * 添加元素到 BitSet
     *
     * @param object obj
     */
    public static void add(Object object) {
        for (SimpleHashFunction function : hashFunction) {
            bitSet.set(function.hash(object), true);
        }
    }

    /**
     * 判断是否存在：只有所有 hash 结果都存在则表示存在，只要有一个hash不存在，则绝对不存在
     *
     * @param object obj
     * @return true/false
     */
    public static boolean exist(Object object) {
        for (SimpleHashFunction function : hashFunction) {
            if (!bitSet.get(function.hash(object))) {
                return false;
            }
        }
        return true;
    }

    /**
     * hash 函数实例：一般传入多个参数进行区别各个 hash
     */
    private static class SimpleHashFunction {
        private int capacity;
        private int seed;

        SimpleHashFunction(int seed, int capacity) {
            this.capacity = capacity;
            this.seed = seed;
        }

        /**
         * 这个 hash 算法没看懂
         *
         * @param obj obj
         * @return hash
         */
        int hash(Object obj) {
            int temp;
            return Objects.isNull(obj) ? 0 : Math.abs(seed * (capacity - 1) & ((temp = obj.hashCode()) ^ temp >>> 16));
        }
    }

}
