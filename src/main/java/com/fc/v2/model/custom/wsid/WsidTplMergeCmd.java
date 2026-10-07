package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 两处汇来的话头：底册一路、页面一路各摆一份。
 *
 * <p>哪一处先落、哪一处后落、以哪为准写在册上（wsid.tpl.source-order）；
 * 两路同时到时按先后认整路，不许两路都写，也不许取页面先亮的那个数。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidTplMergeCmd", description = "两处汇来落一行的话头")
public class WsidTplMergeCmd {

    @ApiModelProperty(value = "底册那一路（可空）")
    private WsidTplLineCmd arch;

    @ApiModelProperty(value = "页面那一路（可空）")
    private WsidTplLineCmd page;

    public WsidTplLineCmd getArch() {
        return arch;
    }

    public void setArch(WsidTplLineCmd arch) {
        this.arch = arch;
    }

    public WsidTplLineCmd getPage() {
        return page;
    }

    public void setPage(WsidTplLineCmd page) {
        this.page = page;
    }
}
