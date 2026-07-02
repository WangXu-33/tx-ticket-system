param(
    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$User = "root",
    [string]$HostName = "localhost",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"

$mysql = "E:\env\mysql\mysql8.0\bin\mysql.exe"
$initSql = Join-Path $PSScriptRoot "..\init.sql"

if (-not (Test-Path $mysql)) {
    throw "mysql.exe 不存在：$mysql"
}

if (-not (Test-Path $initSql)) {
    throw "init.sql 不存在：$initSql"
}

$tempSql = Join-Path $env:TEMP "tx_ticket_init.sql"
Copy-Item -LiteralPath $initSql -Destination $tempSql -Force
$tempSqlPath = (Resolve-Path -LiteralPath $tempSql).Path -replace '\\', '/'

try {
    & $mysql "-h$HostName" "-P$Port" "-u$User" "-p$Password" --default-character-set=utf8mb4 --comments --force "--execute=source $tempSqlPath"
}
finally {
    if (Test-Path $tempSql) {
        Remove-Item -LiteralPath $tempSql -Force
    }
}
