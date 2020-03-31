/*!
 * u main JS.
 * 
 * @since: 1.0.0 2017/3/9
 * @author Way Lau <https://lxk.com>
 */
"use strict";
//# sourceURL=u.js

// DOM 加载完再执行
$(function () {

    // 获取 CSRF Token
    var csrfToken = $("meta[name='_csrf']").attr("content");
    var csrfHeader = $("meta[name='_csrf_header']").attr("content");

    var _pageSize; // 存储用于搜索

    // 根据用户名、页面索引、页面大小获取用户列表
    function getBlogsByName(pageIndex, pageSize) {
        $.ajax({
            url: "/u/" + username + "/blogs",
            contentType: 'application/json',
            data: {
                "async": true,
                "pageIndex": pageIndex,
                "pageSize": pageSize,
                "catalog": catalogId,
                "keyword": $("#keyword").val()
            },
            success: function (data) {
                $("#mainContainer").html(data);
                // 如果是分类查询，则取消最新、最热选中样式
                if(catalogId){
                    $(".nav-item .nav-link").removeClass("active");
                }
            },
            error: function () {
                toastr.error("error!");
            }
        });
    }

    // 分页
    $.tbpage("#mainContainer", function (pageIndex, pageSize) {
        getBlogsByName(pageIndex, pageSize);
        _pageSize = pageSize;
    });

    // 关键字搜索
    $("#searchBlogs").click(function () {
        getBlogsByName(0, _pageSize);
    });

    // 最新\最热切换事件
    $(".nav-item .nav-link").click(function () {

        var url = $(this).attr("url");
        console.log("最新最热切换事件！" + url);

        // 先移除其他的点击样式，再添加当前的点击样式
        $(".nav-item .nav-link").removeClass("active");
        $(this).addClass("active");

        // 加载其他模块的页面到右侧工作区
        $.ajax({
            url: 'http://localhost:8080'+url + '&async=true',
            success: function (data) {
                $("#mainContainer").html(data);
            },
            error: function () {
                toastr.error("error!");
            }
        });

        // 清空搜索框内容
        $("#keyword").val('');
    });
    // 获取分类列表

    function getCatalogs(username) {
        console.log("获取分类列表！");
        // 获取CSRF Token
        $.ajax({
            url:'/catalogs',
            type: 'GET',
            data:{"username":username},
            success: function (data) {
                $("#catalogMain").html(data);
            },
            error:function () {
                toastr.error("获取分类列表 error");
            }
        })
    }

    // 获取编辑分类的页面
    $(".blog-content-container").on("click", ".blog-add-catalog", function () {
        console.log("获取分类的编辑页面");
        $.ajax({
            url: '/catalogs/edit',
            type: 'GET',
            success: function (data) {
                $("#catalogFormContainer").html(data);
            },
            error: function () {
                toastr.error("获取编辑分类页面 error!");
            }
        });
    });

    /*
    * ".blog-edit-catalog":这里必须在 id 前面加上 . 否则，不会响应：找了一个下午的错误
    * */
    //获取编辑某个分类的页面
    $(".blog-content-container").on("click",".blog-edit-catalog",function () {
        console.log("编辑某个分类");
        $.ajax({
            url: '/catalogs/edit/'+$(this).attr('catalogId'),
            type:'GET',
            success:function (data) {
                $("#catalogFormContainer").html(data);
            },
            error:function () {
                toastr.error("获取编辑某个分类页面 error!");
            }
        });
    })

    // 提交分类
    $("#submitEditCatalog").click(function () {

        $.ajax({
            url: '/catalogs',
            type: 'POST',
            contentType: "application/json ;charset=utf-8",
            data: JSON.stringify({
                "username": username,
                "catalog": {"id": $('#catalogId').val(), "name": $('#catalogName').val()}
            }),
            beforeSend: function (request) {
                // 添加 CSRF Token
                request.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (data) {
                if (data.successful) {
                    toastr.success(data.message);
                    // 成功后，刷新分类列表
                    getCatalogs(username);
                } else {
                    toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("提交分类 error!");
            }
        })
    });

    // 删除分类
    $(".blog-content-container").on("click", ".blog-delete-catalog", function () {
        $.ajax({
            url: '/catalogs/' + $(this).attr('catalogId') + '?username=' + username,
            type: 'DELETE',
            beforeSend: function (request) {
                // 添加 CSRF Token 头部信息
                request.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (data) {
                if (data.successful) {
                    toastr.success(data.message);
                    // 成功后，刷新分类列表
                    getCatalogs(username);
                } else {
                    toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("删除分类 error!");
            }
        });

        // 根据分类查询
        $(".blog-content-container").on("click", ".blog-query-by-catalog", function () {
            catalogId = $(this).attr('catalogId');
            getBlogsByName(0, _pageSize);
        });
    });
    getCatalogs(username);
});