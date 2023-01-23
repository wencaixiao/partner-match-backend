package com.xiaowc.partnermatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowc.partnermatch.common.BaseResponse;
import com.xiaowc.partnermatch.common.ErrorCode;
import com.xiaowc.partnermatch.common.ResultUtils;
import com.xiaowc.partnermatch.constant.UserConstant;
import com.xiaowc.partnermatch.exception.BusinessException;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.model.request.UserLoginRequest;
import com.xiaowc.partnermatch.model.request.UserRegisterRequest;
import com.xiaowc.partnermatch.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController // 适用于编写restful风格的api，返回值默认为json类型
@RequestMapping("/user")
// 这个注解的作用是允许跨域，默认允许的域名是*，所有域名都允许跨域，可以通过origins来选择允许跨域的地址
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000" })
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求体
     * @return 返回注册用户的id
     */
    @PostMapping("/register")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 返回参数错误的枚举，表示注册失败，这样比较繁琐
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        // 这里也要加一层校验比较好，为什么呢?
        // controller层倾向于对请求参数本身的校验，不涉及业务本身(越少越好)
        // service层是对业务逻辑的校验(有可能被controller之外的类调用)
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, userPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        //return new BaseResponse<>(0, result, "ok");
        return ResultUtils.success(result); // 优化后
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求体
     * @param request 用于保存session
     * @return 返回登录后脱敏的用户
     */
    @PostMapping("/login")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 这里也要加一层校验比较好，为什么呢?
        // controller层倾向于对请求参数本身的校验，不涉及业务本身(越少越好)
        // service层是对业务逻辑的校验(有可能被controller之外的类调用)
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        //return new BaseResponse<>(0, user, "ok");
        return ResultUtils.success(user); // 优化后
    }

    /**
     * 用户退出登录
     * @param request 用于获取用户session，便于移除
     * @return
     */
    @PostMapping("/logout")
    // 加@RequestBody注解的目的是为了和前端传进来的参数对应上
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        //return new BaseResponse<>(0, result, "ok");
        return ResultUtils.success(result); // 优化后
    }

    /**
     * 获取用户的登录态，获取当前登录用户信息接口
     * @param request
     * @return 返回用户脱敏后的用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // session中存储的是用户登录的凭据
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE); // session中的值相当于是一个缓存
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 不应该直接返回之前用户的登录态currentUser
        // 比如上次登录查看用户的积分是0分，下次登录时用户是100分，那下次登录就要更新用户的积分，因此需要从数据库中去查询更好
        // 信息频繁变化的系统的可以通过查询一次数据库来变化信息
        Long userId = currentUser.getId();
        // TODO: 校验用户是否合法
        User user = userService.getById(userId); // 可以直接从session中取再脱敏，但是去数据库中查更好
        User safetyUser = userService.getSafetyUser(user);// 返回脱敏后的信息(更安全)
        //return new BaseResponse<>(0, safetyUser, "ok");
        return ResultUtils.success(safetyUser); // 优化后
    }

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        // 判断是否为管理员，仅管理员可查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        // 用户已登录，并且是管理员权限
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username); // 模糊查询
        }
        // 应该返回脱敏后的信息
        List<User> userList = userService.list(queryWrapper); // 查询用户列表
        // 遍历userList将里面的userPassword置为空脱敏，再转换成列表进行返回
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        //return new BaseResponse<>(0, list, "ok");
        return ResultUtils.success(list); // 优化后
    }

    /**
     * 根据标签搜索用户
     * @param tagNameList 前端传入过来的标签
     * @return 返回用户信息
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) { // 表示参数不是必填项
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 我们这里使用的是内存查询，具体看service
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 分页、redis缓存
     *  1.第一次从数据库中查询数据，会比较慢，后面直接从缓存中取数据，比较快，可以设置缓存的过期时间，因为不可能每次推荐的信息都是一样的
     *  2.redis内存不能无线增加，一定要设置过期时间
     *
     * 推荐用户
     *  1.要做分页，因为前端不可能一次性加载数据库中的所有数据，数据量大就会奔溃
     *  2.根据不同的参数来选择查询哪部分数据
     *
     * 后期优化：未登录用户推荐的内容一样
     *  1.数据库查询慢？预先把数据查出来，放到一个更快读取的地方，就不用再查数据库了(缓存)
     *      缓存的实现：redis(分布式缓存),memcached(分布式缓存),Etcd(云原生架构的一个分布式存储，存储配置，扩容能力),
     *                 ehcache(单机),本地缓存(java内存map),Caffeine(Java内存缓存，高性能),Google Guava
     *      分布式缓存：多台机器公用一个缓存，从一个缓存中去取数据
     *  2.预加载缓存，定时更新缓存(定时任务)
     *  3.多个及其都要执行任务么？(分布式锁：控制同一时间只有一台及其去执行定时任务，其他及其不用重复执行了)
     *
     * 从缓存中读取数据：
     *  1.不同的用户看到的数据不同，传入用户的key来进行区分，比如：systemId:moduleId:func:options(不要和别人冲突) -> 我们使用：xiaowc:user:recommend:userId
     *
     * 缓存预热：
     *  1.问题：第一个用户访问还是很慢(假如第一个用户是老板)，也能一定程度上保护数据库
     *  2.缓存预热的优点：
     *      1.解决上面的问题，可以让用户始终访问很快
     *  3.缓存预热的缺点：
     *      1.增加开发成本(你要额外的开发、设计)
     *      2.预热的时机和时间如果错了，有可能你缓存的数据不对或者太老
     *      3.需要占用额外空间
     *  4.怎么缓存预热：
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
     * @param pageSize 每一页的大小
     * @param pageNum 当前在第几页
     * @param request
     * @return
     */
    // TODO: 2023/1/12 推荐多个，未实现
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommedUsers(long pageSize, long pageNum, HttpServletRequest request) {
        // 首先得到当前登录的用户
        User loginUser = userService.getLoginUser(request);
        // 因为不同用户看到的数据不同，所以需要传入用户的key来进行区分，这里我们使用：xiaowc:user:recommend:userId
        String redisKey = String.format("xiaowc:user:recommend:%s", loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 如果有缓存，直接从缓存中读取数据
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }
        // 如果没有缓存，直接从数据库中查询数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userList = userService.page(new Page<>(pageNum, pageSize), queryWrapper); // 分页查询
        // 从数据库中查询到数据之后，将数据写入缓存中
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS); // 30s过期
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userList);
    }

    /**
     * 更新用户信息
     * @param user 要修改的用户
     * @param request
     * @return
     */
    @PostMapping("/update")
    // 请求的是JSON数据，要加@RequestBody
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 校验参数是否为空
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 得到当前登录的用户
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据用户id删除用户(逻辑删除)
     * @param id 用户id
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        // 判断是否为管理员，仅管理员可查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH); // 改进：利用自定义的全局异常类来处理
        }
        // 用户已登录，并且是管理员权限
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR); // 改进：利用自定义的全局异常类来处理
        }
        boolean b = userService.removeById(id);// 逻辑删除
        //return new BaseResponse<>(0, b, "ok");
        return ResultUtils.success(b); // 优化后
    }

    /**
     * 用户匹配：获取最匹配的用户，根据分数来进行匹配，用到动态规划
     * @param num 匹配多少个用户
     * @param request
     * @return 返回匹配到的用户
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }
}
