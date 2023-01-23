package com.xiaowc.partnermatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowc.partnermatch.mapper.UserTeamMapper;
import com.xiaowc.partnermatch.model.domain.UserTeam;
import com.xiaowc.partnermatch.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service实现
* @createDate 2023-01-09 16:55:42
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




