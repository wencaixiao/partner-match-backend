package com.xiaowc.partnermatch.service.impl;
import java.util.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiaowc.partnermatch.common.ErrorCode;
import com.xiaowc.partnermatch.constant.UserConstant;
import com.xiaowc.partnermatch.exception.BusinessException;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.service.UserService;
import com.xiaowc.partnermatch.mapper.UserMapper;
import com.xiaowc.partnermatch.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
* @author xiaowc
* @description 用户服务实现类
* @createDate 2022-12-23 20:24:35
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper; // 使用userMapper可以不用写sql自动进行增删改查

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "xiaowc";

    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 返回新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1.校验：
        // (1)账户、密码、校验密码不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR); // 返回参数错误的枚举，表示注册失败，这样比较繁琐
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户、密码或校验密码为空"); // 改进：利用自定义的全局异常类来处理
        }
        // (2)账户不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (3)密码不能小于8位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (4)星球编号的长度不能大于5
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户星球编号过长"); // 改进：利用自定义的全局异常类来处理
        }
        // (5)账户不能包含特殊字符：正则表达式
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~!@#￥%……&*()——+|{}【】';:”“’。,、?]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount); // 看传入的用户名和正则表达式是否匹配
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户包含特殊字符"); // 改进：利用自定义的全局异常类来处理
        }
        // (6)密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户两次输入的密码不相同"); // 改进：利用自定义的全局异常类来处理
        }
        // (7)用户不能重复：传入的用户和数据库中的用户进行比较(如何用户包含了特殊字符，就不用进行这一步了，使内存得到了优化)
        // 查询数据库：
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount); // 指定查询条件
        long count = userMapper.selectCount(queryWrapper); // 查询在数据库中的账户是否和前端传入的注册账户相等，看有多少个
        if (count > 0) { // 用户重复直接返回，表示注册失败
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户重复"); // 改进：利用自定义的全局异常类来处理
        }
        // (8)星球编号不能重复
        // 查询数据库：
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode); // 指定查询条件
        count = userMapper.selectCount(queryWrapper); // 查询在数据库中的账户是否和前端传入的注册账户相等，看有多少个
        if (count > 0) { // 星球编号重复直接返回，表示注册失败
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户星球编号重复"); // 改进：利用自定义的全局异常类来处理
        }
        // 2.对密码进行加密：可以加盐(salt)来进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()); // MD5加密
        // 3.插入数据：将新用户的用户名和密码保存到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) { // 保存数据失败直接返回-1
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新用户保存到数据库失败"); // 改进：利用自定义的全局异常类来处理
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验：
        // (1)账户、密码、校验密码不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户或密码为空"); // 改进：利用自定义的全局异常类来处理
        }
        // (2)账户不能小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (3)密码不能小于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短"); // 改进：利用自定义的全局异常类来处理
        }
        // (4)账户不能包含特殊字符：正则表达式
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~!@#￥%……&*()——+|{}【】';:”“’。,、?]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount); // 看传入的用户名和正则表达式是否匹配
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户包含特殊字符"); // 改进：利用自定义的全局异常类来处理
        }
        // 2.对密码进行加密：可以加盐(salt)来进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes()); // MD5加密
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 定义查询条件
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword); // 注意这个密码一定要是传入加密后的密码
        //User user = this.getOne(queryWrapper);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户不存在"); // 改进：利用自定义的全局异常类来处理
        }
        // 3.用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
        // 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
        User safetyUser = getSafetyUser(user);
        // 4.记录用户的登录态
        // (1)连接服务器端后，得到一个session状态(匿名会话)，返回给前端
        // (2)登录成功后，得到了登录成功的session，并且给该session设置一些值(比如用户信息)，
        //    返回给前端一个设置cookie的命令 session->cookie
        // (3)前端接受到后端的命令，设置cookie，保存到浏览器内
        // (4)前端再次请去后端的时候(相同的域名)，在请求头中带上cookie去请求
        // (5)后端拿到前端传来的cookie，找到对应的session
        // (6)后端从session中可以取出基于该session存储的变量(用户的登录信息，登录名)
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
     * 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
     * @param originUser 原来的用户
     * @return 脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setCreateTime(new Date());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 退出登录功能
     * 只需将用户信息从session中移除即可
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除session
        request.removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     *   1.允许用户传入多个标签，多个标签都存在才搜索出来and，like'%java%' and like '%C++%'
     *   2.允许用户传入多个标签，多个标签任何一个存在就能搜索出来 or，like '%java%' or like '%C++'
     * 两种方式：
     *   1.SQL查询(实现简单，可以通过拆分查询进一步优化)
     *   2.内存查询(灵活，可以通过并发进一步优化)
     * 建议通过实际测试来分析哪种查询比较快，数据量大的时候验证效果更明显
     *   1.如果参数可以分析，根据用户的参数去选择查询方式，比如标签数
     *   2.如果参数不可以分析，并且数据库链接足够，内存空间足够，可以并发同时查询，谁先返回用谁
     *   3.还可以SQL查询与内存计算相结合，比如先用SQL过滤部分tag(可以写在简历上)
     *
     * 我们这里之后使用内存查询
     * @param tagNameList 用户拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) { // 如果为空直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        /**
        // 方式一：SQL查询(实现简单，可以通过拆分查询进一步优化)
        //根据模糊查询来查，like '%java%' and like 'C++'; like拼接的字符串越长查询就越慢
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 定义查询条件，模糊查询
        // 拼接and查询
        // like '%java%' and like '%Python%'
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName); // 不断把like进行叠加
        }
        List<User> userList = userMapper.selectList(queryWrapper); // 直接进行查询
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList()); // 返回脱敏后的用户列表
         */
        // 方式二：内存查询
        //先查询所有对象到内存中，再将内存中的所有对象中的tag标签和穿过来的tag进行比较
        // 解析JSON字符串：
        //   序列化：Java对象转成JSON字符串。
        //   反序列化：把JSON字符串转成Java对象
        // Java JSON 序列化库有很多：
        //   1.gson(google的)
        //   2.fastjson alibaba(ali出品，快，但是漏洞太多)
        //   3.jackson
        //   4.kryo
        // 1.先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        // 2.在内存中判断是否包含要求的标签
        // 如果要并行查询，就将stream()改成parallelStream()，他用的是一个公共线程池，有一定的风险，可能拿不到线程，会被其他程序占用
        return userList.stream().filter(user -> { // 遍历每一个User，用filter来代替for循环，是false就过滤掉，是true就保留
            String tagsStr = user.getTags(); // json字符串
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType()); // 将JSON字符串反序列化成一个对象
            // 用ofNullable()去封装一个可能为空的对象，再用orElse()给对象一个默认值，也就是说如果tempTagNameList为空，则取的值就是默认值new HashSet<>()的值
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            //gson.toJson(tempTagNameList); // 反序列化，这样得到的就是一个JSON字符串了
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) { // set集合不包含要查询的字段就直接返回false，否则返回true
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     *   如果是管理员，允许更新任意用户
     *   如果不是管理员，只允许更新当前用户
     * @param user 表示要修改的用户
     * @param loginUser 表示当前登录的用户
     * @return 返回值大于0表示更新成功
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId(); // 得到要修改的用户的id
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前用户
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId); // 查一下原来的用户
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR); // 表示没有这个用户
        }
        return userMapper.updateById(user); // user就是要修改的用户信息
    }

    /**
     * 获取当前登录用户信息
     * @param request 从request获取session信息
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 从session中获取当前用户信息
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) { // 为空说明没有权限
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员，提取作为一个方法
     * @param request
     * @return
     */
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        // 用户未登录，或者不是管理员权限，就不能进行下一步的查询用户操作
        if (user == null || user.getUserRole() != UserConstant.ADMIN_ROLE) {
            return false;
        }
        return true;
    }

    /**
     * 是否是管理员
     * @param loginUser 当前的登录用户
     * @return
     */
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 用户匹配：获取最匹配的用户，根据分数来进行匹配标签，用到动态规划
     * 这里用到了最佳匹配算法
     *
     * 下面直接取出所有用户，依次和当前用户计算分数就，取top N，优化方法：
     *   1.切忌不要在数据量大的时候循环输出日志，取消掉日志之后会加快很多。
     *   2.map存了所有的分数信息，占用内存。解决：
     *     小顶堆：维护一个固定长度的有序集合(sortedSet)，按照分数降序，只保留分数最高的几个用户，
     *             如果大于顶部的，就弹出顶部元素，压入当前元素
     *   3.细节：剔除自己
     *   4.尽量只查需要的用户：
     *      1.过滤掉标签为空的用户
     *      2.根据部分标签取用户
     *      3.只查需要的数据(比如id和tags列，不查select *，查某一列就行)
     *   5.提前查？
     *      1.提前把所有用户给缓存(不适用于经常更新的数据)
     *      2.提前运算出来结果，缓存(针对一些重点用户，比如vip用户)
     *
     * @param num 匹配多少个用户
     * @param loginUser 当前登录的用户
     * @return 返回匹配到的用户
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags"); // 4.3 只查id和tags这两列，提高查询速度
        queryWrapper.isNotNull("tags"); // 4.1 标签不为空才可以查出来
        List<User> userList = this.list(queryWrapper); // 从数据库中查询所有数据
        String tags = loginUser.getTags(); // 得到当前用户的标签
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {}.getType()); // 将标签json字符串转换成字符串列表
        // 用户列表的下标 -> 相似度，这里还可以用小顶堆进行优化，堆底->堆顶：由小到大排序
        List<Pair<User, Long>> list = new ArrayList<>(); // Pair<User, Long>里面传用户以及他对应的相似度分数
        // 依次计算所有用户和当前用户的相似度，并放到上面定义的List<Pair<User, Long>>列表中
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i); // 拿到一个用户
            String userTags = user.getTags(); // 取到这个用户的标签json格式
            // 3 无标签或者当前标签是自己，就直接跳过
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) { // 因为id在User定义的是Integer类型，所以要用equals()方法来进行比较
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {}.getType()); // 将标签json字符串转换成字符串列表
            // 运用最短距离算法进行计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance)); // 将当前用户以及对应的分数添加进来
        }
        // 按编辑距离升序排序，得到前num的分数的列表，分数越小，匹配度越高，这里已经排好序了
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))  // getValue()取Long，就是分数，按分数升序排序
                .limit(num)  // 取前num条数据
                .collect(Collectors.toList()); // 把数据转换成新的列表
        // 取出topUserPairList中的key里面的id(也就是用户信息)，并把他转换成新的列表，这里已经排好序了
        // 原本按顺序排列的用户列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        // 上面只是从数据库中查询了两个字段id和tags，所以我们还需要从数据库中查询一次符合条件的用户的全部信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList); // 这里在列表中查询是没有顺序的，所以返回的结果也没有顺序
        // 1, 3, 2
        // user1, user2, user3
        // 1->user1, 2->user2, 3->user3
        // 将查询出来的数据脱敏，并且返回给前端，这里查出来的用户是无序的，没有按分数进行排序
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId)); // 这里是根据id去分组
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) { // 遍历原本有顺序的list
            finalUserList.add(userIdUserListMap.get(userId).get(0)); // 因为是根据id进行分组，这里每个组只有一个id，所以取get(0)
        }
        return finalUserList; // 直接返回top N的用户信息
    }

    /**
     * 根据标签搜索用户
     *   SQL查询(实现简单，可以通过拆分查询进一步优化)
     * @param tagNameList 用户拥有的标签
     * @return
     */
    @Deprecated  // 表示过时了
    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) { // 如果为空直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 方式一：SQL查询(实现简单，可以通过拆分查询进一步优化)
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 定义查询条件，模糊查询
        // 拼接and查询
        // like '%java%' and like '%Python%'
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName); // 不断把like进行叠加
        }
        List<User> userList = userMapper.selectList(queryWrapper); // 直接进行查询
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList()); // 返回脱敏后的用户列表
    }
}




