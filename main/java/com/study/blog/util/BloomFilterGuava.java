package com.study.blog.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * @author 10652
 */
public class BloomFilterGuava {

    /**
     * 假设 插入的数据位 1千万条
     * 容错率位 0.01
     * 构建 bloom filter
     */
    private static final BloomFilter<Long> FILTER = BloomFilter.create(
            Funnels.longFunnel(),
            10 ^ 7,
            0.01
    );
}
