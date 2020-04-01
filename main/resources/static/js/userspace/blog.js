/*!
 * blog.html 页面脚本.
 */
"use strict";
//# sourceURL=blog.js

// DOM 加载完再执行
$(function () {
    $.catalog("#catalog", ".post-content");
    var csrfToken = $("meta[name='_csrf']").attr("content");
    var csrfHeader = $("meta[name='_csrf_header']").attr("content");
    // 处理删除博客事件
    $(".blog-content-container").on("click", ".blog-delete-blog", function () {
        // 获取 CSRF Token
        var csrfToken = $("meta[name='_csrf']").attr("content");
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");

        $.ajax({
            url: $(this).attr("blogUrl"),
            type: 'DELETE',
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken); // 添加  CSRF Token
            },
            success: function (data) {
                if (data.successful) {
                    // 成功后，重定向
                    window.location = data.body;
                } else {
                    toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("error!");
            }
        });
    });

    // 获取评论列表
    function getComment(blogId) {
        $.ajax({
            url: '/comments',
            type: 'GET',
            data: {"blogId": blogId},
            success: function (data) {
                /*将返回值传递到指定 id 的 div 页面中进行渲染*/
                $('#mainContainer').html(data);
            },
            error: function () {
                toastr.error("error");
            }
        });
    }

    // 处理提交评论事件
    console.log("blog:" + blogId);
    $(".blog-content-container").on("click", "#submitComment", function () {
        $.ajax({
            url: '/comments',
            type: 'POST',
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                "blogId": blogId,
                "commentContent": $('#commentContent').val()
            }),
            dataType: 'json',
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken); // 添加  CSRF Token
            },
            success: function (data) {
                console.log(data);
                if (data.successful) {

                    // 清空评论框
                    $('#commentContent').val('');
                    // 获取评论列表
                    getComment(blogId);

                } else {
                    alert(data.message);
                    // toastr.error(data.message)
                }
            },
            error: function () {
                // toastr.error("error");
                alert("请先登录！");
            }
        });
    });

    // 处理评论删除事件
    $(".blog-content-container").on("click", ".blog-delete-comment", function () {
        $.ajax({
            url: '/comments/' + $(this).attr("commentId") + '?blogId=' + blogId,
            type: 'DELETE',
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (data) {
                if (data.successful) {
                    // 获取评论列表
                    getComment(blogId);
                } else {
                    toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("error!");
            }
        });
    })
    // 这个可以他么的当作模板了，艹，也不知道是什么原因！！！！
    $(".blog-content-container").on("click", "#submitVote", function () {
        $.ajax({
            url: '/vote',
            type: 'POST',
            data: {"blogId": blogId},
            dataType: 'json',
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken); // 添加  CSRF Token
            },
            success: function (data) {
                console.log(data);
                if (data.successful) {
                    toastr.success(data.message);
                    history.go(0);
                } else {
                    alert(data);
                }
            },
            error: function () {
                // toastr.error("error");
                alert("请先登录！");
            }
        });
    });
    // 取消点赞
    $(".blog-content-container").on("click", "#cancelVote", function () {
        console.info("取消点赞！");
        $.ajax({
            url: '/vote/' + blogId + '/' + $(this).attr('voteId'),
            type: 'DELETE',
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken);
            },
            success: function (data) {
                if (data.successful) {
                    toastr.info(data.message);
                    history.go(0);
                    // window.location = blogUrl;

                } else {
                    toastr.error(data.message);
                }
            },
            error: function () {
                toastr.error("error");
            }
        });
    });
    // 初始化博客
    getComment(blogId);
});