package org.nlpcn.jcoder.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nlpcn.jcoder.domain.Group;
import org.nlpcn.jcoder.domain.User;
import org.nlpcn.jcoder.domain.UserGroup;
import org.nlpcn.jcoder.filter.AuthoritiesManager;
import org.nlpcn.jcoder.util.StaticValue;
import org.nlpcn.jcoder.util.dao.BasicDao;
import org.nutz.dao.Cnd;
import org.nutz.dao.Condition;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.By;
import org.nutz.mvc.annotation.Fail;
import org.nutz.mvc.annotation.Filters;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@IocBean
@Filters(@By(type = AuthoritiesManager.class, args = { "userType", "1", "/login.jsp" }))
public class UserAction {

	private static final Logger LOG = LoggerFactory.getLogger(UserAction.class) ;


	public BasicDao basicDao = StaticValue.systemDao;;

	@At("/user/list")
	@Ok("jsp:/user/user_list.jsp")
	public void userList() {
		Condition con = null;
		List<User> users = basicDao.search(User.class, con);
		Mvcs.getReq().setAttribute("users", users);

		List<Group> groups = basicDao.search(Group.class, con);
		Mvcs.getReq().setAttribute("groups", groups);

	}

	@At("/group/list")
	@Ok("jsp:/user/group_list.jsp")
	public void groupList() {
		Condition con = null;
		List<Group> groups = basicDao.search(Group.class, con);
		// 查找group下所有用户

		for (Group group : groups) {
			List<UserGroup> userGroupList = basicDao.search(UserGroup.class, Cnd.where("groupId", "=", group.getId()));

			List<Map<String, Object>> users = new ArrayList<>();
			for (UserGroup userGroup : userGroupList) {
				Map<String, Object> userInfo = new HashMap<>();
				User user = basicDao.find(userGroup.getUserId(), User.class);
				if (user == null) {
					continue;
				}
				userInfo.put("id", user.getId());
				userInfo.put("createTime", userGroup.getCreateTime());
				userInfo.put("auth", userGroup.getAuth());
				userInfo.put("name", user.getName());
				users.add(userInfo);
			}

			group.setUsers(users);
		}
		Mvcs.getReq().setAttribute("groups", groups);
	}

	@At("/user/nameDiff")
	@Ok("raw")
	public boolean userNameDiff(@Param("name") String name) {
		Condition con = Cnd.where("name", "=", name);
		int count = basicDao.searchCount(User.class, con);
		return count == 0;
	}


	@At("/user/add")
	@Ok("redirect:/user/list")
	@Fail("jsp:/fail.jsp")
	public void addU(@Param("..") User user) throws Exception {
		if (userNameDiff(user.getName())) {
			user.setPassword(StaticValue.passwordEncoding(user.getPassword()));
			user.setCreateTime(new Date());
			basicDao.save(user);
			LOG.info("add user:" + user.getName());
		}
	}

	@At("/user/del")
	@Ok("redirect:/user/list")
	public void delU(@Param("..") User user) {
		// TODO 删除用户后要把用户相关的userTask及userGroup删除，等userTask完成后做
		if (user.getType() == 1) {
			// 保证至少要有一个超级用户
			Condition con = Cnd.where("type", "=", 1);
			int count = basicDao.searchCount(User.class, con);
			if (count == 1) {
				LOG.info("fail to del the last super user!");
				return;
			}
		}
		boolean flag = basicDao.delById(user.getId(), User.class);
		if (flag) {
			LOG.info("del user:" + user.getName());
		}
		Condition co = Cnd.where("userId", "=", user.getId());
		int num = basicDao.delByCondition(UserGroup.class, co);
		if (num > 0) {
			LOG.info("del userGroup's num:" + num);
		}
	}

	@At("/user/modify")
	@Ok("redirect:/user/list")
	@Fail("jsp:/fail.jsp")
	public void modify(@Param("..") User user) throws Exception {
		if (user == null) {
			return;
		}
		
		User dbUser = basicDao.find(user.getId(), User.class);

		if (!user.getPassword().equals(dbUser.getPassword())) {
			user.setPassword(StaticValue.passwordEncoding(user.getPassword()));
		}

		boolean flag = basicDao.update(user);
		if (flag) {
			LOG.info("modify user:" + user);
		}
	}



}
