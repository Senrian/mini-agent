package com.miniagent.controller;

import com.miniagent.skill.SkillRegistry;
import com.miniagent.skill.SkillService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill控制器
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * 获取所有可用技能
     */
    @GetMapping
    public Map<String, Object> listSkills() {
        List<SkillRegistry.SkillInfo> skills = skillService.getAvailableSkills();
        Map<String, Object> result = new HashMap<>();
        result.put("skills", skills);
        result.put("total", skills.size());
        return result;
    }

    /**
     * 获取技能详情
     */
    @GetMapping("/{skillName}")
    public Map<String, Object> getSkill(@PathVariable String skillName) {
        SkillRegistry.Skill skill = skillService.getSkill(skillName);
        if (skill == null) {
            return Map.of("error", "Skill not found: " + skillName);
        }
        
        return Map.of(
            "name", skill.getName(),
            "description", skill.getDescription(),
            "tools", skill.getTools(),
            "systemPrompt", skill.getSystemPrompt() != null ? skill.getSystemPrompt() : ""
        );
    }

    /**
     * 执行技能
     */
    @PostMapping("/{skillName}/execute")
    public Map<String, Object> executeSkill(
            @PathVariable String skillName,
            @RequestBody Map<String, Object> params) {
        try {
            Object result = skillService.executeSkill(skillName, params);
            return Map.of("success", true, "result", result);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
