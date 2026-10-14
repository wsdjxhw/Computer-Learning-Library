// 泛型学习示例
// 运行方式：java 示例代码.java （UTF-8 终端可加 -Dstdout.encoding=UTF-8）
//
// 本示例与《知识详解.md》的章节一一对应：
//   第 1 段：泛型解决什么问题
//   第 2 段：泛型类 Box<T>
//   第 3 段：泛型方法与类型边界
//   第 4 段：通配符 ? extends / ? super
//   第 5 段：类型擦除

import java.util.ArrayList;
import java.util.List;

class GenericDemo {

    public static void main(String[] args) {
        demoWhyGeneric();
        demoGenericClass();
        demoGenericMethod();
        demoWildcard();
        demoErasure();
    }

    // ------------------------------------------------------------------
    // 第 1 段：泛型解决什么问题
    // ------------------------------------------------------------------
    static void demoWhyGeneric() {
        System.out.println("== 第 1 段：为什么需要泛型 ==");

        // Java 5 之前没有泛型，集合只能装 Object（如今等价写法就是 List<Object>）：
        // 什么都能塞，取出来只能当 Object 用，想当具体类型必须强转
        List<Object> oldStyle = new ArrayList<>();
        oldStyle.add("字符串");
        oldStyle.add(42);                        // 编译器不拦：整数也是 Object
        // 强转第 1 个没问题；但第 2 个是整数，强转 String 会在运行时炸
        // —— 类型错误被拖到运行期才暴露，这就是没有类型约束的代价
        Object first = oldStyle.get(0);
        System.out.println("  取出第 1 个并强转: " + (String) first);
        try {
            // 把第 2 个元素（整数）直接强转成 String —— 求值参数时当场爆炸
            System.out.println("  不会被打印: " + (String) oldStyle.get(1));
        } catch (ClassCastException e) {
            System.out.println("  强转第 2 个失败: ClassCastException —— 错误发现得太晚");
        }

        // 泛型把类型检查提前到编译期：List<String> 只能放 String，放别的直接编译报错
        List<String> safe = new ArrayList<>();  // <> 里不用重复写 String（钻石操作符，Java 7+）
        safe.add("字符串");
        // safe.add(42);   // 取消注释：编译期就报错，问题在写代码时就被抓住
        System.out.println("  List<String> 取值无需强转: " + safe.get(0));
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 2 段：泛型类
    // ------------------------------------------------------------------
    static void demoGenericClass() {
        System.out.println("== 第 2 段：泛型类 Box<T> ==");

        // T 像一个"类型占位符"，new 的时候才决定具体是什么类型
        Box<String> nameBox = new Box<>();
        nameBox.set("Alice");
        Box<Integer> ageBox = new Box<>();     // int 的包装类型：泛型参数不能是基本类型
        ageBox.set(30);

        // 同一个类写一次，String、Integer……各种类型都能用 —— 类型安全的代码复用
        System.out.println("  装字符串: " + nameBox.get() + "，装整数: " + ageBox.get());
        // nameBox.set(42);  // 取消注释：编译报错，T 已被确定为 String
        System.out.println();
    }

    // ------------------------------------------------------------------
    // 第 3 段：泛型方法与类型边界
    // ------------------------------------------------------------------
    static void demoGenericMethod() {
        System.out.println("== 第 3 段：泛型方法 ==");

        List<String> names = List.of("Charlie", "Alice", "Bob");
        List<Integer> ages = List.of(30, 25, 35);

        // 泛型方法在返回类型前声明自己的 <T>，调用时由参数自动推断类型
        System.out.println("  first(names) = " + first(names));
        System.out.println("  first(ages)  = " + first(ages));

        // 类型边界 <T extends Comparable<T>>：要求 T 必须可比较，方法内才能调用 compareTo
        // 没有边界的话编译器不知道 T 有什么方法，任何方法调用都会报错
        System.out.println("  max(names) = " + max(names));
        System.out.println("  max(ages)  = " + max(ages));
        System.out.println();
    }

    /** 返回列表第一个元素；<T> 表示这是泛型方法，类型由调用处推断。 */
    static <T> T first(List<T> list) {
        return list.get(0);
    }

    /** 找最大值。T extends Comparable<T> 是类型边界：只接受"可比较"的类型。 */
    static <T extends Comparable<T>> T max(List<T> list) {
        T champion = list.get(0);
        for (T item : list) {
            if (item.compareTo(champion) > 0) {   // 能调用 compareTo 是边界换来的
                champion = item;
            }
        }
        return champion;
    }

    // ------------------------------------------------------------------
    // 第 4 段：通配符
    // ------------------------------------------------------------------
    static void demoWildcard() {
        System.out.println("== 第 4 段：通配符 ==");

        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.5, 2.5);
        List<Number> numbers = new ArrayList<>();

        // 问题背景：List<Integer> 不是 List<Number> 的子类型（尽管 Integer 是 Number 的子类），
        // 所以 sum(List<Number>) 收不下 List<Integer> —— 需要通配符来表达"某种 Number"

        // ? extends Number：Number 或它的任意子类（Integer、Double……）——  生产者，只读
        System.out.println("  sum(ints)   = " + sum(ints));
        System.out.println("  sum(doubles) = " + sum(doubles));

        // ? super Integer：Integer 或它的任意父类（Number、Object……）—— 消费者，可写
        fillWithZero(numbers);                    // List<Number> 可以装 Integer
        System.out.println("  fillWithZero(List<Number>) 后: " + numbers);

        // 记忆口诀 PECS：Producer Extends, Consumer Super（详见《知识详解.md》）
        System.out.println();
    }

    /** 求和：元素是"某种 Number"即可读 —— ? extends 是只读的生产者视角。 */
    static double sum(List<? extends Number> list) {
        double total = 0;
        for (Number n : list) {      // 读出来的元素至少是 Number，可以安全调用 doubleValue
            total += n.doubleValue();
        }
        // list.add(42);  // 取消注释：编译报错！编译器不知道具体类型，禁止写入
        return total;
    }

    /** 填 0：列表是"能装 Integer 的某种列表"即可写 —— ? super 是可写的消费者视角。 */
    static void fillWithZero(List<? super Integer> list) {
        list.add(0);                 // Integer 放得进 List<Integer>/List<Number>/List<Object>
        list.add(0);
        list.add(0);
    }

    // ------------------------------------------------------------------
    // 第 5 段：类型擦除
    // ------------------------------------------------------------------
    static void demoErasure() {
        System.out.println("== 第 5 段：类型擦除 ==");

        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();

        // 泛型只存在于编译期：编译后 List<String> 和 List<Integer> 都是同一个 List 类，
        // T 被擦除成 Object（有边界时擦成边界类型）—— 这就是"类型擦除"
        System.out.println("  strings.getClass() == integers.getClass() ? "
                + (strings.getClass() == integers.getClass()));
        // 因此运行期无法问"这个 List 装的是什么 T"，也无法 new T()、new T[]；
        // List<String> 与 List<Integer> 不能算两个不同的重载（签名擦除后相同）
        System.out.println();
    }
}

// ------------------------------------------------------------------
// 泛型类：T 是类型参数，整个类里都可以当类型用
// ------------------------------------------------------------------
class Box<T> {

    private T value;   // 字段类型是 T：装什么类型由使用者在 new 时决定

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
