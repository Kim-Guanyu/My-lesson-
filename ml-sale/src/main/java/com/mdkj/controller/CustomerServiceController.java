package com.mdkj.controller;

import com.mdkj.dto.ChatAskDTO;
import com.mdkj.dto.ChatFaqVO;
import com.mdkj.dto.ChatReplyVO;
import com.mdkj.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "智能客服接口")
@RequestMapping("/api/v1/customerService")
public class CustomerServiceController {

    @Autowired
    private CustomerService customerService;

    @Operation(summary = "查询 - 常见问题", description = "获取客服常见问题列表")
    @GetMapping("/faq")
    public List<ChatFaqVO> faqList() {
        return customerService.faqList();
    }

    @Operation(summary = "提问 - 智能回复", description = "用户提问并获取智能客服回复")
    @PostMapping("/ask")
    public ChatReplyVO ask(@Validated @RequestBody ChatAskDTO dto) {
        return customerService.ask(dto);
    }
}
