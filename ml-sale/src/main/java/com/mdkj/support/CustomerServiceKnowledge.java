package com.mdkj.support;

import com.mdkj.dto.ChatFaqVO;

import java.util.List;

/**
 * 客服知识库（供 Spring AI 系统提示词与规则兜底共用）
 */
public final class CustomerServiceKnowledge {

    private CustomerServiceKnowledge() {
    }

    public static final List<ChatFaqVO> FAQ_LIST = List.of(
            new ChatFaqVO("如何购买课程？", "在课程详情页点击「加入购物车」或「立即购买」，也可在首页参与秒杀活动。下单后使用支付宝扫码完成支付即可。"),
            new ChatFaqVO("如何查看已购课程？", "支付成功后，进入「我的 → 我的课程」即可查看并学习已购买的课程。"),
            new ChatFaqVO("如何查看订单？", "进入「我的 → 我的订单」可查看全部订单；待付款订单可点击「继续支付」完成付款。"),
            new ChatFaqVO("支持退款吗？", "课程属于虚拟产品，购买成功后不支持退款，请确认后再下单。"),
            new ChatFaqVO("如何使用优惠券？", "在购物车页面输入优惠口令并搜索，满足条件后结算时会自动抵扣。"),
            new ChatFaqVO("秒杀怎么参与？", "首页「整点秒杀」活动进行中时，点击「立即秒杀」抢课，成功后需在15分钟内完成支付。")
    );

    public static final String SYSTEM_ROLE = """
            你是「ML课堂」在线智能客服，专门解答课程购买、支付、订单、秒杀、优惠券、账号与学习相关问题。

            回答要求：
            1. 使用简洁、友好的中文，每次回复控制在 200 字以内，必要时可使用换行。
            2. 只回答与本平台业务相关的问题；与业务无关的问题请礼貌拒绝并引导回到课程/订单话题。
            3. 课程为虚拟产品，购买后不支持退款（平台硬性规则，不可承诺例外）。
            4. 待付款订单请在 15 分钟内完成支付，超时自动取消。
            5. 需要人工协助时，引导用户发送邮件至 support@mylesson.com（工作日 9:00-18:00）。
            6. 若用户上下文中有订单统计或当前咨询课程，回答时结合这些信息，不要编造不存在的订单或价格。
            """;

    public static String formatFaqForPrompt() {
        StringBuilder sb = new StringBuilder("【常见问题知识库】\n");
        for (ChatFaqVO faq : FAQ_LIST) {
            sb.append("问：").append(faq.getQuestion()).append('\n');
            sb.append("答：").append(faq.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }
}
