package com.xiaowc.partnermatch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//加入这个注解后mybatis-plus会扫描这个mapper包下的文件，增删改查就可以注入到这个项目中，
//一行代码不写也可以实现增删改查了
@MapperScan("com/xiaowc/partnermatch/mapper")
@EnableScheduling // 在spingboot中开启一个定时任务的支持
public class PartnerMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartnerMatchApplication.class, args);
    }

}
