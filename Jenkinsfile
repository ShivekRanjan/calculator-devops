/*
 * CI/CD pipeline for the Calculator REST API
 *
 *   Checkout -> Build -> Test -> Docker Build -> Push -> Deploy   (+ email on success/failure)
 *
 * Before the first run:
 *   1. Set the Jenkins global environment variable NOTIFY_EMAIL (kept out of the repo).
 *   2. Create the Jenkins credential "dockerhub-credentials" (Username + Docker Hub access token).
 *   3. Configure tools "JDK-21" and "Maven-3.9" and the email server (see docs/SETUP.md).
 */

// Runs a command with `sh` on Linux/macOS agents and `bat` on Windows agents,
// so the same Jenkinsfile works on either.
def runCmd(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

// Sends the build-status email (Email Extension plugin).
def sendBuildEmail(String status, String colour) {
    emailext(
        to: env.NOTIFY_EMAIL,
        subject: "${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        attachLog: status != 'SUCCESS',
        body: """
            <h2 style="color:${colour}">Build #${env.BUILD_NUMBER}: ${status}</h2>
            <table cellpadding="4">
              <tr><td><b>Job</b></td><td>${env.JOB_NAME}</td></tr>
              <tr><td><b>Image</b></td><td>${env.DOCKER_IMAGE}:${env.IMAGE_TAG}</td></tr>
              <tr><td><b>Duration</b></td><td>${currentBuild.durationString.replace(' and counting', '')}</td></tr>
              <tr><td><b>Console</b></td><td><a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></td></tr>
            </table>
        """
    )
}

pipeline {
    agent any

    tools {
        // Names must match Manage Jenkins -> Tools
        jdk   'JDK-21'
        maven 'Maven-3.9'
    }

    environment {
        DOCKERHUB_USER = 'shivek414'
        // NOTIFY_EMAIL comes from Manage Jenkins -> System -> Global properties
        DOCKER_IMAGE   = "${DOCKERHUB_USER}/calculator"
        IMAGE_TAG      = "${BUILD_NUMBER}"
    }

    triggers {
        githubPush()                 // GitHub webhook -> build immediately on push
        pollSCM('H/5 * * * *')       // fallback if the webhook can't reach Jenkins
    }

    options {
        skipDefaultCheckout(true)    // we check out explicitly in the Checkout stage
        disableConcurrentBuilds()    // two deploys at once would fight over the same stack
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                runCmd 'mvn -B -ntp clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // A failing test makes mvn exit non-zero -> this stage and the build fail
                runCmd 'mvn -B -ntp test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                runCmd "docker build --build-arg BUILD_NUMBER=${env.IMAGE_TAG} -t ${env.DOCKER_IMAGE}:${env.IMAGE_TAG} -t ${env.DOCKER_IMAGE}:latest ."
            }
        }

        stage('Push') {
            environment {
                // Private Docker config for this stage only, so the login/logout below
                // never touches the machine user's own `docker login`
                DOCKER_CONFIG = "${WORKSPACE}/.docker-push"
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials',
                                                  usernameVariable: 'DOCKERHUB_LOGIN',
                                                  passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    script {
                        // Single quotes: the shell (not Groovy) expands the secret, and
                        // --password-stdin keeps it off the command line. Jenkins masks it in logs.
                        if (isUnix()) {
                            sh 'echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_LOGIN" --password-stdin'
                        } else {
                            powershell '$env:DOCKERHUB_TOKEN | docker login -u $env:DOCKERHUB_LOGIN --password-stdin'
                        }
                    }
                }
                runCmd "docker push ${env.DOCKER_IMAGE}:${env.IMAGE_TAG}"
                runCmd "docker push ${env.DOCKER_IMAGE}:latest"
            }
            post {
                always {
                    runCmd 'docker logout'
                }
            }
        }

        stage('Deploy') {
            steps {
                // Rebuilds the proxy image only if nginx.conf changed (otherwise fully cached)
                runCmd 'docker compose build proxy'

                // Compose uses DOCKER_IMAGE / IMAGE_TAG from this pipeline's environment.
                // Only containers whose image/config changed are recreated, and --wait blocks
                // until every container reports healthy. Re-running the same build changes
                // nothing, so the step is idempotent.
                runCmd 'docker compose up -d --remove-orphans --wait --wait-timeout 180'
                runCmd 'docker compose ps'

                // Smoke test through the proxy, from inside the Compose network
                retry(10) {
                    sleep(time: 3, unit: 'SECONDS')
                    runCmd 'docker compose exec -T proxy wget -qO- "http://127.0.0.1/actuator/health"'
                    runCmd 'docker compose exec -T proxy wget -qO- "http://127.0.0.1/api/v1/add?a=2&b=3"'
                }
            }
            post {
                failure {
                    runCmd 'docker compose logs --tail=100'
                }
            }
        }
    }

    post {
        success {
            sendBuildEmail('SUCCESS', '#2e7d32')
        }
        failure {
            sendBuildEmail('FAILURE', '#c62828')
        }
    }
}
