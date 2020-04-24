package com.study.blog.service.impl;

import com.github.pagehelper.Page;
import com.study.blog.entity.Authority;
import com.study.blog.entity.User;
import com.study.blog.repository.UserAuthMiddleMapper;
import com.study.blog.repository.UserMapper;
import com.study.blog.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 10652
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserMapper userMapper;
    private final UserAuthMiddleMapper userAuthMiddleMapper;

    /**
     * 注意：千万千万不能将 UserMapper定义为user，而是该定义为 userMapper，不然它会一位user是User类中的一个属性，混淆起来
     * 将会报错：
     *
     * @param userMapper           userMapper
     * @param userAuthMiddleMapper 中间表mapper
     */
    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserAuthMiddleMapper userAuthMiddleMapper) {
        this.userMapper = userMapper;
        this.userAuthMiddleMapper = userAuthMiddleMapper;
    }

    @Override
    public User searchById(int id) {
        return userMapper.searchById(id);
    }

    @Override
    public Page<User> listUsersByName(String name, int pageIndex, int pageSize) {
        // 去除 name 两端空格再判断
        if (StringUtils.isEmpty(name.trim())) {
            return userMapper.listUser(pageIndex, pageSize);
        }
        name = "%" + name + "%";
        // 未使用分页组件前：手动定位到数据表中具体的行：return userMapper.findByName(name, (pageIndex - 1) * pageSize, pageSize)
        return userMapper.findByName(name, pageIndex, pageSize);
    }

    @Override
    public List<User> listUserByName(List<String> usernameList) {
        return userMapper.findUsersByUsernames(usernameList);
    }

    @Override
    public User findOneByUsername(String username) {
        return userMapper.findOneByUsername(username);
    }

    /**
     * 事务场景中，抛出异常被catch后，如果需要回滚则一定要手动回滚！
     * 需要在Transactional注解中指定rollbackFor回滚，或是在方法中显示使用rollback回滚
     * 如果使用了事务一定需要手动回滚！
     *
     * @param user 用户
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void createUser(User user) {
        List<Integer> authIds = user.getOriginAuthorities().stream().map(Authority::getAuthorityId)
                .collect(Collectors.toList());
        log.info("authIds：{}", authIds);
        log.info("authorities：{}", user.getOriginAuthorities());
        /*
            添加用户的同时，需要在关联表中添加对应的权限信息
         */
        userMapper.createUser(user);
        log.info("usrId：{}", user.getId());
        userAuthMiddleMapper.saveUserAuth(user.getId(), authIds);
    }

    /**
     * @param user 用户
     */
    @Override
    public void updateUser(User user) {
        userMapper.updateUser(user);
    }

    @Override
    public void deleteUser(int id) {
        userMapper.deleteUser(id);
    }

    @Override
    public void updateUserAvatar(String username, String avatarUrl) {
        userMapper.saveUserAvatar(username, avatarUrl);
    }

    @Override
    public User validateUser(String username, String password) {
        return new User();
    }

    /**
     * 实现 UserDetailsService 接口：根据用户账号加载用户认证信息
     *
     * @param username 用户名
     * @return User
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("origin {}", username);
        return userMapper.findOneByUsername(username);
    }
}
