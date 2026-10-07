package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TWsidTplBookMapper;
import com.fc.v2.mapper.auto.TWsidSpeciesBookMapper;
import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.model.auto.TWsidSpeciesBook;
import com.fc.v2.service.ITWsidTplBookService;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 证面版式模板Service业务层处理
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TWsidTplBookServiceImpl extends ServiceImpl<TWsidTplBookMapper, TWsidTplBook> implements ITWsidTplBookService {

    @Autowired
    private TWsidSpeciesBookMapper wsidSpeciesBookMapper;

    @Override
    public TWsidTplBook selectTWsidTplBookById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TWsidTplBook> selectTWsidTplBookList(Wrapper<TWsidTplBook> queryWrapper) {
        QueryWrapper<TWsidTplBook> wrapper = new QueryWrapper<TWsidTplBook>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTWsidTplBook(TWsidTplBook record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getTplNo());
        TWsidSpeciesBook refArch = wsidSpeciesBookMapper.selectOne(new QueryWrapper<TWsidSpeciesBook>()
                .eq("id", record.getSpecId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setSpecNo(refArch.getSpecNo());
        if (StringUtils.isNotEmpty(record.getTplNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TWsidTplBook>()
                    .eq("tpl_no", record.getTplNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTWsidTplBook(TWsidTplBook record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getTplNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TWsidTplBook>()
                    .eq("tpl_no", record.getTplNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TWsidTplBook>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int advanceTWsidTplBook(Long id) {
        if (id == null) {
            return 0;
        }
        TWsidTplBook cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        Integer st = cur.getStatus();
        int next = st.intValue() + 1;
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int revertTWsidTplBook(Long id) {
        if (id == null) {
            return 0;
        }
        TWsidTplBook cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        Integer st = cur.getStatus();
        if (st == null) {
            return 0;
        }
        int next = st.intValue() - 1;
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", id).eq("del_flag", 0);
        uw.set("status", next);
        uw.set("content", null);
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int deleteTWsidTplBookByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTWsidTplBookById(Long id) {
        return this.baseMapper.deleteById(id);
    }
}
