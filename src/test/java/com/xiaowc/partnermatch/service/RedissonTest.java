package com.xiaowc.partnermatch.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 和Java自带的集合进行对比：
 *   使用上基本一致，因为继承了List或者Map或者Set
 */
@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {
        // list：数据存在本地JVM内存中
        List<String> list = new ArrayList<>();
        list.add("xiaowc");
        System.out.println("list:" + list.get(0));
        list.remove(0);

        // 数据存在redis的内存中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("xiaowc");
        System.out.println("rlist:" + rList.get(0));
        rList.remove(0);

        // map
        Map<String, Integer> map = new HashMap<>();
        map.put("xiaowc", 10);
        map.get("xiaowc");

        RMap<String, Integer> map1 = redissonClient.getMap("test-map");
        map.put("xiaowc", 10);
        map.get("xiaowc");

        // set

        //stack
    }

    /**
     * 测试redisson看门狗机制续锁
     */
    @Test
    void testWatchDog() {
        RLock lock = redissonClient.getLock("xiaowc:precachejob:docache:lock");
        try {
            // 尝试获取锁，获取成功会返回true，将第二个参数改为-1，可以实现续锁
            //   1.waitTime设置为0，只抢一次，抢不到就放弃
            //   2.注意释放锁要写在finally中
            //   3.看门狗机制：redisson中提供的续期机制。开一个监听线程，如果方法还没执行完，就帮你重置redis锁的过期时间
            //      将leastTime设置为-1就会开启看门狗续期机制
            //     原理：1.监听当前线程，默认看门狗机制过期时间是30秒，每10秒续期一次(补到30秒)，防止宕机
            //           2.如果线程挂掉(注意debug模式也会被他当成服务器宕机)，则不会续期
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                Thread.sleep(300000); // 模拟方法还没执行完，让redisson开启看门狗机制
                System.out.println("getLock: " + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 只能自己释放锁
            if (lock.isHeldByCurrentThread()) { // 判断当前的锁是不是自己的锁
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
