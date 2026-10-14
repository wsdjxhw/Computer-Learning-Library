"""
哈希表（Hash Table）学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：哈希函数做什么 —— 把任意键变成数组下标
  第 2 段：手写链地址法哈希表 —— 冲突时把元素挂成桶内链表
  第 3 段：负载因子与自动扩容
  第 4 段：键必须可哈希 —— 不可变类型的意义
  第 5 段：与 Python dict 的对照
"""


# ------------------------------------------------------------------
# 第 1 段：哈希函数
# ------------------------------------------------------------------
def demo_hash_function():
    print("== 第 1 段：哈希函数 ==")

    # Python 内置 hash() 把任意对象映射为一个整数（同一个进程内稳定）
    for key in ["apple", "banana", "apple"]:
        print(f'  hash("{key}") = {hash(key)}')

    # 相同的键必然得到相同的哈希值（这是"同一个键总能找到同一个桶"的前提）
    print("  → 两个 'apple' 的哈希值相同")

    # 哈希值可能非常大，甚至为负数；用 % 桶数 折算成合法下标
    bucket_count = 4
    for key in ["apple", "banana", "cherry"]:
        print(f'  "{key}" 落入桶 {hash(key) % bucket_count}（容量 {bucket_count}）')
    # 注意：每次运行程序，字符串的哈希值可能不同（Python 出于安全默认加盐），
    # 但"同键同哈希、同桶"的规律不变，所以下方手写表使用自己的稳定哈希函数。
    print()


# ------------------------------------------------------------------
# 第 2 段：手写链地址法哈希表
# ------------------------------------------------------------------
class SimpleHashTable:
    """链地址法（separate chaining）哈希表。

    结构：一个桶数组，每个桶是一条 [(键, 值), ...] 的列表。
    冲突的键值对就排在同一个桶里，查找时在桶内逐个比对键。
    """

    def __init__(self, capacity=4):
        self._capacity = capacity
        self._buckets = [[] for _ in range(capacity)]  # 每个桶初始为空列表
        self._size = 0                                  # 实际存了多少个键值对

    def _index(self, key):
        """确定 key 属于哪个桶。字符串用 polynomial rolling hash 保证稳定可复现。"""
        h = 0
        for ch in str(key):
            # 经典多项式哈希：h = h*31 + 字符编码，能比"字符编码求和"更均匀
            h = (h * 31 + ord(ch)) % self._capacity
        return h

    def put(self, key, value):
        """插入或更新键值对。"""
        bucket = self._buckets[self._index(key)]
        # 先看桶里是否已有同键元素：有则更新（哈希表"一键一值"）
        for i, (k, _) in enumerate(bucket):
            if k == key:
                bucket[i] = (key, value)
                return
        bucket.append((key, value))  # 桶里没有 → 追加（可能产生冲突链）
        self._size += 1
        self._maybe_resize()

    def get(self, key):
        """按键取值；不存在时抛 KeyError，与 dict 行为一致。"""
        bucket = self._buckets[self._index(key)]
        for k, v in bucket:  # 最理想 1 步命中；最坏要走过整条冲突链
            if k == key:
                return v
        raise KeyError(key)

    def remove(self, key):
        """按键删除；不存在时抛 KeyError。"""
        bucket = self._buckets[self._index(key)]
        for i, (k, _) in enumerate(bucket):
            if k == key:
                bucket.pop(i)
                self._size -= 1
                return
        raise KeyError(key)

    def _maybe_resize(self):
        """负载因子 = 元素数 / 桶数。超过 0.75 就把桶数翻倍并重新分布所有元素。"""
        if self._size / self._capacity <= 0.75:
            return
        old_items = [(k, v) for bucket in self._buckets for (k, v) in bucket]
        self._capacity *= 2
        self._buckets = [[] for _ in range(self._capacity)]  # 全新空桶数组
        self._size = 0
        for k, v in old_items:
            self.put(k, v)  # 逐个重新哈希到新桶（rehash）

    def __len__(self):
        return self._size

    def debug_view(self):
        """打印内部桶结构，仅用于学习观察。"""
        lines = []
        for i, bucket in enumerate(self._buckets):
            lines.append(f"桶{i}: {bucket if bucket else '(空)'}")
        return "\n         ".join(lines)


