package com.xiaowc.partnermatch.once.importuser;

import com.xiaowc.partnermatch.mapper.UserMapper;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 向数据库插入数据
 * 并发异步执行
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    // 创建一个线程池
    // CPU 密集型：分配的核心线程数 = CPU - 1
    // IO 密集型：分配的核心线程数可以大于CPU核数
    private ExecutorService executorService = new ThreadPoolExecutor(60, 1000,
            10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户(单线程)
     * for循环插入数据的问题：时间很慢
     *   1.建立和释放数据库连接(批量查询解决)
     *   2.for循环是绝对线性的
     * 解决方法：使用userService.saveBatch()批量插入数据
     */
    // 开启springboot项目后，initialDelay表示启动项目后延迟多少秒再执行任务，fixedRate表示每隔多长时间再执行一次
    // @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch(); // 记录插入1000条数据需要多长时间
        System.out.println("goodgoodgood");
        stopWatch.start(); // 记录开始时间
        final int INSERT_NUM = 100000;
        // for循环插入数据的问题：时间很慢
        //  1.建立和释放数据库连接(批量查询解决)
        //  2.for循环是绝对线性的
        // 解决方法：使用userService.saveBatch()批量插入数据
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("xiaowc");
            user.setUserAccount("xiaowc");
            user.setAvatarUrl("https://img1.baidu.com/it/u=1295488586,3361919270&fm=253&fmt=auto&app=138&f=PNG?w=601&h=435");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("11111111");
            //userMapper.insert(user); // 一次一次插入，每次插入一条数据
            userList.add(user); // 先将数据放入到list集合中，后续进行批量插入
        }
        userService.saveBatch(userList, 10000); // 批量插入，每次插入100条数据
        stopWatch.stop(); // 记录结束时间
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 批量插入用户(多线程并发执行)
     * 线程并不是越多越好，线程切换也需要时间
     *  创建一个线程池
     *   CPU 密集型：分配的核心线程数 = CPU - 1
     *   IO 密集型：分配的核心线程数可以大于CPU核数
     */
    // 开启springboot项目后，initialDelay表示启动项目后延迟多少秒再执行任务，fixedRate表示每隔多长时间再执行一次
    // @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch(); // 记录插入1000条数据需要多长时间
        System.out.println("goodgoodgood");
        stopWatch.start();
        final int INSERT_NUM = 100000;
        // 分十组
        int batchSize = 2500;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>(); // 任务数组
        for (int i = 0; i < 40; i++) { // 异步执行，10个线程
            List<User> userList = Collections.synchronizedList(new ArrayList<>()); // 因为是并发执行，这里要用线程安全的集合
            while (true) {
                j++;
                User user = new User();
                user.setUsername("xiaowc");
                user.setUserAccount("xiaowc");
                user.setAvatarUrl("https://img1.baidu.com/it/u=1295488586,3361919270&fm=253&fmt=auto&app=138&f=PNG?w=601&h=435");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("11111111");
                userList.add(user); // 先将数据放入到list集合中，后续进行批量插入
                if (j % 10000 == 0) { // 发现插入10000条数据，直接break
                    break;
                }
            }
            // 异步执行
            // 并发要注意执行先后顺序无所谓，不要用到非并发类的集合
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { // 用了runAsync()这个方法，里面的操作就是异步的
                System.out.println(Thread.currentThread().getName()); // 打印当前线程的名称
                userService.saveBatch(userList, batchSize);
            }, executorService); // 从自己定义的线程池executorService去取线程，而对于系统默认的线程池大小等于CPU核数
            futureList.add(future);
        }
        // 上面拿到了10个异步任务，下面去执行这10个异步任务
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join(); // 加join()方法的目的是让这10个异步任务执行完了之后才让程序往下执行
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

}
