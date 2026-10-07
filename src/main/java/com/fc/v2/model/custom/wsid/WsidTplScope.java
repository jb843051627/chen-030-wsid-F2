package com.fc.v2.model.custom.wsid;

import java.util.ArrayList;
import java.util.List;

/**
 * 看册的眼界：按层级岗切出来的可见范围。
 *
 * <p>本层值守的只看见挂靠本层那一些行；省局岗三层都看得见，眼界仍限划归它的那些物种。
 * 眼界由控制层按当值岗位装配，服务层只照此收窄——空着回话是合法结果，
 * 不许从邻层牵行来填。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
public class WsidTplScope {

    /** 是不是省局岗（三层都看得见） */
    private boolean provincePost;

    /** 本层值守的层级（省局岗为 null，即县定／市定／省定三档不设层闸） */
    private String levelKind;

    /** 划归本岗的物种底册主键；null 表示物种这一头上不设闸（全划在眼下） */
    private List<Integer> specIds;

    public static WsidTplScope county() {
        return level(WsidTplConsts.LEVEL_COUNTY);
    }

    public static WsidTplScope city() {
        return level(WsidTplConsts.LEVEL_CITY);
    }

    public static WsidTplScope province() {
        return level(WsidTplConsts.LEVEL_PROVINCE);
    }

    public static WsidTplScope provincePost() {
        WsidTplScope scope = new WsidTplScope();
        scope.provincePost = true;
        scope.levelKind = null;
        return scope;
    }

    private static WsidTplScope level(String levelKind) {
        WsidTplScope scope = new WsidTplScope();
        scope.provincePost = false;
        scope.levelKind = levelKind;
        return scope;
    }

    public WsidTplScope restrictSpecies(List<Integer> specIds) {
        this.specIds = specIds == null ? null : new ArrayList<Integer>(specIds);
        return this;
    }

    public boolean isProvincePost() {
        return provincePost;
    }

    public String getLevelKind() {
        return levelKind;
    }

    public List<Integer> getSpecIds() {
        return specIds;
    }
}
