package com.xiaowc.partnermatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 自定义redisTemplate的配置，自定义序列化
 *   存储k-v的时候，原生的redisTemplate会默认jdk序列化
 *   原生的redisTemplate满足不了我们的要求，所以我们自定义一个redisTemplate
 */
@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory); // 设置链接工厂
        redisTemplate.setKeySerializer(RedisSerializer.string()); // 设置key的序列化器
        return redisTemplate;
    }
}
