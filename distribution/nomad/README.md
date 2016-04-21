# Deploying COMaaS using Nomad

In order to deploy COMaaS as a Nomad job, use the 'comaas.job' job descriptor file and change the following:

* region: insert your OpenStack tenant name here, e.g. "fbar-dev"
* datacenters: insert your OpenStack ds location(s) here, e.g. [ "ams1" ]
* task/artifact/source: See [Artifact location](#artifact-location)
* task/artifact/options/checksum: Make sure this matches the checksum of the file (use the 'md5sum' module and prefix with "md5:")

## Artifact location

Nomad has some issues with a variable-path top-level directory in the artifact (such as "comaas-x.y.z-2jljslk/".) In order to run with Nomad, this top-level folder should be removed and the result re-packaged. In the future we may want to change the assembly configuration accordingly.

<pre><code>cd distribution/target
tar xzvf <file.tar.gz>
cd ecg.comaas.*/
tar czvf ../latest.tar.gz .
cd ../ && rm -rf ecg.comaas.*</code></pre>

This file then has to be uploaded to a cluster-available web server. For dev purposes, the nagios001 node's /var/www/html folder can be used.


## Build, package & deploy comaas for each tenant  

There are pipelines to test, build, package, deploy and upload artifacts for each tenant setup in comaas-qa cloud. http://10.41.136.230/view/Pipelines/

Pipeline execution deploys comaas for a given tenant to 3 core nodes (10.41.136.237-239) in the comaas-qa cluster 

