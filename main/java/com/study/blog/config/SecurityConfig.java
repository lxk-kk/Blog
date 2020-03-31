package com.study.blog.config;

import com.study.blog.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author 10652
 * EnableWebSecurity 注解启用spring security
 * EnableGlobalMethodSecurity 注解启用方法级别的安全设置
 */
@Slf4j
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final String KEY = "lxk";

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(UserServiceImpl service, PasswordEncoder passwordEncoder) {
        this.userDetailsService = service;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 自定义 AuthenticationProvider bean 对象：通过该bean对象 可从 UserDetailsService 中获取用户的登录信息
     *
     * @return 将 AuthenticationProvider bean 对象加入 context container 中
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        // 设置密码加密方式：由于数据库密码都是明文，通过这种方式将密码以密文的方式存储：由于此处使用的PasswordEncoder默认为BCrypt，是个不可解的算法，因此将密码进行加密比较安全
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    /**
     * 必须重载该方法，自定义配置认证请求：拦截请求
     *
     * @param http http
     * @throws Exception e
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                //对所有静态文件的请求以及主页的请求放行（即：所有css、js、fonts文件下的请求和index.html的请求放行）
                .antMatchers("/css/**", "/js/**", "/fonts/**", "/index").permitAll()
                //只有管理员角色才能访问admin url路径下的请求：因为admin url下的请求都是后台管理，所以要求为管理员角色
                .antMatchers("/admin/**").hasRole("ADMIN")
                .and()
                //基于form表单登录验证
                .formLogin()
                //指定登录的页面，并且指定登录失败后的重定向页面
                .loginPage("/login").failureUrl("/loginError")
                // 登录时启用remember-me功能：这里需要一个key，可以随意设置一个字符串
                .and().rememberMe().key(KEY)
                // 处理异常，服务端拒绝访问时，将请求重定向到403页面
                .and().exceptionHandling().accessDeniedPage("/403");
        /*
            由于默认情况下，系统是启用了csrf防护（跨站伪造防御）
            对于有些资源不需要设定跨站防御，所以可以手动将这些资源排除在外，对指定的路径下的请求禁用csrf防护
                http.csrf().ignoringAntMatchers(" ");
         */
        /*
            允许来自同一来源的H2控制台请求？
            http.headers().frameOptions().sameOrigin();
         */

    }

    /**
     * 重载 configure 方法：自定义认证信息管理：用户信息设置：用于登录认证
     * <p>
     * 认证信息是存储于数据库的，所以需要使用UserDetailsService对象将认证信息从数据库中取出
     * UserDetailsService是一个接口，需要自定义一个实现类
     * 由于是与dao层交互，所以正好另 service 层的 UserServiceImpl 实现该接口，通过该service从数据库中获取用户认证信息
     *
     * @param auth auth
     */
    @Autowired
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 通过 service 从数据库中获取用户登录信息
        auth.userDetailsService(userDetailsService);
        // 通过 自定义的 AuthenticationProvider bean 将用户信息从 userDetailsService 中取出
        auth.authenticationProvider(authenticationProvider());
    }
   /* @Bean
    public CorsConfiguration corsConfiguration() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        return corsConfiguration;
    }*/

}
