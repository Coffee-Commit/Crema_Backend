pipeline {
    agent any

    environment {
        GCP_PROJECT_ID = 'coffee-and-commit'
        GCP_REGION = 'asia-northeast3'
        GCS_BUCKET_NAME = 'goorm-crema-coffeechat'
        REPO_NAME = 'coffee'
        INFRA_REPO_URL = 'git@github.com:Coffee-Commit/Crema_Infra.git'
    }

    tools {
        jdk 'JDK21'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Set Dynamic Variables') {
            steps {
                script {
                    echo "This build is for branch: '${env.BRANCH_NAME}'"

                    if (env.BRANCH_NAME == 'main') {
                        env.IMAGE_NAME = 'crema-backend'
                        env.MANIFEST_PATH = 'apps/backend/prod/deployment.yaml'
                    }
                    else if (env.BRANCH_NAME == 'dev' || env.BRANCH_NAME == null) {
                        env.IMAGE_NAME = 'crema-backend-dev'
                        env.MANIFEST_PATH = 'apps/backend/dev/deployment.yaml'
                    }
                    else {
                        error "Unsupported branch"
                    }
                }
            }
        }

        stage('Check JDK') {
            steps {
                sh 'java -version'
                sh 'javac -version'
            }
        }
        stage('Build & Test') {
            steps {
                withCredentials([
                    file(credentialsId: 'gcp-test-credentials', variable: 'GCP_KEY_FILE')
                ]) {
                    sh '''
                        export GOOGLE_APPLICATION_CREDENTIALS=$GCP_KEY_FILE
                        export RUN_GCS_IT='true'

                        chmod +x ./gradlew
                        ./gradlew clean build -Dspring.profiles.active=test
                    '''
                }
            }
        }

        stage('Build & Push Image') {
            steps {
                script {
                    /* groovylint-disable-next-line NestedBlockDepth */
                    withCredentials([file(credentialsId: 'gcp-credentials', variable: 'GCP_KEY_FILE')]) {
                        sh "gcloud auth activate-service-account --key-file=${GCP_KEY_FILE}"
                        sh "gcloud auth configure-docker ${GCP_REGION}-docker.pkg.dev"

                        def imageTag = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                        def fullImageName = "${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${REPO_NAME}/${env.IMAGE_NAME}:${imageTag}"

                        echo "Building Docker image: ${fullImageName}"
                        sh "docker build -t ${fullImageName} ."

                        echo "Pushing Docker image: ${fullImageName}"
                        sh "docker push ${fullImageName}"
                    }
                }
            }
        }

        stage('Update Manifests') {
            steps {
                script {
                    def imageTag = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def fullImageName = "${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${REPO_NAME}/${env.IMAGE_NAME}:${imageTag}"

                    echo "Updating manifest in Infra repository to use image: ${fullImageName}"

                    withCredentials([sshUserPrivateKey(credentialsId: 'github-ssh-key-for-infra', keyFileVariable: 'GIT_SSH_KEY')]) {
                        sh 'rm -rf Crema_Infra'
                        sh "git clone ${INFRA_REPO_URL}"
                        dir('Crema_Infra') {
                            sh """
                            sed -i'' 's|image: .*${env.IMAGE_NAME}.*|image: ${fullImageName}|g' ${env.MANIFEST_PATH}
                            """

                            sh 'git config --global user.email "jenkins@backend.ci"'
                            sh 'git config --global user.name "Jenkins Backend CI"'
                            sh 'git add .'
                            sh "git commit -m \"Update Backend image to ${imageTag}\""
                            sh 'git push origin main'
                        }
                    }
                }
            }
        }
    }
//     post {
//         always {
//             script {
//                 echo "Pipeline finished. Status: ${currentBuild.result}"
//                 withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
//                     def statusEmoji = (currentBuild.result == 'SUCCESS') ? ':white_check_mark:' : ':x:'
//                     def statusColor = (currentBuild.result == 'SUCCESS') ? 65280 : 16711680
//                     def nowIso = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone('UTC'))
//
//                     sh """
//                     curl -H "Content-Type: application/json" -X POST -d '{
//                         "username": "Jenkins CI",
//                         "avatar_url": "https://infra-coffeechat.kro.kr/static/aa133e25/images/svgs/logo.svg",
//                         "embeds": [
//                             {
//                                 "title": "${statusEmoji} 빌드 ${currentBuild.result}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
//                                 "description": "프로젝트: **${env.JOB_NAME}**\\n빌드 번호: **${env.BUILD_NUMBER}**\\n상태: **${currentBuild.result}**\\n자세히 보기: ${env.BUILD_URL}",
//                                 "color": ${statusColor},
//                                 "timestamp": "${nowIso}"
//                             }
//                         ]
//                     }' ${DISCORD_WEBHOOK_URL}
//                     """
//                 }
//             }
//         }
//     }
}