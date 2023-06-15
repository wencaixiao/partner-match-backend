package com.xiaowc.partnermatch.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义redisson配置，
 *     redisson是一个Java操作redis的客户端，提供了大量的分布式数据集来简化对redis的操作和使用，
 *   可以让开发者像使用本地集合一样使用redis，完全感知不到redis的存在。
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis") // 从application.yml这个配置中读取
@Data // 为host和port生成get和set方法
public class RedissonConfig {

    /**
     * 对应application.yml中的字段名
     */
    private String host;

    /**
     * 对应application.yml中的字段名
     */
    private String port;

    @Bean
    public RedissonClient redissonClient() {
        // 1.创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port); // 不要把redisson的配置写死，从yml文件中去读
        config.useSingleServer().setAddress(redisAddress).setDatabase(3); // 设置redisson的地址和数据库
        // 2.创建redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
