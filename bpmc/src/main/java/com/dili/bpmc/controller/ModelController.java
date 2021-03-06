package com.dili.bpmc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dili.bpmc.sdk.domain.ModelMapping;
import com.dili.logger.sdk.annotation.BusinessLogger;
import com.dili.logger.sdk.base.LoggerContext;
import com.dili.logger.sdk.glossary.LoggerConstant;
import com.dili.ss.activiti.service.ActivitiService;
import com.dili.ss.domain.BaseOutput;
import com.dili.ss.domain.EasyuiPageOutput;
import com.dili.ss.metadata.ValueProviderUtils;
import com.dili.uap.sdk.domain.UserTicket;
import com.dili.uap.sdk.session.SessionContext;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 流程模型控制器
 * @author wm
 * @date 2019-3-6
 * @since 1.0
 */
@Controller
@RequestMapping("/model")
public class ModelController {

    private final Logger log = LoggerFactory.getLogger(ModelController.class);

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ActivitiService activitiService;


    /**
     * 模型管理首页
     * @param modelMap
     * @param request
     * @return
     */
    @RequestMapping(value = "/index.html", method = {RequestMethod.GET, RequestMethod.POST})
    public String index(ModelMap modelMap, HttpServletRequest request) {
        int rowSize = 20;
        int page = 1;
        List<Model> list = repositoryService.createModelQuery().listPage(rowSize * (page - 1), rowSize);
        EasyuiPageOutput easyuiPageOutput = new EasyuiPageOutput();
        easyuiPageOutput.setTotal(new Long(list.size()));
        easyuiPageOutput.setRows(list);
        request.setAttribute("models", JSON.toJSONString(easyuiPageOutput, SerializerFeature.WriteDateUseDateFormat));
        return "process/model";
    }

    /**
     * 删除模型
     * @param modelId 模型id
     * @return
     */
    @BusinessLogger(businessType = "bpmc", content = "删除流程", operationType = "del", systemCode = "BPMC")
    @RequestMapping(value = "/del.action", method = {RequestMethod.GET})
    public void deleteModel(@RequestParam String modelId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        activitiService.deleteModel(modelId);
        LoggerContext.put(LoggerConstant.LOG_BUSINESS_CODE_KEY, modelId);
        LoggerContext.put(LoggerConstant.LOG_BUSINESS_ID_KEY, modelId);
        UserTicket userTicket = SessionContext.getSessionContext().getUserTicket();
        if (userTicket != null) {
            LoggerContext.put(LoggerConstant.LOG_OPERATOR_ID_KEY, userTicket.getId());
            LoggerContext.put(LoggerConstant.LOG_OPERATOR_NAME_KEY, userTicket.getRealName());
            LoggerContext.put(LoggerConstant.LOG_MARKET_ID_KEY, userTicket.getFirmId());
        }
        response.sendRedirect(request.getContextPath() + "/model/index.html");
    }

    /**
     * 查询
     * @param model
     * @return
     */
    @RequestMapping(value = "/list.action",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public String list(ModelMapping model) throws Exception {
        ModelQuery modelQuery = repositoryService.createModelQuery();
        if(StringUtils.isNotBlank(model.getCategory())){
            modelQuery.modelCategory(model.getCategory());
        }
        if(StringUtils.isNotBlank(model.getKey())){
            modelQuery.modelKey(model.getKey());
        }
        if(StringUtils.isNotBlank(model.getName())){
            modelQuery.modelNameLike("%"+model.getName()+"%");
        }
        List list = modelQuery.list();

        List results = ValueProviderUtils.buildDataByProvider(model.getMetadata(), list);
        EasyuiPageOutput easyuiPageOutput = new EasyuiPageOutput((long)results.size(), results);
        return easyuiPageOutput.toString();
    }

    /**
     * 修改
     * @param modelParam
     * @return
     */
    @BusinessLogger(businessType = "bpmc", content = "修改流程", operationType = "edit", systemCode = "BPMC")
    @RequestMapping(value = "/update.action",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public BaseOutput<String> update(Model modelParam) {
        Model model = repositoryService.getModel(modelParam.getId());
        model.setName(modelParam.getName());
        model.setCategory(modelParam.getCategory());
        model.setKey(modelParam.getKey());
        Map<String, String> metaInfo = (Map<String, String>) JSON.parse(model.getMetaInfo());
        metaInfo.put("name",modelParam.getName());
        model.setMetaInfo(JSON.toJSONString(metaInfo));
        try {
            repositoryService.saveModel(model);
            LoggerContext.put(LoggerConstant.LOG_BUSINESS_CODE_KEY, modelParam.getName());
            LoggerContext.put(LoggerConstant.LOG_BUSINESS_ID_KEY, modelParam.getId());
            UserTicket userTicket = SessionContext.getSessionContext().getUserTicket();
            if (userTicket != null) {
                LoggerContext.put(LoggerConstant.LOG_OPERATOR_ID_KEY, userTicket.getId());
                LoggerContext.put(LoggerConstant.LOG_OPERATOR_NAME_KEY, userTicket.getRealName());
                LoggerContext.put(LoggerConstant.LOG_MARKET_ID_KEY, userTicket.getFirmId());
            }
        } catch (Exception e) {
            log.error("id为"+modelParam.getId()+"修改模型设计失败:"+e.getMessage());
            return BaseOutput.failure(e.getMessage());
        }
        return BaseOutput.success();
    }
}
