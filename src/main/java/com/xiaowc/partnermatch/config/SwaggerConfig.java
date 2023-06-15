package com.xiaowc.partnermatch.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * 自定义swagger接口文档的配置
 * Swagger原理：
 *   1.自定义Swagger配置类
 *   2.定义需要生成接口文档的代码位置(controller)
 * 可以通过在controller方法上添加@Api,@ApiImplicitParam,@ApiOperation等注解来自定义描述信息
 * 启动之后访问这个网址：http://localhost:8080/api/doc.html#/home，有各种信息，非常方便
 */
// 表示这个类是一个配置类,会把这个类注入到ioc容器中
@Configuration
// 开启swagger2的功能
@EnableSwagger2WebMvc
// 用来指定在哪个环境下加载这个配置，如果不是这两个环境，访问上面的这个网址就不会有接口文档的信息，提高安全性
// 千万注意：线上环境不要把接口暴露出去，可以通过在SwaggerConfig配置文件开头加上@Profile({"dev", "test"})限定配置仅在部分环境开启
@Profile({"dev", "test"})
public class SwaggerConfig {

    @Bean(value = "defaultApi2")
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)  // 指定swagger的版本
                .apiInfo(apiInfo())  // 就是下面定义的这个方法
                .select()
                // 这里一定要标注你控制器的位置
                .apis(RequestHandlerSelectors.basePackage("com.xiaowc.partnermatch.controller")) // 生成接口文档的位置
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * api信息
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("xiaowc伙伴匹配系统")
                .description("xiaowc伙伴匹配系统接口文档")
                .termsOfServiceUrl("https://github.com/wencaixiao")
                .contact(new Contact("xiaowc","https://github.com/wencaixiao","2950426381@qq.com"))
                .version("1.0")
                .build(); // 用build去构造api用户对象
    }
}