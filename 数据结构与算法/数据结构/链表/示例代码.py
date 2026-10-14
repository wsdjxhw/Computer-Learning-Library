"""
单向链表学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：节点与链表的结构定义
  第 2 段：头插 O(1) 与尾插 O(n)
  第 3 段：查找必须沿指针逐个走
  第 4 段：删除 —— 前驱节点接手法
  第 5 段：迭代反转链表
"""

from __future__ import annotations


# ------------------------------------------------------------------
# 第 1 段：节点与链表的定义
# ------------------------------------------------------------------
class Node:
    """链表的节点：一个存数据的地方 + 一个存"下一站"地址的地方。

    与数组的根本区别：节点在内存中不必相邻，彼此只靠 next 指针串联。
    """

    def __init__(self, data):
        self.data = data   # 数据域：真正存的内容
        self.next = None   # 指针域：指向下一个节点；None 表示"到头了"

    def __repr__(self):
        return f"Node({self.data})"


class SinglyLinkedList:
    """单向链表：只持有 head（头节点），其余节点顺藤摸瓜找到。"""

    def __init__(self):
        self.head = None  # 空链表的头指针为 None

    def to_list(self):
        """遍历链表，把数据收集成 Python 列表方便打印观察。"""
        result = []
        current = self.head
        while current:            # 只要没走到 None 就继续
            result.append(current.data)
            current = current.next  # 关键动作：沿指针走到下一站
        return result

    def __str__(self):
        # 用 "10 -> 20 -> 3 -> NULL" 的形式直观展示链
        return " -> ".join(str(x) for x in self.to_list()) + " -> NULL" if self.head else "空链表"


# ------------------------------------------------------------------
# 第 2 段：头插 O(1) 与尾插 O(n)
# ------------------------------------------------------------------
def demo_insert():
    print("== 第 2 段：头插与尾插 ==")
    ll = SinglyLinkedList()

    # 头插法：新节点接管 head，两步完成，与链表长度无关 → O(1)
    # 注意顺序：必须先把新节点的 next 指向原头节点，再让 head 指向新节点。
    # 如果反过来先改 head，原链表就"失联"再也找不回来了。
    for value in [30, 20, 10]:
        node = Node(value)
        node.next = ll.head
        ll.head = node
    print("依次头插 30, 20, 10 后:", ll, "（后插入的排前面）")

    # 尾插法：没有尾指针时必须从头走到最后一个节点 → O(n)
    def insert_tail(linked, value):
        new_node = Node(value)
        if linked.head is None:      # 边界情况：空链表，新节点直接成为头
            linked.head = new_node
            return
        current = linked.head
        while current.next:          # 停在"最后一个节点"（next 为 None 的那个）
            current = current.next
        current.next = new_node      # 把新节点接到尾部

    insert_tail(ll, 40)
    print("尾插 40 后:", ll)
    print()


# ------------------------------------------------------------------
# 第 3 段：查找
# ------------------------------------------------------------------
def demo_find():
    print("== 第 3 段：查找必须逐个走 ==")
    ll = SinglyLinkedList()
    for value in [10, 20, 30, 40]:
        # 复用第 2 段的尾插逻辑构造有序链表
        current = ll.head
        if current is None:
            ll.head = Node(value)
            continue
        while current.next:
            current = current.next
        current.next = Node(value)

    # 链表没有下标！想找 30 只能从 head 开始沿 next 逐个比对，O(n)。
    # 这也是链表不支持二分查找的原因：无法 O(1) 跳到中间。
    target, steps = 30, 0
    current = ll.head
    while current and current.data != target:
        steps += 1
        current = current.next
    print(f"在 {ll} 中查找 {target}")
    print(f"  结果: {current}, 走了 {steps + 1} 步")
    print()


# ------------------------------------------------------------------
# 第 4 段：删除 —— 前驱节点接手法
# ------------------------------------------------------------------
def demo_delete():
    print("== 第 4 段：删除节点 ==")
    ll = SinglyLinkedList()
    for value in [10, 20, 30, 40]:
        node = Node(value)
        if ll.head is None:
            ll.head = node
        else:
            current = ll.head
            while current.next:
                current = current.next
            current.next = node

    def delete_by_value(linked, target):
        """删除第一个值等于 target 的节点。核心：让前驱节点的 next 跨过被删节点。"""
        # 边界 1：删除头节点 —— 没有前驱，直接把 head 后移一位
        if linked.head and linked.head.data == target:
            removed = linked.head
            linked.head = linked.head.next
            removed.next = None  # 断开引用，便于理解（非必需）
            return True

        prev = linked.head
        # 同时追踪"前驱 prev"和"当前 current"两个指针，是链表操作的通用套路
        while prev.next and prev.next.data != target:
            prev = prev.next
        if prev.next is None:
            return False  # 走到头也没找到
        # 接手：prev.next 越过被删节点，直接指向它的下一个
        # prev.next.next 若为 None（删尾节点），此写法同样成立
        prev.next = prev.next.next
        return True

    print("初始链表:", ll)
    print("删除头节点 10:", demo_delete_helper(ll, delete_by_value, 10))
    print("删除中间节点 30:", demo_delete_helper(ll, delete_by_value, 30))
    print("删除不存在的 99:", demo_delete_helper(ll, delete_by_value, 99))
    print()


def demo_delete_helper(ll, fn, target):
    """小工具：调用删除函数并返回可读结果。"""
    return f"{fn(ll, target)} → {ll}"


# ------------------------------------------------------------------
# 第 5 段：迭代反转链表
# ------------------------------------------------------------------
def demo_reverse():
    print("== 第 5 段：迭代反转 ==")
    ll = SinglyLinkedList()
    for value in [1, 2, 3, 4, 5]:
        node = Node(value)
        if ll.head is None:
            ll.head = node
        else:
            current = ll.head
            while current.next:
                current = current.next
            current.next = node
    print("反转前:", ll)

    # 三指针法：prev（已反转部分的头）、current（当前处理）、nxt（先记住下一个）
    # 每一步只做一件事：把 current 的指针掉头指向 prev，然后三个指针整体右移一格。
    # 顺序很关键：必须先把 current.next 存进 nxt，再改 current.next，
    # 否则一掉头，后面的节点就全丢了。
    prev, current = None, ll.head
    while current:
        nxt = current.next      # 1) 暂存下一站
        current.next = prev     # 2) 掉头：指向前驱
        prev = current          # 3) prev 前进
        current = nxt           # 4) current 前进
    ll.head = prev              # 循环结束时 prev 正是原链表的尾节点（新头）
    print("反转后:", ll)
    print()


if __name__ == "__main__":
    demo_insert()
    demo_find()
    demo_delete()
    demo_reverse()
