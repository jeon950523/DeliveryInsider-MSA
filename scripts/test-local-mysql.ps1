param([Parameter(Mandatory)][string]$MySqlCli)
$ErrorActionPreference = 'Stop'
$storeRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $storeRoot 'src/main/java/com/deliveryinsider/store/StoreApplication.java'))) { throw 'Not the Store repository' }
$storeConfig = @{}
Get-Content -LiteralPath (Join-Path $storeRoot '.env') | ForEach-Object {
    if ($_ -match '^\s*(DB_[A-Z_]+)\s*=(.*)$') { $storeConfig[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
if ($storeConfig['DB_HOST'] -notin @('127.0.0.1','localhost') -or $storeConfig['DB_PORT'] -ne '3306') { throw 'Only local MySQL port 3306 is allowed' }
$sourceDatabase = $storeConfig['DB_NAME']
if ($sourceDatabase -notmatch '^[A-Za-z0-9_]+$') { throw 'Invalid source schema identifier' }
$testDatabase = 'store_regression_test_' + [Guid]::NewGuid().ToString('N')
$created = $false
$environmentKeys = @('MYSQL_PWD','SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','WEBHOOK_WORKER_ENABLED','STORE_OUTBOX_ENABLED','CATALOG_CONSUMER_ENABLED','CATALOG_TEST_SCHEMA','PLATFORM_TEST_DB_URL','PLATFORM_TEST_DB_USER','PLATFORM_TEST_DB_PASSWORD')
$previousEnvironment = @{}
foreach ($key in $environmentKeys) { $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$testExit = 1
function Invoke-LocalSql([string]$Database, [string]$Sql) {
    $result = & $MySqlCli --host=127.0.0.1 --port=3306 --user=$($storeConfig['DB_USER']) --database=$Database --batch --skip-column-names --execute=$Sql
    if ($LASTEXITCODE -ne 0) { throw 'Local MySQL test setup/cleanup failed' }
    return $result
}
try {
    $env:MYSQL_PWD = $storeConfig['DB_PASSWORD']
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
    if ($activeDatabase -ne $testDatabase -or $testDatabase -notmatch '^store_regression_test_[0-9a-f]{32}$') { throw 'Private database verification failed' }
    $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/' + $testDatabase + '?serverTimezone=UTC&characterEncoding=UTF-8'
    $env:SPRING_DATASOURCE_USERNAME = $storeConfig['DB_USER']
    $env:SPRING_DATASOURCE_PASSWORD = $storeConfig['DB_PASSWORD']
    $env:WEBHOOK_WORKER_ENABLED = 'false'
    $env:STORE_OUTBOX_ENABLED = 'false'
    $env:CATALOG_CONSUMER_ENABLED = 'false'
    $env:CATALOG_TEST_SCHEMA = $testDatabase
    # Apply only the additive migration to this verified, newly created UUID database.
    foreach ($table in @('stores','menus')) {
        $present = Invoke-LocalSql $testDatabase ("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='$table' AND column_name='event_version'")
        if ($present -eq '0') { Invoke-LocalSql $testDatabase ("ALTER TABLE $table ADD COLUMN event_version BIGINT NOT NULL DEFAULT 1") | Out-Null }
    }
    $migration = Get-Content -LiteralPath (Join-Path $storeRoot 'src/main/resources/db/manual/20260908_catalog_outbox.sql') -Raw
    $createOffset = $migration.IndexOf('CREATE TABLE IF NOT EXISTS outbox_events')
    if ($createOffset -lt 0) { throw 'Expected additive Outbox DDL not found' }
    Invoke-LocalSql $testDatabase $migration.Substring($createOffset) | Out-Null
    # Existing mapper tests require storeId/userId=1. This fixture exists only in our new UUID schema.
    Invoke-LocalSql $testDatabase "INSERT INTO stores(id,user_id,store_name,phone,business_registration_number,business_verification_id,address,address_detail,industry_type,minimum_order_amount,open_time,close_time,operation_status) VALUES(1,1,'CATALOG TEST STORE','01000000000','9000000001','00000000-0000-0000-0000-000000000001','TEST ADDRESS','','TEST',0,'09:00:00','21:00:00','OPERATING')" | Out-Null
    $env:PLATFORM_TEST_DB_URL = $env:SPRING_DATASOURCE_URL
    $env:PLATFORM_TEST_DB_USER = $storeConfig['DB_USER']
    $env:PLATFORM_TEST_DB_PASSWORD = $storeConfig['DB_PASSWORD']
    Write-Output ('Verified private test schema: ' + $testDatabase)
    Push-Location -LiteralPath $storeRoot
    try { & (Join-Path $storeRoot 'gradlew.bat') test bootJar --rerun-tasks --no-daemon; $testExit = $LASTEXITCODE }
    finally { Pop-Location }
} finally {
    try {
        # This invocation can drop only the UUID schema whose creation succeeded above.
        if ($created -and $testDatabase -match '^store_regression_test_[0-9a-f]{32}$' -and $testDatabase -ne $sourceDatabase) {
            Invoke-LocalSql $sourceDatabase ('DROP DATABASE `' + $testDatabase + '`') | Out-Null
            Write-Output 'Removed this run''s private test schema; existing development data was not changed.'
        }
    } finally {
        foreach ($key in $environmentKeys) { [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process') }
    }
}
exit $testExit
