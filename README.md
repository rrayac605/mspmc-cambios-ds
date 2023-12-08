# Introduction 

Servicio encargado de las siguientes funciones

-Insertar los cambios de los registros(Casuísticas)

#Variables de entorno

Para que este microservicio pueda ser ejecutado deber\u00E1n de existir las siguietnes variables de entorno en el SO annfitri\u00F3n donde se ejecute el jar

============== AMBIENTE DE QA ===================
- authenticationdatabaseMongo PMCQA01
- usrMongoCambios pmcmodifica
- pwdMongoCambios pmcmodifica0
- databaseMongo PMCQA01
- portMongo 27017
- hostMongo 10.100.8.78
- portMSCambios 9011
- fileLogCambio /home/weblogic/mspmc/logs/mspmc-cambios.log 

============== AMBIENTE DE UAT ===================
- authenticationdatabaseMongo PMCUAT01
- usrMongoCambios pmcmodifica
- pwdMongoCambios pmcmodifica0
- databaseMongo PMCUAT01
- portMongo 27017
- hostMongo 10.100.8.80
- portMSCambios 9011
- fileLogCambio /home/weblogic/mspmc/logs/mspmc-cambios.log 

============== AMBIENTE DE PROD ===================
- authenticationdatabaseMongo PENDIENTE
- usrMongoCambios pmcmodifica
- pwdMongoCambios pmcmodifica0
- databaseMongo PENDIENTE
- portMongo 27017
- hostMongo PENDIENTE
- portMSCambios 9011
- fileLogCambio /home/weblogic/mspmc/logs/mspmc-cambios.log 


TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project. 

# Getting Started
TODO: Guide users through getting your code up and running on their own system. In this section you can talk about:
1.	Installation process
2.	Software dependencies
3.	Latest releases
4.	API references

# Build and Test
TODO: Describe and show how to build your code and run the tests. 

# Contribute
TODO: Explain how other users and developers can contribute to make your code better. 

If you want to learn more about creating good readme files then refer the following [guidelines](https://docs.microsoft.com/en-us/azure/devops/repos/git/create-a-readme?view=azure-devops). You can also seek inspiration from the below readme files:
- [ASP.NET Core](https://github.com/aspnet/Home)
- [Visual Studio Code](https://github.com/Microsoft/vscode)
- [Chakra Core](https://github.com/Microsoft/ChakraCore)