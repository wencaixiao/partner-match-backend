package com.xiaowc.partnermatch.service;

import com.xiaowc.partnermatch.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author xiaowc
 * @description 用户服务
 * @createDate 2022-12-23 20:24:35
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * 只需将用户信息存入session中即可
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏，隐藏敏感信息，防止数据库中的字段泄露
     * 脱敏就是我们新生成一个对象，我们设置允许返回给前端的值
     * @param originUser 原来的用户
     * @return 脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 退出登录功能
     * 只需将用户信息从session中移除即可
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tagNameList 用户拥有的标签
     * @return 返回匹配标签的用户
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 更新用户信息
     * @param user
     * @return 返回值大于0表示更新成功
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     * @param request 从request获取session信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员，提取作为一个方法
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员，提取作为一个方法
     * @param loginUser 当前登录的用户
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 用户匹配：获取最匹配的用户，根据分数来进行匹配，用到动态规划
     * @param num 匹配多少个用户
     * @param loginUser 当前登录的用户
     * @return 返回匹配到的用户
     */
    List<User> matchUsers(long num, User loginUser);
}
