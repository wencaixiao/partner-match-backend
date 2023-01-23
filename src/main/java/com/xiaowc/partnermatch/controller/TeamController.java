package com.xiaowc.partnermatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaowc.partnermatch.common.BaseResponse;
import com.xiaowc.partnermatch.common.DeleteRequest;
import com.xiaowc.partnermatch.common.ErrorCode;
import com.xiaowc.partnermatch.common.ResultUtils;
import com.xiaowc.partnermatch.exception.BusinessException;
import com.xiaowc.partnermatch.model.domain.Team;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.model.domain.UserTeam;
import com.xiaowc.partnermatch.model.dto.TeamQuery;
import com.xiaowc.partnermatch.model.request.TeamAddRequest;
import com.xiaowc.partnermatch.model.request.TeamJoinRequest;
import com.xiaowc.partnermatch.model.request.TeamQuitRequest;
import com.xiaowc.partnermatch.model.request.TeamUpdateRequest;
import com.xiaowc.partnermatch.model.vo.TeamUserVO;
import com.xiaowc.partnermatch.service.TeamService;
import com.xiaowc.partnermatch.service.UserService;
import com.xiaowc.partnermatch.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组队、队伍接口
 */
@RestController // 适用于编写restful风格的api，返回值默认为json类型
@RequestMapping("/team")
// 这个注解的作用是允许跨域，默认允许的域名是*，所有域名都允许跨域，可以通过origins来选择允许跨域的地址
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000" })
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     * @param teamAddRequest 前端传来的队伍信息，是一些有必要传的信息，过滤了一部分
     * @return 返回队伍id
     */
//    @RequestBody主要用来接收前端传递给后端的json字符串中的数据的(请求体中的数据的)；而最常用的使用请求体传参
//    的无疑是POST请求了，所以使用@RequestBody接收数据时，一般都用POST方式进行提交
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) { // 传入的队伍为空直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 获取当前用户的信息
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team); // 将源对象中的值赋值给目的值对象
        long teamId = teamService.addTeam(team, loginUser); // 往数据库中插入数据
        return ResultUtils.success(teamId);
    }

    /**
     * 更新队伍信息
     * @param teamUpdateRequest 前端传来的要更新的队伍的信息
     * @param request
     * @return 返回更新是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) { // 传入的队伍id小于0直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 得到当前登录用户
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser); // 更新队伍的信息
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 查询单个队伍信息
     * @param id 前端传来的队伍的id
     * @return 返回查询的队伍
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) { // 传入的队伍id小于0直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id); // 根据id查询队伍的信息
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

//    /**
//     * 查询符合条件的队伍的列表
//     * @param teamQuery 前端传来的队伍的id
//     * @return 返回查询的队伍列表
//     */
//    @GetMapping("/list")
//    public BaseResponse<List<Team>> listTeams(TeamQuery teamQuery) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Team team = new Team();
//        BeanUtils.copyProperties(team, teamQuery); // 将teamQuery的属性对象赋给team
//        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
//        List<Team> teamList = teamService.list(queryWrapper); // 查询符合条件的所有队伍
//        return ResultUtils.success(teamList);
//    }

    /**
     * 查询符合条件的队伍的列表
     *
     * TeamUserVO:返回给前端的数据类型
     * @param teamQuery 前端传来的队伍的id
     * @return 返回查询的队伍列表
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request); // 查看当前用户是否为管理员
        // 1.查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin); // 查询到符合条件的队伍的列表
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList()); // 得到查询出来的队伍列表的id
        /**
         * 仅加入队伍和创建队伍的人能看到队伍操作按钮(listTeam接口要能获取我加入的队伍装填)
         *  方案一：前端查询我加入了哪些队伍列表，然后判断每个队伍id是否在列表中(前端要多发一次请求)
         *  方案二：在后端去做上述事情(推荐)
         */
        // 2.判断当前用户是否已加入该队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request); // 获取当前用户
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            // 只需判断当前用户是否在上面查询出来的队伍中即可，因为要展示，未查询到的队伍自然展示不出来
            userTeamQueryWrapper.in("teamId", teamIdList);
            // 查询当前用户加入的队伍或者创建的队伍，便于后续前端标签的展示
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍id集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {  // 遍历teamList(就是查询到的所有符合条件的队伍)，将含有当前用户id的hasJoin字段设为true，其他设为false
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {

        }
        // 3.查询当前加入该队伍的人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList); // 查询每个队伍的人数
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍id -> 加入队伍的用户列表，对查出来的结果进行分组，得到每个队伍对应的人数
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size())); // 设置每个队伍的人数，对hasJoinNum字段进行设置
        return ResultUtils.success(teamList);
    }

    /**
     * 查询符合条件的队伍的列表，分页查询显示出来
     * @param teamQuery 前端传来的队伍的teamQuery
     * @return 返回查询的队伍列表，分页显示
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team); // 将teamQuery的属性对象赋给team，从源对象中赋值给目的对象中
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()); // 设置current和size分页信息
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(); // 设置查询条件
        Page<Team> resultList = teamService.page(page, queryWrapper); // 查询符合条件的所有队伍，分页信息上面设置了
        return ResultUtils.success(resultList);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest 前端传来的用户加入队伍请求体
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录用户
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest 前端传来的用户退出队伍请求体
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录用户
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 队长删除队伍
     * @param deleteRequest 前端传来的队伍的请求体
     * @return 返回删除是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) { // 传入的队伍id小于0直接抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser); // 通过id来删除队伍
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取我创建的队伍
     * 复用listTeam方法，只新增查询条件，不做修改(开闭原则)
     *
     * TeamUserVO:返回给前端的数据类型
     * @param teamQuery 前端传来的队伍的id
     * @return 返回查询的我已创建的队伍列表
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 获取登录用户
        teamQuery.setUserId(loginUser.getId()); // 队伍表中的userId指的是队长的id
        // 上面利用队长的id可以查询到自己创建的队伍列表，从队伍表中去查询
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     * 复用listTeam方法，只新增查询条件，不做修改(开闭原则)
     *
     * TeamUserVO:返回给前端的数据类型
     * @param teamQuery 前端传来的队伍的id
     * @return 返回查询的队伍列表
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request); // 获取当前登录用户
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId()); // 用户-队伍表中的userId指的是加入队伍的用户的id
        // 从用户-队伍表中去查询，用户-队伍表中的userId指的是加入队伍的用户的id
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 -> 2, 3
        // 2 -> 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }
}
