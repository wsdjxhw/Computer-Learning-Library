# Java

存放 Java 语言本身（JavaSE）的学习内容：语言基础、集合框架等。示例统一为单文件 Java 源码，JDK 11+ 可直接 `java 示例代码.java` 运行，无需手动编译。

后续 Java 后端框架（Spring、MyBatis 等）按"核心用途"直接放在 `后端/` 下的对应主题（如未来的 `后端/框架/`），不因语言是 Java 而搬进本目录。

## 与"数据结构与算法"的关系

ArrayList、HashMap 等集合类的底层原理（数组扩容、哈希冲突等）在 `数据结构与算法/数据结构/` 已有完整讲解；本目录聚焦 Java 语法与集合的**用法与语言特性**，两边互相链接、不复制正文。

## 八股追问板块（Java 知识点的固定栏目）

Java 属于面试八股密集的语言，因此本目录每个知识点的 `知识详解.md` 都含一节**「五、八股追问」**（位于"常见误区"之后）：收录该主题的高频面试题，格式为**结论 + 为什么 + 常见追问**——不是背诵答案清单，重点是经得起追问两轮。后续新增 Java 知识点必须延续此栏目。

## 内容规划

### 已完成（31 个知识点）

- `语言基础/`：数据类型与变量、字符串、面向对象三大特性、接口与抽象类、异常处理、泛型、常用API、反射与注解、Lambda与Stream
- `集合框架/`：ArrayList、HashMap、HashSet与LinkedHashSet、TreeMap与比较器、LinkedList与ArrayDeque、LinkedHashMap与LRU、迭代器与fail-fast、ConcurrentHashMap与并发容器
- `IO流/`：字节流与字符流、文件与Path、NIO基础
- `并发编程/`：多线程基础、线程池、锁与原子类、并发工具类、CompletableFuture异步编程
- `网络编程/`：TCP通信、UDP通信
- `JVM/`：内存结构、GC与垃圾收集器、类加载与双亲委派、调优工具与OOM排查

JavaSE 主干（语言 → 集合 → IO → 并发 → 网络 → JVM）全部完成，**JavaSE 至此定稿**。

### 定稿说明

并发深入（JMM/AQS/ThreadLocal）、Java 新特性纵览（record/虚拟线程）、序列化等主题暂不展开，日后有需要再按需补充。框架（Maven、Spring 系、Netty）与数据库/中间件等非 JavaSE 主题不进本目录，规划见[后端总 README](../README.md)。

新增知识点时，先判断是否与已有主题共享稳定的中间分类，再决定目录层级。
