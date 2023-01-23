package com.xiaowc.partnermatch.service;

import com.xiaowc.partnermatch.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * 引入一个库时，先写测试类
 * redis在Java里的实现方式：
 *   1.Spring Data Redis(推荐): 1)引入依赖。2)配置redis地址
 *     spring data: 通用的数据访问框架，定义了一组增删改查的接口
 *     mysql, redis, jpa
 *   2.jedis
 *     独立于spring操作redis的Java客户端，要配合jedis pool使用
 *   3.lettuce
 *     高阶的操作redis的Java客户端，支持异步、连接池
 *   4.redisson
 *     分布式操作redis的Java客户端，让你像在使用本地的集合一样操作redis(分布式redis数据网格)
 *
 * 对比：
 *   1.如果你用的是spring，并且没有过多的定制化要求，可以用spring data redis，最方便
 *   2.如果你用的不是spring，并且追求简单，并且你没有过高的性能要求，可以用redis+jedis pool
 *   3.如果你的项目不是spring，并且追求高性能，高定制化，可以用lettuce，支持异步，连接池
 *   4.如果你的项目是分布式的，需要用到一些分布式的特性(比如分布式锁，分布式集合)，推荐用redisson
 */
@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate; //继承了RedisTemplate

    /**
     * Redis数据结构：
     *   1.String字符串类型：name:"xiao"
     *   2.List列表：names:["xiaowc","catxiaowc","xiaowc"]
     *   3.Set集合：names:["xiaowc","catxiaowc"]
     *   4.Hash哈希：nameAge:{"xiaowc":1,"catxiaowc":1}
     *   5.Zset集合：names:{xiaowc-9,catxiaowc-12} ,带有分数，适合做排行榜
     *   6.bloomfilter：布隆过滤器，主要从大量的数据中快速过滤值，比如邮件黑名单拦截
     *   7.geo：计算地理位置
     *   8.hyperloglog：pv/uv
     *   9.pub/sub：发布订阅，类似消息队列
     *   10.BitMap：(010101010101010101010的方式存储，节省空间)
     */
    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("xiaowcString", "cat");
        valueOperations.set("xiaowcInt", 1);
        valueOperations.set("xiaowcDouble", 1.0);
        User user = new User();
        user.setId(0L);
        user.setUsername("");
        valueOperations.set("xiaowcUser", user);
        // 查
        Object xiaowcString = valueOperations.get("xiaowcString");
        Assertions.assertTrue("cat".equals((String) xiaowcString));
        Object xiaowcInt = valueOperations.get("xiaowcInt");
        Assertions.assertTrue(1 == ((Integer) xiaowcInt));
        Object xiaowcDouble = valueOperations.get("xiaowcDouble");
        Assertions.assertTrue(1.0 == ((Double) xiaowcDouble));
        System.out.println(valueOperations.get("xiaowcUser"));
        // 改
        valueOperations.set("xiaowcString", "birds");
        // 删
        redisTemplate.delete("xiaowcString");
    }
}
