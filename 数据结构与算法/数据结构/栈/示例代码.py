"""
栈（Stack）学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：用数组实现一个栈 —— 后进先出
  第 2 段：字符串反转 —— 体会 LIFO
  第 3 段：括号匹配 —— 栈的经典应用
  第 4 段：函数调用栈 —— 理解递归与栈溢出
"""


# ------------------------------------------------------------------
# 第 1 段：数组实现栈
# ------------------------------------------------------------------
class ArrayStack:
    """基于 Python list 实现的栈。

    选 list 的原因：list 在末尾 append / pop 都是 O(1)，
    恰好对应栈只在"栈顶"操作的特性。所有方法都限制只许碰栈顶，
    这层"限制"正是栈的意义——用约束换取逻辑清晰。
    """

    def __init__(self):
        self._items = []  # 约定：列表末尾是栈顶

    def push(self, item):
        """入栈：新元素放栈顶。"""
        self._items.append(item)

    def pop(self):
        """出栈：移除并返回栈顶元素。空栈弹出是使用错误，应明确报错而不是返回 None。"""
        if self.is_empty():
            raise IndexError("pop from empty stack")
        return self._items.pop()

    def peek(self):
        """查看栈顶但不移除。同样要先检查空栈。"""
        if self.is_empty():
            raise IndexError("peek from empty stack")
        return self._items[-1]

    def is_empty(self):
        return len(self._items) == 0

    def __len__(self):
        return len(self._items)

    def __repr__(self):
        # 底部在左、顶部在右，方便观察
        return f"Stack(底->{self._items}->顶)"


def demo_basic():
    print("== 第 1 段：入栈与出栈 ==")
    stack = ArrayStack()
    for value in [1, 2, 3]:
        stack.push(value)
        print(f"push {value} 后: {stack}")

    print("peek 只看不取:", stack.peek(), f"当前栈: {stack}")
    print("pop 弹出:", stack.pop(), f"弹出后: {stack}")
    print("再 pop 弹出:", stack.pop(), f"弹出后: {stack}")

    # 把栈清空后再 pop，会得到我们主动抛出的明确错误
    stack.pop()
    try:
        stack.pop()
    except IndexError as e:
        print("空栈再 pop:", e)
    print()


# ------------------------------------------------------------------
# 第 2 段：字符串反转 —— LIFO 直观体验
# ------------------------------------------------------------------
def demo_reverse_string():
    print("== 第 2 段：栈反转字符串 ==")
    # 全部入栈：a->b->c->d（d 在栈顶）
    stack = ArrayStack()
    for ch in "abcd":
        stack.push(ch)

    # 依次出栈：d、c、b、a —— 后进的先出来，顺序自然倒过来
    reversed_text = ""
    while not stack.is_empty():
        reversed_text += stack.pop()

    print(f'"abcd" 经栈反转后: "{reversed_text}"')
    print()


# ------------------------------------------------------------------
# 第 3 段：括号匹配 —— 栈的经典应用
# ------------------------------------------------------------------
def is_balanced(text):
    """判断字符串中的三种括号是否正确匹配并嵌套闭合。

    核心思路：
      - 遇到左括号：入栈（"欠一个右括号"）
      - 遇到右括号：检查栈顶是否是对应的左括号，是则出栈（"还上了"），
        不是或栈为空则立刻失败
      - 扫描结束后栈必须为空（没有欠账）
    """
    pairs = {")": "(", "]": "[", "}": "{"}  # 右括号 -> 对应左括号 的映射
    stack = ArrayStack()

    for i, ch in enumerate(text):
        if ch in "([{":
            stack.push(ch)                    # 左括号：入栈等待配对
        elif ch in ")]}":
            if stack.is_empty():
                return False, f"位置 {i}: '{ch}' 没有左括号可配"
            if stack.pop() != pairs[ch]:
                return False, f"位置 {i}: '{ch}' 与栈顶括号类型不匹配"
        # 其他字符一律忽略，只关心括号

    if not stack.is_empty():
        return False, f"扫描结束仍有 {len(stack)} 个左括号未闭合"
    return True, "完全匹配"


def demo_brackets():
    print("== 第 3 段：括号匹配 ==")
    cases = [
        "(1 + 2) * [3 + (4 - 1)]",   # 正确嵌套
        "([)]",                       # 交叉嵌套：错误
        "((()))",                     # 深度嵌套：正确
        "(()",                        # 左括号多了
        "())",                        # 右括号多了
    ]
    for case in cases:
        ok, reason = is_balanced(case)
        print(f'  "{case}" -> {"匹配" if ok else "不匹配"}（{reason}）')
    print()


# ------------------------------------------------------------------
# 第 4 段：函数调用栈
# ------------------------------------------------------------------
def factorial(n):
    """递归阶乘：每一层调用都会压入调用栈，直观展示栈的进出过程。"""
    print(f"  进入 factorial({n})：这一层调用被压入调用栈")
    if n <= 1:
        print(f"  factorial({n}) 到达基线，开始逐层返回（弹栈）")
        return 1
    result = n * factorial(n - 1)  # 调用下一层时，本层的状态被"挂起"保存在栈帧里
    print(f"  factorial({n}) 弹栈，返回 {result}")
    return result


def demo_call_stack():
    print("== 第 4 段：函数调用栈 ==")
    print("递归计算 4! 的压栈/弹栈过程：")
    print("最终结果: 4! =", factorial(4))
    print("（如果递归太深没有出口，调用栈被压满就会 StackOverflow——"
          "这就是名字里 overflow 的含义）")
    print()


if __name__ == "__main__":
    demo_basic()
    demo_reverse_string()
    demo_brackets()
    demo_call_stack()
