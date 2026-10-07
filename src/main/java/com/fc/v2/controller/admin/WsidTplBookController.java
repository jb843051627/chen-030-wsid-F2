package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TSysRole;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.model.custom.wsid.WsidTplConsts;
import com.fc.v2.model.custom.wsid.WsidTplLineCmd;
import com.fc.v2.model.custom.wsid.WsidTplMergeCmd;
import com.fc.v2.model.custom.wsid.WsidTplScope;
import com.fc.v2.model.custom.wsid.WsidTplSearchCmd;
import com.fc.v2.service.ITWsidTplBookService;
import com.fc.v2.service.ITSysRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * 专用标识证面版式模板册 Controller。
 *
 * <p>薄控制层：一行落不落得进库、空栏上溯哪一版、完成比轧几位、三个检索口子认哪路写法、
 * 撤下的行还动得了动不了，五样只认服务层那一条建版取式的方法，这里不另辟径路绕开；
 * 册上条数与算出的比也只按服务层写回的结果作准。本层只多做一件事：按当值层级岗装配眼界。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Api(value = "专用标识证面版式模板册")
@Controller
@RequestMapping("/WsidTplBookController")
public class WsidTplBookController extends BaseController {

    private final String prefix = "admin/wsidTplBook";

    @Autowired
    private ITWsidTplBookService wsidTplBookService;

