#!/usr/bin/env bash
set -euo pipefail

service_name="${1:?service name is required}"
source_dir="${2:?source directory is required}"

: "${CI_DB_HOST:?CI_DB_HOST is required}"
: "${CI_DB_PORT:?CI_DB_PORT is required}"
: "${CI_DB_USER:?CI_DB_USER is required}"
: "${CI_DB_PASSWORD:?CI_DB_PASSWORD is required}"

mysql_image="${MYSQL_CLIENT_IMAGE:-mysql:8.4}"
source_database=""
test_prefix=""

case "${service_name}" in
  auth)
    source_database="baef_auth"
    test_prefix="auth_ci_test_"
    ;;
  store)
    source_database="baef_store"
    test_prefix="store_regression_test_"
    ;;
  platform)
    source_database="baef_platform"
    test_prefix="platform_regression_test_"
    ;;
  order)
    source_database="baef_order"
    test_prefix="order_ci_test_"
    ;;
  report)
    source_database="baef_report"
    test_prefix="report_ci_test_"
    ;;
  billing)
    source_database="baef_billing"
    test_prefix="billing_ci_test_"
    ;;
  external-backend)
    source_database="baef_external_platform"
    test_prefix="external_ci_test_"
    ;;
  notification|scg)
    ;;
  *)
    echo "Unsupported Gradle service: ${service_name}" >&2
    exit 2
    ;;
esac

if [[ ! -d "${source_dir}" || ! -f "${source_dir}/gradlew" ]]; then
  echo "Gradle source directory not found for ${service_name}" >&2
  exit 2
fi

mysql_command() {
  docker run --rm -i \
    --env MYSQL_PWD \
    "${mysql_image}" \
    mysql --protocol=TCP \
      --host="${CI_DB_HOST}" \
      --port="${CI_DB_PORT}" \
      --user="${CI_DB_USER}" \
      --batch --skip-column-names "$@"
}

mysql_sql() {
  local database="$1"
  local sql="$2"
  mysql_command --database="${database}" --execute="${sql}"
}

test_database=""
created_database="false"

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM

  if [[ "${created_database}" == "true" ]]; then
    if [[ "${test_database}" =~ ^${test_prefix}[0-9a-f]{32}$ && "${test_database}" != "${source_database}" ]]; then
      mysql_sql "${source_database}" "DROP DATABASE \`${test_database}\`" >/dev/null
      echo "Removed this run's UUID test schema; source data was not changed."
    else
      echo "Refusing to remove an unverified schema name: ${test_database}" >&2
      exit_code=1
    fi
  fi

  exit "${exit_code}"
}
trap cleanup EXIT INT TERM

export MYSQL_PWD="${CI_DB_PASSWORD}"
export INTERNAL_API_KEY="$(openssl rand -hex 32)"
export JWT_SECRET="$(openssl rand -hex 32)"
export SPRING_KAFKA_LISTENER_AUTO_STARTUP="false"
export SPRING_TASK_SCHEDULING_ENABLED="false"
export WEBHOOK_WORKER_ENABLED="false"
export STORE_OUTBOX_ENABLED="false"
export CATALOG_CONSUMER_ENABLED="false"
export BILLING_SCHEDULER_ENABLED="false"
export SIMULATOR_SQL_INIT_MODE="never"

if [[ -n "${source_database}" ]]; then
  if [[ ! "${source_database}" =~ ^[a-z0-9_]+$ || ! "${test_prefix}" =~ ^[a-z0-9_]+_$ ]]; then
    echo "Invalid CI schema contract" >&2
    exit 2
  fi

  schema_exists="$(mysql_sql information_schema "SELECT COUNT(*) FROM schemata WHERE schema_name='${source_database}'")"
  if [[ "${schema_exists}" != "1" ]]; then
    echo "Required source schema is unavailable: ${source_database}" >&2
    exit 1
  fi

  test_database="${test_prefix}$(openssl rand -hex 16)"
  if [[ ! "${test_database}" =~ ^${test_prefix}[0-9a-f]{32}$ ]]; then
    echo "Generated test schema did not satisfy the UUID contract" >&2
    exit 1
  fi

  mysql_sql "${source_database}" "CREATE DATABASE \`${test_database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci" >/dev/null
  created_database="true"

  mysql_sql information_schema "SELECT CONCAT('CREATE TABLE \`${test_database}\`.\`', table_name, '\` LIKE \`${source_database}\`.\`', table_name, '\`;') FROM tables WHERE table_schema='${source_database}' AND table_type='BASE TABLE' ORDER BY table_name" \
    | mysql_command --database="${test_database}"

  active_database="$(mysql_sql "${test_database}" "SELECT DATABASE()")"
  if [[ "${active_database}" != "${test_database}" ]]; then
    echo "MySQL did not select the verified UUID test schema" >&2
    exit 1
  fi

  if [[ "${service_name}" == "store" ]]; then
    mysql_sql "${test_database}" "INSERT INTO stores(id,user_id,store_name,phone,business_registration_number,business_verification_id,address,address_detail,industry_type,minimum_order_amount,open_time,close_time,operation_status) VALUES(1,1,'CATALOG TEST STORE','01000000000','9000000001','00000000-0000-0000-0000-000000000001','TEST ADDRESS','','TEST',0,'09:00:00','21:00:00','OPERATING')" >/dev/null
    export CATALOG_TEST_SCHEMA="${test_database}"
  elif [[ "${service_name}" == "platform" ]]; then
    export CATALOG_TEST_SCHEMA="${test_database}"
  elif [[ "${service_name}" == "external-backend" ]]; then
    mysql_sql "${test_database}" "INSERT INTO external_stores(provider_type,external_store_id,store_name,enabled) VALUES('BAEMIN','BAE-STORE-003','CI FIXTURE STORE',1),('COUPANG_EATS','CPE-STORE-003','CI FIXTURE STORE',1),('YOGIYO','YGY-STORE-003','CI FIXTURE STORE',1),('DDANGYO','DDG-STORE-003','CI FIXTURE STORE',1)" >/dev/null
    echo "Loaded external simulator fixtures into the disposable CI schema."
  fi

  export DB_HOST="${CI_DB_HOST}"
  export DB_PORT="${CI_DB_PORT}"
  export DB_NAME="${test_database}"
  export DB_USER="${CI_DB_USER}"
  export DB_PASSWORD="${CI_DB_PASSWORD}"
  export SPRING_DATASOURCE_URL="jdbc:mysql://${CI_DB_HOST}:${CI_DB_PORT}/${test_database}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
  export SPRING_DATASOURCE_USERNAME="${CI_DB_USER}"
  export SPRING_DATASOURCE_PASSWORD="${CI_DB_PASSWORD}"

  echo "Verified isolated CI schema for ${service_name}: ${test_database}"
else
  echo "Running ${service_name} without a database dependency."
fi

(
  cd "${source_dir}"
  chmod +x gradlew
  ./gradlew clean test bootJar --no-daemon \
    --init-script "${source_dir}/ci/jenkins-test-policy.gradle"
)
