"""
图（Graph）学习示例 —— 邻接表 + BFS + DFS
运行方式：python 示例代码.py
环境要求：Python 3.8+，仅使用标准库

本示例与《知识详解.md》的章节一一对应：
  第 1 段：邻接表表示法
  第 2 段：广度优先搜索（BFS）—— 一圈一圈向外扩
  第 3 段：无权图最短路径 —— BFS 顺带完成
  第 4 段：深度优先搜索（DFS）—— 一条路走到黑再回头
"""

from collections import deque


# ------------------------------------------------------------------
# 第 1 段：邻接表
# ------------------------------------------------------------------
class Graph:
    """无向图的邻接表表示。

    结构：{ 节点: [所有相邻节点, ...] }
    每个节点维护一张"邻居清单"，存图只需存这些清单。
    """

    def __init__(self):
        self.adj = {}  # dict: 节点 -> 邻居列表

    def add_node(self, node):
        # 先注册节点（哪怕暂时没有邻居），避免后续访问时 KeyError
        if node not in self.adj:
            self.adj[node] = []

    def add_edge(self, u, v):
        """无向边 u-v：两个方向都要登记（有向图只登记 u->v 一个方向）。"""
        self.add_node(u)
        self.add_node(v)
        if v not in self.adj[u]:      # 去重，防止重复 add_edge 出现重复邻居
            self.adj[u].append(v)
        if u not in self.adj[v]:
            self.adj[v].append(u)

    def __str__(self):
        # 每行一个节点和它的邻居，直观展示"谁的清单上有谁"
        return "\n".join(f"  {node} 的邻居: {nbrs}" for node, nbrs in self.adj.items())


def build_sample_graph():
    """构造示例图（详见《知识详解.md》中的示意图）。"""
    g = Graph()
    edges = [("A", "B"), ("A", "C"), ("B", "D"), ("B", "E"), ("C", "F"), ("E", "F")]
    for u, v in edges:
        g.add_edge(u, v)
    return g


# ------------------------------------------------------------------
# 第 2 段：BFS
# ------------------------------------------------------------------
def bfs(graph, start):
    """广度优先搜索：按"离起点由近到远"的层序访问。

    两件装备缺一不可：
      queue —— 待访问节点的排队处，保证同一层的先被处理（先来先服务）
      visited 集合 —— 已访问标记，防止重复入队导致死循环（图中有环！）
    """
    visited = {start}          # 入队的同时就标记，而不是出队时才标记，
    queue = deque([start])     # 否则同一节点可能被多个邻居重复入队
    order = []

    while queue:
        node = queue.popleft()  # 取队头：更早入队的 = 离起点更近的
        order.append(node)
        for neighbor in graph.adj[node]:
            if neighbor not in visited:
                visited.add(neighbor)  # 标记 + 入队
                queue.append(neighbor)
    return order


def demo_bfs():
    print("== 第 2 段：BFS 层序扩展 ==")
    g = build_sample_graph()
    print(g)
    print()

    order = bfs(g, "A")
    print("从 A 出发的 BFS 访问顺序:", order)
    # A 是第 0 层；B、C 是第 1 层；D、E、F 是第 2 层 ——
    # 输出顺序就是按层推进的："一圈一圈向外扩"
    print()


# ------------------------------------------------------------------
# 第 3 段：无权图最短路径
# ------------------------------------------------------------------
def bfs_shortest_path(graph, start):
    """BFS 顺带求出 start 到每个节点的最少边数。

    原理：节点第一次被访问时走过的层数就是最短距离——
    第 1 层的邻居都是 1 步可达，第 2 层都是 2 步……不可能更少，
    因为更少的层数早就被前面的层"抢先用掉"了。
    """
    dist = {start: 0}      # 距离表：起点到自己的距离为 0
    queue = deque([start])
    while queue:
        node = queue.popleft()
        for neighbor in graph.adj[node]:
            if neighbor not in dist:            # 还没算过距离的才处理
                dist[neighbor] = dist[node] + 1  # 邻居的距离 = 当前距离 + 1 条边
                queue.append(neighbor)
    return dist


def demo_shortest_path():
    print("== 第 3 段：最少边数（无权最短路径） ==")
    g = build_sample_graph()
    dist = bfs_shortest_path(g, "A")
    print("A 到各点的最少边数:", dist)
    print("  A->F 要 2 步：A-C-F 或 A-B-E-F（两条等长路径，BFS 只保证长度）")
    # 注意：边有权重时（如地图里程）BFS 不再适用，需要 Dijkstra 等算法，
    # 这是从本篇出发的自然延伸方向。
    print()


# ------------------------------------------------------------------
# 第 4 段：DFS
# ------------------------------------------------------------------
def dfs(graph, start):
    """深度优先搜索（迭代版，用栈代替递归，与《知识详解.md》的递归版对照）。

    栈的语义与 BFS 的队列刚好相反：
      队列 → 先处理老的 → 横向铺开（层序）
      栈   → 先处理新的 → 纵向深挖（一条路走到黑）
    """
    visited = set()
    stack = [start]  # 栈：最后压入的最先被处理
    order = []

    while stack:
        node = stack.pop()
        if node in visited:  # 同一节点可能被多个邻居压栈，弹出时要再查一次
            continue
        visited.add(node)
        order.append(node)
        # 逆序压栈是为了让"列表里靠前的邻居"更先被弹出处理，
        # 这样访问顺序与递归版 DFS 一致，方便对照
        for neighbor in reversed(graph.adj[node]):
            if neighbor not in visited:
                stack.append(neighbor)
    return order


def dfs_recursive(graph, node, visited=None, order=None):
    """递归版 DFS：代码最短，但深度过大时会栈溢出（默认递归上限约 1000 层）。"""
    if visited is None:
        visited, order = set(), []
    visited.add(node)
    order.append(node)
    for neighbor in graph.adj[node]:
        if neighbor not in visited:
            dfs_recursive(graph, neighbor, visited, order)  # 先深挖这个邻居到底
    return order


def demo_dfs():
    print("== 第 4 段：DFS 深挖与回溯 ==")
    g = build_sample_graph()
    print("从 A 出发的 DFS 访问顺序(迭代版):", dfs(g, "A"))
    print("从 A 出发的 DFS 访问顺序(递归版):", dfs_recursive(g, "A"))
    # 路径走向：A -> B -> D（走到黑）回溯 -> E -> F -> C
    # 与 BFS 的 A B C D E F 对比：BFS 按层铺开，DFS 一头扎到底再回头
    print()


if __name__ == "__main__":
    demo_bfs()
    demo_shortest_path()
    demo_dfs()