    @Autowired
    private ITSysRoleService sysRoleService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("wsid:wsidTplBook:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    // ---------------------------------------------------------------- 建行／改一处

    @Log(title = "版式册建行／改一处", action = "save")
    @ApiOperation(value = "建行／改一处：代号系统排上；三同撞键且不是自己挡回；栏位不合逐栏点名", notes = "建行／改一处")
    @PostMapping("/save")
    @RequiresPermissions(value = {"wsid:wsidTplBook:add", "wsid:wsidTplBook:edit"},
            logical = org.apache.shiro.authz.annotation.Logical.OR)
    @ResponseBody
    public AjaxResult save(@RequestBody WsidTplLineCmd cmd) {
        return AjaxResult.success(200, "这一行已落册", wsidTplBookService.saveLine(cmd));
    }

    @Log(title = "版式册两处汇来落一行", action = "merge")
    @ApiOperation(value = "两处汇来：按册上配置的先后整路为准，不两路都写", notes = "两处汇来")
    @PostMapping("/merge")
    @RequiresPermissions("wsid:wsidTplBook:add")
    @ResponseBody
    public AjaxResult merge(@RequestBody WsidTplMergeCmd cmd) {
        return AjaxResult.success(200, "已按取舍次序认下一路落册",
                wsidTplBookService.mergeLine(cmd == null ? null : cmd.getArch(),
                        cmd == null ? null : cmd.getPage()));
    }

    @Log(title = "版式册已渲张数更正", action = "correctDone")
    @ApiOperation(value = "已渲张数更正：完成比落库前重算；撤下的行点不动", notes = "已渲张数更正")
    @PostMapping("/correctDone/{id}")
    @RequiresPermissions("wsid:wsidTplBook:edit")
    @ResponseBody
    public AjaxResult correctDone(@PathVariable("id") Long id,
                                  @RequestParam("doneNum") Integer doneNum) {
        return AjaxResult.success(200, "已渲张数已更正、完成比已重算",
                wsidTplBookService.correctDoneNum(id, doneNum));
    }

    // ---------------------------------------------------------------- 撤下（单条／成批同一个说法）

    @Log(title = "版式册撤下", action = "takeOff")
    @ApiOperation(value = "撤下（单条／成批同口）：撤下后文面改不了、整行抽不走，仍在册计数", notes = "撤下")
    @PostMapping("/off")
    @RequiresPermissions("wsid:wsidTplBook:remove")
    @ResponseBody
    public AjaxResult off(@RequestBody List<Long> ids) {
        int n = wsidTplBookService.takeOff(ids);
        return AjaxResult.success(200, "新撤下 " + n + " 行（先前已撤下的照旧一张脸，不重复算）", n);
    }

    // ---------------------------------------------------------------- 取式（定死出口）

    @ApiOperation(value = "按层回落取式：县定缺退市定、市定缺退省定，三级全空才取兜底且只出正联",
            notes = "按层回落取式")
    @GetMapping("/pick")
    @RequiresPermissions("wsid:wsidTplBook:view")
    @ResponseBody
    public AjaxResult pick(@RequestParam("specId") Integer specId,
                           @RequestParam("tplName") String tplName,
                           @RequestParam("fromLevel") String fromLevel) {
        return AjaxResult.success(200, "取式在此（回落轨迹逐格列着）",
                wsidTplBookService.pick(specId, tplName, fromLevel, buildScope()));
    }

    @ApiOperation(value = "对外排面：只挑库里实有且在位的行，本层一行未立就空着回话", notes = "对外排面")
    @GetMapping("/active")
    @RequiresPermissions("wsid:wsidTplBook:view")
    @ResponseBody
    public AjaxResult active(@RequestParam(value = "specId", required = false) Integer specId,
                             @RequestParam(value = "tplName", required = false) String tplName) {
        List<TWsidTplBook> rows = wsidTplBookService.activeLines(specId, tplName, buildScope());
        return AjaxResult.success(200, rows.isEmpty() ? "本层此刻一行未立，空着回话" : "对外排面在此", rows);
    }

    // ---------------------------------------------------------------- 在册册面／检索

    @ApiOperation(value = "在册册面分页：在位与已撤下都列得出；三个检索口子全等模糊各一路", notes = "在册册面")
    @GetMapping("/book")
    @RequiresPermissions("wsid:wsidTplBook:list")
    @ResponseBody
    public ResultTable book(WsidTplSearchCmd cmd) {
        WsidTplScope scope = buildScope();
        long total = wsidTplBookService.bookCount(cmd, scope); // 条数只按服务层这一口径
        List<TWsidTplBook> rows = wsidTplBookService.book(cmd, scope);
        return pageTable(rows, total);
    }

    @ApiOperation(value = "在册套数：撤下的仍计入，不兴抹掉", notes = "在册套数")
    @GetMapping("/countRegistered")
    @RequiresPermissions("wsid:wsidTplBook:list")
    @ResponseBody
    public AjaxResult countRegistered() {
        return AjaxResult.success(200, "在册套数在此", wsidTplBookService.countRegistered(buildScope()));
    }

    @ApiOperation(value = "按主键取一条：撤下的也取得出，删了的取不着", notes = "取一条")
    @GetMapping("/get/{id}")
    @RequiresPermissions("wsid:wsidTplBook:view")
    @ResponseBody
    public AjaxResult get(@PathVariable("id") Long id) {
        TWsidTplBook row = wsidTplBookService.selectTWsidTplBookById(id);
        return row == null ? AjaxResult.error("这一条在册上找不着（或已抽了册）")
                : AjaxResult.success(200, "这一条在此", row);
    }

    // ---------------------------------------------------------------- 眼界装配

    /**
     * 按当值岗位的名号切眼界：
     * 角色名带「省局」即省局岗，三层都看得见；带「市局」守市定一层；带「县局」守县定一层；
     * 名号都不带的按县定本层值守落（本层只看挂靠本层那些行）。
     * 物种划归范围这一版底册上没有分岗表，暂不收窄；分岗表立起来后在此挂 specIds，
     * 服务层那一条路子不动。
     */
    private WsidTplScope buildScope() {
        TSysUser user = com.fc.v2.shiro.util.ShiroUtils.getUser();
        List<String> roleNames = new ArrayList<String>();
        if (user != null && user.getId() != null) {
            List<TSysRole> roles = sysRoleService.queryUserRole(user.getId());
            if (roles != null) {
                for (TSysRole role : roles) {
                    if (role != null && role.getName() != null) {
                        roleNames.add(role.getName());
                    }
                }
            }
        }
        for (String name : roleNames) {
            if (name.contains(WsidTplConsts.PROVINCE_ROLE_TOKEN)) {
                return WsidTplScope.provincePost();
            }
        }
        for (String name : roleNames) {
            if (name.contains("市局")) {
                return WsidTplScope.city();
            }
        }
        for (String name : roleNames) {
            if (name.contains("县局")) {
                return WsidTplScope.county();
            }
        }
        return WsidTplScope.county();
    }
}
