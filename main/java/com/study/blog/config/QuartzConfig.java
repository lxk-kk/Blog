package com.study.blog.config;

import com.study.blog.job.BlogEvaluationQuartzJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 10652
 */
@Configuration
@Slf4j
public class QuartzConfig {
    /**
     * 设置 标识：trigger 与 jobDetail 的标识
     */
    private static final String BLOG_EVALUATION_QUARTZ_IDENTITY = "BlogEvaluationQuartzIdentity";

    @Bean
    public JobDetail quartzJobDetail() {
        return JobBuilder.newJob(BlogEvaluationQuartzJob.class)
                // 设置标识 jobDetail
                .withIdentity(BLOG_EVALUATION_QUARTZ_IDENTITY)
                /*
                 storeDurably()方法：可以在没有触发器指向任务的时候，使用 sched.addJob(job, true) 将任务保存在队列中了，而后使用 sched.scheduleJob 触发。
                    如果不使用 storeDurably ，则在添加 Job 到引擎的时候会抛异常，意思就是该 Job 没有对应的 Trigger。
                 */
                .storeDurably()
                // 创建 JobDetail 实例
                .build();
    }

    @Bean
    public Trigger quartzTrigger() {
        // 设置 定时机制
        SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder
                .simpleSchedule()
                // 永久重复
                .repeatForever()
                // 1min 重复一次
                //.withIntervalInMinutes(1)
                // 1h 重复一次
                .withIntervalInHours(1);


        return TriggerBuilder.newTrigger()
                // 每个触发器 关联 一个 Job
                .forJob(quartzJobDetail())
                // 设置 标识：trigger
                .withIdentity(BLOG_EVALUATION_QUARTZ_IDENTITY)
                // SimpleTrigger
                .withSchedule(simpleScheduleBuilder)
                // 创建 SimpleTrigger 实例
                .build();
    }

}
