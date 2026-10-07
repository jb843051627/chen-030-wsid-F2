package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.conf.WsidTplProperties;
import com.fc.v2.mapper.auto.TWsidSpeciesBookMapper;
import com.fc.v2.mapper.auto.TWsidTplBookMapper;
import com.fc.v2.model.auto.TWsidSpeciesBook;
import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.model.custom.wsid.WsidTplConsts;
import com.fc.v2.model.custom.wsid.WsidTplLineCmd;
import com.fc.v2.model.custom.wsid.WsidTplPick;
import com.fc.v2.model.custom.wsid.WsidTplScope;
import com.fc.v2.model.custom.wsid.WsidTplSearchCmd;
import com.fc.v2.service.ITWsidTplBookService;
import com.fc.v2.util.SnowflakeIdWorker;
import com.fc.v2.util.StringUtils;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 证面版式模板册 Service：建版取式唯一条路子的落点。
 *
 * <p>六段规矩一一在此收口：</p>
 * <ol>
 *   <li>建行／改一处同走 {@link #saveLine}：代号系统排（{@link #nextTplNo()}），
 *       物种＋层别＋名目三同撞键且立着的那一条不是自己即挡回，栏位不合逐栏点名；
 *       物种代号对不进底册建不成、已调出名录的段收不进新行，冗余的物种代号／称谓以底册为准回写。</li>
 *   <li>取用只 0/1/2 三式；一屏并列几联量程只认 1..3，0/9 与空着同，按册上配置落。</li>
 *   <li>{@link #pick} 是按层回落取式的定死出口：县定缺退市定、市定缺退省定，
 *       三级全空才取系统兜底版且强制只出正联；同名目取最后落笔（ver_no 大），
 *       落笔分不出先后比代号串字面取靠后；轨迹逐格留痕，年底复核指得出回落处。</li>
 *   <li>完成比 cover_num 不留填处，每回落库前由应发／已渲重算（75.00／100.00），
 *       算出的数盖上一轮的；应发空、带负号、小于已渲，或已渲反超应发，都进不了库。</li>
 *   <li>行只有在位／已撤下两态：{@link #takeOff} 单条成批同一个说法；撤下后文面改不了、
 *       整行抽不走；撤下的不上对外排面，但在册册面仍列得出、仍计入在册套数，不兴抹掉。</li>
 *   <li>检索开物种代号／槽位文面／层别名目三口子，代号、名目走前缀，文面走全文关键词，
 *       全等模糊各一路；眼界按层级岗切，本层一行未立空着回话合法，不牵邻层的行来填。</li>
 * </ol>
 *
 * <p>两处汇来（{@link #mergeLine}）的取舍次序写在 wsid.tpl.source-order 配置里，
 * 两路同时到按先后认整路，不两路都写、不取页面先亮的数。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Service
public class TWsidTplBookServiceImpl implements ITWsidTplBookService {

    @Resource
    private TWsidTplBookMapper tplBookMapper;

    @Resource
    private TWsidSpeciesBookMapper speciesBookMapper;

    @Resource
    private WsidTplProperties tplProperties;

    /** 同名目并立几行的裁决：先比最后落笔（ver_no 大），再比代号串字面（靠后说话）。 */
    private static final Comparator<TWsidTplBook> NEWEST_THEN_NO_DESC = new Comparator<TWsidTplBook>() {
        @Override
        public int compare(TWsidTplBook a, TWsidTplBook b) {
            int va = a.getVerNo() == null ? 0 : a.getVerNo().intValue();
            int vb = b.getVerNo() == null ? 0 : b.getVerNo().intValue();
            if (va != vb) {
                return vb - va;
            }
            String ca = a.getTplNo() == null ? "" : a.getTplNo();
            String cb = b.getTplNo() == null ? "" : b.getTplNo();
            return cb.compareTo(ca);
        }
    };

    // ============================================================ 建行／改一处

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TWsidTplBook saveLine(WsidTplLineCmd cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("没接着建行／改一处的话头");
        }

        Integer specId = cmd.getSpecId();
        String levelKind = trimToNull(cmd.getLevelKind());
        String tplName = trimToNull(cmd.getTplName());
        String tplText = trimToNull(cmd.getTplText());
        Integer tplStyle = cmd.getTplStyle();

        // —— 栏位逐栏点名，空着不合行进不了库 ——
        List<String> invalid = new ArrayList<String>();
        if (specId == null) {
            invalid.add("挂靠物种代号（spec_id）这一栏空着");
        }
        if (levelKind == null) {
            invalid.add("层别名目（level_kind）这一栏空着");
        } else if (!isKnownLevel(levelKind)) {
            invalid.add("层别名目（level_kind）只认县定／市定／省定／系统兜底，接到的是「"
                    + levelKind + "」");
        }
        if (tplName == null) {
            invalid.add("版式名目（tpl_name）这一栏空着");
        }
        if (tplText == null) {
            invalid.add("槽位文面（tpl_text）这一栏空着");
        }
        if (tplStyle == null) {
            invalid.add("版式取用三式（tpl_style）这一栏空着");
        } else if (tplStyle.intValue() != WsidTplConsts.STYLE_FRONT
                && tplStyle.intValue() != WsidTplConsts.STYLE_COPY
                && tplStyle.intValue() != WsidTplConsts.STYLE_FALLBACK) {
            invalid.add("版式取用三式（tpl_style）只认 0只出正联／1只出副本联／2顺层回落，接到的是「"
                    + tplStyle + "」");
        }
        if (cmd.getPlanNum() == null) {
            invalid.add("本套应发张数（plan_num）这一栏空着");
        } else if (cmd.getPlanNum().intValue() < 0) {
            invalid.add("本套应发张数（plan_num）带负号（" + cmd.getPlanNum() + "），进不了库");
        }
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException(joinChinese(invalid) + "，这一行进不了库");
        }

        // 系统兜底版只给正联，建版时就不许摆成全出副本联
        if (WsidTplConsts.LEVEL_SYSTEM.equals(levelKind)
                && tplStyle.intValue() != WsidTplConsts.STYLE_FRONT) {
            throw new IllegalArgumentException("系统兜底那一版只许只出正联（tpl_style=0），"
                    + "不许顺手取成全出副本联");
        }

        // 一屏并列几联只认 1..3，0／9 与空着同，按册上配置落
        Integer styleNum = WsidTplProperties.normalizeStyleNum(cmd.getStyleNum());
        if (cmd.getStyleNum() == null
                || cmd.getStyleNum().intValue() < WsidTplConsts.STYLE_NUM_MIN
                || cmd.getStyleNum().intValue() > WsidTplConsts.STYLE_NUM_MAX) {
            styleNum = tplProperties.getStyleNum();
        }

        // —— 代号拿到名录底册里对不出账，这一行建不成 ——
        TWsidSpeciesBook species = speciesBookMapper.selectOne(new QueryWrapper<TWsidSpeciesBook>()
                .eq("id", specId)
                .eq("del_flag", 0));
        if (species == null) {
            throw new IllegalArgumentException("挂靠物种代号（spec_id=" + specId
                    + "）这一栏对不进名录底册，这一行建不成");
        }
        Integer doneNum;
        TWsidTplBook existing = null;
        boolean movedSpecies = false;
        if (cmd.getId() == null) {
            doneNum = cmd.getDoneNum() == null ? 0 : cmd.getDoneNum().intValue();
            // 新建行：物种已调出名录，这一段再也收不进新行
            if (species.getStatus() != null && species.getStatus().intValue() == 1) {
                throw new IllegalArgumentException("物种「" + species.getSpecName() + "」已调出名录，"
                        + "这一段再也收不进新行（调出前立在库里的旧行照旧数得着）");
            }
        } else {
            existing = tplBookMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                    .eq("id", cmd.getId())
                    .eq("del_flag", 0));
            if (existing == null) {
                throw new IllegalArgumentException("要改的第 " + cmd.getId() + " 行在册上找不着（或已抽了册）");
            }
            // 撤下之后只剩一张脸：文面改不了
            if (existing.getStatus() != null && existing.getStatus().intValue() == WsidTplConsts.STATUS_OFF) {
                throw new IllegalArgumentException("第 " + cmd.getId()
                        + " 行已撤下，文面改不了、整行也抽不走");
            }
            // 已渲张数只走「更正」那一条路，建行改一处不顺手改它
            doneNum = existing.getDoneNum() == null ? 0 : existing.getDoneNum().intValue();
            movedSpecies = !specId.equals(existing.getSpecId());
            // 挪挂到别的物种底下而那一段已调出名录，等于往收不进的段里添新行，挡回；
            // 留在调出前早已立着的原段改文面，照旧办
            if (movedSpecies && species.getStatus() != null && species.getStatus().intValue() == 1) {
                throw new IllegalArgumentException("要挪挂过去的物种「" + species.getSpecName()
                        + "」已调出名录，这一段再也收不进新行");
            }
        }
        if (doneNum < 0) {
            throw new IllegalArgumentException("已渲张数（done_num）带负号（" + doneNum + "），进不了库");
        }
        if (doneNum > cmd.getPlanNum().intValue()) {
            throw new IllegalArgumentException("已渲张数（" + doneNum + "）反超本套应发张数（"
                    + cmd.getPlanNum() + "），这一行进不了库");
        }

        // —— 物种、层别名目、版式名目三同撞键：那一条「仍立着」且不是自己，挡回去；
        //    已撤下的那一条只剩一张脸、不挡新行 ——
        List<TWsidTplBook> clash = tplBookMapper.selectList(new QueryWrapper<TWsidTplBook>()
                .eq("spec_id", specId)
                .eq("level_kind", levelKind)
                .eq("tpl_name", tplName)
                .eq("status", WsidTplConsts.STATUS_ON)
                .eq("del_flag", 0));
        for (TWsidTplBook one : clash) {
            if (existing != null && one.getId().equals(existing.getId())) {
                continue; // 递来的正是那一条自己，按改一处处理，不当多出来的一条拦
            }
            throw new IllegalArgumentException("物种＋层别名目＋版式名目三样已撞在「"
                    + one.getTplNo() + "」那一条上且那一条仍立着，这一趟挡回；"
                    + "若要改它，请照那一条自己递来");
        }

        // 冗余的物种代号／称谓以物种底册为准回写，两处不许各摆一套
        String specNo = species.getSpecNo();
        String specName = species.getSpecName();

        // —— 完成比每回落库之前重算，算出的数盖掉上一轮存的那一个 ——
        BigDecimal coverNum = calcCover(cmd.getPlanNum().intValue(), doneNum);

        if (existing == null) {
            TWsidTplBook row = new TWsidTplBook();
            row.setTplNo(nextTplNo()); // 代号系统排上，不留敲键的格子
            row.setTplName(tplName);
            row.setTplText(tplText);
            row.setTplStyle(tplStyle);
            row.setStyleNum(styleNum);
            row.setLevelKind(levelKind);
            row.setParentNo(trimToNull(cmd.getParentNo()));
            row.setSpecId(specId);
            row.setSpecNo(specNo);
            row.setSpecName(specName);
            row.setVerNo(1);
            row.setPlanNum(cmd.getPlanNum());
            row.setDoneNum(doneNum);
            row.setCoverNum(coverNum);
            row.setEffStart(cmd.getEffStart());
            row.setEffEnd(cmd.getEffEnd());
            row.setStatus(WsidTplConsts.STATUS_ON);
            row.setDelFlag(0);
            row.setRemark(trimToNull(cmd.getRemark()));
            tplBookMapper.insert(row);
            return selectTWsidTplBookById(row.getId());
        }

        // 改一处：只许动文面与挂点，代号、行态不从这口改；已渲张数走更正那一条
        int nextVer = (existing.getVerNo() == null ? 0 : existing.getVerNo().intValue()) + 1;
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", existing.getId())
                .eq("del_flag", 0)
                .eq("status", WsidTplConsts.STATUS_ON); // 双保险：撤下的行落不上笔
        uw.set("spec_id", specId);
        uw.set("spec_no", specNo);
        uw.set("spec_name", specName);
        uw.set("level_kind", levelKind);
        uw.set("tpl_name", tplName);
        uw.set("tpl_text", tplText);
        uw.set("tpl_style", tplStyle);
        uw.set("style_num", styleNum);
        uw.set("parent_no", trimToNull(cmd.getParentNo()));
        uw.set("plan_num", cmd.getPlanNum());
        uw.set("cover_num", coverNum); // 改应发、挪挂物种都在落库前重算这一遍
        uw.set("eff_start", cmd.getEffStart());
        uw.set("eff_end", cmd.getEffEnd());
        uw.set("remark", trimToNull(cmd.getRemark()));
        uw.set("ver_no", nextVer);
        uw.set("update_time", new Date());
        int affected = tplBookMapper.update(null, uw);
        if (affected != 1) {
            throw new IllegalStateException("改一处只落了 " + affected + " 行（id=" + existing.getId()
                    + "），本趟作废，不许留残");
        }
        return selectTWsidTplBookById(existing.getId());
    }

    // ============================================================ 两路汇来

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TWsidTplBook mergeLine(WsidTplLineCmd fromArch, WsidTplLineCmd fromPage) {
        List<String> order = tplProperties.getSourceOrder();
        if (order == null || order.isEmpty()) {
            order = java.util.Arrays.asList("底册", "页面");
        }
        // 谁先谁后在册上写着：排在前头的那一路整路为准，两路同时到按先后认
        WsidTplLineCmd chosen = null;
        String chosenFrom = null;
        for (String from : order) {
            if ("底册".equals(from.trim()) && fromArch != null) {
                chosen = fromArch;
                chosenFrom = "底册";
                break;
            }
            if ("页面".equals(from.trim()) && fromPage != null) {
                chosen = fromPage;
                chosenFrom = "页面";
                break;
            }
        }
        if (chosen == null) {
            chosen = fromArch != null ? fromArch : fromPage;
            chosenFrom = fromArch != null ? "底册" : "页面";
        }
        if (chosen == null) {
            throw new IllegalArgumentException("两处汇来都空着，没一行可落库");
        }
        // 认下哪一路也只走建版那一条方法，不另辟径路；取舍凭据随备注落库，不两路都写
        chosen.setRemark(appendNote(chosen.getRemark(), "两路汇来以「" + chosenFrom + "」一路为准"));
        return saveLine(chosen);
    }

    // ============================================================ 已渲张数更正

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TWsidTplBook correctDoneNum(Long id, Integer doneNum) {
        if (id == null) {
            throw new IllegalArgumentException("没点更正哪一行");
        }
        TWsidTplBook cur = tplBookMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (cur == null) {
            throw new IllegalArgumentException("第 " + id + " 行在册上找不着（或已抽了册）");
        }
        if (cur.getStatus() != null && cur.getStatus().intValue() == WsidTplConsts.STATUS_OFF) {
            throw new IllegalArgumentException("第 " + id + " 行已撤下，已渲张数也点不动");
        }
        int done = doneNum == null ? 0 : doneNum.intValue();
        if (done < 0) {
            throw new IllegalArgumentException("已渲张数（done_num）带负号（" + done + "），不让落");
        }
        int plan = cur.getPlanNum() == null ? 0 : cur.getPlanNum().intValue();
        if (done > plan) {
            throw new IllegalArgumentException("已渲张数（" + done + "）反超本套应发张数（" + plan
                    + "），不让落");
        }
        BigDecimal coverNum = calcCover(plan, done); // 重算第二回：已渲张数被更正
        int nextVer = (cur.getVerNo() == null ? 0 : cur.getVerNo().intValue()) + 1;
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0)
                .eq("status", WsidTplConsts.STATUS_ON);
        uw.set("done_num", done);
        uw.set("cover_num", coverNum);
        uw.set("ver_no", nextVer);
        uw.set("update_time", new Date());
        int affected = tplBookMapper.update(null, uw);
        if (affected != 1) {
            throw new IllegalStateException("已渲张数更正只落了 " + affected + " 行（id=" + id
                    + "），本趟作废，不许留残");
        }
        return selectTWsidTplBookById(id);
    }

    // ============================================================ 撤下（单条／成批同口）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int takeOff(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("没点要撤下哪几行");
        }
        List<Long> distinct = new ArrayList<Long>();
        for (Long id : ids) {
            if (id != null && !distinct.contains(id)) {
                distinct.add(id);
            }
        }
        // 先逐条验明：一处位置堵了另一处照旧漏即算失守——验不过整趟不办
        for (Long id : distinct) {
            Integer cnt = tplBookMapper.selectCount(new QueryWrapper<TWsidTplBook>()
                    .eq("id", id)
                    .eq("del_flag", 0));
            if (cnt == null || cnt.intValue() == 0) {
                throw new IllegalArgumentException("第 " + id + " 行在册上找不着（或已抽了册），"
                        + "成批撤下整趟不办，已点的一行也不动");
            }
        }
        // 单条与成批同走这一句；已撤下的再点一遍照旧回话（幂等，只数新撤下的）
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .in("id", distinct)
                .eq("del_flag", 0)
                .eq("status", WsidTplConsts.STATUS_ON);
        uw.set("status", WsidTplConsts.STATUS_OFF);
        uw.set("update_time", new Date());
        return tplBookMapper.update(null, uw);
    }

    // ============================================================ 按层回落取式（定死出口）

    @Override
    public WsidTplPick pick(Integer specId, String tplName0, String fromLevel0, WsidTplScope scope) {
        if (specId == null) {
            throw new IllegalArgumentException("取式没带挂靠物种代号");
        }
        String tplName = trimToNull(tplName0);
        if (tplName == null) {
            throw new IllegalArgumentException("取式没带版式名目");
        }
        String fromLevel = trimToNull(fromLevel0);
        int startIdx = chainIndex(fromLevel);
        if (startIdx < 0) {
            throw new IllegalArgumentException("起问层只认县定／市定／省定，接到的是「" + fromLevel + "」");
        }
        assertSpeciesInScope(specId, scope);

        WsidTplPick pick = new WsidTplPick();
        pick.setAskedLevel(WsidTplConsts.FALLBACK_CHAIN[startIdx]);

        // 一格一格顺链条上去；写着「顺层回落」的那一条不自己接话，继续往上交
        for (int i = startIdx; i < WsidTplConsts.FALLBACK_CHAIN.length; i++) {
            String level = WsidTplConsts.FALLBACK_CHAIN[i];
            List<TWsidTplBook> candidates = findActive(specId, tplName, level, scope);
            TWsidTplBook winner = candidates.isEmpty() ? null : candidates.get(0);
            pick.getTrail().add(new WsidTplPick.TraceStep(level, candidates.size(),
                    winner == null ? null : winner.getTplNo()));
            if (winner != null) {
                if (winner.getTplStyle() != null
                        && winner.getTplStyle().intValue() == WsidTplConsts.STYLE_FALLBACK) {
                    // 这一条写的是「顺层回落」：链上还有上一层就往上交；
                    // 若省定层也写着回落，上头没有市县级，循环尽了自然落到系统兜底版
                    continue;
                }
                pick.setLine(winner);
                pick.setResolvedLevel(level);
                pick.setFellBack(i > startIdx);
                pick.setSystemDefault(false);
                pick.setEffectiveStyle(winner.getTplStyle() == null
                        ? WsidTplConsts.STYLE_FRONT : winner.getTplStyle());
                pick.setStyleNum(resolveStyleNum(winner.getStyleNum()));
                return pick;
            }
        }

        // 三级全空，才轮到系统兜底那一版
        List<TWsidTplBook> defaults = findActive(specId, tplName, WsidTplConsts.LEVEL_SYSTEM, scope);
        TWsidTplBook def = defaults.isEmpty() ? null : defaults.get(0);
        pick.getTrail().add(new WsidTplPick.TraceStep(WsidTplConsts.LEVEL_SYSTEM,
                defaults.size(), def == null ? null : def.getTplNo()));
        pick.setResolvedLevel(WsidTplConsts.LEVEL_SYSTEM);
        pick.setFellBack(true);
        pick.setSystemDefault(true);
        // 兜底那一版只给正联：它册上即便写着出副本联，也不许顺手取成全出副本联
        pick.setEffectiveStyle(WsidTplConsts.STYLE_FRONT);
        if (def != null) {
            pick.setLine(def);
            pick.setStyleNum(resolveStyleNum(def.getStyleNum()));
        } else {
            // 三级全空、系统兜底也没立：空着回话，不从邻层牵行来凑数
            pick.setStyleNum(tplProperties.getStyleNum());
        }
        return pick;
    }

    // ============================================================ 对外排面

    @Override
    public List<TWsidTplBook> activeLines(Integer specId, String tplName, WsidTplScope scope) {
        QueryWrapper<TWsidTplBook> qw = new QueryWrapper<TWsidTplBook>()
                .eq("del_flag", 0)
                .eq("status", WsidTplConsts.STATUS_ON); // 撤下的不上对外排面
        if (specId != null) {
            qw.eq("spec_id", specId);
        }
        if (StringUtils.isNotEmpty(tplName)) {
            qw.eq("tpl_name", tplName.trim());
        }
        applyScope(qw, scope, true);
        qw.orderByDesc("ver_no").orderByDesc("tpl_no");
        List<TWsidTplBook> rows = tplBookMapper.selectList(qw);
        // 只挑库里实有的行；本层此刻一行未立就空着回话，不从邻层牵行来填
        return rows == null ? new ArrayList<TWsidTplBook>() : rows;
    }

    // ============================================================ 在册册面（含撤下）

    @Override
    public List<TWsidTplBook> book(WsidTplSearchCmd cmd, WsidTplScope scope) {
        QueryWrapper<TWsidTplBook> qw = buildBookWrapper(cmd, scope);
        int pageNum = cmd != null && cmd.getPageNum() != null && cmd.getPageNum() > 0
                ? cmd.getPageNum() : 1;
        int pageSize = WsidTplProperties.normalizePageSize(cmd == null ? null : cmd.getPageSize());
        PageHelper.startPage(pageNum, pageSize); // 一屏几条写在配置里，往下接续看
        List<TWsidTplBook> rows = tplBookMapper.selectList(qw);
        return rows == null ? new ArrayList<TWsidTplBook>() : rows;
    }

    @Override
    public long bookCount(WsidTplSearchCmd cmd, WsidTplScope scope) {
        Integer cnt = tplBookMapper.selectCount(buildBookWrapper(cmd, scope));
        return cnt == null ? 0L : cnt.longValue();
    }

    @Override
    public long countRegistered(WsidTplScope scope) {
        QueryWrapper<TWsidTplBook> qw = new QueryWrapper<TWsidTplBook>()
                .eq("del_flag", 0); // 撤下的仍计入在册套数，不存在顺手抹掉这一说
        applyScope(qw, scope, true);
        Integer cnt = tplBookMapper.selectCount(qw);
        return cnt == null ? 0L : cnt.longValue();
    }

    @Override
    public TWsidTplBook selectTWsidTplBookById(Long id) {
        if (id == null) {
            return null;
        }
        // 撤下的也取得出（仍列得出来）；删了的（del_flag=1）取不着
        return tplBookMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    // ============================================================ 小工具

    /** 在位候选：含撤下过滤、眼界物种闸；同名目按裁决序排好，头一条说话。 */
    private List<TWsidTplBook> findActive(Integer specId, String tplName, String levelKind,
                                          WsidTplScope scope) {
        QueryWrapper<TWsidTplBook> qw = new QueryWrapper<TWsidTplBook>()
                .eq("del_flag", 0)
                .eq("status", WsidTplConsts.STATUS_ON)
                .eq("spec_id", specId)
                .eq("tpl_name", tplName)
                .eq("level_kind", levelKind);
        applyScope(qw, scope, false);
        List<TWsidTplBook> rows = tplBookMapper.selectList(qw);
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<TWsidTplBook>();
        }
        List<TWsidTplBook> sorted = new ArrayList<TWsidTplBook>(rows);
        Collections.sort(sorted, NEWEST_THEN_NO_DESC);
        return sorted;
    }

    /**
     * 眼界收口：本层值守的只看见挂靠本层那些行；省局岗三层都看得见，
     * 眼界仍限划归它的那些物种。取式回落是系统按链条上溯，层闸不收窄（物种闸仍在）。
     */
    private void applyScope(QueryWrapper<TWsidTplBook> qw, WsidTplScope scope, boolean levelGate) {
        if (scope == null) {
            return;
        }
        if (levelGate && !scope.isProvincePost() && scope.getLevelKind() != null) {
            qw.eq("level_kind", scope.getLevelKind());
        }
        if (scope.getSpecIds() != null) {
            if (scope.getSpecIds().isEmpty()) {
                qw.apply("1 = 0"); // 划归它的行一条没有，空着回话是合法结果
            } else {
                qw.in("spec_id", scope.getSpecIds());
            }
        }
    }

    private void assertSpeciesInScope(Integer specId, WsidTplScope scope) {
        if (scope == null || scope.getSpecIds() == null) {
            return;
        }
        if (!scope.getSpecIds().contains(specId)) {
            throw new IllegalArgumentException("物种（spec_id=" + specId
                    + "）不在划归本岗的范围内，不许从邻层牵行来问");
        }
    }

    private QueryWrapper<TWsidTplBook> buildBookWrapper(WsidTplSearchCmd cmd, WsidTplScope scope) {
        QueryWrapper<TWsidTplBook> qw = new QueryWrapper<TWsidTplBook>().eq("del_flag", 0);
        // 默认在位与已撤下都列得出；只在点名时才按态收窄
        if (cmd != null) {
            if (cmd.getStatus() != null
                    && (cmd.getStatus().intValue() == WsidTplConsts.STATUS_ON
                        || cmd.getStatus().intValue() == WsidTplConsts.STATUS_OFF)) {
                qw.eq("status", cmd.getStatus());
            }
            String by = trimToNull(cmd.getBy());
            String kw = trimToNull(cmd.getKeyword());
            boolean exact = Boolean.TRUE.equals(cmd.getExact());
            if (by != null && kw != null) {
                if (WsidTplConsts.BY_SPEC_NO.equals(by)) {
                    // 代号这一路：模糊只认起头那几个字
                    qw.eq(exact, "spec_no", kw).likeRight(!exact, "spec_no", kw);
                } else if (WsidTplConsts.BY_LEVEL_KIND.equals(by)) {
                    // 名目这一路：模糊只认起头那几个字
                    qw.eq(exact, "level_kind", kw).likeRight(!exact, "level_kind", kw);
                } else if (WsidTplConsts.BY_TPL_TEXT.equals(by)) {
                    // 文面那一路：模糊认全文关键词（包含）
                    qw.eq(exact, "tpl_text", kw).like(!exact, "tpl_text", kw);
                } else {
                    throw new IllegalArgumentException("检索口子只认 specNo／levelKind／tplText，"
                            + "接到的是「" + by + "」");
                }
            }
        }
        applyScope(qw, scope, true);
        qw.orderByDesc("ver_no").orderByDesc("tpl_no").orderByDesc("id");
        return qw;
    }

    /** 完成比：已渲÷应发×100，两位小数四舍五入；应发 4 已渲 3 得 75.00，4/4 得 100.00。 */
    private BigDecimal calcCover(int planNum, int doneNum) {
        if (planNum <= 0) {
            // 应发为 0 时没有发套可言，完成比记 0.00；负应发在栏位校验处已挡
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(doneNum)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(planNum), 2, RoundingMode.HALF_UP);
    }

    /** 行上一屏几联先过自己的量程，不合量程再回配置缺省。 */
    private Integer resolveStyleNum(Integer raw) {
        if (raw != null && raw.intValue() >= WsidTplConsts.STYLE_NUM_MIN
                && raw.intValue() <= WsidTplConsts.STYLE_NUM_MAX) {
            return raw;
        }
        return tplProperties.getStyleNum();
    }

    private int chainIndex(String level) {
        if (level == null) {
            return -1;
        }
        for (int i = 0; i < WsidTplConsts.FALLBACK_CHAIN.length; i++) {
            if (WsidTplConsts.FALLBACK_CHAIN[i].equals(level)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isKnownLevel(String level) {
        if (WsidTplConsts.LEVEL_SYSTEM.equals(level)) {
            return true;
        }
        return chainIndex(level) >= 0;
    }

    /**
     * 证面版式代号由系统排上：定长 TPL＋雪花号，单调可排；不留敲键格子，
     * 同名目裁决时代号串字面先后也有定准。
     */
    private synchronized String nextTplNo() {
        return "TPL" + SnowflakeIdWorker.getUUID();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String joinChinese(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String appendNote(String origin, String note) {
        String o = trimToNull(origin);
        return o == null ? note : o + "；" + note;
    }
}
