package com.xiaomi.controller;

import com.xiaomi.entity.User;
import com.xiaomi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login"; // 返回 templates/login.html
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // 返回 templates/register.html
    }

    /**
     * 【关键修改】处理用户注册的POST请求
     * @param user  通过 @ModelAttribute 绑定表单数据
     * @param model 用于向视图传递数据
     * @return 视图名称
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            // 尝试执行注册逻辑
            userService.register(user);
            // 如果没有异常，重定向到登录页面并附带成功参数
            return "redirect:/login?success";
        } catch (IllegalStateException e) {
            // 如果捕捉到用户已存在的异常
            // 1. 向模型中添加错误消息
            model.addAttribute("errorMessage", e.getMessage());
            // 2. 将用户已填写的数据再次传回页面，避免用户重新输入
            model.addAttribute("user", user);
            // 3. 返回到注册页面，而不是重定向
            return "register";
        }
    }

    // 假设的首页或问答页，登录后跳转
    @GetMapping({"/"})
    public String home() {
        // 返回一个重定向指令，让浏览器跳转到 /login 路径
        return "redirect:/login";
    }
}
