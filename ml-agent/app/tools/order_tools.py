"""订单与已购课程工具。

—— 安全设计：userId 不是模型可填的参数 ——

注意这几个工具的 args_schema 里**没有** user_id 字段，甚至没有任何参数。
用户身份统一由 require_user_id() 从请求上下文取（上下文里的 userId 又只能由
服务端从 Redis 登录令牌解析写入，见 app/api/context.py 的说明）。

这样即便用户在对话框里写「查询 userId=123 的订单」，模型也无处可填这个值，
prompt injection 无法转化为越权读取。

对应的 Java 接口：
    order-server /order/myPage          我的订单（分页）
    order-server /orderDetail/myCourses 我的已购课程（分页）
"""

from langchain_core.tools import tool

from app.api.context import require_user_id, trace_tool
from app.clients import gateway
from app.clients.gateway import GatewayError

ORDER_SERVICE = "order-server"

# 未登录时统一的回复口径，交给模型转述给用户
NOT_LOGGED_IN = "用户当前未登录，无法查询个人订单或已购课程。请引导用户先登录。"

# 订单状态码 -> 中文，与 ml-common 的 ML.Order.statusFormat 保持一致
ORDER_STATUS = {0: "未付款", 1: "已付款", 2: "已取消", 3: "其他"}


@tool
async def get_my_purchased_courses() -> str:
    """查询当前登录用户已经购买的课程列表。

    用于回答「我买过哪些课」「我能看哪些课」，也可用于在讲解课程内容前
    确认用户是否有该课程的访问权限。无需任何参数，用户身份由系统自动识别。
    """
    trace_tool("get_my_purchased_courses")
    try:
        user_id = require_user_id()
    except PermissionError:
        return NOT_LOGGED_IN

    try:
        page = await gateway.get(
            ORDER_SERVICE, "orderDetail", "/myCourses",
            params={"fkUserId": user_id, "pageNum": 1, "pageSize": 50},
        )
    except GatewayError as exc:
        return f"查询已购课程失败：{exc}"

    records = (page or {}).get("records") or []
    if not records:
        return "该用户目前还没有购买任何课程。"

    lines = [f"该用户已购买 {len(records)} 门课程："]
    for item in records:
        lines.append(
            f"- 《{item.get('courseTitle', '')}》"
            f"（课程ID={item.get('fkCourseId')}）"
        )
    return "\n".join(lines)


@tool
async def get_my_orders() -> str:
    """查询当前登录用户的订单情况，包含待付款和已付款的统计与明细。

    用于回答「我的订单」「有没有待付款的」「我买过没有」这类问题。
    无需任何参数，用户身份由系统自动识别。
    """
    trace_tool("get_my_orders")
    try:
        user_id = require_user_id()
    except PermissionError:
        return NOT_LOGGED_IN

    try:
        page = await gateway.get(
            ORDER_SERVICE, "order", "/myPage",
            params={"fkUserId": user_id, "pageNum": 1, "pageSize": 20},
        )
    except GatewayError as exc:
        return f"查询订单失败：{exc}"

    records = (page or {}).get("records") or []
    if not records:
        return "该用户暂无订单记录。"

    unpaid = sum(1 for o in records if o.get("status") == 0)
    paid = sum(1 for o in records if o.get("status") == 1)

    lines = [f"该用户共 {len(records)} 笔订单：待付款 {unpaid} 笔，已付款 {paid} 笔。"]
    for o in records[:10]:
        status = ORDER_STATUS.get(o.get("status"), "未知")
        lines.append(
            f"- 订单号 {o.get('sn', '')}｜{status}｜应付 {o.get('payAmount', '')} 元"
        )
    if unpaid:
        lines.append("提醒：待付款订单需在 15 分钟内完成支付，超时会自动取消。")
    return "\n".join(lines)
