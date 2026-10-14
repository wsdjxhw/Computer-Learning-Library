"""
数组（动态数组）学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：下标随机访问为什么是 O(1)
  第 2 段：插入与删除为什么是 O(n) —— 手动搬移元素
  第 3 段：动态数组的扩容 —— 观察内存占用的阶梯式增长
  第 4 段：线性查找与遍历
  第 5 段：浅拷贝的别名陷阱
"""

import copy
import sys


# ------------------------------------------------------------------
# 第 1 段：下标随机访问
# ------------------------------------------------------------------
def demo_random_access():
    print("== 第 1 段：下标随机访问 ==")
    scores = [90, 85, 78, 92, 66]
    print("数组内容:", scores)

    # 核心机制：地址 = 起始地址 + 下标 × 单个元素大小
    # 无论数组有 5 个还是 500 万个元素，这个公式都只算一次，
    # 所以按下标访问的时间复杂度是 O(1)。
    print("scores[0] =", scores[0])
    print("scores[4] =", scores[4])

    # 负下标从末尾往前数，是 Python 提供的便利写法：scores[-1] 等价于 scores[len-1]
    print("scores[-1] =", scores[-1], "（负下标：倒数第 1 个）")

    # 下标超出 [0, len-1] 范围会抛 IndexError，这是数组最典型的越界错误
    try:
        _ = scores[10]
    except IndexError as e:
        print("访问 scores[10] 触发越界:", e)
    print()


# ------------------------------------------------------------------
# 第 2 段：插入与删除 —— 手动搬移元素
# ------------------------------------------------------------------
# 说明：Python 的 list 自带 insert/pop 方法，它们内部同样要做搬移。
# 这里故意手写搬移过程，把 O(n) 的代价"放大"给读者看。
def insert_at(arr, index, value):
    """在 index 位置插入 value，模拟底层实现中的元素搬移。"""
    # 先追加一个占位元素把数组撑长 1 格（容量问题见第 3 段）
    arr.append(None)
    # 从最后一个"有效"元素开始，逐个向后挪一格，给 index 位置腾出空位。
    # 注意方向：必须从后往前挪；若从前往后挪，前面的值会先覆盖掉后面还没搬的值。
    for i in range(len(arr) - 2, index - 1, -1):
        arr[i + 1] = arr[i]
    arr[index] = value


def delete_at(arr, index):
    """删除 index 位置的元素并返回它，同样手动搬移。"""
    removed = arr[index]
    # 从删除点开始，把后面的每个元素向前挪一格补上空洞
    for i in range(index, len(arr) - 1):
        arr[i] = arr[i + 1]
    arr.pop()  # 挪完后末尾多出一个重复元素，把它去掉
    return removed


def demo_insert_delete():
    print("== 第 2 段：插入与删除的搬移过程 ==")
    nums = [10, 20, 30, 40]
    print("初始数组:", nums)

    # 在下标 1 处插入 15：需要把 20、30、40 全部向后搬一格，移动了 3 个元素
    insert_at(nums, 1, 15)
    print("在下标 1 插入 15 后:", nums)

    # 删除下标 0 的元素：需要把后面 4 个元素全部向前搬一格
    deleted = delete_at(nums, 0)
    print(f"删除下标 0 的元素 {deleted} 后:", nums)

    # 结论：插入/删除平均要搬移约 n/2 个元素，时间复杂度 O(n)；
    # 只有在数组末尾操作（append / pop()）才可能免于搬移。
    print()
    return nums


# ------------------------------------------------------------------
# 第 3 段：动态数组的扩容
# ------------------------------------------------------------------
def demo_dynamic_growth():
    print("== 第 3 段：动态扩容的阶梯式增长 ==")
    nums = []
    last_size = sys.getsizeof(nums)  # getsizeof 返回对象当前占用的字节数
    print(f"空列表占用: {last_size} 字节")

    # 逐个 append，只在内存占用发生变化时打印。
    # 预期现象：字节数不是每次 +一点点，而是长时间不变、然后突然跳一档——
    # 这就是"容量已满 → 申请新的更大空间 → 整体搬迁"的扩容时刻。
    for i in range(1, 31):
        nums.append(i)
        size = sys.getsizeof(nums)
        if size != last_size:
            print(f"append 第 {i:2d} 个元素时，占用 {last_size} -> {size} 字节")
            last_size = size
    print("（具体字节数因 Python 版本而异，关键看阶梯式跳变）")
    print()


# ------------------------------------------------------------------
# 第 4 段：线性查找与遍历
# ------------------------------------------------------------------
def demo_linear_search():
    print("== 第 4 段：线性查找 ==")
    nums = [10, 20, 30, 40, 15, 25]
    target = 15
    found_index = -1
    comparisons = 0

    # 数组本身无序时，只能从头到尾逐个比对，这叫线性查找，复杂度 O(n)
    for i, value in enumerate(nums):
        comparisons += 1
        if value == target:
            found_index = i
            break  # 找到后立即结束，避免多余比较

    print(f"在 {nums} 中查找 {target}:")
    print(f"  找到，下标 = {found_index}，比较了 {comparisons} 次")
    # 最坏情况（目标不存在或在末尾）要比较 n 次；这是"无序数组只能 O(n) 查找"的原因，
    # 也是后续"哈希表 O(1) 查找""二分查找 O(log n)"要解决的问题。
    print()


# ------------------------------------------------------------------
# 第 5 段：浅拷贝的别名陷阱
# ------------------------------------------------------------------
def demo_aliasing():
    print("== 第 5 段：赋值、浅拷贝与深拷贝 ==")
    a = [1, 2, [3, 4]]

    # 直接赋值不复制数组，只是让 b 指向同一个对象（"别名"）
    b = a
    b[0] = 99
    print("b = a 后修改 b[0]，a[0] 也变成:", a[0], "→ a 和 b 是同一个数组")

    # 浅拷贝（切片或 copy()）会创建新的外层数组，
    # 但内层子对象仍被共享——修改嵌套列表会互相影响
    c = a.copy()
    c[2].append(5)
    print("浅拷贝 c 修改内层列表后，a 的内层也变成:", a[2], "→ 内层仍共享")

    # 深拷贝递归复制所有层级，两棵"数组树"完全独立
    d = copy.deepcopy(a)
    d[2].append(6)
    print("深拷贝 d 修改内层列表后，a 的内层仍是:", a[2], "→ 完全独立")
    print()


if __name__ == "__main__":
    demo_random_access()
    demo_insert_delete()
    demo_dynamic_growth()
    demo_linear_search()
    demo_aliasing()
