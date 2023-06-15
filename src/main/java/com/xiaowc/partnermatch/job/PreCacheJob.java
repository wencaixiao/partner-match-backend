package com.xiaowc.partnermatch.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1.缓存预热定时任务：预热推荐任务
 *   1.用定时任务之前，得先要让主入口上面加一个@EnableScheduling注解
 *
 * 2.缓存预热：
 *   1.问题：第一个用户访问还是很慢(假如第一个用户是老板)，也能一定程度上保护数据库
 *   2.缓存预热的优点：
 *      1.解决上面的问题，可以让用户始终访问很快
 *   3.缓存预热的缺点：
 *      1.增加开发成本(你要额外的开发、设计)
 *      2.预热的时机和时间如果错了，有可能你缓存的数据不对或者太老
 *      3.需要占用额外空间
 *   4.怎么缓存预热：
 *      1.定时触发
 *         定时任务实现：用定时任务，每天刷新所有用户的推荐列表
 *            1.Spring Scheduler(Spring boot默认整合了)。
 *            2.Quartz(独立于Spring存在定时任务框架)。
 *            3.XXL-job之类的分布式任务调度平台(界面+sdk)。
 *         注意点：
 *            1.缓存预热的意义(新增少，总用户多)
 *            2.缓存的空间不能太大，要预留给其他缓存空间
 *            3.缓存数据的周期(此次每天一次)
 *      2.模拟触发(手动触发)
 *
 * 3.控制定时任务的执行：这里讲的是只有一个redis，是一个单机的redis
 *   1.为啥？我们要控制一个定时任务在同一时间只有一个服务器能执行
 *      1.浪费资源，想象10000台服务器同时执行这个定时任务(打鸣)
 *      2.脏数据，比如重复插入
 *   2.怎么做？
 *      1.分离定时任务和主程序，只在一个服务器运行定时任务，但是成本太大
 *      2.写死配置，每隔服务器都执行定时任务，但是只有ip符合配置的服务器才真是执行业务逻辑，其他的直接返回，成
 *        本最低，但是我们的ip可能是不固定的，把ip写的太死了，单机就会出现单点故障
 *      3.动态配置，配置是可以轻松的、很方便的更新的(代码无需重启)，但是只有ip符合配置的服务器才真是执行业务逻辑。
 *         1.数据库
 *         2.Redis
 *         3.配置中心(Nacos,Apollo,Spring Cloud Config)
 *        问题：服务器多了，ip不可控还是很麻烦，还是要人工修改
 *      4.分布式锁，只有抢到锁的服务器才能执行业务逻辑。坏处：增加成本。好处：不用手动配置，多少个服务器都一样
 *   3.锁：
 *      1.有限资源的情况下，控制同一时间(段)只有某些线程(用户/服务器)能访问到资源
 *      2.Java实现锁：synchronized关键字、并发包的类
 *      3.问题：synchronized只对单个JVM有效，也就是说只在自身的服务器有效，如果有多个服务器之前抢占资源，就无效
 *   4.分布式锁：为啥需要分布式锁
 *      1.有限资源的情况下，控制同一时间(段)只有某些线程(用户/服务器)能访问到资源。
 *      2.解决单个锁只对单个JVM有效的问题，可以多个服务器之前进行抢锁。
 *      3.分布式锁实现的关键：抢锁机制
 *         怎么保证同一时间只有1个服务器能抢到锁？
 *         1.核心思想：先来的人把数据改成自己的标识(服务器ip)，后来的人发现标识已存在，就抢锁失败，继续等待，
 *                  等先来的人执行方法结束，把标识清空，其他的人继续抢锁。
 *         2.实现方式：
 *            1.MySQL数据库：select for update行级锁(最简单的)
 *            2.乐观锁：
 *            3.redis实现(推荐)：内存数据库，读写速度快，有些业务每次执行都要去抢锁，因此redis会比较快，
 *              支持setnx(不存在就设置，只有设置成功才会返回true，否则返回false，用来存标识),lua脚本，比较方便我们实现分布式锁
 *              注意：
 *                 1.用完锁要释放。(节约资源，腾地方，用完几十释放)
 *                 2.锁一定要加过期时间。
 *                 3.如果方法执行时间过长，锁提前过期了，这样还是会存在多个方法同时执行的情况？
 *                    问题：1.连锁效应：释放掉别人的锁。2.这样还是会存在多个方法同时执行的情况
 *                    解决方案：续期
 *                       boolean end = false;
 *                       new Thread(() - > {
 *                           if (!end) {
 *                               续期
 *                           }
 *                       })
 *                       end = true;
 *                 4.释放锁的时候，有可能先判断出是自己的锁，但这时锁过期了，最后还是释放了别人的锁
 *                    if (get lock == A) {
 *                        当前位置，锁过期了，这时B进来加锁，往下面执行的时候，A把B的锁释放了，这时C又进来加锁，最终B和C一起执行
 *                        // set lock B (因此这里一定要用原子操作，就是说程序执行的时候不允许加锁)
 *                        del lock
 *                    }
 *                    可以通过redis+lua脚本实现(可以理解为一个事务)
 *            4.Zookeeper实现(不推荐)：
 *            5.redisson实现分布式锁：实现了很多Java里支持的接口和数据结构
 *                redisson是一个Java操作redis的客户端，提供了大量的分布式数据集来简化对redis的操作和使用，可以让开发者像使用本地集合
 *              一样使用redis，完全感知不到redis的存在。
 * 4.如果redis也是分布式的，redis是一个集群：
 *      redis如果是集群(而不是只有一个redis)，如果分布式锁的数据不同步怎么办？
 *        可以使用红锁！！！是一个分布式的分布式锁
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient; // 引入redisson，实现分布式锁

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    // 每天执行，预热推荐用户
    // 去网上查一下crontab表达式怎么写
    // 如果是分布式，会出现问题
    @Scheduled(cron = "0 0 0 * * *")  // 每天0点执行这个任务
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("xiaowc:precachejob:docache:lock"); // 创建一个锁
        try {
            // 尝试获取锁，获取成功会返回true，将第二个参数改为-1，可以实现续锁
            //   1.waitTime设置为0，只抢一次，抢不到就放弃
            //   2.注意释放锁要写在finally中
            //   3.看门狗机制：redisson中提供的续期机制。开一个监听线程，如果方法还没执行完，就帮你重置redis锁的过期时间
            //      将leastTime设置为-1就会开启看门狗续期机制
            //     原理：1.监听当前线程，默认看门狗机制过期时间是30秒，每10秒续期一次(补到30秒)，防止宕机
            //           2.如果线程挂掉(注意debug模式也会被他当成服务器宕机)，则不会续期
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) { // 所有线程都去抢这个锁
                System.out.println("getLock: " + Thread.currentThread().getId());
                for (Long userId : mainUserList) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    // 分页查询数据库
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                    String redisKey = String.format("xiaowc:user:recommend:%s", userId); // redis的key
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    // 写缓存
                    try {
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally { // 最后执行完这个逻辑再把锁释放掉
            // 只能自己释放锁
            if (lock.isHeldByCurrentThread()) { // 判断当前的锁是不是自己的锁
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
