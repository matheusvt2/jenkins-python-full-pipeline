#!groovy

"""
Arquivo de configuração de pipeline

__author__ = "Matheus Villela Torres"
__copyright__ = "None"
__credits__ = ["Matheus Villela Torres"]
__license__ = "MIT"
__version__ = "1.0"
"""

//-----------------------------DEFINICOES DE VARIAVEIS----------------------------------
def credentialId = "sua_credencial-jenkins"
def projetcId = "python_pipeline_${BUILD_NUMBER}"
def buildIdOld = funcLastSuceesfulID()
def scannerHome = '/home/bitnami/sonar-scanner-4.0.0.1744-linux' //caminho da instalacao do sonar-scanner
def dockerCredentials = 'dockerHubAccount'
def sshprofile = 'execServer'
def dockerhub = "sua_conta_dockerhub"
//--------------------------------------------------------------------------------------

pipeline {

    agent any
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr:'10')) 
    }

    environment { //Definicao de variaveis de ambiente
        PATH="/home/bitnami/miniconda3/bin:/home/bitnami/miniconda3/lib:$PATH"  //Caminho do Anaconda
         }

    stages {
        //------------------------------------------------ESTAGIO 01---------------------------------------------------------------------------
        stage('Load Parameters'){
            steps{
                script {
                    dir ('Arquivos'){ //Fetch dos arquivos repositório do projeto para a pasta Arquivos, dentro do workspace do Jenkins
                        checkout([$class: 'GitSCM', branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout']], gitTool: '${gitInit}', submoduleCfg: [], userRemoteConfigs: [[credentialsId: credentialId, url: "https://github.com/matheusvt2/jenkins-python-full-pipeline.git"]]])
                    }

                    tools = load "${WORKSPACE}/Arquivos/Jenkins/jenkins-python-full-pipeline.groovy"  //Carrega dados do arquivo de config deste projeto na pasta Jenkins (tem que estar no GIT)

                    println "Utilizando informacoes do arquivo jenkins-python-full-pipeline.groovy"
                    println  " => Ling Selecionada: ${env.ProjectLang}  => Ambiente: ${env.ProjectEnv} => Executar Código: ${env.ExecutarPrograma}"

                }//Script - Load Parameters 
            }//Steps - Load Parameters
        }//Fim do Load Parameters
        //------------------------------------------------FIM ESTAGIO 01 - Load Parameters---------------------------------------------------

        //------------------------------------------------ESTAGIO 02---------------------------------------------------------------------------
        stage('Environment Build') {
            steps { 
                script{
                    if (env.ProjectLang == "PYTHON" && env.ProjectEnv == "DOCKER"){
                        println "Construindo build nome jenkins-python-full-pipeline:${BUILD_NUMBER}"
                        withCredentials([usernamePassword(credentialsId: "${dockerCredentials}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            sh "docker login -u ${USERNAME} -p ${PASSWORD}"
                            sh "docker build -t ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER} . --no-cache --rm"
                        }

                    }else{
                        println ("Configurações não suportadas")
                    }
                }
            }
        }        
        //------------------------------------------------FIM ESTAGIO 02 - Docker Build --------------------------------------------------

        //------------------------------------------------INICIO ESTAGIO 03 -----------------------------------------------------------------
        stage('Quality Tests'){
            parallel{
                stage ('Lynt Test'){
                    steps{
                        script{
                                pyLynt("Arquivos",100)
                        }
                    }    
                }
                
                stage('SonarQube analysis'){
                    steps {
                        script{
                            dir ("Arquivos"){
                                sh "coverage run teste.py"
                                sh "coverage report"
                                sh "coverage xml -o coverage.xml"
                                withSonarQubeEnv('sonarQube') {
                                    sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectName='Python :: docker-pipeline' -Dsonar.projectKey='docker-pipeline' -Dsonar.sources=. -Dsonar.projectVersion=${BUILD_NUMBER} -Dsonar.language=py -Dsonar.sourceEncoding=UTF-8 -Dsonar.python.coverage.reportPath=coverage.xml -Dsonar.python.xunit.reportPath=TEST-teste-report.xml"
                                }
                            }
                        }
                    }
                }
            }
        }
        //------------------------------------------------FIM ESTAGIO 03 - Lynt & Sonar Test ----------------------------------------------------------

        //------------------------------------------------FIM ESTAGIO 04 ----------------------------------------------------------
         stage("Quality Gate") {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                    // true = set pipeline to UNSTABLE, false = don't
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        //------------------------------------------------FIM ESTAGIO 04 - Quality Gate ----------------------------------------------------------

       //------------------------------------------------ESTAGIO 05---------------------------------------------------------------------------
        stage('Docker Push') {
             steps {
                script {
                    if (env.ProjectLang == "PYTHON"  && env.ProjectEnv == "DOCKER"){
                        withCredentials([usernamePassword(credentialsId: dockerCredentials, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            sh "docker login -u ${USERNAME} -p ${PASSWORD}"
                            sh "docker push  ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER}"
                        }
                        println "Push da imagem completo"
                    }else{
                        println "Configurações não suportadas"
                    }
                }
            }
        }
        //------------------------------------------------FIM ESTAGIO 05 - Docker Push -------------------------------------------------------
        
        //------------------------------------------------ESTAGIO 07---------------------------------------------------------------------------
        stage('Exec Program'){
            steps {
                script {
                    if(env.ExecutarPrograma == "SIM" && env.ProjectLang == "PYTHON" && env.ProjectEnv == "DOCKER"){
                        withCredentials([usernamePassword(credentialsId: dockerCredentials, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                           funcSshCommand(sshprofile,"""
                                docker container rm jenkins-python-full-pipeline_container
                                docker login -u ${USERNAME} -p ${PASSWORD}
                                docker pull ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER}
                                docker run --name jenkins-python-full-pipeline_container ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER}
                                docker logout
                                """)
                        }
                    }else{
                        println "Configurações não suportadas"
                    }
                }
            }
        }//Fim do Exec Programa
        //------------------------------------------------FIM ESTAGIO 07 - Exec Program -------------------------------------------------------

        //------------------------------------------------ESTAGIO 08---------------------------------------------------------------------------
        stage('Results'){ //Informa se a build foi realizada com sucesso ou não
            steps {
                script {
                    if(currentBuild.currentResult == "SUCCESS" || currentBuild.currentResult == "UNSTABLE"){
                        println "Build Succeded."
                   }
                }
            }
        }
        //------------------------------------------------FIM ESTAGIO 08 - Results -------------------------------------------------------------
            
    }//Fim dos stages

    post {//Apos o build (com ou sem erros) sempre executa esses comandos
        always {
            script{
                if(env.ProjectEnv == "VENV"){
                    println "Apagando arquivos temporários..."
                    sh "conda remove --yes -p ${WORKSPACE}/VirtualEnv/${BUILD_TAG} --all"
                    sh ''' cd ${WORKSPACE}
                    ls -la
                    rm -fr *
                    ls -la 
                    '''
                    println "Arquivos temporários apagados."
                }else if(env.ProjectEnv == "DOCKER"){
                    println "Removendo build nome ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER}"
                    sh "docker rmi ${dockerhub}/jenkins-python-full-pipeline:${BUILD_NUMBER}"
                    sh "docker logout"
                }
            }
        }
    }//Fim do post Pipeline
}//Fim do Pipeline


//-----------------------------DEFINICOES DE FUNCOES------------------------------------
def pyLynt(folder, max_errors){
    //Funcao para execucao do Lynt de Python
    //Variaveis de entrada:
    //                      -folder: str, informa a pasta onde o projeto está, padrão é pasta a Arquivos, criada neste pipeline 
    //                      -max_errors: int, informa a quantidade de erros permitidos para o Lynter
    dir ("${folder}"){
        sh "pylint --output-format=parseable --reports=y --exit-zero --disable=E0401 ./*.py  > ./pylint.log "
        sh 'cat ./pylint.log'
    }
    step([$class: 'WarningsPublisher',
    parserConfigurations: [[
    parserName: 'pylint',
    pattern: '**/pylint.log'
    ]],
    unstableTotalAll: "${max_errors}",
    usePreviousBuildAsReference: true
    ])  
}

def funcSshCommand(ssh_profile,ssh_command){
    //Funcao para execucao de codigo via ssh em um servidor especifico
    //Variaveis de entrada:
    //                      -ssh_profile: str, informa o profile de ssh criado no jenkins (servidor destino)
    //                      -ssh_command: str, comandos a serem executados apos conexao ssh
    sshPublisher(
        continueOnError: true, failOnError: false,
        publishers: [
            sshPublisherDesc(
                configName: "${ssh_profile}", //Profile SSH criado no Jenkins
                verbose: true,
                    transfers: [
                    sshTransfer(
                        execCommand: ("${ssh_command}") 
                    )
                ]
            )
        ]
    )
}

def funcSshTransfer(ssh_profile,ssh_remoteDir,ssh_files,ssh_command){
    //Funcao para transferencia de arquivos e execucao de codigo via ssh em um servidor especifico
    //Variaveis de entrada:
    //                      -ssh_profile: str, informa o profile de ssh criado no jenkins (servidor destino)
    //                      -ssh_remoteDir: str, diretorio destino da transferencia de arquivos 
    //                      -ssh_files: str separada por virgulas, diretorios a serem enviados para o servidor destino
    //                      -ssh_command: str, comandos a serem executados apos transferencia dos arquivos
    sshPublisher(
        continueOnError: false, failOnError: true,
        publishers: [
            sshPublisherDesc(
                configName: "${ssh_profile}", //Profile SSH criado no Jenkins
                verbose: true,
                cleanRemote: true,
                transfers: [
                    sshTransfer(
                        sourceFiles: "${ssh_files}",
                        remoteDirectory: "${ssh_remoteDir}",
                        execCommand: ("${ssh_command}")
                    ) 
                ]
            )
        ]
    )
}

def funcLastSuceesfulID(){
   //Funcao para identificar o penultimo build com sucesso

    def lastSuccessfulBuildID = 0
    def build = currentBuild.previousBuild
    def counter = 0
    while (build != null) {
        if (build.result == "SUCCESS" || build.result ==  "UNSTABLE")
        {
            counter = counter +1
            if (counter == 2){
                lastSuccessfulBuildID = build.id as Integer
                break
            }
        }
        build = build.previousBuild
    }
        return lastSuccessfulBuildID
}
