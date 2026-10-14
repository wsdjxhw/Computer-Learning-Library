"""
二叉搜索树（Binary Search Tree）学习示例
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：节点定义与 BST 性质 —— 左小右大
  第 2 段：插入与查找 —— 每一步排除一半
  第 3 段：中序遍历 —— 遍历结果天然有序
  第 4 段：删除 —— 三种情形
  第 5 段：退化问题 —— 顺序插入会让树变成链表
"""

from __future__ import annotations


# ------------------------------------------------------------------
# 第 1 段：节点定义
# ------------------------------------------------------------------
class TreeNode:
    """二叉树节点：最多两个孩子的树节点。"""

    def __init__(self, val):
        self.val = val
        self.left = None   # 左子树：所有值都小于本节点（BST 性质）
        self.right = None  # 右子树：所有值都大于本节点


class BinarySearchTree:
    def __init__(self):
        self.root = None

    # ------------------------------------------------------------------
    # 第 2 段：插入与查找
    # ------------------------------------------------------------------
    def insert(self, val):
        """递归插入。每层只和当前节点比一次：小往左、大往右，天然二分。

        insert 返回"子树的新根"，所以可以用
            node.left = self._insert(node.left, val)
        一行完成父节点与新子树的重新接线——这是树结构递归写法的通用套路。
        """
        self.root = self._insert(self.root, val)

    def _insert(self, node, val):
        # 递归出口：走到空位，说明这里就是新节点该待的地方
        if node is None:
            return TreeNode(val)
        if val < node.val:
            node.left = self._insert(node.left, val)   # 比当前小 → 去左子树找位置
        elif val > node.val:
            node.right = self._insert(node.right, val)  # 比当前大 → 去右子树
        # 相等则什么都不做：BST 通常不存重复值
        return node

    def search(self, val):
        """查找：同样每步排除一半，理想（树平衡）时 O(log n)。"""
        node = self.root
        path = []  # 记录走过的路径，便于观察"每步排除一半"
        while node:
            path.append(node.val)
            if val == node.val:
                return True, path
            node = node.left if val < node.val else node.right
        return False, path  # 走到 None 也没撞上 → 不存在

    # ------------------------------------------------------------------
    # 第 3 段：中序遍历
    # ------------------------------------------------------------------
    def inorder(self):
        """中序遍历：左 → 根 → 右。

        BST 性质保证这个顺序输出的值恰好是升序排列，
        这也是"BST 中序遍历有序"这一经典结论的现场验证。
        """
        result = []

        def visit(node):
            if node is None:
                return
            visit(node.left)       # 1) 先把左子树全部输出（都更小）
            result.append(node.val)  # 2) 再输出当前节点
            visit(node.right)      # 3) 最后输出右子树（都更大）

        visit(self.root)
        return result

    # ------------------------------------------------------------------
    # 第 4 段：删除
    # ------------------------------------------------------------------
    def delete(self, val):
        self.root = self._delete(self.root, val)

    def _delete(self, node, val):
        if node is None:
            return None  # 没找到要删的值，直接返回空树
        if val < node.val:
            node.left = self._delete(node.left, val)
        elif val > node.val:
            node.right = self._delete(node.right, val)
        else:
            # 找到了。按孩子个数分三种情形处理：
            # 情形 1 & 2：0 个或 1 个孩子 —— 用孩子（或空）直接顶替自己
            if node.left is None:
                return node.right
            if node.right is None:
                return node.left
            # 情形 3：2 个孩子 —— 找"右子树的最小值"（中序后继）来接班。
            # 它大于左子树全部、又小于右子树其余部分，是唯一能稳住排序的位置。
            successor = node.right
            while successor.left:  # 右子树一路向左走到底就是最小值
                successor = successor.left
            node.val = successor.val                          # 用后继值覆盖自己
            node.right = self._delete(node.right, successor.val)  # 再删掉重复的后继
        return node


def build_tree(values):
    """按列表顺序插入建树，返回 BST。"""
    bst = BinarySearchTree()
    for v in values:
        bst.insert(v)
    return bst


def demo_insert_search():
    print("== 第 2 段：插入与查找 ==")
    bst = build_tree([50, 30, 70, 20, 40, 60, 80])
    print("依次插入 50, 30, 70, 20, 40, 60, 80")

    # 查找路径演示：每比较一次就排除一半的子树
    found, path = bst.search(40)
    print(f"查找 40: 找到={found}, 走过的节点 {path}")
    found, path = bst.search(45)
    print(f"查找 45: 找到={found}, 走过的节点 {path}（走到头也没有，仅 3 步排除）")
    print()


def demo_inorder():
    print("== 第 3 段：中序遍历 ==")
    bst = build_tree([50, 30, 70, 20, 40, 60, 80])
    print("插入顺序: [50, 30, 70, 20, 40, 60, 80]")
    print("中序遍历结果:", bst.inorder(), "→ 与插入顺序无关，输出总是升序")
    print()


def demo_delete():
    print("== 第 4 段：删除的三种情形 ==")
    bst = build_tree([50, 30, 70, 20, 40, 60, 80])
    print("初始中序:", bst.inorder())

    # 情形 1：删除叶子节点 20 —— 直接摘除，不影响其他节点
    bst.delete(20)
    print("删除叶子 20 后:", bst.inorder())

    # 情形 2：删除只有一个孩子的节点 30（此时它的孩子只剩 40）—— 孩子上位顶替
    bst.delete(30)
    print("删除单孩子节点 30 后:", bst.inorder())

    # 情形 3：删除有两个孩子的节点 50（根，孩子是 40 和 70）
    # 右子树最小值 60 接班成为新根，整棵树依然保持左小右大
    bst.delete(50)
    print("删除双孩子节点 50 后:", bst.inorder(), "← 60 接班，仍然升序")
    print()


# ------------------------------------------------------------------
# 第 5 段：退化问题
# ------------------------------------------------------------------
def height(node):
    """递归计算树高：空树为 0。树高越低，查找越快。"""
    if node is None:
        return 0
    return 1 + max(height(node.left), height(node.right))


def demo_degeneration():
    print("== 第 5 段：顺序插入导致退化 ==")
    # 同样 7 个值，插入顺序不同，树的形态截然不同
    balanced = build_tree([50, 30, 70, 20, 40, 60, 80])  # 从中间开始插 → 矮胖
    degenerate = build_tree([20, 30, 40, 50, 60, 70, 80])  # 升序插入 → 每个都往右塞

    print("两组数据都存了 7 个节点：")
    print(f"  [50,30,70,20,40,60,80] 建树 → 树高 = {height(balanced.root)}")
    print(f"  [20,30,40,50,60,70,80] 建树 → 树高 = {height(degenerate.root)}")

    # 升序插入时每个新节点都比之前所有的大，一路向右，BST 退化成"链表"，
    # 查找从 O(log n) 恶化到 O(n)。这正是平衡树（AVL、红黑树）要解决的问题：
    # 它们在插入/删除时主动旋转，把树高压在 O(log n) 以内。
    found, path = degenerate.search(80)
    print(f"  在退化的树里查找 80 需要走 {len(path)} 步（撞上了每个节点）")
    print()


if __name__ == "__main__":
    demo_insert_search()
    demo_inorder()
    demo_delete()
    demo_degeneration()
