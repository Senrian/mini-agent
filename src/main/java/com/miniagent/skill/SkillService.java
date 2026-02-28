package com.miniagent.skill;

import java.util.List;

/**
 * Skill服务 - 管理Skill的加载和使用
 */
public class SkillService {

    private final SkillRegistry skillRegistry;
    
    public SkillService(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }
    
    /**
     * 获取所有可用技能
     */
    public List<SkillRegistry.SkillInfo> getAvailableSkills() {
        return skillRegistry.listSkills();
    }
    
    /**
     * 获取技能详情
     */
    public SkillRegistry.Skill getSkill(String name) {
        return skillRegistry.get(name);
    }
    
    /**
     * 执行技能
     */
    public Object executeSkill(String skillName, java.util.Map<String, Object> params) {
        SkillRegistry.Skill skill = skillRegistry.get(skillName);
        if (skill == null) {
            throw new IllegalArgumentException("Skill not found: " + skillName);
        }
        
        if (skill.getHandler() != null) {
            return skill.getHandler().handle(params);
        }
        
        // 返回技能的提示词
        return java.util.Map.of(
            "skill", skillName,
            "description", skill.getDescription(),
            "tools", skill.getTools(),
            "systemPrompt", skill.getSystemPrompt()
        );
    }
}
