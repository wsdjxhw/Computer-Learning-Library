// 异常处理学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：异常体系 —— Throwable 的家族树
//   第 2 段：try/catch/finally 的执行顺序
//   第 3 段：受检异常与非受检异常、自定义异常
//   第 4 段：try-with-resources 自动关闭资源
//   第 5 段：多异常合并捕获

import java.io.IOException;  // java.lang 包以外的类需要显式 import

class ExceptionDemo {

    public static void main(String[] args) {
        demoHierarchy();
        demoTryCatchFinally();
        demoCheckedAndCustom();
        demoTryWithResources();
        demoMultiCatch();
    }

    // ------------------------------------------------------------------
    // 第 1 段：异常体系
    // ------------------------------------------------------------------
    static void demoHierarchy() {
        System.out.println("== 第 1 段：异常体系 ==");

        // 沿 getSuperclass() 向上走，打印一个异常的"家族树"
        printFamilyTree(new ArithmeticException("/ by zero"));
        printFamilyTree(new IOException("模拟 IO 故障"));

        // 家族结构（详见《知识详解.md》中的树状图）：
        //   Throwable
        //   ├── Error：严重错误（OutOfMemoryError 等），程序处理不了，不用 catch
        //   └── Exception
        //       ├── RuntimeException（非受检）：空指针、越界、算术异常……多为程序写错了
        //       └── 其他（受检）：IOException、SQLException……外部环境问题，编译器强制处理
        System.out.println();
    }

    static void printFamilyTree(Throwable t) {
        StringBuilder chain = new StringBuilder();
        Class<?> c = t.getClass();
        while (c != null) {                       // 逐级向上拼出继承链
            chain.insert(0, c.getSimpleName());
            c = c.getSuperclass();
            if (c != null) {
                chain.insert(0, " -> ");
            }
        }
        System.out.println("  " + t + " 的家族树: " + chain);
    }

    // ------------------------------------------------------------------
    // 第 2 段：try/catch/finally 顺序
    // ------------------------------------------------------------------
    static void demoTryCatchFinally() {
        System.out.println("== 第 2 段：try/catch/finally ==");

        // 执行顺序：try 出错前的语句 -> catch -> finally（无论出不出错都执行）
        try {
            System.out.println("  try: 开始计算 10 / 0");
            int result = 10 / 0;                  // 抛出 ArithmeticException
            System.out.println("  try: 结果是 " + result + "（这行永远走不到：异常即跳转）");
        } catch (ArithmeticException e) {
            System.out.println("  catch: 捕获到 " + e.getClass().getSimpleName()
                    + "，信息: " + e.getMessage());
        } finally {
            System.out.println("  finally: 无论如何都会执行（常用于收尾清理）");
        }

        // try 没出错时：跳过 catch，finally 照样执行
        try {
            System.out.println("  try: 计算 10 / 2 = " + (10 / 2));
        } catch (ArithmeticException e) {
            System.out.println("  catch: 不会进来");
        } finally {
            System.out.println("  finally: try 正常结束时也执行");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：受检异常、自定义异常
    // ------------------------------------------------------------------

    /** 自定义受检异常：继承 Exception。表示"年龄数据不合法"这种业务错误。 */
    static class InvalidAgeException extends Exception {
        public InvalidAgeException(String message) {
            super(message);   // 把说明文字传给父类，getMessage() 能取到
        }
    }

    /** 这个方法声明 throws：受检异常要么 try/catch，要么继续向上抛，编译器强制二选一 */
    static int parseAge(String text) throws InvalidAgeException {
        int age;
        try {
            age = Integer.parseInt(text);   // NumberFormatException 是非受检异常
        } catch (NumberFormatException e) {
            // 把底层异常包装成业务异常，附上原因（cause），调用方看到的是业务语言
            throw new InvalidAgeException("年龄必须是数字，收到: \"" + text + "\"");
        }
        if (age < 0 || age > 150) {
            throw new InvalidAgeException("年龄超出合理范围: " + age);
        }
        return age;
    }

    static void demoCheckedAndCustom() {
        System.out.println("== 第 3 段：受检异常与自定义异常 ==");

        // parseAge 声明了 throws，编译器强制调用方处理 —— 这就是"受检"的含义
        for (String input : new String[] { "30", "abc", "999" }) {
            try {
                System.out.println("  parseAge(\"" + input + "\") = " + parseAge(input));
            } catch (InvalidAgeException e) {
                System.out.println("  parseAge(\"" + input + "\") 失败: " + e.getMessage());
            }
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：try-with-resources
    // ------------------------------------------------------------------

    /** 模拟需要关闭的资源：实现 AutoCloseable 才能放进 try(...) 括号里 */
    static class Resource implements AutoCloseable {
        private final String name;

        public Resource(String name) {
            this.name = name;
            System.out.println("  打开资源: " + name);
        }

        public void use() {
            System.out.println("  使用资源: " + name);
        }

        @Override
        public void close() {
            System.out.println("  自动关闭: " + name);
        }
    }

    static void demoTryWithResources() {
        System.out.println("== 第 4 段：try-with-resources ==");

        // 在 try(...) 里声明的资源，无论正常结束还是抛异常，都会自动调用 close()，
        // 关闭顺序与声明顺序相反（后打开的先关，像套娃）
        try (Resource a = new Resource("配置文件");
             Resource b = new Resource("数据库连接")) {
            a.use();
            b.use();
        }   // 这里不需要 finally：编译器自动生成 close 调用
        System.out.println("  try 块结束，两个资源已被自动关闭（逆序）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：多异常合并捕获
    // ------------------------------------------------------------------
    static void demoMultiCatch() {
        System.out.println("== 第 5 段：多异常合并捕获 ==");

        String[] inputs = { "12", "3", "0" };
        for (String input : inputs) {
            try {
                int value = Integer.parseInt(input);
                System.out.println("  100 / " + value + " = " + (100 / value));
            } catch (NumberFormatException | ArithmeticException e) {
                // 两种异常的处理逻辑一样时，用 | 合并成一个 catch，减少重复代码
                // 注意：此时变量 e 的类型是它们的公共父类 Exception
                System.out.println("  输入 \"" + input + "\" 出问题: "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        System.out.println();
    }
}
