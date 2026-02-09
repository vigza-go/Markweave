# MarkWeave - *正在开发*

**前端** : vite vue monaco-editor

**后端** : Spring Boot, WebSocket, MySQL, Redis, RabbitMQ, MinIO, OT 算法, SpringAI 

**项目描述**： 
- 该项目是一款实时 Markdown 协同编辑平台，支持多人在线同时编辑、AI 辅助创作及多格式转换。为解决协作过程中的内容冲突，自研了基于 OT算法的冲突协同模块，实现了高可用、低延迟的办公协作环境。

**todolist:**
- [x] 登陆jwt访问控制
- [x] 文件系统云盘服务
- [x] 文件权限管理服务
- [x] 后端/前端ot算法
- [x] 分布式系统设计，使用Redis作为分布式锁、缓存各个文档的版本号、操作列表
- [x] redis对文档读写请求的旁路缓存设计
- [x] afterConnectionClosed的分布式化
- [x] 分布式系统设计，使用RabbitMQ 传递消息、异步落库、延时重试
- [x] 重试机制下的TTL指数退避延时队列
- [x] ai生成服务
- [x] redis消息幂等处理
- [x] websocket数据封装
- [x] 前端超时重发
- [ ] 让ai的输出内容进行差量修正，而非全量修正
- [ ] ai接口限流、ai接口计费
- [ ] 解决在数据库里存文档的问题
- [ ] 前端开发
- [ ] 前端undo/redo 栈
- [ ] handler的sessionMap内存溢出风险 (OOM)
- [ ] 开发管理员后台
- [ ] 性能监控：集成 Prometheus + Grafana
- [ ] 集成日志收集工具

**todolist**
- [ ] 需考虑使用RabbitMq的数据一致性
- [ ] 考虑前后端消息丢失的问题 
- [ ] 需要处理前端发来操作消息的幂等性



### “为什么不用 RocketMQ？” 
“在 Markweave 的选型中，优先考虑的是实时交互的低延迟。RabbitMQ 基于 Erlang 开发，在消息转发的时延上具有微秒级的优势，非常适合 OT 算法的操作流同步。此外，RabbitMQ 灵活的 Exchange 模型能通过 Fanout 模式轻松实现 WebSocket 集群的广播，而 RocketMQ 的设计初衷更多是解决海量吞吐和事务一致性，对于这种实时办公场景，RabbitMQ 的性价比和响应速度更高。”




***分布式 OT 协作 UML 时序图***
```mermaid
sequenceDiagram
    participant UserA as 用户 A (Client)
    participant S1 as Server 1 (WebSocket)
    participant Redis as Redis (Lock & Data)
    participant MQ as RabbitMQ (Fanout Exchange)
    participant S2 as Server 2 (WebSocket)
    participant UserB as 用户 B (Client)

    Note over UserA, UserB: 实时协作开始
    UserA->>S1: 发送编辑操作 (op, clientVer)
    
    S1->>Redis: 尝试获取分布式锁 (docId)
    alt 获取锁成功
        Redis-->>S1: Lock Acquired (Watchdog 启动)
        S1->>Redis: 获取当前版本 & 历史 Ops
        S1->>S1: 执行 OT Transform (计算新 op')
        S1->>Redis: 更新 FullText & Version++ & Push History
        S1->>Redis: 释放锁
        
        S1->>MQ: 发送广播消息 (docId, finalOp, newVer)
    else 获取锁失败 (冲突)
        S1->>MQ: 转发至重试队列 (Retry Queue)
        S1-->>UserA: (可选) 发送 Ack/等待通知
    end

    Note over MQ: Fanout 广播给所有服务器实例

    MQ-->>S1: 收到广播消息
    MQ-->>S2: 收到广播消息

    S1->>S1: 检查本地是否有 docId 的 Session
    S1->>UserA: 推送最终结果 (或 ACK)

    S2->>S2: 检查本地是否有 docId 的 Session
    S2->>UserB: 推送最终结果 (newOp)
    
    Note over UserB: 编辑同步完成
```


## 为什么选择markdown？
- markdown 语法简单，轻松上手，无需繁杂点击，即可实现富文本特性
- markdown 规则简洁 可以轻松转化为其他格式的文件 html docx pdf ...
- markdown 扩展性高  呈现 Latex 图表 代码 思维导图 uml 手到擒来 
- markdown ai时代的基础设施，方便通过ai对md文档进gg行智能改写，提升工作效率
- markdown 非常轻量 节约空间

### `markdown` 应用

- 支持部分html语法
使用 <kbd>Ctrl</kbd>+<kbd>Alt</kbd>+<kbd>Del</kbd> 重启电脑

- 呈现数学表达式
$ f(x) = sin(x) + cos(x) $
$  \sum_{n = 1}^{100} n$
$$
\begin{Bmatrix}
   a & b \\
   c & d
\end{Bmatrix}
$$
$$
\begin{CD}
   A @>a>> B \\
@VbVV @AAcA \\
   C @= D
\end{CD}
$$

- 图表

```mermaid
pie
    title 浏览器市场份额
    "Chrome" : 65
    "Safari" : 15
    "Firefox" : 10
    "其他" : 10
```

| 功能 | 描述 | 状态 |
|------|------|:----:|
| **用户登录** | 支持邮箱和手机号登录 | &#x2705; |
| *密码重置* | 通过邮箱重置密码 | &#x26a0;&#xfe0f; |
| `API接口` | RESTful API 设计 | &#x2705; |
| [文档链接](https://example.com) | 查看详细文档 | &#x1f4d6; |

- 绘制 UML 图表

```mermaid
%% 时序图例子,-> 直线，-->虚线，->>实线箭头
  sequenceDiagram
    participant 张三
    participant 李四
    张三->王五: 王五你好吗？
    loop 健康检查
        王五->王五: 与疾病战斗
    end
    Note right of 王五: 合理 食物 <br/>看医生...
    李四-->>张三: 很好!
    王五->李四: 你怎么样?
    李四-->王五: 很好!
```




```mermaid
%% 语法示例
        gantt
        dateFormat  YYYY-MM-DD
        title 软件开发甘特图
        section 设计
        需求                      :done,    des1, 2014-01-06,2014-01-08
        原型                      :active,  des2, 2014-01-09, 3d
        UI设计                     :         des3, after des2, 5d
    未来任务                     :         des4, after des3, 5d
        section 开发
        学习准备理解需求                      :crit, done, 2014-01-06,24h
        设计框架                             :crit, done, after des2, 2d
        开发                                 :crit, active, 3d
        未来任务                              :crit, 5d
        耍                                   :2d
        section 测试
        功能测试                              :active, a1, after des3, 3d
        压力测试                               :after a1  , 20h
        测试报告                               : 48h
```