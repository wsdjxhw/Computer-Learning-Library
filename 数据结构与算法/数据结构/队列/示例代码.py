"""
队列（Queue）学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：为什么 list 不适合做队列 —— pop(0) 的 O(n) 代价
  第 2 段：环形队列 —— 用取模运算让数组首尾相接
  第 3 段：collections.deque —— 工程实践中的标准选择
  第 4 段：队列的任务调度直觉 —— 先来先服务
"""

import time
from collections import deque


# ------------------------------------------------------------------
# 第 1 段：list.pop(0) 的问题
# ------------------------------------------------------------------
def demo_list_queue_problem():
    print("== 第 1 段：list 做队列的性能问题 ==")

    def bench(container_factory, n=30_000):
        """构造容器、依次入队再依次出队，返回总耗时（秒）。"""
        q = container_factory()
        for i in range(n):
            q.append(i)          # 入队：两者都在尾部追加，都很快
        start = time.perf_counter()
        while q:
            q.pop(0) if isinstance(q, list) else q.popleft()  # 出队：list 从头删，deque 从头删
        return time.perf_counter() - start

    list_time = bench(list)
    deque_time = bench(deque)

    # list.pop(0) 删除首元素后，后面所有元素都要整体前移一格 → 单次 O(n)
    # deque 是双向链表式块结构，两头增删都是 O(1)
    print(f"各执行 30000 次出队：")
    print(f"  list.pop(0):  {list_time * 1000:8.1f} 毫秒")
    print(f"  deque.popleft: {deque_time * 1000:7.1f} 毫秒")
    print("（数值因机器而异，但数量级差距稳定：list 随规模线性变慢，deque 几乎不变）")
    print()


# ------------------------------------------------------------------
# 第 2 段：环形队列
# ------------------------------------------------------------------
class CircularQueue:
    """固定容量的环形队列：把普通数组"弯"成一个环。

    设计要点：
      - 用 front 记录队头下标，用 count 记录当前元素个数；
        队尾位置不直接存，而是由 (front + count) % capacity 算出来。
      - 出队只移动 front，入队只写 rear 位置，两边都不搬移元素 → 都是 O(1)。
      - %（取模）是"绕环"的关键：下标越过末尾后自动回到开头。
    """

    def __init__(self, capacity):
        if capacity <= 0:
            raise ValueError("容量必须为正数")
        self._data = [None] * capacity  # 预分配固定大小，占位 None 表示空格
        self._capacity = capacity
        self._front = 0   # 队头元素所在下标
        self._count = 0   # 当前元素个数（0 表示空，等于容量表示满）

    def enqueue(self, item):
        """入队：放到"逻辑队尾"，即物理位置 (front + count) % capacity。"""
        if self._count == self._capacity:
            raise IndexError("队列已满（enqueue on full queue）")
        rear = (self._front + self._count) % self._capacity  # 取模实现首尾相接
        self._data[rear] = item
        self._count += 1

    def dequeue(self):
        """出队：取走 front 位置的元素，front 向前走一格（同样取模绕环）。"""
        if self._count == 0:
            raise IndexError("队列为空（dequeue on empty queue）")
        item = self._data[self._front]
        self._data[self._front] = None  # 清掉引用，便于观察，也利于垃圾回收
        self._front = (self._front + 1) % self._capacity
        self._count -= 1
        return item

    def __len__(self):
        return self._count

    def internal_view(self):
        """暴露内部数组状态，仅用于学习观察，工程代码不要这么干。"""
        return f"内部数组={self._data}, front={self._front}, count={self._count}"


def demo_circular_queue():
    print("== 第 2 段：环形队列的绕环过程 ==")
    q = CircularQueue(capacity=4)
    print("初始状态:", q.internal_view())

    for value in [10, 20, 30, 40]:
        q.enqueue(value)
    print("连入 10,20,30,40 装满后:", q.internal_view())

    # 队满再入队会明确报错 —— 环形队列是固定容量的，这是它和动态队列的区别
    try:
        q.enqueue(50)
    except IndexError as e:
        print("队满再入队:", e)

    # 出队两个：front 前移，前两个物理格子空出来
    print(f"出队 {q.dequeue()}, {q.dequeue()}")
    print("出队两个后:", q.internal_view())

    # 关键时刻：新元素 50 落在下标 0 —— "绕"回了数组开头，这就是环形
    q.enqueue(50)
    q.enqueue(60)
    print("再入 50, 60 后:", q.internal_view(), "← 50 绕回了下标 0")

    order = []
    while len(q) > 0:
        order.append(q.dequeue())
    print("全部出队顺序:", order, "→ 先进先出（FIFO）得到保证")
    print()


# ------------------------------------------------------------------
# 第 3 段：collections.deque
# ------------------------------------------------------------------
def demo_deque():
    print("== 第 3 段：标准库 deque ==")
    q = deque()

    # deque 的常规用法和 list 几乎一样，只是队头操作换成 popleft / appendleft
    for name in ["任务A", "任务B", "任务C"]:
        q.append(name)
        print(f"  入队 {name}, 当前队列: {list(q)}")

    print(f"  出队: {q.popleft()}（最早来的先走）")
    print(f"  出队: {q.popleft()}")
    print(f"  剩余: {list(q)}")

    # deque 也能当栈用（append + pop），两端都 O(1)；
    # 但它没有完全实现 list 的接口（如支持切片），各司其职即可
    print()


# ------------------------------------------------------------------
# 第 4 段：先来先服务的直觉
# ------------------------------------------------------------------
def demo_fifo_intuition():
    print("== 第 4 段：先来先服务 ==")
    queue = deque(["打印任务1", "打印任务2", "打印任务3"])

    print("打印机队列:", list(queue))
    while queue:
        current = queue.popleft()
        print(f"  正在处理: {current}（剩余 {len(queue)} 项）")

    # 队列天然表达"公平"：到达顺序即处理顺序。
    # BFS（图的广度优先搜索）按"离起点由近到远"扩展，用的也是同一个队列机制，
    # 详见 ../图/。
    print()


if __name__ == "__main__":
    demo_list_queue_problem()
    demo_circular_queue()
    demo_deque()
    demo_fifo_intuition()
