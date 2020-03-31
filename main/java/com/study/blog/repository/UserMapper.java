package com.study.blog.repository;

import com.github.pagehelper.Page;
import com.study.blog.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 10652
 * 在启动类上添加 该注解 MapperScan("com.study.blog.repository") 替代该包下的所有 mapper 注解
 */
@Repository
public interface UserMapper {
    /**
     * 创建用户
     *
     * @param user 用户
     * @return int
     */
    int createUser(User user);

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
     * 更新用户的头像
     *
     * @param username  用户名
     * @param avatarUrl 头像保存的url
     */
    void saveUserAvatar(@Param("username") String username, @Param("avatarUrl") String avatarUrl);

    /**
     * 根据id查询用户
     *
     * @param id id
     * @return 用户
     */
    User searchById(int id);

    /**
     * 根据用户账号查询用户
     *
     * @param username 用户账号
     * @return 用户
     */
    User findOneByUsername(String username);

    /**
     * 列出所有用户
     *
     * @param pageIndex 页码
     * @param pageSize  页大小
     * @return users
     */
    Page<User> listUser(@Param("pageIndex") int pageIndex, @Param("pageSize") int pageSize);

    /**
     * 通过用户名模糊查询用户
     *
     * @param name      用户姓名
     * @param pageIndex 起始页
     * @param pageSize  每页的大小
     * @return 用户列表
     */
    Page<User> findByName(@Param("name") String name, @Param("pageIndex") int pageIndex, @Param("pageSize") int
            pageSize);

    /**
     * 根据用户名列表获取用户列表
     *
     * @param username 用户名列表
     * @return 用户列表
     */
    List<User> findUsersByUsernames(List<String> username);
}


