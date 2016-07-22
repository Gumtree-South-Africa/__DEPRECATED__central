#Organisation

Folders tenant/propertyset
 
#noenv 
Empty folder used by the build system to indicate that properties managed/provided externally 
  
#comaasqa
A set of properties to run comaas in comaasqa cloud.
 
#local
This is a set of properties to run comaas against locally run comaas-vagrant instance.   

#bare
This is a collection of properties to run Comaas locally (from comaas-qa cloud machine)
with only Riak/Cassandra repo configured. 
This is NOT intended as fully functional configuration and some services are expected to fail. 
This is just to test that the Bean initialization works and Comaas is able to start.

#prod | qa 
Prod/QA properties for given tenant

#other 
Alternative sets of properties specific to each tenant
