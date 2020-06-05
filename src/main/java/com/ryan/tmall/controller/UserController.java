package com.ryan.tmall.controller;
 
import java.util.List;

import com.ryan.tmall.service.UserService;
import com.ryan.tmall.util.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
 
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ryan.tmall.pojo.User;

@Controller
@RequestMapping("")
public class UserController {
    @Autowired
    UserService userService;

    /**
     * UserController的list方法被调用
     1. 获取分页对象
     2. 设置分页信息
     3. 查询用户集合
     4. 通过PageInfo获取总数，并设置在page对象上
     5. 把用户集合设置到model的"us"属性上
     6. 把分页对象设置到model的"page"属性上
     5. 服务端跳转到admin/listUser.jsp页面
     6. 在listUser.jsp用c:forEach遍历"us"集合
     */
    @RequestMapping("admin_user_list")
    public String list(Model model, Page page){
        PageHelper.offsetPage(page.getStart(),page.getCount());
 
        List<User> us= userService.list();
 
        int total = (int) new PageInfo<>(us).getTotal();
        page.setTotal(total);
 
        model.addAttribute("us", us);
        model.addAttribute("page", page);
 
        return "admin/listUser";
    }
 
}