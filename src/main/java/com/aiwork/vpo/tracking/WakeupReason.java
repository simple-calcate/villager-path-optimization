package com.aiwork.vpo.tracking;

/**
 * 唤醒原因枚举，记录村民被唤醒重新评估路径的触发来源。
 */
public enum WakeupReason {
    /** 路径依赖方块被破坏 */
    BLOCK_BROKEN,
    /** 路径依赖方块被放置（可能阻挡通行） */
    BLOCK_PLACED,
    /** 方块状态变化影响通行性（如门开关） */
    BLOCK_STATE_CHANGED,
    /** 邻居通知导致方块更新 */
    NEIGHBOR_NOTIFY,
    /** 村民正常走完路径 */
    PATH_COMPLETED,
    /** TTL 超时兜底清理 */
    TTL_EXPIRED,
    /** 路径被替换 */
    PATH_REPLACED,
    /** 手动调试触发 */
    DEBUG_TRIGGER
}
