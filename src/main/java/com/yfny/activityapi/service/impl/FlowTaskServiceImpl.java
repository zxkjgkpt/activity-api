package com.yfny.activityapi.service.impl;

import com.codingapi.txlcn.tc.annotation.LcnTransaction;
import com.yfny.activityapi.service.FlowTaskService;
import com.yfny.activityapi.utils.ActivitiUtils;
import com.yfny.activityapi.utils.DeleteTaskCmd;
import com.yfny.activityapi.utils.SetFLowNodeAndGoCmd;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程任务Service实现类
 * <p>
 * Created  by  jinboYu  on  2019/3/11
 */
@Service
public class FlowTaskServiceImpl implements FlowTaskService {

    @Autowired
    private ActivitiUtils activitiUtils;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyServicel;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private IdentityService identityService;


    /**
     * 创建任务
     *
     * @param userId
     * @param key
     * @param variables
     * @return
     */
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    @LcnTransaction
    public Map<String,String> createTask(String userId, String key, Map<String, Object> variables) throws Exception {
        Map<String,String> resultMap = new HashMap<>();
        //获取当前流程实例ID
        String processInstanceId = activitiUtils.getProcessInstance(userId, key).getId();
        //查询第一个任务
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        //设置流程任务变量
        taskService.setVariables(task.getId(), variables);
        //完成任务
        taskService.complete(task.getId());
        //返回下一个任务的ID
        Task result = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        if (result!=null){
            resultMap.put("taskId",result.getId());
            resultMap.put("taskName",result.getName());
            resultMap.put("flowId",result.getProcessInstanceId());
        }
        return resultMap;
    }

    /**
     * 创建任务,不设置流程变量
     *
     * @param userId 任务创建人ID
     * @param key    部署流程ID
     * @return 返回任务ID
     * @throws Exception
     */
    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    @LcnTransaction
    public String createTask(String userId, String key) throws Exception {
        //获取当前流程实例ID
        String processInstanceId = activitiUtils.getProcessInstance(userId, key).getId();
        //查询第一个任务
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        //完成任务
        taskService.complete(task.getId());
        //返回下一个任务的ID
        String taskId = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
        return "任务ID:" + taskId + ",流程实例ID:" + processInstanceId;
    }

    /**
     * 完成任务
     *
     * @param taskId    任务ID
     * @param variables 流程变量
     * @return
     */
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    @LcnTransaction
    public Map<String,String> fulfilTask(String taskId, Map<String, Object> variables) {
        Map<String,String> resultMap = new HashMap<>();
        //根据任务ID获取当前任务实例
        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        //设置流程任务变量
        taskService.setVariables(taskId, variables);
        //完成任务
        taskService.complete(taskId);
        //返回下一个任务的ID
        Task resultTask = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        if (resultTask!=null){
            resultMap.put("taskId",resultTask.getId());
            resultMap.put("taskName",resultTask.getName());
        }
        return resultMap;
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return
     */
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    @LcnTransaction
    public int revocationTask(String taskId) {
        //获取当前任务
        Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
        //获取流程定义
        Process process = repositoryService.getBpmnModel(currentTask.getProcessDefinitionId()).getMainProcess();
        //获取目标节点定义
        FlowNode targetNode = (FlowNode) process.getFlowElement("endevent1");
        //删除当前运行任务
        String executionEntityId = managementService.executeCommand(new DeleteTaskCmd(currentTask.getId()));
        //流程执行到来源节点
        managementService.executeCommand(new SetFLowNodeAndGoCmd(targetNode, executionEntityId));
        return 1;
    }

    /**
     * 创建用户
     *
     * @param userId
     * @return
     */
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    @LcnTransaction
    public int createUser(String userId) {
        User user = identityService.newUser(userId);
        user.setFirstName("edison" + userId);
        user.setLastName("yu");
        user.setId(userId);
        identityService.saveUser(user);
        return 1;
    }

    /**
     * 根据流程实例ID查询流程历史记录
     *
     * @param processInstanceId
     * @throws Exception
     */
    @Override
    public List<Map<String,Object>> getHistories(String processInstanceId) throws Exception {
        List<Map<String,Object>> resultList = new ArrayList<>();
        List<HistoricTaskInstance> historicTaskInstanceList = historyServicel.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).list();
        if (historicTaskInstanceList!=null){
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstanceList) {
                Map<String,Object> map = new HashMap<>();
                map.put("name",historicTaskInstance.getName());
                map.put("startTime",historicTaskInstance.getStartTime());
                map.put("endTime",historicTaskInstance.getEndTime());
                map.put("id",historicTaskInstance.getId());
                map.put("claimTime",historicTaskInstance.getClaimTime());
                resultList.add(map);
            }
            return resultList;
        }else {
            return null;
        }
    }

    /**
     * 根据当前任务ID查询历史记录
     *
     * @param taskId 当前任务ID
     * @return
     * @throws Exception
     */
    @Override
    public List<Map<String, Object>> getHistoriesByTaskId(String taskId) throws Exception {
        List<Map<String,Object>> resultList = new ArrayList<>();
        List<HistoricTaskInstance> historicTaskInstanceList = historyServicel.createHistoricTaskInstanceQuery().taskId(taskId).list();
        if (historicTaskInstanceList!=null){
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstanceList) {
                Map<String,Object> map = new HashMap<>();
                map.put("name",historicTaskInstance.getName());
                map.put("startTime",historicTaskInstance.getStartTime());
                map.put("endTime",historicTaskInstance.getEndTime());
                map.put("id",historicTaskInstance.getId());
                map.put("claimTime",historicTaskInstance.getClaimTime());
                resultList.add(map);
            }
            return resultList;
        }
        return null;
    }
}
