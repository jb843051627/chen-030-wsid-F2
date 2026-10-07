package com.fc.v2.service;

import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.model.custom.wsid.WsidTplLineCmd;
import com.fc.v2.model.custom.wsid.WsidTplPick;
import com.fc.v2.model.custom.wsid.WsidTplScope;
import com.fc.v2.model.custom.wsid.WsidTplSearchCmd;

import java.util.List;

/**
 * 专用标识证面版式模板册 Service：建版取式只此一条路子。
 *
 * <p>一行能不能落进库、空栏上溯到哪一版取数、完成比轧几位、三个检索口子各认哪一路写法、
 * 已撤下的那条还动得了动不了——这五样只认这里的方法；控制层与页面不许另辟径路绕开。
 * 本接口入参与返回定死，换一种取数思路另立接口，本体形参不动。</p>
 *
 * <ul>
 *   <li>{@link #saveLine}：新建一行（代号系统排上）与「递来的正是那一条自己」的改一处同口，
 *       三同撞键／栏位点名／底册对账／完成比重算都在落库前办完。</li>
 *   <li>{@link #correctDoneNum}：已渲张数被更正，完成比跟着重算。</li>
 *   <li>{@link #takeOff}：单条与成批撤下走同一个说法；撤下后只剩一张脸。</li>
 *   <li>{@link #pick}：顺层回落取式的唯一定死出口，三级全空才轮到系统兜底，兜底只出正联。</li>
 *   <li>{@link #activeLines}：对外排面只挑库里实有且在位的行，本层一行未立就空着回话。</li>
 *   <li>{@link #book}/{@link #bookCount}：在册册面，撤下的行列得出、计入在册套数，不兴抹掉。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-07
 */
public interface ITWsidTplBookService {

    /**
     * 建行／改一处。id 空着是新建（tpl_no 由系统排上，不留敲键格子）；
     * id 带着且正是三同撞键的那一条自己，按改一处处理。
     * 栏位不合逐栏点名抛出；物种代号对不进底册、已调出段收新行，一律挡回。
     */
    TWsidTplBook saveLine(WsidTplLineCmd cmd);

    /**
     * 两处汇来落一行：谁先谁后按册上配置的取舍次序认，整路为准，不许两路都写、
     * 也不许取页面先亮的那个数；认下后仍走 {@link #saveLine} 同一条建版路。
     *
     * @param fromArch 底册那一路（可空）
     * @param fromPage 页面那一路（可空）
     */
    TWsidTplBook mergeLine(WsidTplLineCmd fromArch, WsidTplLineCmd fromPage);

    /** 已渲张数被更正：重算完成比；撤下的行点不动，已渲反超应发不让落。 */
    TWsidTplBook correctDoneNum(Long id, Integer doneNum);

    /**
     * 撤下：单条与成批同一个说法。撤下后文面改不了、整行抽不走；
     * 已撤下的再点一遍照旧回话（幂等）。返回实际新撤下的条数。
     */
    int takeOff(List<Long> ids);

    /**
     * 按层回落取式（唯一定死出口）：起问层缺就一格一格顺链条上去，
     * 写着「顺层回落」的行也往上交；三级全空才取系统兜底版且强制只出正联。
     * 同名目并立取最后落笔（ver_no 大）；落笔分不出先后比代号串字面，取靠后那条。
     */
    WsidTplPick pick(Integer specId, String tplName, String fromLevel, WsidTplScope scope);

    /** 对外排面：只列库里实有且在位的行（不回落、不牵邻层）；空着是合法结果。 */
    List<TWsidTplBook> activeLines(Integer specId, String tplName, WsidTplScope scope);

    /** 在册册面分页：在位与已撤下都列得出，按眼界与三个检索口子收窄。 */
    List<TWsidTplBook> book(WsidTplSearchCmd cmd, WsidTplScope scope);

    /** 册面条数（与 {@link #book} 同一套收窄口径）。 */
    long bookCount(WsidTplSearchCmd cmd, WsidTplScope scope);

    /** 在册套数：撤下的仍计入，不存在顺手抹掉这一说。 */
    long countRegistered(WsidTplScope scope);

    /** 按主键取一条：撤下的也取得出，删了的取不着。 */
    TWsidTplBook selectTWsidTplBookById(Long id);
}
