pipeline {
  agent any

  options {
    skipDefaultCheckout(true)
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    // Configure these for the target container registry and repository namespace.
    IMAGE_REGISTRY = 'ghcr.io'
    IMAGE_NAMESPACE = 'ghcr.io/andreseagle1927/core-banking'
    REGISTRY_CREDENTIALS_ID = 'core-banking-registry'
    KUBECONFIG_CREDENTIALS_ID = 'core-banking-prod-kubeconfig'
    PROD_VALUES_CREDENTIALS_ID = 'core-banking-prod-values'
    KUBERNETES_NAMESPACE = 'core-banking'
    HELM_RELEASE_NAME = 'banking'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG = env.GIT_COMMIT.take(12)
        }
      }
    }

    stage('Maven verify') {
      steps {
        sh 'mvn -B -ntp verify'
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
        }
      }
    }

    stage('Validate Helm chart') {
      steps {
        sh 'scripts/validate-kubernetes-chart.sh'
      }
    }

    stage('Build container images') {
      steps {
        script {
          def modules = [
            'api-gateway': 'gateway/api-gateway',
            'customer-service': 'services/customer-service',
            'wallet-service': 'services/wallet-service',
            'transfer-service': 'services/transfer-service',
            'ledger-service': 'services/ledger-service',
            'loan-service': 'services/loan-service',
            'notification-service': 'services/notification-service'
          ]

          modules.each { service, module ->
            sh "docker build --pull --build-arg MODULE=${module} --tag ${env.IMAGE_NAMESPACE}/${service}:${env.IMAGE_TAG} ."
          }
        }
      }
    }

    stage('Publish container images') {
      when {
        anyOf {
          branch 'main'
          buildingTag()
        }
      }
      steps {
        withCredentials([usernamePassword(
          credentialsId: env.REGISTRY_CREDENTIALS_ID,
          usernameVariable: 'REGISTRY_USERNAME',
          passwordVariable: 'REGISTRY_PASSWORD'
        )]) {
          sh '''
            set -eu
            printf '%s' "$REGISTRY_PASSWORD" | docker login "$IMAGE_REGISTRY" \
              --username "$REGISTRY_USERNAME" --password-stdin
            trap 'docker logout "$IMAGE_REGISTRY" >/dev/null 2>&1 || true' EXIT

            for service in api-gateway customer-service wallet-service transfer-service ledger-service loan-service notification-service; do
              docker push "$IMAGE_NAMESPACE/$service:$IMAGE_TAG"
            done
          '''
        }
      }
    }

    stage('Deploy production') {
      when {
        branch 'main'
      }
      steps {
        withCredentials([
          file(credentialsId: env.KUBECONFIG_CREDENTIALS_ID, variable: 'KUBECONFIG_FILE'),
          file(credentialsId: env.PROD_VALUES_CREDENTIALS_ID, variable: 'PROD_VALUES_FILE')
        ]) {
          sh '''
            set -eu
            export KUBECONFIG="$KUBECONFIG_FILE"

            helm upgrade --install "$HELM_RELEASE_NAME" deploy/helm/core-banking \
              --namespace "$KUBERNETES_NAMESPACE" \
              --create-namespace \
              --values deploy/helm/core-banking/values-cloud.yaml \
              --values "$PROD_VALUES_FILE" \
              --set-string services.api-gateway.image="$IMAGE_NAMESPACE/api-gateway:$IMAGE_TAG" \
              --set-string services.customer-service.image="$IMAGE_NAMESPACE/customer-service:$IMAGE_TAG" \
              --set-string services.wallet-service.image="$IMAGE_NAMESPACE/wallet-service:$IMAGE_TAG" \
              --set-string services.transfer-service.image="$IMAGE_NAMESPACE/transfer-service:$IMAGE_TAG" \
              --set-string services.ledger-service.image="$IMAGE_NAMESPACE/ledger-service:$IMAGE_TAG" \
              --set-string services.loan-service.image="$IMAGE_NAMESPACE/loan-service:$IMAGE_TAG" \
              --set-string services.notification-service.image="$IMAGE_NAMESPACE/notification-service:$IMAGE_TAG" \
              --atomic --wait --timeout 10m
          '''
        }
      }
    }
  }

  post {
    always {
      cleanWs()
    }
  }
}
