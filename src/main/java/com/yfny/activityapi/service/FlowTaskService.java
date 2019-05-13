package com.yfny.activityapi.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 流程任务Service
 * <p>
 * Created  by  jinboYu  on  2019/3/26
 */
public interface FlowTaskService {

    /**
     * 创建任务
     * @param userId    任务创建人ID
     * @param key       部署流程ID
     * @param variables 任务变量
     * @return          返回任务ID
     * @throws Exception
     */
    Map<String,String> createTask(String userId, String key, Map<String, Object> variables) throws Exception;

    /**
     * 创建任务,不设置流程变量
     * @param userId    任务创建人ID
     * @param key       部署流程ID
     * @return          返回任务ID
     * @throws Exception
     */
    String createTask(String userId, String key) throws Exception;

    /**
     * 完成任务
     * @param taskId    任务ID
     * @param variables 任务变量
     * @return          返回任务ID
     * @throws Exception
     */
    Map<String,String> fulfilTask(String taskId, Map<String, Object> variables) throws Exception;

    /**
     * 取消任务
     * @param taskId    任务ID
     * @return          1为取消成功，2为取消失败
     * @throws Exception
     */
    int revocationTask(String taskId) throws Exception;

    /**
     * 创建用户
     * @param userId    用户ID
     * @return          1为创建成功。2为创建失败
     * @throws Exception
     */
    int createUser(String userId) throws Exception;

    /**
     * 根据流程实例ID查询流程历史记录
     * @param processInstanceId 流程实例ID
     * @throws Exception
     */
    List<Map<String,Object>> getHistories(String processInstanceId)throws Exception;

    /**
     * 根据当前任务ID查询历史记录
     * @param taskId    当前任务ID
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> getHistoriesByTaskId(String taskId)throws Exception;
}
