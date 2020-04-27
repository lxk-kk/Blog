package com.study.blog.service.impl;

import com.github.pagehelper.Page;
import com.study.blog.entity.Authority;
import com.study.blog.entity.User;
import com.study.blog.repository.AuthorityMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author 10652
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserMapper userMapper;
    private final UserAuthMiddleMapper userAuthMiddleMapper;
    private final AuthorityMapper authorityMapper;

    /**
     * 注意：千万千万不能将 UserMapper定义为user，而是该定义为 userMapper，不然它会一位user是User类中的一个属性，混淆起来
     * 将会报错：
     *
     * @param userMapper           userMapper
     * @param userAuthMiddleMapper 中间表mapper
     * @param authorityMapper      权限表
     */
    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           UserAuthMiddleMapper userAuthMiddleMapper,
                           AuthorityMapper authorityMapper) {
        this.userMapper = userMapper;
        this.userAuthMiddleMapper = userAuthMiddleMapper;
        this.authorityMapper = authorityMapper;
    }

    /**
     * 联表查询
     *
     * @param userId id
     * @return user
     */
    private User searchUserDetailById(int userId) {
        return userMapper.searchUserDetailById(userId);
    }

    /**
     * 联表查询
     *
     * @param username 账号
     * @return user
     */
    private User searchUserDetailByUsername(String username) {
        return userMapper.searchUserDetailByUsername(username);
    }

    /**
     * 分表查询
     *
     * @param userId id
     * @return user
     */
    private User searchUserSimpleById(int userId) {
        User user = userMapper.searchById(userId);
        if (Objects.isNull(user)) {
            return null;
        }
        List<Integer> idList = userAuthMiddleMapper.getAuthorityIdByUserId(userId);
        if (Objects.isNull(idList) || idList.isEmpty()) {
            user.setAuthorities(new ArrayList<>());
            // 一个人不可能没有角色！
            // todo 抛出异常
            log.error("【查询用户】：用户角色信息异常");
        } else {
            List<Authority> authorityList = authorityMapper.getAuthorityList(idList);
            user.setAuthorities(authorityList);
        }
        return user;
    }

    /**
     * 分表查询
     *
     * @param username 账号
     * @return user
     */
    private User searchUserSimpleByUsername(String username) {
        User user = userMapper.findOneByUsername(username);
        if (Objects.isNull(user)) {
            return null;
        }
        List<Integer> idList = userAuthMiddleMapper.getAuthorityIdByUserId(user.getId());
        if (Objects.isNull(idList) || idList.isEmpty()) {
            user.setAuthorities(new ArrayList<>());
            // 一个人不可能没有角色！
            // todo 抛出异常
            log.error("【查询用户】：用户角色信息异常");
        } else {
            List<Authority> authorityList = authorityMapper.getAuthorityList(idList);
            user.setAuthorities(authorityList);
        }
        return user;
    }

    @Override
    public User searchById(int userId) {
        return searchUserDetailById(userId);
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
    public User searchByUsername(String username) {
        return searchUserDetailByUsername(username);
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
        // log.info("authIds：{}", authIds);
        // log.info("authorities：{}", user.getOriginAuthorities());
        // 添加用户的同时，需要在关联表中添加对应的权限信息
        userMapper.createUser(user);
        // log.info("usrId：{}", user.getId());
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
        return searchUserDetailByUsername(username);
    }
}
