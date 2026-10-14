"""
堆（Heap）学习示例 —— 以最小堆为例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：堆的数组表示 —— 下标即结构
  第 2 段：上浮与下沉 —— 堆的两个核心动作
  第 3 段：push 与 pop —— 完整的堆操作
  第 4 段：堆排序直觉 —— 反复弹出即有序
  第 5 段：Top-K 问题 —— 用小堆维护"最大的 K 个"
  第 6 段：标准库 heapq
"""

import heapq


class MinHeap:
    """最小堆：父节点的值总是 <= 两个孩子。用数组存储，不建指针。"""

    def __init__(self):
        self._data = []

    # ------------------------------------------------------------------
    # 第 1 段：下标换算
    # ------------------------------------------------------------------
    @staticmethod
    def _parent(i):
        # 父节点下标 = (i - 1) // 2，整除自动处理了左右孩子两种情况
        return (i - 1) // 2

    @staticmethod
    def _children(i):
        # 左孩子 2i+1，右孩子 2i+2：由"从上到下、从左到右"编号自然得出
        return 2 * i + 1, 2 * i + 2

    def __len__(self):
        return len(self._data)

    def internal_view(self):
        return str(self._data)

    # ------------------------------------------------------------------
    # 第 2 段：上浮与下沉
    # ------------------------------------------------------------------
    def _sift_up(self, i):
        """上浮：新元素加在数组末尾后，与父节点比大小，比父小就一路换上去。"""
        while i > 0:
            p = self._parent(i)
            if self._data[i] < self._data[p]:
                # Python 的多元赋值无需临时变量即可完成交换
                self._data[i], self._data[p] = self._data[p], self._data[i]
                i = p  # 换到父位置后，继续和"祖父"比较
            else:
                break  # 不比父小 → 有序了，提前结束（这是堆高效的关键）

    def _sift_down(self, i):
        """下沉：堆顶被换成数组末尾元素后，与较小的孩子比，比孩子大就一路换下来。"""
        n = len(self._data)
        while True:
            left, right = self._children(i)
            smallest = i
            # 找出"自己和两个孩子"三者中的最小者
            if left < n and self._data[left] < self._data[smallest]:
                smallest = left
            if right < n and self._data[right] < self._data[smallest]:
                smallest = right
            if smallest == i:
                break  # 自己已经是最小 → 堆性质恢复，提前结束
            self._data[i], self._data[smallest] = self._data[smallest], self._data[i]
            i = smallest  # 继续在新的位置向下检查

    # ------------------------------------------------------------------
    # 第 3 段：push 与 pop
    # ------------------------------------------------------------------
    def push(self, value):
        """入堆：先放数组末尾（保持完全二叉树形态），再上浮恢复堆性质。"""
        self._data.append(value)
        self._sift_up(len(self._data) - 1)

    def pop(self):
        """出堆：取走堆顶（最小值），末尾元素补位，再下沉恢复堆性质。"""
        if not self._data:
            raise IndexError("pop from empty heap")
        top = self._data[0]
        last = self._data.pop()  # 先摘掉末尾元素
        if self._data:  # 堆还有剩余时才需要补位
            self._data[0] = last  # 末尾元素顶到堆顶（形态仍是完全二叉树）
            self._sift_down(0)    # 下沉找回堆性质
        return top

    def peek(self):
        if not self._data:
            raise IndexError("peek from empty heap")
        return self._data[0]

    def values(self):
        """返回堆内所有值的副本（无序），供外部观察，不暴露内部数组本身。"""
        return list(self._data)


def demo_structure():
    print("== 第 1 段：数组表示堆 ==")
    h = MinHeap()
    for v in [9, 4, 7, 1, 8, 2]:
        h.push(v)

    # 对应的完全二叉树形态（画出来对照下标）：
    #            1[0]
    #          /      \
    #        4[1]     2[2]
    #       /   \    /
    #     9[3] 8[4] 7[5]
    print("依次 push 9,4,7,1,8,2 后内部数组:", h.internal_view())
    print("                树形态:    1")
    print("                        /   \\")
    print("                       4     2")
    print("                      / \\   /")
    print("                     9   8 7")
    print("父(i) = (i-1)//2, 左孩子 = 2i+1, 右孩子 = 2i+2 —— 下标就是树的结构")
    print("每个父节点都 <= 它的孩子 → 最小值永远在 data[0]:", h.peek())
    print()


def demo_operations():
    print("== 第 3 段：push 与 pop ==")
    h = MinHeap()
    for v in [9, 4, 7, 1, 8, 2]:
        h.push(v)
        print(f"  push {v} 后数组: {h.internal_view()}（最小值始终在下标 0）")

    print(f"  pop 得到 {h.pop()}，数组变为 {h.internal_view()}")
    print(f"  pop 得到 {h.pop()}，数组变为 {h.internal_view()}")
    # push/pop 的代价只与树高有关 → O(log n)
    print()


def demo_heap_sort():
    print("== 第 4 段：反复弹出即有序 ==")
    h = MinHeap()
    data = [9, 4, 7, 1, 8, 2, 6, 5]
    for v in data:
        h.push(v)

    # 每次 pop 都取出当前最小值，依次弹出天然升序 —— 这就是堆排序的主循环
    ordered = []
    while len(h):
        ordered.append(h.pop())
    print(f"乱序: {data}")
    print(f"全部弹出后: {ordered}")
    print()


def demo_top_k():
    print("== 第 5 段：Top-K —— 从 8 个数里找最大的 3 个 ==")
    data = [9, 4, 7, 1, 8, 2, 6, 5]
    k = 3

    # 直觉做法：全排序取前 3 → O(n log n)
    print(f"全排序取前 {k}: {sorted(data, reverse=True)[:k]}")

    # 堆做法：维护一个容量为 k 的最小堆，堆顶是"目前第 k 大"的守门员。
    # 新元素只要打过守门员（大于堆顶）就挤进去，否则直接丢弃。
    h = MinHeap()
    for v in data:
        if len(h) < k:
            h.push(v)          # 前 k 个先无脑入堆
        elif v > h.peek():
            h.pop()            # 把守门员（当前第 k 大）踢出去
            h.push(v)          # 新人入堆并下沉到正确位置
    print(f"小堆维护 Top-{k}: {sorted(h.values(), reverse=True)}")

    # n 很大、k 很小时（如"10 亿条日志取前 100"），
    # 堆法 O(n log k) 不需要全排序，内存也只占 k 个位置。
    print()


def demo_heapq():
    print("== 第 6 段：标准库 heapq ==")
    # heapq 操作的正是普通 list，所有函数第一个参数是列表
    nums = [9, 4, 7, 1, 8, 2]
    heapq.heapify(nums)  # 原地把无序列表调整成堆，O(n)
    print("heapify 后:", nums, "→ 最小值在下标 0")

    heapq.heappush(nums, 0)
    print("heappush 0 后:", nums)
    print("heappop 弹出:", heapq.heappop(nums))
    print("nlargest(3):", heapq.nlargest(3, nums), "/ nsmallest(3):", heapq.nsmallest(3, nums))
    # Java 的 PriorityQueue、C++ 的 priority_queue 是各语言里的同类设施。
    print()


if __name__ == "__main__":
    demo_structure()
    demo_operations()
    demo_heap_sort()
    demo_top_k()
    demo_heapq()
