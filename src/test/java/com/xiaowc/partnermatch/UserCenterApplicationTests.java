package com.xiaowc.partnermatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

@SpringBootTest // 这个测试类自动识别上面的入口类
class PartnerMatchApplicationTests {

    @Test
    void testDigest() throws NoSuchAlgorithmException {
        String newPassword = DigestUtils.md5DigestAsHex(("abcd" + "mypassword").getBytes(StandardCharsets.UTF_8));
        System.out.println(newPassword);
    }

    @Test
    void contextLoads() {

    }

}
