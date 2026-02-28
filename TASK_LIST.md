# Mini-Agent 开发任务清单 (v2 - 对标Qwen-Agent)

## 阶段一：核心基础 (Core Foundation) ✅
- [x] 1. pom.xml 配置 - Spring Boot + AI SDK
- [x] 2. 启动类 MiniAgentApplication
- [x] 3. 配置文件 application.yml
- [x] 4. AI服务接口定义 (AiService)
- [x] 5. 配置属性类 (AiProperties)
- [x] 6. 基础Controller (HealthController)
- [x] 7. 全局异常处理

## 阶段二：Agent核心 (Agent Core) 🔄
- [x] 8. Message/Role 消息模型
- [x] 9. ChatMemory 会话记忆
- [x] 10. Prompt模板引擎
- [x] 11. Agent核心类
- [x] 12. 工具系统 (Tool/Function Calling)

## 阶段三：Web层 (Web Layer) ✅
- [x] 13. Chat REST API
- [x] 14. SSE流式响应
- [x] 15. 会话管理
- [x] 16. Agent管理API

## 阶段四：对标Qwen-Agent功能

### 4.1 多Agent系统
- [ ] 17. Assistant Agent (基础对话Agent)
- [ ] 18. FnCall Agent (函数调用Agent)
- [ ] 19. ReAct Chat Agent (推理+行动)
- [ ] 20. TIR Agent (Tool Integrated Reasoning)
- [ ] 21. Group Chat (多Agent协作)
- [ ] 22. Router Agent (路由分发)

### 4.2 工具系统 (Tools)
- [ ] 23. Code Interpreter (代码执行)
- [ ] 24. Python Executor
- [ ] 25. Web Search (网页搜索)
- [ ] 26. Web Extractor (网页抓取)
- [ ] 27. Image Generation (图片生成)
- [ ] 28. Doc Parser (文档解析)
- [ ] 29. Retrieval (检索工具)
- [ ] 30. MCP Manager (MCP协议支持)

### 4.3 记忆系统 (Memory)
- [ ] 31. 基础记忆 (Memory)
- [ ] 32. 虚拟记忆 (Virtual Memory)
- [ ] 33. 对话检索 (Dialogue Retrieval)

### 4.4 RAG/知识库
- [ ] 34. 长文档RAG (支持超长文档)
- [ ] 35. 向量检索
- [ ] 36. 文档解析

### 4.5 GUI/Web
- [ ] 37. Web UI界面
- [ ] 38. Agent可视化

### 4.6 Server
- [ ] 39. HTTP Server
- [ ] 40. WebSocket支持

## 阶段五：完善 (Polish)
- [ ] 41. 单元测试
- [ ] 42. Docker支持 ✅
- [ ] 43. CI/CD
- [ ] 44. Benchmark测试

---

# Qwen-Agent 功能分析

## 核心模块 (qwen_agent/)
- **agent.py** - Agent基类
- **agents/** - 多种Agent实现
- **llm/** - LLM调用封装
- **memory/** - 记忆系统
- **tools/** - 工具集
- **gui/** - 图形界面
- **utils/** - 工具类
- **settings.py** - 配置

## Agents列表
- assistant.py - 基础助手
- fncall_agent.py - 函数调用
- react_chat.py - ReAct推理
- tir_agent.py - TIR推理
- group_chat.py - 群聊
- user_agent.py - 用户模拟
- human_simulator.py - 人类模拟
- dialogue_simulator.py - 对话模拟
- write_from_scratch.py - 从零写作
- article_agent.py - 文章Agent

## Tools列表
- code_interpreter.py - 代码解释器
- python_executor.py - Python执行
- web_search.py - 网页搜索
- web_extractor.py - 网页抓取
- image_gen.py - 图片生成
- doc_parser.py - 文档解析
- retrieval.py - 检索
- mcp_manager.py - MCP协议
- storage.py - 存储

---

*最后更新: 2026-02-28*
