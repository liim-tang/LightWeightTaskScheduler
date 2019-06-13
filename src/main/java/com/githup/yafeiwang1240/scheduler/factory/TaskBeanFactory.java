package com.githup.yafeiwang1240.scheduler.factory;

import com.githup.yafeiwang1240.scheduler.context.JobExecutionContext;
import com.githup.yafeiwang1240.scheduler.core.TimeDecoder;
import com.githup.yafeiwang1240.scheduler.handler.TaskManageHandler;
import com.githup.yafeiwang1240.scheduler.job.JobTrigger;
import com.githup.yafeiwang1240.scheduler.worker.Worker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskBeanFactory implements TaskFactory {

    public enum TaskState {
        NEW,
        RUNNABLE,
        RUNNING,
        WAITING,
        SHUTDOWN,
    }

    private TaskState state = TaskState.NEW;

    private Map<String, Worker> workerMap = new ConcurrentHashMap<>();

    private TaskManageHandler<String, Worker> executeWorkerHandler;

    private TaskManageHandler<String, TaskTracker> trackerWorkerHandler;

    private TaskManageHandler<String, Worker> removeWorkerHandler;

    private TaskTracker taskTracker;

    public void setExecuteWorkerHandler(TaskManageHandler<String, Worker> executeWorkerHandler) {
        this.executeWorkerHandler = executeWorkerHandler;
    }

    public void setTrackerWorkerHandler(TaskManageHandler<String, TaskTracker> trackerWorkerHandler) {
        this.trackerWorkerHandler = trackerWorkerHandler;
    }

    public void setRemoveWorkerHandler(TaskManageHandler<String, Worker> removeWorkerHandler) {
        this.removeWorkerHandler = removeWorkerHandler;
    }

    public TaskState getState() {
        return state;
    }

    public boolean removeWorker(String fullName, Worker worker) {
        removeWorker(fullName);
        if(worker == null) {
            return false;
        }
        removeWorkerHandler.invoke(fullName, worker);
        return true;
    }

    public boolean executeWorker(String fullName, Worker worker) {
        executeWorkerHandler.invoke(fullName, worker);
        worker.getContext().getJobTrigger().compute();
        return true;
    }

    public Worker removeWorker(String fullName) {
        Worker worker = workerMap.remove(fullName);
        return worker;
    }

    public void addWorker(String fullName, Worker worker) {
        workerMap.put(fullName, worker);
    }

    @Override
    public boolean start() {
        // 重复启动
        if(!(state == TaskState.NEW || state == TaskState.SHUTDOWN)) {
            return false;
        }
        if(taskTracker == null) {
            taskTracker = new TaskTracker();
        }
        state = TaskState.RUNNABLE;
        taskTracker.ready();
        trackerWorkerHandler.invoke("start", taskTracker);
        state = TaskState.RUNNING;
        return true;
    }

    @Override
    public boolean shutdown() {
        // 已关闭 或者 未启动
        if(state == TaskState.NEW || state == TaskState.SHUTDOWN) {
            return false;
        }
        taskTracker.stop();
        state = TaskState.SHUTDOWN;
        return true;
    }

    public class TaskTracker implements Runnable {
        private boolean exit = false;
        @Override
        public void run() {
            while(!exit) {
                long now = System.currentTimeMillis();
                Map<String, Worker> removes = null;
                Map<String, Worker> executes = null;
                for(Map.Entry<String, Worker> work : workerMap.entrySet()) {
                    String key = work.getKey();
                    Worker worker = work.getValue();
                    JobExecutionContext context = worker.getContext();
                    JobTrigger jobTrigger = context.getJobTrigger();
                    long nextTime = jobTrigger.getNextTime();
                    // 使用创建 防止任务稀疏
                    if(nextTime == TimeDecoder.IS_STOP_TIME) {
                        if(removes == null) {
                            removes = new HashMap<>();
                        }
                        removes.put(key, worker);
                    } else if(now >= nextTime) {
                        if(executes == null) {
                            executes = new HashMap<>();
                        }
                        executes.put(key, worker);
                    }
                }
                if(removes != null) {
                    for(Map.Entry<String, Worker> work : removes.entrySet()) {
                        String key = work.getKey();
                        Worker worker = work.getValue();
                        removeWorker(key, worker);
                    }
                }

                if(executes != null) {
                    for(Map.Entry<String, Worker> work : executes.entrySet()) {
                        String key = work.getKey();
                        Worker worker = work.getValue();
                        executeWorker(key, worker);
                    }
                }
            }
        }
        private void stop() {
            exit = true;
        }
        private void ready() {
            exit = false;
        }
    }

}