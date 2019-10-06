## jenkins-python-full-pipeline
Pipeline configurável de entrega de código utilizando o Jenkins e Docker para Python.

### Objetivo
Configurar um pipeline "as code" para o Jenkins, realizando testes estático e unitário para programas em python.

### Infraestrutura mínima
- Servidor Jenkins
- Servidor SonarQube
- Servidor para execução (Docker)
- Repositório no github
- Repositório no dockerhub

### Plugins Jenkins utilizados
- Pipeline
- Warnings
- Warnings Next Generation
- Publish Over SSH
- SonarQube
- Git
- BlueOcean (para vizualização)
- Credentials

### Dependências nos servidores
- Sonar-scanner https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.0.0.1744-linux.zip
- Docker

### Configurações SonarQube
- Webhook Jenkins https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-jenkins/

### Publicação 

https://medium.com/@matheusvt/cria%C3%A7%C3%A3o-de-uma-esteira-ci-cd-para-python-usando-o-jenkins-afe47b9acd22
