package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 版式册检索话头：三个检索口子，全等与模糊各一路。
 *
 * <ul>
 *   <li>{@link WsidTplConsts#BY_SPEC_NO} 物种代号：模糊只认起头那几个字（前缀）。</li>
 *   <li>{@link WsidTplConsts#BY_LEVEL_KIND} 层别名目：模糊只认起头那几个字（前缀）。</li>
 *   <li>{@link WsidTplConsts#BY_TPL_TEXT} 槽位文面：模糊认全文关键词（包含）。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidTplSearchCmd", description = "版式册三口子检索话头")
public class WsidTplSearchCmd {

    @ApiModelProperty(value = "检索口子：specNo／levelKind／tplText", required = true)
    private String by;

    @ApiModelProperty(value = "检索词（空着不拦，视同这一口子没开）")
    private String keyword;

    @ApiModelProperty(value = "true 走全等一路；false（缺省）走模糊一路（前缀／全文关键词）")
    private Boolean exact;

    @ApiModelProperty(value = "行态过滤：0在位 1已撤下；空着则在册的两路都列得出")
    private Integer status;

    @ApiModelProperty(value = "第几屏（从 1 起），空着按第一屏")
    private Integer pageNum;

    @ApiModelProperty(value = "一屏几条，空着或越过量程按册上配置落")
    private Integer pageSize;

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getExact() {
        return exact;
    }

    public void setExact(Boolean exact) {
        this.exact = exact;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
