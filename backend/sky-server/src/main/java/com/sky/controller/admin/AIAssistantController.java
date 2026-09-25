package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.AIAssistantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 数据分析助手控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
@Api(tags = "AI 数据分析助手")
public class AIAssistantController {

    @Autowired
    private AIAssistantService aiAssistantService;

    /**
     * AI 数据分析助手提问接口
     *
     * @param request 包含 question 字段的请求体
     * @return AI 分析结果
     */
    @PostMapping("/ask")
    @ApiOperation("AI 数据分析助手提问")
    public Result<String> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        log.info("AI 助手收到问题: {}", question);
        String answer = aiAssistantService.ask(question);
        return Result.success(answer);
    }
}
