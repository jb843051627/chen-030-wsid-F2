package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TWsidTplBook;

import java.util.List;

/**
 * 专用标识证面版式模板册 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITWsidTplBookService {

    /** 按主键查询 */
    TWsidTplBook selectTWsidTplBookById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TWsidTplBook> selectTWsidTplBookList(Wrapper<TWsidTplBook> queryWrapper);

    /** 新增 */
    int insertTWsidTplBook(TWsidTplBook record);

    /** 修改 */
    int updateTWsidTplBook(TWsidTplBook record);

    /** 批量删除 */
    int deleteTWsidTplBookByIds(String ids);

    /** 推进一态 */
    int advanceTWsidTplBook(Long id);

    /** 退回一态 */
    int revertTWsidTplBook(Long id);

    /** 按主键删除 */
    int deleteTWsidTplBookById(Long id);
}
