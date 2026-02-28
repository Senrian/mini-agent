package com.miniagent.skill;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill注册表 - 对标OpenClaw的Skill系统
 * 
 * Skill结构:
 * - name: 技能名称
 * - description: 描述
 * - tools: 可用工具列表
 * - prompt: 系统提示词模板
 * - handler: 执行处理器
 */
@Slf4j
public class SkillRegistry {

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    private final SkillLoader skillLoader;
    
    public SkillRegistry() {
        this.skillLoader = new FileSkillLoader();
    }

    /**
     * 注册技能
     */
    public void register(Skill skill) {
        skills.put(skill.getName(), skill);
        log.info("Registered skill: {}", skill.getName());
    }

    /**
     * 加载技能目录
     */
    public void loadSkillsFromDirectory(String directory) {
        try {
            skillLoader.loadFromDirectory(directory, this);
        } catch (Exception e) {
            log.error("Failed to load skills from {}", directory, e);
        }
    }

    /**
     * 获取技能
     */
    public Skill get(String name) {
        return skills.get(name);
    }

    /**
     * 获取所有技能
     */
    public Map<String, Skill> getAll() {
        return new HashMap<>(skills);
    }

    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    /**
     * 获取技能列表(用于展示)
     */
    public List<SkillInfo> listSkills() {
        List<SkillInfo> result = new ArrayList<>();
        for (Skill skill : skills.values()) {
            result.add(new SkillInfo(skill.getName(), skill.getDescription(), skill.getTools()));
        }
        return result;
    }

    /**
     * 技能类
     */
    @Data
    public static class Skill {
        private String name;
        private String description;
        private List<String> tools;
        private String systemPrompt;
        private SkillHandler handler;

        public interface SkillHandler {
            Object handle(Map<String, Object> params) throws Exception;
        }
    }

    /**
     * 技能信息
     */
    @Data
    public static class SkillInfo {
        private final String name;
        private final String description;
        private final List<String> tools;
    }

    /**
     * 技能加载器接口
     */
    public interface SkillLoader {
        void loadFromDirectory(String directory, SkillRegistry registry) throws Exception;
    }

    /**
     * 文件系统技能加载器
     * 从skills目录加载技能配置
     */
    public static class FileSkillLoader implements SkillLoader {
        
        @Override
        public void loadFromDirectory(String directory, SkillRegistry registry) throws Exception {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                log.warn("Skills directory not found: {}", directory);
                return;
            }
            
            Files.list(dirPath).filter(Files::isDirectory).forEach(skillDir -> {
                try {
                    Skill skill = loadSkill(skillDir);
                    if (skill != null) {
                        registry.register(skill);
                    }
                } catch (Exception e) {
                    log.error("Failed to load skill from {}", skillDir, e);
                }
            });
        }
        
        private Skill loadSkill(Path skillDir) throws Exception {
            Path skillMd = skillDir.resolve("SKILL.md");
            if (!Files.exists(skillMd)) {
                return null;
            }
            
            String content = Files.readString(skillMd);
            return parseSkillFile(skillDir.getFileName().toString(), content);
        }
        
        private Skill parseSkillFile(String name, String content) {
            Skill skill = new Skill();
            skill.setName(name);
            
            // 简单解析 YAML frontmatter
            if (content.startsWith("---")) {
                int end = content.indexOf("---", 3);
                if (end > 0) {
                    String frontmatter = content.substring(3, end);
                    // 解析description
                    if (content.contains("description:")) {
                        int start = content.indexOf("description:") + 12;
                        int lineEnd = content.indexOf("\n", start);
                        if (lineEnd > start) {
                            String desc = content.substring(start, lineEnd).trim();
                            desc = desc.replaceAll("^\"|\"$", "");
                            skill.setDescription(desc);
                        }
                    }
                }
            }
            
            // 默认描述
            if (skill.getDescription() == null) {
                skill.setDescription("Custom skill: " + name);
            }
            
            return skill;
        }
    }
}
