/*!
  * Bolg main JS.
 * 
 * @since: 1.0.0 2017/3/9
 * @author Way Lau <https://lxk.com>
 */
"use strict";
//# sourceURL=main.js

// DOM 加载完再执行
$(function () {

    var _pageSize; // 存储用于搜索

    // 根据用户名、页面索引、页面大小获取用户列表
    function getUersByName(pageIndex, pageSize) {
        $.ajax({
            url: "/users",
            contentType: 'application/json',
            data: {
                "async": true,
                "pageIndex": pageIndex,
                "pageSize": pageSize,
                "name": $("#searchName").val()
            },
            success: function (data) {
                $("#mainContainer").html(data);
            },
            error: function () {
                toastr.error("error!");
            }
        });
    }

    // 分页
    $.tbpage("#mainContainer", function (pageIndex, pageSize) {
        getUersByName(pageIndex, pageSize);
        _pageSize = pageSize;
    });

    // 搜索
    $("#searchNameBtn").click(function () {
        getUersByName(0, _pageSize);
    });

    // 获取添加用户的界面
    $("#addUser").click(function () {
        $.ajax({
            url: "/users/add",
            success: function (data) {
                $("#userFormContainer").html(data);
            },
            error: function (data) {
                toastr.error("error!");
            }
        });
    });

    // 获取编辑用户的界面
    $("#rightContainer").on("click", ".blog-edit-user", function () {
        $.ajax({
            url: "/users/edit/" + $(this).attr("userId"),
            success: function (data) {
                $("#userFormContainer").html(data);
            },
            error: function () {
                toastr.error("error!");
            }
        });
    });

    // 提交变更后，清空表单
    $("#submitEdit").click(function () {
        $.ajax({
            url: "/users",
            type: 'POST',
            data: $('#userForm').serialize(),
            success: function (data) {
                if (data.successful) {
                    $('#userForm')[0].reset();
                    toastr.success(data.message);
                    history.go(0);
                } else {
                    alert(data.message);
                    // toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("error");
            }
        });
    });

    // 删除用户
    $("#rightContainer").on("click", ".blog-delete-user", function () {
        // 从页面head标签中的 meat 标签中获取属性：CSRF Token 以及 CSRF Header，在发送数据之前需要将这两项信息放入请求头中
        var csrfToken = $("meta[name='_csrf']").attr("content");
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        alert($(this).attr("userId"));
        $.ajax({
            url: "/users/delete/" + $(this).attr("userId"),
            type: 'DELETE',
            // 在 ajax 中通过 beforeSend 方法可以修改 request 的头部信息
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (data) {
                toastr.success(data.message);
                // 删除成功则刷新页面
                history.go(0);
            },
            error: function () {
                // 否则报错
                toastr.error("error!");
            }
        });
    });
});