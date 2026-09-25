package com.sky.statemachine;

import com.sky.entity.OrderAction;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.vo.ActionDetailVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单状态机
 * 集中管理订单状态转移规则，替代散落在 Service 中的 if-else 校验
 *
 * 状态定义：
 * 1 = PENDING_PAYMENT   待付款
 * 2 = TO_BE_CONFIRMED   待接单
 * 3 = CONFIRMED         已接单
 * 4 = DELIVERY_IN_PROGRESS 派送中
 * 5 = COMPLETED         已完成
 * 6 = CANCELLED         已取消
 */
public class OrderStateMachine {

    /**
     * 状态转移表：key = 当前状态, value = 该状态允许执行的动作列表
     */
    private static final Map<Integer, List<OrderAction>> TRANSITION_MAP;

    /**
     * 动作到目标状态的映射：key = 动作, value = 目标状态
     */
    private static final Map<OrderAction, Integer> TARGET_STATUS_MAP;

    static {
        // 初始化转移表
        Map<Integer, List<OrderAction>> transitionMap = new HashMap<>();

        // 1 待付款：可支付、可取消、可超时
        transitionMap.put(Orders.PENDING_PAYMENT,
                Arrays.asList(OrderAction.PAY, OrderAction.CANCEL, OrderAction.SET_TIMEOUT));

        // 2 待接单：可接单、可拒单、可取消
        transitionMap.put(Orders.TO_BE_CONFIRMED,
                Arrays.asList(OrderAction.CONFIRM, OrderAction.REJECT, OrderAction.CANCEL));

        // 3 已接单：可派送
        transitionMap.put(Orders.CONFIRMED,
                Collections.singletonList(OrderAction.DELIVER));

        // 4 派送中：可完成、可超时（自动完成）
        transitionMap.put(Orders.DELIVERY_IN_PROGRESS,
                Arrays.asList(OrderAction.COMPLETE, OrderAction.SET_TIMEOUT));

        // 5 已完成：终态，无允许动作
        transitionMap.put(Orders.COMPLETED, Collections.emptyList());

        // 6 已取消：终态，无允许动作
        transitionMap.put(Orders.CANCELLED, Collections.emptyList());

        TRANSITION_MAP = Collections.unmodifiableMap(transitionMap);

        // 初始化动作 -> 目标状态映射
        Map<OrderAction, Integer> targetMap = new EnumMap<>(OrderAction.class);
        targetMap.put(OrderAction.PAY, Orders.TO_BE_CONFIRMED);
        targetMap.put(OrderAction.CANCEL, Orders.CANCELLED);
        targetMap.put(OrderAction.CONFIRM, Orders.CONFIRMED);
        targetMap.put(OrderAction.REJECT, Orders.CANCELLED);
        targetMap.put(OrderAction.DELIVER, Orders.DELIVERY_IN_PROGRESS);
        targetMap.put(OrderAction.COMPLETE, Orders.COMPLETED);
        targetMap.put(OrderAction.SET_TIMEOUT, null); // 超时需根据当前状态计算

        TARGET_STATUS_MAP = Collections.unmodifiableMap(targetMap);
    }

    /**
     * 私有构造器，禁止实例化（静态工具类）
     */
    private OrderStateMachine() {
    }

    /**
     * 判断当前状态是否允许执行指定动作
     *
     * @param currentStatus 当前订单状态
     * @param action        待执行的动作
     * @return 是否允许
     */
    public static boolean canExecute(Integer currentStatus, OrderAction action) {
        if (currentStatus == null || action == null) {
            return false;
        }
        List<OrderAction> allowedActions = TRANSITION_MAP.get(currentStatus);
        return allowedActions != null && allowedActions.contains(action);
    }

    /**
     * 执行动作校验，不允许则抛出异常
     *
     * @param currentStatus 当前订单状态
     * @param action        待执行的动作
     * @throws OrderBusinessException 如果动作不允许
     */
    public static void validate(Integer currentStatus, OrderAction action) {
        if (!canExecute(currentStatus, action)) {
            throw new OrderBusinessException("当前订单状态不允许执行该操作");
        }
    }

    /**
     * 获取当前状态允许执行的所有动作
     *
     * @param currentStatus 当前订单状态
     * @return 允许的动作列表（不可变）
     */
    public static List<OrderAction> getAllowedActions(Integer currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }
        return TRANSITION_MAP.getOrDefault(currentStatus, Collections.emptyList());
    }

    /**
     * 获取当前状态允许执行的动作详情（返回给前端）
     *
     * @param currentStatus 当前订单状态
     * @return ActionDetailVO 列表
     */
    public static List<ActionDetailVO> getAllowedActionDetails(Integer currentStatus) {
        return getAllowedActions(currentStatus).stream()
                .map(action -> ActionDetailVO.builder()
                        .action(action.getCode())
                        .label(action.getLabel())
                        .needReason(action.isNeedReason())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 执行动作并返回目标状态
     *
     * @param currentStatus 当前订单状态
     * @param action        执行的动作
     * @return 目标状态
     * @throws OrderBusinessException 如果动作不允许
     */
    public static Integer execute(Integer currentStatus, OrderAction action) {
        validate(currentStatus, action);

        // 超时动作需要根据当前状态计算目标状态
        if (action == OrderAction.SET_TIMEOUT) {
            return resolveTimeoutTarget(currentStatus);
        }

        return TARGET_STATUS_MAP.get(action);
    }

    /**
     * 根据当前状态解析超时处理的目标状态
     * - 待付款(1) -> 已取消(6)
     * - 派送中(4) -> 已完成(5)
     */
    private static Integer resolveTimeoutTarget(Integer currentStatus) {
        if (Objects.equals(currentStatus, Orders.PENDING_PAYMENT)) {
            return Orders.CANCELLED;
        }
        if (Objects.equals(currentStatus, Orders.DELIVERY_IN_PROGRESS)) {
            return Orders.COMPLETED;
        }
        throw new OrderBusinessException("当前状态不支持超时处理");
    }

    /**
     * 判断状态是否为终态（不可再变更）
     *
     * @param status 订单状态
     * @return 是否为终态
     */
    public static boolean isFinalStatus(Integer status) {
        return Objects.equals(status, Orders.COMPLETED) || Objects.equals(status, Orders.CANCELLED);
    }
}
