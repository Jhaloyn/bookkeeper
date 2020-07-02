1) Per il lancio di Pit sul progetto Avro è necessario specificare il profilo nel quale è stato aggiunto il plugin di Pit. Il nome del profilo è "pit", quindi il comando Maven da eseguire è: mvn -Ppit org.pitest:pitest-maven:mutationCoverage

2) Se si vogliono eseguire i test in locale con JUnit senza usare Maven per la classe TestSchemaCompatibility, è necessario commentare e togliere i commenti ad alcune parti di codice per via di un problema che non è stato possibile risolvere riguardo al tipo di eccezione che viene lanciata a seguito dell'esecuzione di un test in particolare. Quando viene usato direttamente Junit senza fare il build con Maven viene sollevato un AssertionError, se invece si effettua l'esecuzione dei test con il processo di build di Maven viene lanciata una NullPointerException. 
Quindi per eseguire senza fallimenti i test contenuti nella classe TestSchemaCompatibility direttamente con Junit è necessario:

i) commentare linea: 54
ii) togliere i commenti alle linee: 53, 100, 101, 102 
