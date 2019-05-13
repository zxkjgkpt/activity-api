package com.yfny.activityapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yfny.activityapi.service.FlowDiagramService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 流程图相关Controller
 * <p>
 * Created  by  jinboYu  on  2019/3/28
 */
@RestController
@RequestMapping(value = "/flowDiagram")
public class FlowDiagramController {

    //流程图相关Service
    @Autowired
    private FlowDiagramService flowDiagramService;

    /**
     * 根据流程实例ID获取流程图，高亮当前任务节点及历史节点
     *
     * @param taskId   任务ID
     * @param response
     */
    @GetMapping(value = "/getImage/{taskId}")
    public void getImage(@PathVariable String taskId,
                         HttpServletResponse response) throws Exception {
        //根据当前流程实例ID获取图片输入流
        InputStream is = flowDiagramService.getResourceDiagramInputStream(taskId);
        if (is == null)
            return;
        response.setContentType("image/png");
        BufferedImage image = ImageIO.read(is);
        OutputStream out = response.getOutputStream();
        ImageIO.write(image, "png", out);
        is.close();
        out.close();
    }

    /**
     * 根据流程实例ID生成流程图，只高亮当前任务节点
     *
     * @param taskId   任务ID
     */
    @PostMapping(value = "/getDiagram/{taskId}")
    public String getDiagram(@PathVariable String taskId) throws Exception {
        //根据当前流程实例ID获取图片输入流
        InputStream is = flowDiagramService.getDiagram(taskId);
        if (is == null){
            return null;
        }else {
            BufferedImage image = ImageIO.read(is);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();//io流
            ImageIO.write(image,"png",bos);//写入流中
            byte[] bytes = bos.toByteArray();//转换成字符串
            BASE64Encoder encoder = new BASE64Encoder();
            String png_base64 = encoder.encodeBuffer(bytes).trim();//转换成base64串
            png_base64 = png_base64.replaceAll("\n","").replaceAll("\r","");//删除\n\r
            return png_base64;
        }
    }

    /**
     * 根据流程实例ID生成流程图，只高亮当前任务节点,返回图片
     *
     * @param taskId   任务ID
     */
    @PostMapping(value = "/getDiagramImage/{taskId}")
    public void getDiagramImage(@PathVariable String taskId,HttpServletResponse response) throws Exception {
        //根据当前流程实例ID获取图片输入流
        InputStream is = flowDiagramService.getDiagram(taskId);
        if (is == null){
            return;
        }else {
            response.setContentType("image/png");
            BufferedImage image = ImageIO.read(is);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "png", out);
            is.close();
            out.close();
        }
    }
}
