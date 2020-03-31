package com.study.blog.job;

import com.study.blog.service.BlogEvaluationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @author 10652
 */
@Slf4j
public class BlogEvaluationQuartzJob extends QuartzJobBean {

    private final BlogEvaluationCacheService cacheService;

    @Autowired
    public BlogEvaluationQuartzJob(BlogEvaluationCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            cacheService.saveBlogEvaluation2Mysql();
        } catch (Throwable throwable) {
            log.error("【定时任务】持久化数据到mysql：{}", throwable.getMessage());
            throw new JobExecutionException("持久化数据到mysql：失败");
        }

    }
}
