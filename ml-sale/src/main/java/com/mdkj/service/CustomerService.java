package com.mdkj.service;

import com.mdkj.dto.ChatAskDTO;
import com.mdkj.dto.ChatFaqVO;
import com.mdkj.dto.ChatReplyVO;

import java.util.List;

public interface CustomerService {

    List<ChatFaqVO> faqList();

    ChatReplyVO ask(ChatAskDTO dto);
}
