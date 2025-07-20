package com.xiaomi.controller;

import com.xiaomi.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping("/history")
    public String showHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            Authentication authentication,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);

        // 使用新的服务方法获取包含格式化日期的数据
        Page<Map<String, Object>> historyPage = historyService.getHistoryWithFormattedDates(authentication, pageable);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("currentPage", page);
        return "history";
    }
}
