package com.xiaomi.controller;

import com.xiaomi.entity.Knowledge;
import com.xiaomi.repository.KnowledgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.xiaomi.service.StorageService; // 引入StorageService

@Controller
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeRepository knowledgeRepository;
    @Autowired
    private StorageService storageService; // 注入文件存储服务

    /**
     * 查看知识库页面，所有登录用户都可访问。
     */
    @GetMapping
    public String viewKnowledgeBase(Model model) {
        model.addAttribute("knowledgeList", knowledgeRepository.findAll());
        model.addAttribute("newKnowledge", new Knowledge()); // 准备一个空对象给管理员的表单
        return "knowledge"; // 对应 templates/knowledge.html
    }

    /**
     * 处理从文件批量上传知识条目的请求。
     * 仅限管理员访问。
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "请选择一个文件上传。");
            return "redirect:/knowledge";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            // 假设文件格式为：问题,答案 (以英文逗号分隔)
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    Knowledge newKnowledge = new Knowledge();
                    newKnowledge.setQuestion(parts[0].trim());
                    newKnowledge.setAnswer(parts[1].trim());
                    knowledgeRepository.save(newKnowledge);
                    count++;
                }
            }
            redirectAttributes.addFlashAttribute("message", "成功上传并新增了 " + count + " 条知识。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "文件上传失败: " + e.getMessage());
        }

        return "redirect:/knowledge";
    }

    /**
     * 【关键修改】处理新增知识条目的请求，现在包含文件上传
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addKnowledge(@ModelAttribute Knowledge newKnowledge,
                               @RequestParam("imageFile") MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) {

        // 处理图片上传
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = storageService.store(imageFile);
                newKnowledge.setImageUrl(imageUrl);
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "图片上传失败: " + e.getMessage());
                return "redirect:/knowledge";
            }
        }

        knowledgeRepository.save(newKnowledge);
        redirectAttributes.addFlashAttribute("message", "新知识条目已成功添加！");
        return "redirect:/knowledge";
    }
}