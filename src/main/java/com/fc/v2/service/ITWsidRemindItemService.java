package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TWsidRemindItem;

/**
 * 年度核查与换证提醒条目 Service接口（scheduling-job 形状：周期执行，无增删改查入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITWsidRemindItemService {

    /** 按主键回查条目 */
    TWsidRemindItem selectTWsidRemindItemById(Long id);

    /** 该时刻可处理的条目（执行窗口内 + 到期 + 尚未处理） */
    List<TWsidRemindItem> listDue(Date at);

    /** 执行一次，返回**成功条数**；单条失败跳过继续 */
    int runOnce(Date at);
}
