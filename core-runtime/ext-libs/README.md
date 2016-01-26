This directory contains a Maven repository of libraries that are not available in any reputable public repo.

To add a library (using icu4j as an example):

    # Assuming you're in the current directory, run the following changing versions and paths accordingly:
    mvn install:install-file -DlocalRepositoryPath=. -DcreateChecksum=true -Dpackaging=jar -Dfile=/path/to/icu4j-charset-55_1.jar -DgroupId=com.ibm.icu -DartifactId=icu4j-charset -Dversion=55.1