def demo_hand_written():
    print("== 第 2 段：链地址法哈希表 ==")
    table = SimpleHashTable(capacity=4)
    data = {"apple": 3, "banana": 5, "cherry": 2, "date": 7, "elderberry": 1, "fig": 9}
    for k, v in data.items():
        table.put(k, v)

    # 初始容量 4，插入第 4 个键时负载因子达到 1.0 > 0.75，已自动扩容到 8（详见第 3 段）
    print(f"存入 {len(data)} 个键值对后（初始容量 4，现容量 {len(table._buckets)}）:")
    print("         " + table.debug_view())
    # 可以看到 'elderberry' 和 'fig' 落进了同一个桶 —— 这就是"哈希冲突"，
    # 链地址法的处理方式是让冲突元素在桶内排队，而不是覆盖。

    print("  get('cherry') =", table.get("cherry"))
    table.put("banana", 10)  # 同键再次 put 是更新
    print("  put('banana', 10) 后 get('banana') =", table.get("banana"))
    table.remove("cherry")
    try:
        table.get("cherry")
    except KeyError:
        print("  remove('cherry') 后再 get('cherry') 抛出 KeyError（键已不存在）")
    print()


# ------------------------------------------------------------------
# 第 3 段：负载因子与扩容
# ------------------------------------------------------------------
def demo_resize():
    print("== 第 3 段：自动扩容 ==")
    table = SimpleHashTable(capacity=4)
    print("初始容量 4。逐个插入 a~h：")
    for i, key in enumerate("abcdefgh"):
        before = len(table._buckets)
        table.put(key, i)
        after = len(table._buckets)
        marker = f"  插入 '{key}' 后元素数={len(table)}, 容量 {before} -> {after}"
        print(marker + ("（触发扩容+rehash）" if after != before else ""))

    # 扩容让每个键重新均匀散开，冲突链变短，后续查找更接近 O(1)。
    # 代价：扩容那一刻要把所有元素重新哈希，是一次 O(n) 的集中开销。
    print()
    print("扩容后的桶分布:")
    print("         " + table.debug_view())
    print()


# ------------------------------------------------------------------
# 第 4 段：键必须可哈希
# ------------------------------------------------------------------
def demo_hashable():
    print("== 第 4 段：可哈希性 ==")
    # 列表是可变对象，内容随时能改；如果允许它做键，
    # 放入桶后再修改内容，哈希值就变了，将永远找不到它 → Python 直接禁止
    try:
        hash([1, 2])
    except TypeError as e:
        print("hash([1, 2]) 抛 TypeError:", e)

    # 元组和字符串、数字一样不可变，内容定死后哈希值稳定 → 可做键
    print("hash((1, 2)) =", hash((1, 2)), "→ 元组可作为键")

    # 工程惯例：需要"列表状"的键时，先转成元组
    lookup = {(1, 2): "格子内容"}
    print("用元组做键查 dict:", lookup[(1, 2)])
    print()


# ------------------------------------------------------------------
# 第 5 段：与 dict 对照
# ------------------------------------------------------------------
def demo_dict():
    print("== 第 5 段：dict 就是最成熟的哈希表 ==")
    ages = {"alice": 30, "bob": 25}

    # dict 的增删改查就是哈希表操作的标准形态，且大概率是 O(1)
    ages["carol"] = 28          # 插入
    ages["alice"] = 31          # 更新
    _ = ages.pop("bob")  # 删除
    print("dict 操作后:", ages)

    # 常用方法背后都是哈希查找
    print("'carol' in ages:", "carol" in ages)      # O(1) 成员判断
    print("ages.get('dave', 默认0):", ages.get("dave", 0))  # 不抛错的取值
    print()
    # 大多数语言的标准库同理：Java 的 HashMap、Go 的 map、C++ 的 unordered_map。


if __name__ == "__main__":
    demo_hash_function()
    demo_hand_written()
    demo_resize()
    demo_hashable()
    demo_dict()
