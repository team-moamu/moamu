pipeline {
    agent { label 'docker' }

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Check configuration') {
            steps {
                script {
                    if (env.BRANCH_NAME != 'develop') {
                        error("Unsupported branch: ${env.BRANCH_NAME}")
                    }
                    if (!env.DOCKERHUB_NAMESPACE?.trim()) {
                        error('Set DOCKERHUB_NAMESPACE in Jenkins before building')
                    }
                    env.IMAGE_TAG = env.GIT_COMMIT.take(12)
                }
                sh 'docker version && git --version'
            }
        }

        stage('Build images') {
            steps {
                sh '''
                    set -eu
                    docker build -t "docker.io/${DOCKERHUB_NAMESPACE}/moamu-back:${IMAGE_TAG}" backend
                    docker build -t "docker.io/${DOCKERHUB_NAMESPACE}/moamu-front:${IMAGE_TAG}" frontend
                '''
            }
        }

        stage('Push images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        set -eu
                        printf '%s' "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker push "docker.io/${DOCKERHUB_NAMESPACE}/moamu-back:${IMAGE_TAG}"
                        docker push "docker.io/${DOCKERHUB_NAMESPACE}/moamu-front:${IMAGE_TAG}"
                        docker logout
                    '''
                }
            }
        }

        stage('Update development manifests') {
            when { branch 'develop' }
            steps {
                sshagent(credentials: ['moamu-deploy-ssh']) {
                    sh '''
                        set -eu
                        git clone --branch main git@github.com:team-moamu/moamu-deploy.git deploy
                        test -f deploy/environments/dev/back/kustomization.yaml
                        test -f deploy/environments/dev/front/kustomization.yaml
                        cd deploy/environments/dev/back
                        kustomize edit set image "moamu-api=docker.io/${DOCKERHUB_NAMESPACE}/moamu-back:${IMAGE_TAG}"
                        cd ../front
                        kustomize edit set image "moamu-web=docker.io/${DOCKERHUB_NAMESPACE}/moamu-front:${IMAGE_TAG}"
                        cd ../../..
                        kustomize build environments/dev > /dev/null
                        git add environments/dev/back/kustomization.yaml environments/dev/front/kustomization.yaml
                        if ! git diff --cached --quiet; then
                            git -c user.name=moamu-deploy-bot -c user.email=moamu-deploy-bot@users.noreply.github.com commit -m "chore: 개발 이미지 ${IMAGE_TAG} 갱신"
                            git push origin HEAD:main
                        fi
                    '''
                }
            }
        }

    }
}
