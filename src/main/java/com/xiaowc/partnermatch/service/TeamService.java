package com.xiaowc.partnermatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaowc.partnermatch.model.domain.Team;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.model.dto.TeamQuery;
import com.xiaowc.partnermatch.model.request.TeamJoinRequest;
import com.xiaowc.partnermatch.model.request.TeamQuitRequest;
import com.xiaowc.partnermatch.model.request.TeamUpdateRequest;
import com.xiaowc.partnermatch.model.vo.TeamUserVO;

import java.util.List;

/**
* @author wenca
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2023-01-09 16:54:07
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     * @param team 创建的队伍相关信息
     * @param loginUser 当前用户
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery 要查询的队伍的信息
     * @param isAdmin 当前登录用户是否为管理员，只有管理员才可以查看加密的状态
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍的信息
     * @param teamUpdateRequest 要更新的队伍的信息
     * @param loginUser 当前登录用户
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest 前端传过来的要加入的队伍信息
     * @param loginUser 当前登录的用户
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     * @param teamQuitRequest 前端传过来的要退出的队伍信息
     * @param loginUser 当前登录的用户
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 队长删除队伍
     * @param id 前端传来的队伍的id
     * @return 返回删除是否成功
     */
    boolean deleteTeam(long id, User loginUser);
}
