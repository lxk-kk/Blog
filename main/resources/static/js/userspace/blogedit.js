/*!
 * blogedit.html 页面脚本.
 */
"use strict";
//# sourceURL=blogedit.js

// DOM 加载完再执行
$(function () {

    // 初始化 md 编辑器
    $("#md").markdown({
        language: 'zh',
        fullscreen: {
            enable: true
        },
        resize: 'vertical',
        localStorage: 'md',
        imgurl: 'http://localhost:9090/file',
        base64url: 'http://localhost:9090/file'
    });

    // 初始化下拉
    $('.form-control-chosen').chosen();

    // 初始化标签
    $('.form-control-tag').tagsInput({
        'defaultText': '输入标签'
    });

    // 初始化标签控件
    $('.form-control-tag').tagEditor({
        initialTags: [],
        maxTags: 5,
        delimiter: ', ',
        forceLowercase: false,
        animateDelete: 0,
        placeholder: '请输入标签'
    });

    $('.form-control-chosen').chosen();


    $("#uploadImage").click(function () {
        console.log("upload:");
        var fileServer = $("meta[name='file_server']").attr("content");
        console.info($('#uploadformid')[0]);
        var dataIma = new FormData($('#uploadformid')[0]);
        console.info(dataIma);
        if(dataIma === null) {
            alert("请正确上传图片");
            return;
        }
        $.ajax({
            url: fileServer+'upload',
            //http://localhost:9090/file/upload
            type: 'POST',
            cache: false,
            data: dataIma,
            processData: false,
            contentType: false,
            success: function (data) {
                // alert("图片保存成功 path=" + data);
                var mdcontent = $("#md").val();
                $("#md").val(mdcontent + "\n![](" + data + ") \n");
            }
        }).done(function (res) {
            $('#file').val('');
        }).fail(function (res) {
        });
    });

    // 发布博客
    $("#submitBlog").click(function () {
        // 获取 CSRF Token
        var csrfToken = $("meta[name='_csrf']").attr("content");
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var userName = $("meta[name='userName']").attr("content");
        $.ajax({
            url: '/u/' + userName + '/blogs/edit',
            type: 'POST',
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                "blogId": Number($('#id').val()),
                "title": $('#title').val(),
                "summary": $('#summary').val(),
                "content": $('#md').val(),
                "userId": $('#userId').val(),
                "catalog":{"id":$('#catalogSelect').val()},
                "tags": $('.form-control-tag').val()
            }),
            beforeSend: function (request) {
                request.setRequestHeader(csrfHeader, csrfToken); // 添加  CSRF Token
            },
            success: function (data) {
                console.log(data);
                if (data.successful) {
                    // 成功后，重定向
                    window.location = data.body;
                } else {
                    alert(data.message);
                    // toastr.error("error!" + data.message);
                }

            },
            error: function () {
                toastr.error("error!");
            }
        });
    })
});