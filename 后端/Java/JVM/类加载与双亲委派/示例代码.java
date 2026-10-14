// 类加载与双亲委派学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：三层类加载器（启动/平台/应用）
//   第 2 段：类初始化的时机——静态代码块什么时候执行
//   第 3 段：双亲委派——子加载器永远先问父亲
//   第 4 段：谁在【打破】双亲委派

class ClassLoadingDemo {

    public static void main(String[] args) throws Exception {
        demoHierarchy();
        demoInitTiming();
        demoDelegation();
        demoBreakers();
    }

    // ------------------------------------------------------------------
    // 第 1 段：类加载器层级——从本类出发一路 getParent 走到顶
    // ------------------------------------------------------------------
    static void demoHierarchy() {
        System.out.println("== 第 1 段：加载器层级 ==");
        System.out.println("  本类的加载链（getParent 一路向上）：");
        ClassLoader cl = ClassLoadingDemo.class.getClassLoader();
        for (int level = 1; cl != null; cl = cl.getParent(), level++) {
            System.out.println("    第 " + level + " 层: " + cl);
        }
        System.out.println("    链的尽头是 null = 启动类加载器（Bootstrap，C++ 实现，Java 层拿不到引用）");
        System.out.println("  小发现：单文件【java 示例代码.java】运行时编译产物在内存里，");
        System.out.println("  所以最底层多了一层 MemoryClassLoader；正常 javac/java 编译运行时它就是 AppClassLoader");
        System.out.println("  JDK 9+ 标准三层：启动类加载器（核心库）→ 平台类加载器（java.sql 等）");
        System.out.println("                    → 应用类加载器（你的代码与 classpath 上的 jar）");
        System.out.println("  String 的加载器: " + String.class.getClassLoader()
                + "（null = 启动类加载器，核心类库归它管）");
        System.out.println("  DriverManager 的加载器: " + java.sql.DriverManager.class.getClassLoader()
                + "（平台类加载器，JDK 9 起取代扩展类加载器）");
        System.out.println("  注意：层级是【组合】不是继承——每层持有 parent 的引用，父加载器不认识儿子");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：类的初始化时机——静态块只执行一次
    // ------------------------------------------------------------------
    static class Late {
        static int initCount = 0;
        static {
            initCount++;
            System.out.println("  【Late 的静态代码块执行了】initCount = " + initCount);
        }
    }

    static void demoInitTiming() throws Exception {
        System.out.println("== 第 2 段：初始化时机 ==");
        System.out.println("  ① Late.class 类字面量——只加载不初始化（下面不该有静态块输出）");
        Class<?> c1 = Late.class;
        System.out.println("     拿到 Class 对象: " + c1.getName() + "，但静态块没跑");

        System.out.println("  ② loader.loadClass——同样只加载不初始化");
        Class<?> c2 = ClassLoadingDemo.class.getClassLoader().loadClass("ClassLoadingDemo$Late");
        System.out.println("     拿到 Class 对象: " + c2.getName() + "，静态块还是没跑");

        System.out.println("  ③ Class.forName(默认 initialize=true)——触发初始化！");
        Class.forName("ClassLoadingDemo$Late");

        System.out.println("  ④ 再 forName 一次——已初始化的类不会重复初始化");
        Class.forName("ClassLoadingDemo$Late");
        System.out.println("     new Late() 也一样触发初始化，且一个类【整个生命周期只初始化一次】");
        System.out.println("     记忆点：JDBC 用 Class.forName 加载驱动，靠的就是【强制初始化】执行驱动里的静态注册代码");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：双亲委派——子加载器收到请求，先原样递交给父亲
    // ------------------------------------------------------------------

    /** 自定义加载器：不改行为，只在收到请求时【打日志】，看清楚委派顺序。 */
    static class LoggingLoader extends ClassLoader {
        LoggingLoader(ClassLoader parent) { super(parent); }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            System.out.println("    LoggingLoader 收到请求: " + name);
            System.out.println("    → 按 JVM 规范：先委托父亲加载，父亲加载不了才自己来");
            Class<?> c = super.loadClass(name, resolve);      // 老老实实走标准双亲委派流程
            System.out.println("    → 实际加载者: " + c.getClassLoader());
            return c;
        }
    }

    static void demoDelegation() throws Exception {
        System.out.println("== 第 3 段：双亲委派 ==");
        LoggingLoader child = new LoggingLoader(ClassLoadingDemo.class.getClassLoader());
        System.out.println("  让【自定义子加载器】去加载 java.lang.String，观察委派链：");
        Class<?> s = child.loadClass("java.lang.String");
        System.out.println("  最终 String 的加载器: " + s.getClassLoader()
                + "（null = 启动类加载器——根本轮不到子加载器插手）");
        System.out.println("  双亲委派的三大作用：");
        System.out.println("    安全——自定义加载器永远无法伪造 java.lang.String（防止核心 API 被篡改）");
        System.out.println("    唯一——同一个类只被加载一次，Class 对象全 JVM 唯一");
        System.out.println("    高效——已加载的类父亲直接返回缓存，不重复加载");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：谁在打破双亲委派——都是【有明确理由】的特例
    // ------------------------------------------------------------------
    static void demoBreakers() {
        System.out.println("== 第 4 段：打破双亲委派的场景 ==");
        System.out.println("  ① SPI/线程上下文加载器：DriverManager 在核心库（父加载器管），");
        System.out.println("     却要加载厂商驱动 jar（应用层，子加载器管）——父亲【反向】委托儿子，");
        System.out.println("     Thread.currentThread().getContextClassLoader() 就是那座桥");
        System.out.println("  ② Web 容器（Tomcat）：每个 Web 应用一个独立 WebappClassLoader，");
        System.out.println("     应用自己的类【先自己加载】（隔离不同应用的依赖版本），加载不到再委托父亲");
        System.out.println("  ③ 热部署/OSGi：网状委派，模块按需互托——复杂度高，如今主流已回归简单");
        System.out.println("  面试答法：先讲清默认机制的三大作用，再讲打破的场景与动机，别一上来就背特例");
    }
}
