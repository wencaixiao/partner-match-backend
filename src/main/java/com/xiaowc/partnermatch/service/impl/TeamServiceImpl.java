package com.xiaowc.partnermatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaowc.partnermatch.common.ErrorCode;
import com.xiaowc.partnermatch.exception.BusinessException;
import com.xiaowc.partnermatch.mapper.TeamMapper;
import com.xiaowc.partnermatch.model.domain.Team;
import com.xiaowc.partnermatch.model.domain.User;
import com.xiaowc.partnermatch.model.domain.UserTeam;
import com.xiaowc.partnermatch.model.dto.TeamQuery;
import com.xiaowc.partnermatch.model.enums.TeamStatusEnum;
import com.xiaowc.partnermatch.model.request.TeamJoinRequest;
import com.xiaowc.partnermatch.model.request.TeamQuitRequest;
import com.xiaowc.partnermatch.model.request.TeamUpdateRequest;
import com.xiaowc.partnermatch.model.vo.TeamUserVO;
import com.xiaowc.partnermatch.model.vo.UserVO;
import com.xiaowc.partnermatch.service.TeamService;
import com.xiaowc.partnermatch.service.UserService;
import com.xiaowc.partnermatch.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @description 针对表【team(队伍表)】的数据库操作Service实现
* @createDate 2023-01-09 16:54:07
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加队伍
     * @param team 创建的队伍相关信息
     * @param loginUser 当前用户
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)  // 开启全局事务，要么都执行成功，要么都执行失败
    public long addTeam(Team team, User loginUser) {
        // 1.请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        // 3.校验信息
        //  (1)队伍人数 > 1且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0); // 为空就默认为0
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //  (2)队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        //  (3)描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不满足要求");
        }
        //  (4)status是否公开(int) 不传默认为0(公开)
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status); // 获取枚举值
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //  (5)如果status是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不满足要求");
            }
        }
        //  (6)当前时间在过期时间之后，不满足要求
        Date expireTime = team.getExpireTime(); // 过期时间
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前时间在过期时间之后，不满足要求");
        }
        //  (7)校验用户最多创建5个队伍
        // TODO: 2023/1/9  有bug，可能同时创建100个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId); // 设置查询条件
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个用户");
        }

        // 4和5要执行事务，要么同时成功，要么同时失败。因此在整个方法上开启了全局事务
        // 4.插入  队伍信息  到  队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team); // 将数据插入到数据库中
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 5.插入  用户 -> 队伍关系  到  关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam); // 将数据插入到数据库中
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return team.getId(); // 创建队伍成功
    }

    /**
     * 搜索队伍
     * TeamUserVO:返回给前端的数据类型
     * @param teamQuery 要查询的队伍的信息
     * @param isAdmin 当前登录用户是否为管理员，只有管理员才可以查看加密的状态
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 1.组合查询条件
        if (teamQuery != null) {
            // (1)根据id来进行查询，精确查询
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList(); // 得到id列表
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            // (2)搜索关键词(同时对队伍名称和描述搜索)，有一个满足条件就可以查询出来，模糊查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            // (3)根据队伍名字来进行查询，模糊查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name); // 模糊查询
            }
            // (4)根据队伍描述来进行查询，模糊查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description); // 模糊查询
            }
            // (5)根据最大人数是否相等来进行查询，精确查询
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            // (6)根据创建人来进行查询，精确查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // (7)根据队伍状态来进行查询，精确查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            // 如果枚举值不存在，将状态设置为公开的
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            // 如果不是管理员，并且状态是私密的，则抛出异常没有权限
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        // (8)不展示已过期的队伍，有一个满足条件就可以查出来
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        // 2.开始查询
        List<Team> teamList = this.list(queryWrapper); // 从数据库中去查询
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        // 3.关联查询创建人的用户信息
        // (1)自己写SQL
        //   查询队伍和创建人的信息
        //   select * from team t
        //                 left join user u on t.userId = u.id;
        //   查询队伍和已加入队伍成员的信息
        //   select * from team t
        //                 left join user_team ut on t.id = ut.teamId
        //                 left join user u on ut.userId = u.id;
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId); // 通过id查询用户信息
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO); // 将team的信息传给teamUserVO中
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO); // 将user的信息传给userVO
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * 更新队伍的信息
     * @param teamUpdateRequest 要更新的队伍的信息
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id); // 查询原来的队伍信息
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        // 只有管理员或创建者可以修改
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 如果队伍状态要改为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam); // 将teamUpdateRequest对象中的值赋值到updateTeam中，返回给前端
        return this.updateById(updateTeam); // 自动识别到updateTeam对象的id，来根据id进行更新
    }

    /**
     * 用户加入队伍
     *
     * 防止用户疯狂点击加入队伍，多个线程一起进来，造成用户多次加入队伍，这里加一个分布式锁，一个线程进来了，另一个线程只能等待
     *
     * @param teamJoinRequest 前端传来的用户加入队伍请求体
     * @param loginUser 当前登录的用户
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1.队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId); // 根据队伍id获取队伍信息
        // 2.只能加入未过期的队伍
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 3.禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) { // 判断队伍的状态
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        // 4.如果加入的队伍是加密的，必须密码匹配才可以加入
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) { // 队伍是加密的
            if (StringUtils.isNotBlank(password) || !password.equals(team.getPassword())) { // 必须密码匹配
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 5.用户最多加入5个队伍(注意：并发请求时可能会出现问题，请求同时进来，可能会超过5个)
        Long userId = loginUser.getId();
        // 分布式锁：只有一个线程可以获取到锁
        RLock lock = redissonClient.getLock("xiaowc:join_team"); // 创建一个锁
        try {
            // 尝试获取锁，获取成功会返回true，将第二个参数改为-1，可以实现续锁
            //   1.waitTime设置为0，只抢一次，抢不到就放弃
            //   2.注意释放锁要写在finally中
            //   3.看门狗机制：redisson中提供的续期机制。开一个监听线程，如果方法还没执行完，就帮你重置redis锁的过期时间
            //      将leastTime设置为-1就会开启看门狗续期机制
            //     原理：1.监听当前线程，默认看门狗机制过期时间是30秒，每10秒续期一次(补到30秒)，防止宕机
            //           2.如果线程挂掉(注意debug模式也会被他当成服务器宕机)，则不会续期

            // 抢到锁后并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) { // 所有线程都去抢这个锁
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper); // 查询当前用户加入了多少个队伍
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建和加入5个队伍");
                    }
                    // 6.不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper); // 查询用户是否已加入改队伍
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入改队伍");
                    }
                    // 7.已加入的队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId); // 根据teamId查询已加入队伍的人数
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 8.新增队伍-用户关联信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally { // 最后执行完这个逻辑再把锁释放掉
            // 只能自己释放锁
            if (lock.isHeldByCurrentThread()) { // 判断当前的锁是不是自己的锁
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest 前端传过来的要退出的队伍信息
     * @param loginUser 当前登录的用户
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class) //加上事务，数据库有多个增删改的操作，最好加上事务，防止错误导致数据不一致
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        // 1.校验请求参数
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.校验队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId); // 根据队伍id查询队伍信息
        // 3.检验我是否加入队伍
        Long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper); // 查询该用户是否存在该队伍中
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "你未加入该队伍");
        }
        // 4.如果队伍：
        //   (1)如果是队长退出队伍，权限转移给第二早加入的用户--先来后到(取id最小的两条数据)
        //   (2)如果不是队长，自己退出队伍
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId); // 查询这个队有多少人
        // 队伍只剩一人，解散
        if (teamHasJoinNum == 1) {
            // 删除当前用户和这个队伍的关系
            this.removeById(teamId);
        } else {
            // 队伍还剩至少两人
            // 是否为队长
            if (team.getUserId().equals(userId)) { // 是队长
                // 把队伍转移给最早加入的用户
                // (1)查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                // 不需要查询全部已加入队伍的信息，只需查询前面两个人即可，队长退出，第二个人接管
                userTeamQueryWrapper.last("order by id asc limit 2"); // 在SQL语句的最后面拼接SQL语句
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper); // 查询到两条数据
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1); // 第二早加入的用户
                // (2)将这个用户设置为队长
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam); // 更新队长
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // (3)删除当前用户和这个队伍的关系
        return userTeamService.remove(queryWrapper);
    }

    /**
     * 队长删除队伍
     * @param id 前端传来的队伍的id
     * @return 返回删除是否成功
     */
    @Override
    // 如果队伍的关联信息已经删除了，这时数据库挂了，这时我们的队伍还没有删除，这时候就会有脏数据，导致数据不一致
    @Transactional(rollbackFor = Exception.class) //加上事务
    public boolean deleteTeam(long id, User loginUser) {
        // 1.检验请求参数
        // 2.校验队伍是否存在
        Team team = getTeamById(id); // 根据队伍id获取队伍信息
        Long teamId = team.getId();
        // 3.校验你是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限访问");
        }
        // 4.移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 5.删除队伍
        return this.removeById(teamId);
    }

    /**
     * 根据队伍的teamId查询当前队伍的人数
     * @param teamId 当前队伍的人数
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

    /**
     * 根据id获取队伍信息
     * @param teamId 队伍id
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }
}