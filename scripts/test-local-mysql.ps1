param([Parameter(Mandatory)][string]$MySqlCli)
$ErrorActionPreference = 'Stop'
$platformRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $platformRoot 'src/main/java/com/deliveryinsider/platform/PlatformApplication.java'))) { throw 'Not the Platform repository' }
$platformConfig = @{}
Get-Content -LiteralPath (Join-Path $platformRoot '.env') | ForEach-Object {
    if ($_ -match '^\s*(DB_[A-Z_]+)\s*=(.*)$') { $platformConfig[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
if ($platformConfig['DB_HOST'] -notin @('127.0.0.1','localhost') -or $platformConfig['DB_PORT'] -ne '3306') { throw 'Only local MySQL port 3306 is allowed' }
$sourceDatabase = $platformConfig['DB_NAME']
if ($sourceDatabase -notmatch '^[A-Za-z0-9_]+$') { throw 'Invalid source schema identifier' }
$testDatabase = 'platform_regression_test_' + [Guid]::NewGuid().ToString('N')
$created = $false
$environmentKeys = @('MYSQL_PWD','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','WEBHOOK_WORKER_ENABLED','CATALOG_CONSUMER_ENABLED','CATALOG_TEST_SCHEMA','PLATFORM_TEST_DB_URL','PLATFORM_TEST_DB_USER','PLATFORM_TEST_DB_PASSWORD')
$previousEnvironment = @{}
foreach ($key in $environmentKeys) { $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$testExit = 1
function Invoke-LocalSql([string]$Database, [string]$Sql) {
    $result = & $MySqlCli --host=127.0.0.1 --port=3306 --user=$($platformConfig['DB_USER']) --database=$Database --batch --skip-column-names --execute=$Sql
    if ($LASTEXITCODE -ne 0) { throw 'Local MySQL test setup/cleanup failed' }
    return $result
}
try {
    $env:MYSQL_PWD = $platformConfig['DB_PASSWORD']
    # Existing schema is read as a structure template only; no rows are copied or modified.
    $tables = @(Invoke-LocalSql $sourceDatabase "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")
    Invoke-LocalSql $sourceDatabase ('CREATE DATABASE `' + $testDatabase + '`') | Out-Null
    $created = $true
    foreach ($tableLine in $tables) {
        $table = ($tableLine -split "`t")[0]
        if ($table -notmatch '^[A-Za-z0-9_]+$') { throw 'Invalid table identifier' }
        Invoke-LocalSql $testDatabase ('CREATE TABLE `' + $testDatabase + '`.`' + $table + '` LIKE `' + $sourceDatabase + '`.`' + $table + '`') | Out-Null
    }
    $activeDatabase = Invoke-LocalSql $testDatabase 'SELECT DATABASE()'
    if ($activeDatabase -ne $testDatabase -or $testDatabase -notmatch '^platform_regression_test_[0-9a-f]{32}$') { throw 'Private database verification failed' }
    $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/' + $testDatabase + '?serverTimezone=UTC&characterEncoding=UTF-8'
    $env:SPRING_DATASOURCE_USERNAME = $platformConfig['DB_USER']
    $env:SPRING_DATASOURCE_PASSWORD = $platformConfig['DB_PASSWORD']
    $env:WEBHOOK_WORKER_ENABLED = 'false'
    $env:CATALOG_CONSUMER_ENABLED = 'false'
    $env:CATALOG_TEST_SCHEMA = $testDatabase
    Invoke-LocalSql $testDatabase (Get-Content -LiteralPath (Join-Path $platformRoot 'src/main/resources/db/manual/20260908_catalog_inbox.sql') -Raw) | Out-Null
    $env:PLATFORM_TEST_DB_URL = $env:SPRING_DATASOURCE_URL
    $env:PLATFORM_TEST_DB_USER = $platformConfig['DB_USER']
    $env:PLATFORM_TEST_DB_PASSWORD = $platformConfig['DB_PASSWORD']
    Write-Output ('Verified private test schema: ' + $testDatabase)
    Push-Location -LiteralPath $platformRoot
    try { & (Join-Path $platformRoot 'gradlew.bat') test bootJar --rerun-tasks --no-daemon; $testExit = $LASTEXITCODE }
    finally { Pop-Location }
} finally {
    try {
        # This invocation can drop only the UUID schema whose creation succeeded above.
        if ($created -and $testDatabase -match '^platform_regression_test_[0-9a-f]{32}$' -and $testDatabase -ne $sourceDatabase) {
            Invoke-LocalSql $sourceDatabase ('DROP DATABASE `' + $testDatabase + '`') | Out-Null
            Write-Output 'Removed this run''s private test schema; existing development data was not changed.'
        }
    } finally {
        foreach ($key in $environmentKeys) { [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process') }
    }
}
exit $testExit
