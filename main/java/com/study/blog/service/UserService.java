package com.study.blog.service;

import com.github.pagehelper.Page;
import com.study.blog.entity.User;

import java.util.List;

/**
 * @author 10652
 */
public interface UserService {
    /**
     * 创建用户
     *
     * @param user 用户
     */
    void createUser(User user);

    /**
     * 修改用户
     *
     * @param user 用户
     */
    void updateUser(User user);

    /**
     * 根据id删除用户
     *
     * @param id id
     */
    void deleteUser(int id);

    /**
     * 根据id查询用户
     *
     * @param id id
     * @return 用户
     */
    User searchById(int id);

    /**
     * 通过用户名模糊查询博客用户（用户名并不是用户的唯一标识）
     *
     * @param name      用户名
     * @param pageIndex 起始页
     * @param pageSize  每页的大小
     * @return 用户列表
     */
    Page<User> listUsersByName(String name, int pageIndex, int pageSize);

    /**
     * 根据用户名列表获取用户列表
     *
     * @param usernameList 用户名列表
     * @return 用户列表
     */
    List<User> listUserByName(List<String> usernameList);

    /**
     * 通过用户账号查找用户（用户账号是唯一标识）
     *
     * @param username 用户账号
     * @return 用户
     */
    User searchByUsername(String username);

    /**
     * 更新用户头像
     *
     * @param username  用户名
     * @param avatarUrl 头像URL
     */
    void updateUserAvatar(String username, String avatarUrl);

    /**
     * @param username 用户名
     * @param password 密码
     * @return 用户
     */
    User validateUser(String username, String password);
}
