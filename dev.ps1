# 启动 quote-api，并在 src 变更后自动编译以触发 DevTools 重启
# 用法：在 quote-api 目录执行  .\dev.ps1

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
Set-Location $Root

$mvnw = Join-Path $Root 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
  throw '找不到 mvnw.cmd'
}

# 释放本机 8080（若被旧实例占用）
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
  ForEach-Object {
    Write-Host "[dev] 结束占用 8080 的进程 $($_.OwningProcess)"
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
  }
Start-Sleep -Seconds 1

$compileLock = [System.Threading.Mutex]::new($false, 'Global\quote-api-dev-compile')
$pending = $false
$timer = $null

function Request-Compile {
  $script:pending = $true
  if ($null -ne $script:timer) {
    $script:timer.Stop()
    $script:timer.Dispose()
  }
  $script:timer = New-Object System.Timers.Timer 800
  $script:timer.AutoReset = $false
  Register-ObjectEvent -InputObject $script:timer -EventName Elapsed -Action {
    $script:pending = $false
    if (-not $compileLock.WaitOne(0)) {
      return
    }
    try {
      Write-Host ''
      Write-Host "[dev] $(Get-Date -Format 'HH:mm:ss') 检测到变更，重新编译…"
      & $using:mvnw -q -DskipTests compile
      if ($LASTEXITCODE -eq 0) {
        Write-Host '[dev] 编译成功，DevTools 将自动重启'
      } else {
        Write-Host "[dev] 编译失败 (exit $LASTEXITCODE)"
      }
    } finally {
      $compileLock.ReleaseMutex()
    }
  } | Out-Null
  $script:timer.Start()
}

$watchers = @()
foreach ($dir in @(
  (Join-Path $Root 'src\main\java'),
  (Join-Path $Root 'src\main\resources')
)) {
  if (-not (Test-Path $dir)) { continue }
  $w = New-Object System.IO.FileSystemWatcher $dir, '*.*'
  $w.IncludeSubdirectories = $true
  $w.EnableRaisingEvents = $true
  foreach ($evt in @('Changed', 'Created', 'Deleted', 'Renamed')) {
    Register-ObjectEvent -InputObject $w -EventName $evt -Action {
      $name = $Event.SourceEventArgs.Name
      if ($name -match '\.(java|yml|yaml|properties|xml|sql)$') {
        Request-Compile
      }
    } | Out-Null
  }
  $watchers += $w
}

Write-Host '[dev] 源码监视已开启（.java / .yml / .sql 等变更 → 自动编译 → DevTools 重启）'
Write-Host '[dev] 启动 spring-boot:run …'
Write-Host ''

try {
  & $mvnw -DskipTests spring-boot:run
} finally {
  Get-EventSubscriber | Unregister-Event -Force -ErrorAction SilentlyContinue
  foreach ($w in $watchers) {
    $w.EnableRaisingEvents = $false
    $w.Dispose()
  }
  if ($null -ne $timer) {
    $timer.Stop()
    $timer.Dispose()
  }
  $compileLock.Dispose()
}
