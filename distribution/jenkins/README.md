# Jenkins 2 pipelines

## General information

This repository is a storage of new Jenkins pipelines.

### Project structure
 - `pipelines` - all jenkins pipelines together with configurations would
 be kept here in separate folders
 - `vars` - shared functions suitable for all pipelines

### Pipeline structure
 - `configuration.yaml` - pipeline configuration
 - `Jenkinsfile` - actual pipeline (limited Groovy functionality for
 scripting pipelines and no Groovy at all for declarative pipelines)

## Currently available

 - Automatic Testing (run all tests against Gerrit review in parallel, triggered by Gerrit)
 - Manual Testing (provide GERRIT_CHANGE_NUMBER, GERRIT_PATCHSET_NUMBER,
 GERRIT_REFSPEC, GERRIT_CHANGE_URL)
 - Automatic Sandbox deployment + smoke tests (deploy COMaaS for every tenant from
 configuration file on Sandbox)
 - Manual Sandbox deployment + smoke tests (select for which tenant to deploy COMaaS
 and manually trigger pipeline)
 - Manual Prod deployment (select DC, tenant and provide short Git hash (7 characters),
 be aware that provided Git hash should be successfully deployed to sandbox first
 and be still available in Swift repository)
 - Slack notifications - all testing pipeline and sandbox deployment pipeline
 notifications will go to `ecg-comaas-jenkins` Slack channel, prod deployment
 pipeline notifications will go to tenants channel (for example, `ecg-comaas-kjca`)
 - Graphite metrics - for both sandbox and prod deployment Jenkins pipeline
 sends start deployment event and either succeeded or failed deployment event,
 those can be afterwords found in Grafana
 - Cleanups
    - Pipeline workspace is wiped out after pipeline finishes
    - Pipeline runs history is usually kept for last 25 runs (configurable in Jenkins UI)
    - COMaaS tarballs are being removed after deployment finished

## Configuration features

 - `tenants.all` - full list of all tenants COMaaS has
 - `tenants.disabled` - a list of tenants for which automatic pipeline
 will NOT be executed

## Manual tests execution

Testing pipeline is automatically triggered by submitting patch to Gerrit review.
With Gerrit you can easily `Retrigger` the run. However, there may be some
cases when you just want to run tests not sending statistics to Gerrit.
You can trigger pipeline manually by clicking `Build with Parameters` and providing
`GERRIT_CHANGE_NUMBER`, `GERRIT_PATCHSET_NUMBER`, `GERRIT_REFSPEC`, `GERRIT_CHANGE_URL`

## Manual deploys to lp

If your tenant is not the one who wants automatic deployments, you still
can benefit from deployment pipeline.
Go to [new Jenkins](https://builder.comaas-control-prod.dus1.cloud/),
select `comaas-sandbox-deploy-pipeline`, click `Build with Parameters` and
select your tenant in the list. The pipeline would run only for the selected
tenant and will try to deploy latest version of COMaaS on sandbox for that tenant.
Be aware, if you would select `all-enabled` (which is the default parameter),
tenants list would be taken from configuration file in this repository

## Endpoints

 - [New Jenkins](https://builder.comaas-control-prod.dus1.cloud/)
 - [Blue Ocean view](https://builder.comaas-control-prod.dus1.cloud/blue/pipelines)
 - [Builders list](https://builder.comaas-control-prod.dus1.cloud/computer/)
 - [Pipeline syntax](https://builder.comaas-control-prod.dus1.cloud/pipeline-syntax/)
