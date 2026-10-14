// 接口与抽象类学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：抽象类 —— "是什么"，半成品模板
//   第 2 段：接口 —— "能做什么"，能力契约
//   第 3 段：接口的多实现 vs 类的单继承
//   第 4 段：默认方法

class InterfaceAbstractDemo {

    public static void main(String[] args) {
        demoAbstract();
        demoInterface();
        demoMultipleInterfaces();
        demoDefaultMethod();
    }

    // ------------------------------------------------------------------
    // 第 1 段：抽象类
    // ------------------------------------------------------------------
    static void demoAbstract() {
        System.out.println("== 第 1 段：抽象类 ==");

        // AbstractShape 是抽象类，不能 new（里面有没有实现完的方法，实例化不安全）
        // AbstractShape bad = new AbstractShape("无名");  // 取消注释会编译报错

        // 只能 new 它的具体子类；子类必须补全 area() 才有资格被实例化
        Circle circle = new Circle(2.0);
        System.out.println("子类实现 area(): " + circle.area());

        // describe() 是父类里写好的具体方法，所有子类直接复用 —— 模板的意义：
        // 公共骨架父类写一次，变化的细节留给子类填
        System.out.println("父类模板 describe(): " + circle.describe());
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：接口
    // ------------------------------------------------------------------
    static void demoInterface() {
        System.out.println("== 第 2 段：接口 ==");
        Circle circle = new Circle(2.0);

        // Drawable 是"可绘制"这个能力的契约：实现它 = 承诺提供 draw()
        // 接口引用同样可以指向实现类对象（接口也是一种多态类型）
        Drawable d = circle;
        d.draw();
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：单继承 vs 多实现
    // ------------------------------------------------------------------
    static void demoMultipleInterfaces() {
        System.out.println("== 第 3 段：单继承 vs 多实现 ==");

        // Person 只能 extends 一个父类（Java 类单继承，避免菱形继承歧义），
        // 但可以同时 implements 多个接口 —— 一个对象可以具备多种"能力"
        Person bob = new Person("Bob");
        bob.swim();
        bob.run();

        // 用不同的接口引用同一个对象，看到的是不同的"能力侧面"
        Swimmer asSwimmer = bob;
        Runner asRunner = bob;
        asSwimmer.swim();
        asRunner.run();
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：默认方法
    // ------------------------------------------------------------------
    static void demoDefaultMethod() {
        System.out.println("== 第 4 段：默认方法 ==");
        Circle circle = new Circle(3.0);

        // drawWithFrame() 是接口里的 default 方法：带默认实现，实现类不写也能用，
        // 默认实现内部还能调用抽象方法 draw()（由具体类提供）——接口版的"模板方法"
        circle.drawWithFrame();
        System.out.println();
    }
}

// ------------------------------------------------------------------
// 抽象类：形状的"半成品模板"
// ------------------------------------------------------------------
abstract class AbstractShape {

    private final String name;  // final：构造后不可再改，见《字符串》篇的不可变思想

    // protected：仅本类和子类可见 —— 给子类构造用，又不暴露给外界
    protected AbstractShape(String name) {
        this.name = name;
    }

    // 抽象方法：只有声明没有实现。它声明"每个形状都必须会算面积"，
    // 但怎么算每个子类自己决定
    public abstract double area();

    // 具体方法：写一次，全部子类共享
    public String describe() {
        return name + " 的面积 = " + String.format("%.2f", area());
    }
}

// ------------------------------------------------------------------
// 接口：能力契约
// ------------------------------------------------------------------
interface Drawable {

    void draw();  // 接口方法默认就是 public abstract，实现类必须提供 draw()

    // 默认方法（Java 8+）：带实现的兜底逻辑，实现类可直接使用或覆盖
    default void drawWithFrame() {
        System.out.println("先画边框，再调用 draw():");
        draw();
    }
}

interface Swimmer {
    void swim();  // 约定：会游泳的东西要实现这个方法
}

interface Runner {
    void run();   // 约定：会跑的东西要实现这个方法
}

// ------------------------------------------------------------------
// 具体类：一个类 + 单继承 + 多实现
// ------------------------------------------------------------------
class Circle extends AbstractShape implements Drawable, Swimmer, Runner {

    private double radius;

    public Circle(double radius) {
        super("圆");            // 先完成父类（抽象类）的初始化
        this.radius = radius;
    }

    // 必须实现抽象类的抽象方法，否则本类也得声明成 abstract
    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    // 必须实现接口的全部抽象方法
    @Override
    public void draw() {
        System.out.println("画一个半径 " + radius + " 的圆");
    }

    @Override
    public void swim() {
        System.out.println("圆滚进水里漂着游");
    }

    @Override
    public void run() {
        System.out.println("圆一路滚着跑");
    }
}

class Person implements Swimmer, Runner {

    private final String name;

    public Person(String name) {
        this.name = name;
    }

    @Override
    public void swim() {
        System.out.println(name + " 在游泳");
    }

    @Override
    public void run() {
        System.out.println(name + " 在跑步");
    }
}
