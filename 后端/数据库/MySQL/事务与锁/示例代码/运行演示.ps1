# =====================================================================
# 事务与锁 · 并发演示驱动脚本
# 用法（在 PowerShell 中，位于 示例代码/ 目录）：
#   .\运行演示.ps1 -Demo 1 -User root -Password 你的密码
#   .\运行演示.ps1 -Demo All -User root -Password 你的密码     # 跑全部 1~9
#   .\运行演示.ps1 -Demo 1 -User root -Password 你的密码 -Rebuild  # 先重建演示库
# 说明：
#   会同时启动两个 mysql 连接分别执行 会话A/会话B 脚本（SLEEP 已对齐时间线），
#   输出保存为 输出_演示N_A/B.txt 并打印到屏幕。
#   mysql 需在 PATH 中（默认安装路径 C:\Program Files\MySQL\MySQL Server 8.0\bin）。
# =====================================================================
param(
    [Parameter(Mandatory = $true)][string]$Demo,   # 1~9 或 All
    [string]$User = "root",
    [string]$Password = "",
    [string]$ServerHost = "127.0.0.1",
    [int]$Port = 3306,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path

$common = "--no-defaults -h $ServerHost -P $Port -u $User --default-character-set=utf8mb4 -t --comments --force"
# --force：演示5/6/7 的会话B 有一条“预期报错”（1205/1213），加它才能继续执行后面的语句
if ($Password -ne "") { $common += " -p$Password" }

if ($Rebuild) {
    Write-Host "== 重建 learn_tx 演示库 ==" -ForegroundColor Cyan
    cmd /c "mysql $common < `"$dir\00_建库与数据.sql`""
}

# 单会话演示：9（保存点）
$singleDemos = @(9)

function Invoke-PairDemo([int]$n) {
    $a = Get-Item (Join-Path $dir ("演示{0}_*_会话A.sql" -f $n)) | Select-Object -First 1
    $b = Get-Item (Join-Path $dir ("演示{0}_*_会话B.sql" -f $n)) | Select-Object -First 1
    if (-not $a -or -not $b) { Write-Error "找不到演示 $n 的会话文件"; exit 1 }
    $outA = Join-Path $dir ("输出_演示{0}_A.txt" -f $n)
    $outB = Join-Path $dir ("输出_演示{0}_B.txt" -f $n)

    Write-Host ("== 演示{0}：两个会话并发执行（约 8 秒）==" -f $n) -ForegroundColor Cyan
    # 用 cmd 的 < > 重定向，避免 PowerShell 管道对中文的编码转换
    $pA = Start-Process cmd -ArgumentList "/c mysql $common < `"$($a.FullName)`" > `"$outA`" 2>&1" -PassThru -WindowStyle Hidden
    $pB = Start-Process cmd -ArgumentList "/c mysql $common < `"$($b.FullName)`" > `"$outB`" 2>&1" -PassThru -WindowStyle Hidden
    $pA.WaitForExit(); $pB.WaitForExit()

    Write-Host "----- 会话A -----" -ForegroundColor Yellow
    Get-Content $outA -Encoding UTF8
    Write-Host "----- 会话B -----" -ForegroundColor Yellow
    Get-Content $outB -Encoding UTF8
}

function Invoke-SingleDemo([int]$n) {
    $f = Get-Item (Join-Path $dir ("演示{0}_*.sql" -f $n)) | Where-Object { $_.Name -notmatch "会话" } | Select-Object -First 1
    Write-Host ("== 演示{0}：单会话 ==" -f $n) -ForegroundColor Cyan
    cmd /c "mysql $common < `"$($f.FullName)`""
}

if ($Demo -eq "All") {
    foreach ($n in 1..8) { Invoke-PairDemo $n }
    Invoke-SingleDemo 9
} elseif ($singleDemos -contains [int]$Demo) {
    Invoke-SingleDemo([int]$Demo)
} elseif ($Demo -match '^[1-8]$') {
    Invoke-PairDemo([int]$Demo)
} else {
    Write-Error "Demo 取值应为 1~9 或 All"
}
