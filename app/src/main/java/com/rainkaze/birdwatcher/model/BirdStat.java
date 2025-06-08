package com.rainkaze.birdwatcher.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用于封装鸟类统计数据的简单数据模型。
 */
@Data
@AllArgsConstructor
public class BirdStat {
    private String birdName;
    private int count;
}