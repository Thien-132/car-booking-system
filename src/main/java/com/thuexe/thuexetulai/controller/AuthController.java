package com.thuexe.thuexetulai.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.thuexe.thuexetulai.model.User;
import com.thuexe.thuexetulai.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // ================= LOGIN PAGE =================
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ================= REGISTER PAGE =================
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           @RequestParam String confirmPassword,
                           Model model) {

        // check email tồn tại
        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email đã tồn tại");
            return "register";
        }

        // check password
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu không khớp");
            return "register";
        }

        user.setRole("USER");
        userRepository.save(user);

        return "redirect:/login";
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        // ❌ email không tồn tại
        if (optionalUser.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại");
            return "login";
        }

        User user = optionalUser.get();

        // ❌ sai mật khẩu
        if (!user.getPassword().equals(password)) {
            model.addAttribute("error", "Sai mật khẩu");
            return "login";
        }

        // chuẩn hóa role
        String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "";
        user.setRole(role);

        session.setAttribute("user", user);

        System.out.println("LOGIN SUCCESS: " + email + " | ROLE: " + role);

        // phân quyền
        if (role.equals("ADMIN")) {
            return "redirect:/admin/cars";
        }

        return "redirect:/";
    }

    // ================= LOGOUT =================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    @GetMapping("/profile")
    public String profile(HttpSession session){

        // nếu chưa login → quay về trang chủ
        if(session.getAttribute("user") == null){
            return "redirect:/";
        }

        return "profile";
    }
}