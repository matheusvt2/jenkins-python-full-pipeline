/**********************************************************************
*                                                                     *
*    Exemplo de utilizacao do groovy open-pipeline .NET               *
*                                                                     *
*    Altere os parametros seguindo o exemplo abaixo:                  *  
*                 env.projectLang = "PYTHON"                          *
**********************************************************************/

//--------- ALTERAR AS VARIAVEIS ABAIXO CONFORME SEU PROJETO ----------

//Informar nome da linguagem que está utilizando no projeto, default=PYTHON
env.projectLang = "PYTHON" // Opções: [PYTHON, , , ]

//Informar se o ambiente será DOCKER, default=DOCKER
env.projectEnv = "DOCKER" // Opções: [DOCKER, , , ]

//Informar SIM caso queira executar o programa no pipelie e NAO, caso contrário; default=NAO
env.executarPrograma = "SIM"  // Opções: [SIM, NAO] 
