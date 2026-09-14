pipeline {
    agent any
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        skipDefaultCheckout(true)
    }
    environment {
        K8S_GIT_CREDENTIALS_ID = 'msa4-team4-github-k8s-write'
        MYSQL_CI_CREDENTIALS_ID = 'msa4-team4-mysql-ci'
        IMAGE_ROOT = '192.168.0.5:6901/msa4/team4'
        MANIFEST_REPOSITORY = 'https://github.com/greencomacademy/baef-p2-k8s.git'
        SOURCE_REPOSITORY = 'https://github.com/greencomacademy/baef-p2-store.git'
        SERVICE_NAME = 'store'
        MANIFEST_PATH = 'store-service'
        SERVICE_KIND = 'gradle'
        CLIENT_API_BASE_URL = 'https://api.baef.meerkat.p-e.kr'
        EXTERNAL_API_BASE_URL = 'https://api.baef.meerkat.p-e.kr'
        CI_DB_HOST = '112.222.157.156'
        CI_DB_PORT = '6740'
        MYSQL_CLIENT_IMAGE = 'mysql:8.4'
    }
    stages {
        stage('Checkout Source') {
            steps {
                dir('source') {
                    checkout scm
                }
            }
        }
        stage('Test & Build') {
            steps {
                script {
                    if (env.SERVICE_KIND == 'gradle') {
                        withCredentials([usernamePassword(
                            credentialsId: env.MYSQL_CI_CREDENTIALS_ID,
                            usernameVariable: 'CI_DB_USER',
                            passwordVariable: 'CI_DB_PASSWORD'
                        )]) {
                            sh '''
                                set +x
                                bash source/ci/run-gradle-service.sh "$SERVICE_NAME" "$WORKSPACE/source"
                            '''
                        }
                    } else if (env.SERVICE_KIND == 'client') {
                        dir('source') {
                            sh '''
                                set -eu
                                npm ci
                                VITE_API_BASE_URL="$CLIENT_API_BASE_URL" npm run build
                            '''
                        }
                    } else if (env.SERVICE_KIND == 'external-front') {
                        dir('source') {
                            sh '''
                                set -eu
                                npm ci
                                VITE_EXTERNAL_API_BASE_URL="$EXTERNAL_API_BASE_URL" VITE_APP_BASE_PATH=/simulator/ npm run build
                            '''
                        }
                    } else {
                        error("Unsupported service kind: ${env.SERVICE_KIND}")
                    }
                }
            }
        }
        stage('Build Image') {
            steps {
                dir('source') {
                    script {
                        def image = "${env.IMAGE_ROOT}/${env.SERVICE_NAME}:${env.BUILD_NUMBER}"
                        if (env.SERVICE_KIND == 'client') {
                            sh "docker build --build-arg VITE_API_BASE_URL=${env.CLIENT_API_BASE_URL} -t ${image} ."
                        } else if (env.SERVICE_KIND == 'external-front') {
                            sh "docker build --build-arg VITE_EXTERNAL_API_BASE_URL=${env.EXTERNAL_API_BASE_URL} --build-arg VITE_APP_BASE_PATH=/simulator/ -t ${image} ."
                        } else {
                            sh "docker build -t ${image} ."
                        }
                    }
                }
            }
        }
        stage('Push Image') {
            steps {
                sh 'docker push "${IMAGE_ROOT}/${SERVICE_NAME}:${BUILD_NUMBER}"'
            }
        }
        stage('Update Manifest') {
            steps {
                dir('manifests') {
                    checkout([$class: 'GitSCM', branches: [[name: '*/main']], userRemoteConfigs: [[credentialsId: env.K8S_GIT_CREDENTIALS_ID, url: env.MANIFEST_REPOSITORY]]])
                    withCredentials([usernamePassword(credentialsId: env.K8S_GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                        sh '''
                            set +x
                            set -eu
                            MANIFEST_FILE="${MANIFEST_PATH}/deployment.yaml"
                            IMAGE="${IMAGE_ROOT}/${SERVICE_NAME}:${BUILD_NUMBER}"
                            AUTH_HEADER="$(printf '%s' "${GIT_USER}:${GIT_TOKEN}" | base64 | tr -d '\n')"

                            if [ ! -f "$MANIFEST_FILE" ]; then
                                echo "Manifest not found: $MANIFEST_FILE" >&2
                                exit 1
                            fi

                            for ATTEMPT in 1 2 3; do
                                git -c http.extraHeader="Authorization: Basic ${AUTH_HEADER}" fetch origin main
                                git reset --hard origin/main

                                IMAGE_LINE_COUNT="$(grep -Ec '^[[:space:]]*image:[[:space:]]+' "$MANIFEST_FILE")"
                                if [ "$IMAGE_LINE_COUNT" -ne 1 ]; then
                                    echo "Expected exactly one image line in $MANIFEST_FILE; found $IMAGE_LINE_COUNT" >&2
                                    exit 1
                                fi

                                sed -i -E "s#^[[:space:]]*image:.*#          image: ${IMAGE}#" "$MANIFEST_FILE"
                                git add "$MANIFEST_FILE"

                                if git diff --cached --quiet; then
                                    echo 'No manifest change to commit.'
                                    exit 0
                                fi

                                CHANGED_FILES="$(git diff --cached --name-only)"
                                if [ "$CHANGED_FILES" != "$MANIFEST_FILE" ]; then
                                    echo "Unexpected manifest diff: $CHANGED_FILES" >&2
                                    exit 1
                                fi

                                git diff --cached --check
                                git config user.email 'meerkat@ci'
                                git config user.name 'meerkatCi'
                                git commit -m "deploy: ${SERVICE_NAME}:${BUILD_NUMBER}"

                                if git -c http.extraHeader="Authorization: Basic ${AUTH_HEADER}" push origin HEAD:main; then
                                    exit 0
                                fi

                                if [ "$ATTEMPT" -eq 3 ]; then
                                    echo 'Manifest push failed after 3 attempts.' >&2
                                    exit 1
                                fi

                                echo "Manifest main changed concurrently; retrying ($ATTEMPT/3)."
                            done
                        '''
                    }
                }
            }
        }
    }
    post { always { cleanWs() } }
}\n
