package com.lee.dynamic.pool;

import com.lee.dynamic.pool.bean.TaskBean;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 定时任务业务
 */
@org.springframework.stereotype.Service
public class Service {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);

    @Autowired
    private Mapper mapper;
    /**
     * 定时任务线程池
     */
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    /**
     * 可重入锁
     */
    private ReentrantLock lock = new ReentrantLock();
    /**
     * 所有定时任务存放Map
     * key :任务 key
     * value:任务实现
     */
    @Resource(name = "taskJobMap")
    private Map<String, TaskBean> scheduledTaskJobMap;
    /**
     * 存放已经启动的任务map
     */
    private Map<String, ScheduledFuture> scheduledFutureMap = new ConcurrentHashMap<>();

    /**
     * 添加任务
     */
    public String add(TaskBean bean) {
        return mapper.add(bean);
    }

    /**
     * 获取所有任务
     */
    public List<TaskBean> taskList() {
        LOGGER.info(">>>>>> 获取任务列表开始 >>>>>> ");
        //数据库查询所有任务 => 未做分页
        List<TaskBean> taskBeanList = mapper.getAllTask();
        if (CollectionUtils.isEmpty(taskBeanList)) {
            return new ArrayList<>();
        }

        for (TaskBean taskBean : taskBeanList) {
            String taskKey = taskBean.getTaskKey();
            //是否启动标记处理
            taskBean.setStartFlag(this.isStart(taskKey));
        }
        LOGGER.info(">>>>>> 获取任务列表结束 >>>>>> ");
        return taskBeanList;
    }

    /**
     * 根据任务key 启动任务
     */
    public Boolean start(String taskKey) {
        LOGGER.info(">>>>>> 启动任务 {} 开始 >>>>>>", taskKey);
        //添加锁放一个线程启动，防止多人启动多次
        lock.lock();
        LOGGER.info(">>>>>> 添加任务启动锁完毕");
        try {
            //校验是否已经启动
            if (this.isStart(taskKey)) {
                LOGGER.info(">>>>>> 当前任务已经启动，无需重复启动！");
                return false;
            }
            //校验任务是否存在
            if (!scheduledTaskJobMap.containsKey(taskKey)) {
                return false;
            }
            //根据key数据库获取任务配置信息
            TaskBean scheduledTask = mapper.getByKey(taskKey);
            //启动任务
            this.doStartTask(scheduledTask);
        } finally {
            // 释放锁
            lock.unlock();
            LOGGER.info(">>>>>> 释放任务启动锁完毕");
        }
        LOGGER.info(">>>>>> 启动任务 {} 结束 >>>>>>", taskKey);
        return true;
    }

    /**
     * 根据 key 停止任务
     */
    @SneakyThrows
    public Boolean stop(String taskKey) {
        LOGGER.info(">>>>>> 进入停止任务 {}  >>>>>>", taskKey);
        //当前任务实例是否存在
        boolean taskStartFlag = scheduledFutureMap.containsKey(taskKey);
        LOGGER.info(">>>>>> 当前任务实例是否存在 {}", taskStartFlag);
        if (taskStartFlag) {
            //获取任务实例
            ScheduledFuture runnable = scheduledFutureMap.get(taskKey);
            //关闭实例
            runnable.cancel(true);
        }
        LOGGER.info(">>>>>> 结束停止任务 {}  >>>>>>", taskKey);
        return taskStartFlag;
    }

    /**
     * 根据任务key 重启任务
     */
    public Boolean restart(String taskKey) {
        LOGGER.info(">>>>>> 进入重启任务 {}  >>>>>>", taskKey);
        //先停止
        this.stop(taskKey);
        //再启动
        return this.start(taskKey);
    }

    /**
     * 程序启动时初始化  ==> 启动所有正常状态的任务
     */
    public void initAllTask(List<TaskBean> scheduledTaskBeanList) {
        LOGGER.info("程序启动 ==> 初始化所有任务开始 ！size={}", scheduledTaskBeanList.size());
        if (CollectionUtils.isEmpty(scheduledTaskBeanList)) {
            return;
        }
        for (TaskBean scheduledTask : scheduledTaskBeanList) {
            //任务 key
            String taskKey = scheduledTask.getTaskKey();
            //校验是否已经启动
            if (this.isStart(taskKey)) {
                continue;
            }
            //启动任务
            this.doStartTask(scheduledTask);
        }
        LOGGER.info("程序启动 ==> 初始化所有任务结束 ！size={}", scheduledTaskBeanList.size());
    }



    /**
     * 执行启动任务
     */
    @SneakyThrows
    private void doStartTask(TaskBean scheduledTask) {
        //任务key
        String taskKey = scheduledTask.getTaskKey();
        //定时表达式
        String taskCron = scheduledTask.getTaskCron();
        //获取需要定时调度的接口
        TaskBean task = scheduledTaskJobMap.get(taskKey);
        Runnable runnable = (Runnable) Class.forName(task.getTaskBean()).newInstance();
        LOGGER.info(">>>>>> 任务 [ {} ] ,cron={}", scheduledTask.getTaskBean(), taskCron);
        ScheduledFuture scheduledFuture = threadPoolTaskScheduler.schedule(runnable,
                new Trigger() {
                    @Override
                    public Date nextExecutionTime(TriggerContext triggerContext) {
                        CronTrigger cronTrigger = new CronTrigger(taskCron);
                        return cronTrigger.nextExecutionTime(triggerContext);
                    }
                });
        //将启动的任务放入 map
        scheduledFutureMap.put(taskKey, scheduledFuture);
    }


    /**
     * 任务是否已经启动
     */
    private Boolean isStart(String taskKey) {
        //校验是否已经启动
        if (scheduledFutureMap.containsKey(taskKey)) {
            if (!scheduledFutureMap.get(taskKey).isCancelled()) {
                return true;
            }
        }
        return false;
    }

}
