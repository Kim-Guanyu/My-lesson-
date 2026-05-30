package com.mdkj.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.ChatAskDTO;
import com.mdkj.dto.ChatFaqVO;
import com.mdkj.dto.ChatReplyVO;
import com.mdkj.entity.Order;
import com.mdkj.feign.OrderFeign;
import com.mdkj.service.CustomerService;
import com.mdkj.util.ML;
import com.mdkj.util.Result;
import com.mdkj.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final List<ChatFaqVO> FAQ_LIST = List.of(
            new ChatFaqVO("如何购买课程？", "在课程详情页点击「加入购物车」或「立即购买」，也可在首页参与秒杀活动。下单后使用支付宝扫码完成支付即可。"),
            new ChatFaqVO("如何查看已购课程？", "支付成功后，进入「我的 → 我的课程」即可查看并学习已购买的课程。"),
            new ChatFaqVO("如何查看订单？", "进入「我的 → 我的订单」可查看全部订单；待付款订单可点击「继续支付」完成付款。"),
            new ChatFaqVO("支持退款吗？", "课程属于虚拟产品，购买成功后不支持退款，请确认后再下单。"),
            new ChatFaqVO("如何使用优惠券？", "在购物车页面输入优惠口令并搜索，满足条件后结算时会自动抵扣。"),
            new ChatFaqVO("秒杀怎么参与？", "首页「整点秒杀」活动进行中时，点击「立即秒杀」抢课，成功后需在15分钟内完成支付。")
    );

    @Resource
    private OrderFeign orderFeign;

    @Override
    public List<ChatFaqVO> faqList() {
        return FAQ_LIST;
    }

    @Override
    public ChatReplyVO ask(ChatAskDTO dto) {
        String question = StrUtil.trim(dto.getQuestion());
        ChatReplyVO replyVO = new ChatReplyVO();

        for (ChatFaqVO faq : FAQ_LIST) {
            if (match(question, faq.getQuestion())) {
                replyVO.setReply(buildContextReply(dto, faq.getAnswer()));
                replyVO.setMatched(true);
                return replyVO;
            }
        }

        if (containsAny(question, "购买", "怎么买", "如何买", "下单")) {
            replyVO.setReply(buildContextReply(dto,
                    "您可以在课程详情页「立即购买」，或先「加入购物车」批量结算。支付时使用支付宝扫码即可。"));
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "支付", "付款", "支付宝", "扫码", "二维码")) {
            replyVO.setReply("下单后会弹出支付宝二维码，请使用支付宝 App 扫码支付。请在15分钟内完成付款，超时订单将失效。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "退款", "退换", "退货")) {
            replyVO.setReply("课程为虚拟产品，一经购买不支持退款，请您谅解。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "我的课程", "已购", "在哪看", "怎么看课", "学习")) {
            replyVO.setReply("请进入「我的 → 我的课程」查看已购课程，点击课程卡片即可进入详情页学习。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "订单", "待付款", "未支付")) {
            replyVO.setReply(buildOrderReply(dto));
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "秒杀", "抢课", "活动")) {
            replyVO.setReply("首页可查看当日秒杀场次，活动进行中点击「立即秒杀」即可抢购，成功后请尽快完成支付。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "优惠", "优惠券", "口令", "折扣")) {
            replyVO.setReply("在购物车页面输入优惠口令并搜索，满足使用条件后结算时会自动减免相应金额。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "登录", "账号", "密码", "注册")) {
            replyVO.setReply("首页或「我的」页面可登录/注册。忘记密码可在「我的账号」中修改密码。");
            replyVO.setMatched(true);
            return replyVO;
        }
        if (containsAny(question, "人工", "联系", "电话", "邮箱")) {
            replyVO.setReply("如需人工协助，请发送邮件至 support@mylesson.com，工作日 9:00-18:00 会尽快回复。");
            replyVO.setMatched(true);
            return replyVO;
        }

        replyVO.setReply("抱歉，暂未理解您的问题。您可以点击上方常见问题，或换个方式描述，例如「如何购买」「查看订单」「我的课程」。");
        replyVO.setMatched(false);
        return replyVO;
    }

    private String buildContextReply(ChatAskDTO dto, String baseReply) {
        if (ObjectUtil.isNotNull(dto.getCourseId()) && StrUtil.isNotBlank(dto.getCourseTitle())) {
            return "关于《" + dto.getCourseTitle() + "》：\n" + baseReply;
        }
        return baseReply;
    }

    private String buildOrderReply(ChatAskDTO dto) {
        String base = "请进入「我的 → 我的订单」查看订单详情。";
        Long fkUserId = dto.getFkUserId();
        if (ObjectUtil.isNull(fkUserId)) {
            return base + "登录后可为您查询待付款订单数量。";
        }
        try {
            Result<PageVO<Order>> result = orderFeign.myPage(fkUserId, 1, 50);
            if (ObjectUtil.isNull(result) || ObjectUtil.isNull(result.getData())) {
                return base;
            }
            List<Order> records = result.getData().getRecords();
            if (ObjectUtil.isEmpty(records)) {
                return base + "\n您当前暂无订单记录。";
            }
            long unpaid = records.stream().filter(o -> ML.Order.UNPAID.equals(o.getStatus())).count();
            long paid = records.stream().filter(o -> ML.Order.PAID.equals(o.getStatus())).count();
            return base + String.format("\n您共有 %d 笔待付款订单、%d 笔已付款订单。", unpaid, paid);
        } catch (Exception e) {
            return base;
        }
    }

    private boolean match(String question, String faqQuestion) {
        String q = normalize(question);
        String f = normalize(faqQuestion);
        if (q.contains(f.replace("？", "").replace("?", ""))) {
            return true;
        }
        return containsAny(q, extractKeywords(f));
    }

    private String[] extractKeywords(String faqQuestion) {
        String text = faqQuestion.replace("？", "").replace("?", "");
        if (text.contains("购买")) return new String[]{"购买", "怎么买", "如何买"};
        if (text.contains("已购") || text.contains("课程")) return new String[]{"我的课程", "已购", "怎么看课"};
        if (text.contains("订单")) return new String[]{"订单", "待付款"};
        if (text.contains("退款")) return new String[]{"退款", "退换"};
        if (text.contains("优惠")) return new String[]{"优惠", "优惠券", "口令"};
        if (text.contains("秒杀")) return new String[]{"秒杀", "抢课"};
        return new String[]{text};
    }

    private String normalize(String text) {
        return StrUtil.blankToDefault(text, "").toLowerCase();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
