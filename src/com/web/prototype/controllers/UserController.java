package com.web.prototype.controllers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.web.common.WebConstants;


@Controller("prototype.UserController")
@RequestMapping("prototype/user")
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	
	@Autowired
	private SqlSessionTemplate oracleSqlSessionTemplate;
	
	
	
	@RequestMapping(value="loginproc", method=RequestMethod.POST)
	public void loginproc(@RequestParam Map<?, ?> param, ModelMap modelMap, HttpServletRequest request, HttpSession session) throws Exception {
		
		log.debug("=========================================================================================");
		log.debug("== START");
		log.debug("== param : {}", param);
		log.debug("=========================================================================================");
		
		Map<?, ?> user = (Map<?, ?>)oracleSqlSessionTemplate.selectOne("prototype.user.select", param);
		log.debug("USER Data : {}", user);
		
		if(user != null) {
			modelMap.addAttribute("userData", user);
			session.setAttribute(WebConstants.SESSION_KEY, user);
			String returnUrl = (String)session.getAttribute(WebConstants.RETURN_URL);
			if(StringUtils.isEmpty(returnUrl)) {
				modelMap.addAttribute("returnUrl", request.getContextPath() + "/prototype");
			} else {
				modelMap.addAttribute("returnUrl", returnUrl);
			}
		}
		
	}
	
	
	
	@RequestMapping(value="logout")
	public ModelAndView logout(@RequestParam Map<?, ?> param, ModelMap modelMap, HttpSession session) throws Exception {
		
		log.debug("=========================================================================================");
		log.debug("== START");
		log.debug("== param : {}", param);
		log.debug("=========================================================================================");
		
		session.removeAttribute(WebConstants.SESSION_KEY);
		session.invalidate();
		
		return new ModelAndView("redirect:/prototype/user/login.view");
	}
	
	
}
