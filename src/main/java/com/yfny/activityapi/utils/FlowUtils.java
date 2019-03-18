package com.yfny.activityapi.utils;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Created  by  jinboYu  on  2019/3/13
 */
@Component
public class FlowUtils {
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngineFactoryBean processEngine;
    /**
     * 获取历史节点流程图
     * @param id
     * @return
     */
    public InputStream getResourceDiagramInputStream(String id) {
        try {
            // 获取历史流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(id).singleResult();
            // 获取流程中已经执行的节点，按照执行先后顺序排序
            List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery().processInstanceId(id).orderByHistoricActivityInstanceId().asc().list();
            // 构造已执行的节点ID集合
            List<String> executedActivityIdList = new ArrayList<String>();
            for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
                executedActivityIdList.add(activityInstance.getActivityId());
            }
            // 获取bpmnModel
            BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
            // 获取流程已发生流转的线ID集合
            List<String> flowIds = this.getExecutedFlows(bpmnModel, historicActivityInstanceList);
            // 使用默认配置获得流程图表生成器，并生成追踪图片字符流
            ProcessDiagramGenerator processDiagramGenerator = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
            //你也可以 new 一个
            //DefaultProcessDiagramGenerator processDiagramGenerator = new DefaultProcessDiagramGenerator();
            InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", executedActivityIdList, flowIds, "宋体", "微软雅黑", "黑体", null, 1.0);
            return imageStream;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取历史连线记录
     * @param bpmnModel
     * @param historicActivityInstances
     * @return
     */
    private List<String> getExecutedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // 流转线ID集合
        List<String> flowIdList = new ArrayList<String>();
        // 全部活动实例
        List<FlowNode> historicFlowNodeList = new LinkedList<FlowNode>();
        // 已完成的历史活动节点
        List<HistoricActivityInstance> finishedActivityInstanceList = new LinkedList<HistoricActivityInstance>();
        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            historicFlowNodeList.add((FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId(), true));
            //如果当前节点已经完成则添加到已完成的历史活动节点List集合中
            if (historicActivityInstance.getEndTime() != null) {
                finishedActivityInstanceList.add(historicActivityInstance);
            }
        }
        // 遍历已完成的活动实例，从每个实例的outgoingFlows中找到已执行的
        FlowNode currentFlowNode = null;
        for (int i =0;i<historicActivityInstances.size()-1;i++) {
            // 获得当前活动对应的节点信息及outgoingFlows信息
            currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i).getActivityId(), true);
            List<SequenceFlow> sequenceFlowList = currentFlowNode.getOutgoingFlows();
            /**
             * 遍历outgoingFlows并找到已已流转的
             * 满足如下条件认为已已流转：
             * 1.当前节点是并行网关或包含网关，则通过outgoingFlows能够在历史活动中找到的全部节点均为已流转
             * 2.当前节点是以上两种类型之外的，通过outgoingFlows查找到的时间最近的流转节点视为有效流转
             */
            FlowNode targetFlowNode = null;
            if (BpmsActivityTypeEnum.PARALLEL_GATEWAY.getType().equals(historicActivityInstances.get(i).getActivityType())
                    || BpmsActivityTypeEnum.INCLUSIVE_GATEWAY.getType().equals(historicActivityInstances.get(i).getActivityType())) {
                // 遍历历史活动节点，找到匹配Flow目标节点的
                for (SequenceFlow sequenceFlow : sequenceFlowList) {
                    targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
                    if (historicFlowNodeList.contains(targetFlowNode)) {
                        flowIdList.add(sequenceFlow.getId());
                    }
                }
            }
            //如果是排他网关
            else if (BpmsActivityTypeEnum.EXCLUSIVE_GATEWAY.getType().equals(historicActivityInstances.get(i).getActivityType())) {
                for (SequenceFlow sequenceFlow : sequenceFlowList) {
                    targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
                    if (historicFlowNodeList.contains(targetFlowNode)){
                       if (historicActivityInstances.get(i + 1).getActivityId().equals(targetFlowNode.getId())){
                           flowIdList.add(sequenceFlow.getId());
                       }

                    }
                }

            }else {
                    List<Map<String, String>> tempMapList = new LinkedList<Map<String,String>>();
                    // 遍历历史活动节点，找到匹配Flow目标节点的
                    for (SequenceFlow sequenceFlow : sequenceFlowList) {
                        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                            if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
                                tempMapList.add(UtilMisc.toMap("flowId", sequenceFlow.getId(), "activityStartTime", String.valueOf(historicActivityInstance.getStartTime().getTime())));
                            }
                        }
                    }
                    // 遍历匹配的集合，取得开始时间最早的一个
                    long earliestStamp = 0L;
                    String flowId = null;
                    for (Map<String, String> map : tempMapList) {
                        long activityStartTime = Long.valueOf(map.get("activityStartTime"));
                        if (earliestStamp == 0 ||earliestStamp>=activityStartTime) {
                            earliestStamp = activityStartTime;
                            flowId = map.get("flowId");
                        }
                    }
                    flowIdList.add(flowId);
                }
            }
        return flowIdList;
    }
}
