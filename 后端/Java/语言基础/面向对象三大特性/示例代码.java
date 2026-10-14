// 面向对象三大特性（封装、继承、多态）学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：封装 —— private 字段 + 公开方法把关
//   第 2 段：继承 —— 子类复用父类，super 与方法重写
//   第 3 段：多态 —— 父类引用指向子类对象，动态绑定
//   第 4 段：instanceof、向下转型与方法重载

// 主类放在文件最前面：java 单文件运行模式执行的是第一个顶层类的 main
class OopDemo {

    public static void main(String[] args) {
        demoEncapsulation();
        demoInheritance();
        demoPolymorphism();
        demoOverloadAndCast();
    }

    // ------------------------------------------------------------------
    // 第 1 段：封装
    // ------------------------------------------------------------------
    static void demoEncapsulation() {
        System.out.println("== 第 1 段：封装 ==");
        Dog dog = new Dog("旺财", 3);

        // 字段是 private 的，外界只能通过 getter/setter 访问
        System.out.println("名字=" + dog.getName() + ", 年龄=" + dog.getAge());

        // setter 是唯一的写入口，可以在入口校验、拦截非法状态
        // ——这就是封装的价值：对象自己保护自己的合法性
        dog.setAge(-1);
        System.out.println("尝试 setAge(-1) 后，年龄仍 = " + dog.getAge());
        // 若写 dog.name = "xxx" 直接访问 private 字段，编译直接报错（可自行取消注释验证）
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：继承
    // ------------------------------------------------------------------
    static void demoInheritance() {
        System.out.println("== 第 2 段：继承 ==");
        Dog dog = new Dog("旺财", 3);

        // getName() 是父类 Animal 定义的方法，Dog 没写一行就复用了 —— 代码复用
        System.out.println("子类直接使用父类方法 getName(): " + dog.getName());
        // makeSound() 被子类重写（override），调用时执行的是 Dog 的版本
        System.out.println("重写后的 makeSound(): " + dog.makeSound());
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：多态
    // ------------------------------------------------------------------
    static void demoPolymorphism() {
        System.out.println("== 第 3 段：多态 ==");

        // 父类引用指向子类对象（向上转型）：只声明"它是动物"，不关心具体种类
        // 动物园数组可以混装 Dog 和 Cat，新增种类也不影响这段遍历代码 —— 扩展性的来源
        Animal[] zoo = { new Dog("旺财", 3), new Cat("咪咪", 2), new Animal("神秘生物", 1) };
        for (Animal a : zoo) {
            // 同一句 a.makeSound()，运行时按对象真实类型选择执行版本 → 动态绑定
            System.out.println("  " + a.makeSound());
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 4 段：instanceof、向下转型与方法重载
    // ------------------------------------------------------------------
    static void demoOverloadAndCast() {
        System.out.println("== 第 4 段：转型与重载 ==");

        // 向上转型后，父类引用看不到子类特有方法（fetch 只在 Dog 里定义）
        Animal maybeDog = new Dog("阿黄", 5);

        // 先 instanceof 判断真实类型，再向下转型（强转）成 Dog，才能调用子类方法
        if (maybeDog instanceof Dog) {
            Dog d = (Dog) maybeDog;
            System.out.println("传统写法: " + d.fetch());
        }
        // Java 16+ 模式匹配：判断 + 转型 + 绑定变量一步完成，推荐写法
        if (maybeDog instanceof Dog d) {
            System.out.println("模式匹配: " + d.fetch());
        }

        // 方法重载（overload）：同一个类里方法名相同、参数不同，编译期按参数类型选择
        // 与"重写 override（子类覆盖父类同名方法，运行期绑定）"是两回事，别混淆
        Dog puppy = new Dog("豆豆", 1);
        System.out.println("bark(3)     = " + puppy.bark(3));
        System.out.println("bark(\"开心\") = " + puppy.bark("开心"));
        System.out.println();
    }
}

// ------------------------------------------------------------------
// 父类：演示封装
// ------------------------------------------------------------------
class Animal {

    private String name;  // private：仅本类可见，外界不能直接读写
    private int age;

    // 构造方法：与类同名、无返回类型，new 的时候执行，负责初始化字段
    public Animal(String name, int age) {
        this.name = name;  // this.name 是成员变量，name 是参数，this 用来区分
        this.age = age;
    }

    // getter：外界读取 private 字段的受控入口
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    // setter：写入入口，在这里拦截非法值 —— 封装的核心价值
    public void setAge(int age) {
        if (age < 0) {
            System.out.println("  拒绝设置年龄 " + age + "（年龄不能为负）");
            return;
        }
        this.age = age;
    }

    // 父类提供的默认实现，子类可以重写
    public String makeSound() {
        return name + ": 某种叫声";
    }
}

// ------------------------------------------------------------------
// 子类：演示继承与重写
// ------------------------------------------------------------------
class Dog extends Animal {  // extends：Dog 继承 Animal 的全部非 private 成员

    public Dog(String name, int age) {
        super(name, age);  // super(...) 调用父类构造方法，必须写在第一行
    }

    // 方法重写（override）：与父类方法签名完全一致，执行时覆盖父类版本
    // @Override 注解让编译器帮忙检查"确实重写了父类方法"，防手误写错签名
    @Override
    public String makeSound() {
        return getName() + ": 汪汪";
    }

    // 子类独有的方法：父类引用直接调用不到它（见第 4 段）
    public String fetch() {
        return getName() + " 把球捡回来了";
    }

    // 方法重载（overload）：同一个类中同名但参数列表不同
    public String bark(int times) {
        return "汪".repeat(Math.max(times, 0)) + "（叫了 " + times + " 声）";
    }

    public String bark(String mood) {
        return "汪～（情绪: " + mood + "）";
    }
}

class Cat extends Animal {

    public Cat(String name, int age) {
        super(name, age);
    }

    @Override
    public String makeSound() {
        return getName() + ": 喵";
    }
}
