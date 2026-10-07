package com.fc.v2.model.custom.wsid;

/**
 * 专用标识证面版式模板册一摊的定数。
 *
 * <p>层级、取用三式、行态、检索口子、配置缺省，全册只认这一处写法；
 * 控制层与页面不得另立一套。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
public final class WsidTplConsts {

    private WsidTplConsts() {
    }

    /** 行态：在位（上对外排面） */
    public static final int STATUS_ON = 0;

    /** 行态：已撤下（文面改不了、整行抽不走；仍列得出、仍计入在册套数） */
    public static final int STATUS_OFF = 1;

    /** 取用式 0：只出正联 */
    public static final int STYLE_FRONT = 0;

    /** 取用式 1：只出副本联 */
    public static final int STYLE_COPY = 1;

    /** 取用式 2：顺层回落 */
    public static final int STYLE_FALLBACK = 2;

    /** 一屏并列联数量程下沿（含） */
    public static final int STYLE_NUM_MIN = 1;

    /** 一屏并列联数量程上沿（含） */
    public static final int STYLE_NUM_MAX = 3;

    /** 层级：县定（回落链头一档） */
    public static final String LEVEL_COUNTY = "县定";

    /** 层级：市定 */
    public static final String LEVEL_CITY = "市定";

    /** 层级：省定 */
    public static final String LEVEL_PROVINCE = "省定";

    /** 层级：系统兜底（末档，只出正联） */
    public static final String LEVEL_SYSTEM = "系统兜底";

    /** 回落链：县定 → 市定 → 省定，一格一格顺链条上去 */
    public static final String[] FALLBACK_CHAIN = {LEVEL_COUNTY, LEVEL_CITY, LEVEL_PROVINCE};

    /** 检索口子：物种代号（只认起头那几个字，前缀） */
    public static final String BY_SPEC_NO = "specNo";

    /** 检索口子：层别名目（只认起头那几个字，前缀） */
    public static final String BY_LEVEL_KIND = "levelKind";

    /** 检索口子：槽位文面（认全文关键词） */
    public static final String BY_TPL_TEXT = "tplText";

    /** 省局岗名号里带这一串即认（省局岗三层都看得见，范围仍限划归它的物种） */
    public static final String PROVINCE_ROLE_TOKEN = "省局";

    /** 配置缺省：一屏并列摆几联（量程只认 1..3，填 0/9 与空着同，按此落） */
    public static final int DEFAULT_STYLE_NUM = 1;

    /** 配置缺省：一屏摆几条（往下接续看） */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 配置页数/条数的有量上沿：写成没边的大数与空着等价 */
    public static final int PAGE_SIZE_SANITY = 500;
}
