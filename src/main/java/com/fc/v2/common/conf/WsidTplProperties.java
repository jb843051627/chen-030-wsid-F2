package com.fc.v2.common.conf;

import com.fc.v2.model.custom.wsid.WsidTplConsts;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 版式模板册一摊写在配置里的两处定数与两路汇来的取舍次序。
 *
 * <ul>
 *   <li>一屏并列摆几联：只认 1..3，写成 0/9 与空着同，落 {@link WsidTplConsts#DEFAULT_STYLE_NUM}。</li>
 *   <li>一屏摆几条：往下接续看；空着或写成没边的大数按 {@link WsidTplConsts#DEFAULT_PAGE_SIZE}。</li>
 *   <li>两处汇来谁先谁后：sourceOrder 按先后认，不许两路都写、也不许取页面先亮的那个数。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Component
@ConfigurationProperties(prefix = "wsid.tpl")
public class WsidTplProperties {

    /** 一屏并列摆几联 1..3 */
    private Integer styleNum;

    /** 一屏摆几条（往下接续看） */
    private Integer pageSize;

    /**
     * 两处汇来的取舍次序：排在前头的那一路为准，两路同时到时按先后认。
     * 例：[底册, 页面]——底册先落即以底册为准。
     */
    private java.util.List<String> sourceOrder;

    public Integer getStyleNum() {
        return normalizeStyleNum(styleNum);
    }

    public void setStyleNum(Integer styleNum) {
        this.styleNum = styleNum;
    }

    public Integer getPageSize() {
        return normalizePageSize(pageSize);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public java.util.List<String> getSourceOrder() {
        return sourceOrder;
    }

    public void setSourceOrder(java.util.List<String> sourceOrder) {
        this.sourceOrder = sourceOrder;
    }

    /** 量程只认 1..3，填 0 或填 9 都视同没填。 */
    public static Integer normalizeStyleNum(Integer raw) {
        if (raw == null || raw.intValue() < WsidTplConsts.STYLE_NUM_MIN
                || raw.intValue() > WsidTplConsts.STYLE_NUM_MAX) {
            return WsidTplConsts.DEFAULT_STYLE_NUM;
        }
        return raw.intValue();
    }

    /** 空着、零、负数、没边的大数，统一按缺省落。 */
    public static int normalizePageSize(Integer raw) {
        if (raw == null || raw.intValue() <= 0 || raw.intValue() > WsidTplConsts.PAGE_SIZE_SANITY) {
            return WsidTplConsts.DEFAULT_PAGE_SIZE;
        }
        return raw.intValue();
    }
}
