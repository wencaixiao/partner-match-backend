package com.xiaowc.partnermatch;

import com.xiaowc.partnermatch.mapper.UserMapper;
import com.xiaowc.partnermatch.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

// 方法一：
// 使用@SpringBootTest后，Spring将加载所有被管理的Bean，基本等同于启动了整个服务，测试就可以进行功能测试
@SpringBootTest //用这个import org.junit.jupiter.api.Test;包下面的@Test注解就可以不用下面的@RunWith(SpringRunner.class)了
// 意义在于Test测试类要使用注入的类，比如@Autowired注入的类，有了@RunWith(SpringRunner.class)这些类才能
// 实例化到spring容器中，自动注入才能生效，不然直接一个NullPointerExecption。
// @RunWith(SpringRunner.class) //如果是用的import org.junit.Test;这个包下的@Test注解就需要加@RunWith(SpringRunner.class)注解了

//// 方法二：这个好像不行
// @SpringBootTest(classes = { PartnerMatchApplication.class }) //测试类要指定一个入口类的名称
public class SampleTest {

    @Resource // @Resource是按照javabean的名称去注入，@Autowire是按照类型去注入，一般用@Resource自动注入
    private UserMapper userMapper;

    @Test
    public void testSelect() {
        System.out.println(("----- selectAll method test -----"));
        List<User> userList = userMapper.selectList(null); //利用mybatis内置的查询查询所有
        Assert.assertEquals(5, userList.size()); //预期与查出来的大小是否相等
        userList.forEach(System.out::println);
    }
}
