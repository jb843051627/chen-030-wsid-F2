package com.fc.v2.model.custom.wsid;

import com.fc.v2.model.auto.TWsidTplBook;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 取式回话：按层回落的结果与一格一格的上溯轨迹。
 *
 * <p>年底复核逐条回看时，缺槽位回落到哪一版、各层见着几条、同名目为何取这一条，
 * 都照此回话指得出来；按层代号问与通表翻查走的是服务层同一方法，回话不会两样。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidTplPick", description = "按层回落取式回话")
public class WsidTplPick {

    @ApiModelProperty(value = "落定说话的那一行（三级全空时为系统兜底那版；一版都没有为 null，空着回话）")
    private TWsidTplBook line;

    @ApiModelProperty(value = "起问层级（县定／市定／省定）")
    private String askedLevel;

    @ApiModelProperty(value = "实际取到的层级；系统兜底时为「系统兜底」")
    private String resolvedLevel;

    @ApiModelProperty(value = "是不是顺层回落取得的（起问层缺、往上级取到）")
    private boolean fellBack;

    @ApiModelProperty(value = "是不是三级全空后落到的系统兜底版（兜底版只出正联）")
    private boolean systemDefault;

    @ApiModelProperty(value = "一屏并列摆几联（取自配置与行上量程校过的那个数）")
    private Integer styleNum;

    @ApiModelProperty(value = "实际摆出的取用式 0只出正联 1只出副本联（系统兜底强制 0，不许取成全出副本联）")
    private Integer effectiveStyle;

    @ApiModelProperty(value = "一格一格顺链条上去的轨迹，供年底复核指认")
    private List<TraceStep> trail = new ArrayList<TraceStep>();

    /** 轨迹一格：某一层见着几条在位的同名目行、取了哪一条代号。 */
    public static class TraceStep {
        private String levelKind;
        private int candidateCount;
        private String pickedTplNo;

        public TraceStep() {
        }

        public TraceStep(String levelKind, int candidateCount, String pickedTplNo) {
            this.levelKind = levelKind;
            this.candidateCount = candidateCount;
            this.pickedTplNo = pickedTplNo;
        }

        public String getLevelKind() {
            return levelKind;
        }

        public int getCandidateCount() {
            return candidateCount;
        }

        public String getPickedTplNo() {
            return pickedTplNo;
        }
    }

    public TWsidTplBook getLine() {
        return line;
    }

    public void setLine(TWsidTplBook line) {
        this.line = line;
    }

    public String getAskedLevel() {
        return askedLevel;
    }

    public void setAskedLevel(String askedLevel) {
        this.askedLevel = askedLevel;
    }

    public String getResolvedLevel() {
        return resolvedLevel;
    }

    public void setResolvedLevel(String resolvedLevel) {
        this.resolvedLevel = resolvedLevel;
    }

    public boolean isFellBack() {
        return fellBack;
    }

    public void setFellBack(boolean fellBack) {
        this.fellBack = fellBack;
    }

    public boolean isSystemDefault() {
        return systemDefault;
    }

    public void setSystemDefault(boolean systemDefault) {
        this.systemDefault = systemDefault;
    }

    public Integer getStyleNum() {
        return styleNum;
    }

    public void setStyleNum(Integer styleNum) {
        this.styleNum = styleNum;
    }

    public Integer getEffectiveStyle() {
        return effectiveStyle;
    }

    public void setEffectiveStyle(Integer effectiveStyle) {
        this.effectiveStyle = effectiveStyle;
    }

    public List<TraceStep> getTrail() {
        return trail;
    }
}
