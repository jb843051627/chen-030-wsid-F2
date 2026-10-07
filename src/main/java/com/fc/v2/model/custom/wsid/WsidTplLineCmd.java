package com.fc.v2.model.custom.wsid;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 版式册一行的建行／改一处话头。
 *
 * <p>不递、递了也不认的格子：证面版式代号（系统排上）、物种代号与物种称谓（以物种底册为准回写）、
 * 完成比（应发／已渲相除轧出）、改动次序、行态（撤下另走撤下那一条）——这五处页面上没有敲键的地方。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidTplLineCmd", description = "版式册建行／改一处话头")
public class WsidTplLineCmd {

    @ApiModelProperty(value = "主键：带着是改自己那一条；空着是建行（代号仍由系统排）")
    private Long id;

    @ApiModelProperty(value = "挂靠物种底册主键（对不进名录底册建不成，已调出的段收不进新行）", required = true)
    private Integer specId;

    @ApiModelProperty(value = "层别名目：县定／市定／省定／系统兜底", required = true)
    private String levelKind;

    @ApiModelProperty(value = "版式名目（与物种、层别三样一同撞键）", required = true)
    private String tplName;

    @ApiModelProperty(value = "槽位文面（空着进不了库）", required = true)
    private String tplText;

    @ApiModelProperty(value = "取用三式 0只出正联 1只出副本联 2顺层回落", required = true)
    private Integer tplStyle;

    @ApiModelProperty(value = "一屏并列几联，只认 1..3；0／9 与空着同，按册上配置落")
    private Integer styleNum;

    @ApiModelProperty(value = "本套应发张数：空着、带负号、小于已渲都进不了库", required = true)
    private Integer planNum;

    @ApiModelProperty(value = "已渲张数：反超应发不让落；空着按 0 认")
    private Integer doneNum;

    @ApiModelProperty(value = "继承自哪一版式（顺层回落链的备注挂点）")
    private String parentNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "启用那一一日")
    private Date effStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "交班那一一日(不含)")
    private Date effEnd;

    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }

    public String getLevelKind() {
        return levelKind;
    }

    public void setLevelKind(String levelKind) {
        this.levelKind = levelKind;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getTplText() {
        return tplText;
    }

    public void setTplText(String tplText) {
        this.tplText = tplText;
    }

    public Integer getTplStyle() {
        return tplStyle;
    }

    public void setTplStyle(Integer tplStyle) {
        this.tplStyle = tplStyle;
    }

    public Integer getStyleNum() {
        return styleNum;
    }

    public void setStyleNum(Integer styleNum) {
        this.styleNum = styleNum;
    }

    public Integer getPlanNum() {
        return planNum;
    }

    public void setPlanNum(Integer planNum) {
        this.planNum = planNum;
    }

    public Integer getDoneNum() {
        return doneNum;
    }

    public void setDoneNum(Integer doneNum) {
        this.doneNum = doneNum;
    }

    public String getParentNo() {
        return parentNo;
    }

    public void setParentNo(String parentNo) {
        this.parentNo = parentNo;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getEffEnd() {
        return effEnd;
    }

    public void setEffEnd(Date effEnd) {
        this.effEnd = effEnd;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
