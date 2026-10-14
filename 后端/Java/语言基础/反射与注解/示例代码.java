// 反射与注解学习示例
// 运行方式：java 示例代码.java （需 JDK 11+，本库以 JDK 21 验证）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：Class 对象的三种获取方式
//   第 2 段：解剖一个类（构造器/方法/字段）
//   第 3 段：动态创建实例、调用方法、访问私有字段
//   第 4 段：读取运行时注解
//   第 5 段：综合小例子——注解 + 反射生成建表 SQL（框架的基本套路）

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.StringJoiner;

class ReflectionDemo {

    public static void main(String[] args) throws Exception {
        demoGetClass();
        demoDissect();
        demoDynamicCall();
        demoReadAnnotation();
        demoBuildSql();
    }

    // ------------------------------------------------------------------
    // 第 1 段：三种获取 Class 对象的方式
    // ------------------------------------------------------------------
    static void demoGetClass() throws ClassNotFoundException {
        System.out.println("== 第 1 段：获取 Class 对象 ==");

        User u = new User("小明", 18);

        // 方式 1：实例的 getClass()——运行时拿到对象的真实类型
        Class<?> c1 = u.getClass();
        // 方式 2：类字面量——编译期已知类型时用，最简洁、不会触发类初始化
        Class<?> c2 = User.class;
        // 方式 3：Class.forName 按全限定名加载——配置文件里写字符串的场景（JDBC 驱动）
        Class<?> c3 = Class.forName("User");   // 单文件模式类在默认包，名字就是 "User"

        // 三种方式拿到的是同一个 Class 对象：Class 是 JVM 里的"单例"，一个类只有一份
        System.out.println("c1 == c2 ? " + (c1 == c2) + "，c2 == c3 ? " + (c2 == c3));
        System.out.println("类名: " + c2.getName() + "（简单名: " + c2.getSimpleName() + "）");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：解剖一个类
    // ------------------------------------------------------------------
    static void demoDissect() {
        System.out.println("== 第 2 段：解剖 User 类 ==");

        Class<?> c = User.class;

        // getDeclaredXxx 返回本类声明的所有成员（含 private）；getXxx 只返回 public（含继承的）
        System.out.println("字段:");
        for (Field f : c.getDeclaredFields()) {
            System.out.println("  " + f.getType().getSimpleName() + " " + f.getName());
        }
        System.out.println("构造器:");
        for (Constructor<?> con : c.getDeclaredConstructors()) {
            System.out.println("  参数个数 = " + con.getParameterCount());
        }
        // 方法只打印名字（getDeclaredMethods 的返回顺序不保证，别依赖顺序）
        System.out.print("方法: ");
        StringJoiner sj = new StringJoiner(", ");
        for (Method m : c.getDeclaredMethods()) {
            sj.add(m.getName());
        }
        System.out.println(sj);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：动态创建实例、调用方法、访问私有字段
    // ------------------------------------------------------------------
    static void demoDynamicCall() throws Exception {
        System.out.println("== 第 3 段：动态操作 ==");

        Class<?> c = User.class;

        // 反射创建实例：按参数类型拿到构造器再 newInstance（new User("小红", 20) 的反射版）
        Constructor<?> con = c.getDeclaredConstructor(String.class, int.class);
        Object user = con.newInstance("小红", 20);
        System.out.println("反射创建: " + user);

        // 反射调用方法：getMethod("getName") 拿到方法对象，invoke(目标, 参数...)
        Method getName = c.getMethod("getName");
        System.out.println("invoke getName(): " + getName.invoke(user));

        // 访问私有字段：setAccessible(true) 越过 private 检查——框架的"特权"，也破坏封装
        Field age = c.getDeclaredField("age");
        age.setAccessible(true);
        System.out.println("私有字段 age 原值 = " + age.get(user) + "，反射改为 21");
        age.set(user, 21);
        System.out.println("修改后: " + user);
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：读取运行时注解
    // ------------------------------------------------------------------
    static void demoReadAnnotation() {
        System.out.println("== 第 4 段：读取注解 ==");

        Class<?> c = User.class;

        // 注解本身不做任何事——它是贴在代码上的"标签"，必须有程序来读才有意义。
        // 能否被反射读到取决于 @Retention：RUNTIME 才行（SOURCE/CLASS 运行期已丢弃）
        if (c.isAnnotationPresent(MyTable.class)) {
            MyTable table = c.getAnnotation(MyTable.class);
            System.out.println("User 类上发现 @MyTable，表名 = " + table.value());  // value 属性可省略名字
        }

        // @Override 为什么反射读不到？它是 RetentionPolicy.SOURCE——编译器检查完就丢弃
        System.out.println("@Override 是 SOURCE 级，运行期不存在；自定义注解默认 CLASS 级也不行，");
        System.out.println("想被反射读取必须显式写 @Retention(RetentionPolicy.RUNTIME)");
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 5 段：注解 + 反射 = 框架的基本套路（生成建表 SQL）
    // ------------------------------------------------------------------
    static void demoBuildSql() {
        System.out.println("== 第 5 段：生成建表 SQL ==");

        Class<?> c = User.class;
        MyTable table = c.getAnnotation(MyTable.class);
        if (table == null) {
            System.out.println("没有 @MyTable，跳过");
            return;
        }

        // 遍历字段：带 @MyColumn 的才进 SQL，类型映射成数据库列类型
        StringJoiner columns = new StringJoiner(", ");
        for (Field f : c.getDeclaredFields()) {
            MyColumn col = f.getAnnotation(MyColumn.class);
            if (col == null) continue;
            String sqlType = f.getType() == String.class ? "VARCHAR(255)" : "INT";
            columns.add(col.name() + " " + sqlType);
        }
        System.out.println("CREATE TABLE " + table.value() + " (" + columns + ");");
        System.out.println("—— Spring 用类上的 @Component 找 Bean、JUnit 用 @Test 找测试方法，全是这一套");
        System.out.println();
    }
}

// ------------------------------------------------------------------
// 自定义注解：@Retention 声明存活到运行期，@Target 声明能贴在哪
// ------------------------------------------------------------------
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)              // 只能贴在类/接口上
@interface MyTable {
    String value();                    // 属性名叫 value 时，使用处可省略 "value ="
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)             // 只能贴在字段上
@interface MyColumn {
    String name();
}

// ------------------------------------------------------------------
// 被注解标记、被反射剖析的目标类
// ------------------------------------------------------------------
@MyTable("t_user")
class User {

    @MyColumn(name = "name")
    private String name;

    @MyColumn(name = "age")
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }

    @Override
    public String toString() { return name + "(" + age + ")"; }
}
