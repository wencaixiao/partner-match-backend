package com.xiaowc.partnermatch.service;

import com.xiaowc.partnermatch.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 用户服务测试
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testAddUser() {
        User user = new User();
        user.setUsername("xiaowc");
        user.setUserAccount("123");
        user.setAvatarUrl("https://baidu.com");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setEmail("2950426381@qq.com");
        user.setPhone("123456");
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertEquals(true, result);
    }

    @Test
    void userRegister() {
        // 验证密码不为空
        String userAccount = "xiaowc";
        String userPassword = "";
        String checkPassword = "123456";
        String planetCode = "1";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        // 验证账户不小于4位
        userAccount = "wc";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        // 验证密码不小于8位
        userAccount = "xiaowc";
        userPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        // 验证账户不能包含特殊字符
        userAccount = "xiao wc";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        // 验证两次密码输入相同
        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        // 验证插入数据成功
        userAccount = "catxiaowc";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertEquals(-1, result);
        userAccount = "xiaowc";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assert.assertTrue(result > 0);
    }

    @Test
    void userRegister1() {
        // 验证密码不为空
        String userAccount = "pigxiaowc";
        String userPassword = "12345678";
        String checkPassword = "12345678";
        String planetCode = "7";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        userAccount = "birdxiaowc";
        planetCode = "8";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
    }

    @Test
    void testSearchUsersByTags() {
        List<String> tagNameList = Arrays.asList("java", "python");
        List<User> userList = userService.searchUsersByTags(tagNameList);
        Assert.assertNotNull(userList);
    }
}