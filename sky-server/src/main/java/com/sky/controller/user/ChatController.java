package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.result.Result;
import com.sky.service.ChatService;
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
 * 用户端智能客服
 */
@RestController("userChatController")
@RequestMapping("/user/chat")
@Slf4j
@Api(tags = "智能客服")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 用户发送消息，获取智能客服回复
     *
     * @param payload 包含 message 字段
     * @return 客服回复文本
     */
    @PostMapping
    @ApiOperation("智能客服对话")
    public Result<String> chat(@RequestBody Map<String, String> payload) {
        Long userId = BaseContext.getCurrentId();
        String message = payload.get("message");
        log.info("用户 {} 向智能客服发送消息：{}", userId, message);
        String reply = chatService.chat(userId, message);
        return Result.success(reply);
    }
}
