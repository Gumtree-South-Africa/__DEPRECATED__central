#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

static def allTenants() { ["ebayk", "gtau", "gtuk", "it", "kjca", "mde", "mp", "ar", "mx", "sg", "za", "mvca", "be"] }

static def activeTenants(List disabled_tenants) {
    def all = allTenants()
    all.removeAll(disabled_tenants)
    return all
}

static def tenantAliases() {
    [
            'mp'     : 'mp',
            'ebayk'  : 'ek',
            'it'     : 'it',
            'kjca'   : 'ca',
            'mde'    : 'mo',
            'gtuk'   : 'uk',
            'gtau'   : 'au',
            'ar'     : 'ar',
            'mx'     : 'mx',
            'sg'     : 'sg',
            'za'     : 'za',
            'mvca'   : 'mvca',
            'be'     : 'be',
    ]
}

static String getAlias(String longName) {
    return tenantAliases().get(longName)
}

def checkoutGithubRepo(final String branch, final String relativeDir) {
    echo "Checking out repository branch $branch relativeDir $relativeDir"
    checkout([
            $class                           : 'GitSCM',
            branches                         : [[name: branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions                       : [
                    [$class: 'RelativeTargetDirectory', relativeTargetDir: relativeDir],
                    [$class: 'SubmoduleOption',
                     disableSubmodules: false,
                     parentCredentials: true,
                     recursiveSubmodules: true,
                     reference: '',
                     trackingSubmodules: false]
            ],
            submoduleCfg                     : [],
            userRemoteConfigs                : [[credentialsId: 'GITHUB', url: 'git@github.corp.ebay.com:ecg-comaas/central.git']]
    ])
}

def checkoutComaasCentralRepo(String gitHash) {
    checkoutGithubRepo(gitHash, "ecg-comaas-central")

    setGitCommitInfo("ecg-comaas-central")
}

String getCurrentlyDeployedVersion(String tenantLongName, String dc, String environmentShort) {
    String tenant = getAlias(tenantLongName)
    String dc_ip = sh(
            script: "dig +short dmz-vip.comaas-${environmentShort}.${dc}.cloud",
            returnStdout: true
    ).trim()
    String version = sh(
            script: "curl -s --resolve ${tenant}.${environmentShort}.comaas.cloud:443:${dc_ip} https://${dc}.${tenant}.${environmentShort}.comaas.cloud/health | jq -r .version",
            returnStdout: true
    ).trim()
    return version
}

String getActiveDC(String tenantLongName, String env) {
    if (env == 'lp') {
        return 'ams1'
    }

    final String tenant = getAlias(tenantLongName)
    String ip = sh(
            script: "dig +short ${tenant}.prod.comaas.cloud",
            returnStdout: true
    ).trim()

    echo "getActiveDC: ip = $ip"

    String dc = "???"
    if (ip.startsWith("10.32.24.")) { // fabio ams1
        dc = "ams1"
    }
    if (ip.startsWith("10.32.56.")) { // fabio dus1
        dc = "dus1"
    }
    if (ip.startsWith("10.32.25.")) { // traefik ams1
        dc = "ams1"
    }
    if (ip.startsWith("10.32.57.")) { // traefik dus1
        dc = "dus1"
    }

    echo "getActiveDC: dc = $dc"

    return dc
}

String getPassiveDcProd(final String tenant) {
    final String activeDc = getActiveDC(tenant, 'prod')
    if (activeDc != "dus1" && activeDc != "ams1") {
        error("Active dc could not be established. Please specify the dc")
    }
    return activeDc == "dus1" ? "ams1" : "dus1"
}

def downloadComaasDocker() {
    withCredentials([string(credentialsId: 'GITHUBBOT-COMAAS-PAT', variable: 'TOKEN')]) {
        sh """curl -sLo ecg-comaas-docker.tgz -H 'Authorization: token $TOKEN' https://github.corp.ebay.com/api/v3/repos/ecg-comaas/docker/tarball ;
            tar xf ecg-comaas-docker.tgz ;
            mv ecg-comaas-docker-* ecg-comaas-docker
            """
    }
}

void setGitCommitInfo(String targetDir) {
    env.LATEST_GIT_HASH = sh(
            script: "cd $targetDir; git log -1 --format=%h",
            returnStdout: true
    ).trim()

    env.LATEST_GIT_TIMESTAMP = sh(
            script: "cd $targetDir; date -d @\$(git log -n1 --format=%at) +%Y%m%d-%H%M",
            returnStdout: true
    ).trim()

    env.LATEST_GIT_AUTHOR = sh(
            script: "cd $targetDir; git log -1 --format=%ae",
            returnStdout: true
    ).trim()
}

String getTriggerer() {
    // 1. User who triggered the build
    String buildUser
    wrap([$class: 'BuildUser']) {
        if (env.BUILD_USER_ID != null) {
            echo "triggered by $env.BUILD_USER_ID from env.BUILD_USER_ID"
            buildUser = env.BUILD_USER_ID
        }
    }
    if (buildUser != null) {
        // the return should be outside of the "wrap" block
        return buildUser
    }

    // 2. PR trigger person by typing "test this please"
    if (env.ghprbTriggerAuthorLoginMention != null) {
        echo "triggered by $env.ghprbTriggerAuthorLoginMention from env.ghprbTriggerAuthorLoginMention"
        return env.ghprbTriggerAuthorLoginMention
    }

    // 3. Author of the pull request
    if (env.ghprbPullAuthorLogin != null) {
        echo "triggered by $env.ghprbPullAuthorLogin from env.ghprbPullAuthorLogin"
        return env.ghprbPullAuthorLogin
    }

    // 4. Author of the last change, only set if project was checked out using checkoutComaasCentralRepo()
    if (env.LATEST_GIT_AUTHOR != null) {
        String userIdFromEmail = getUserIdFromEmail(env.LATEST_GIT_AUTHOR as String)
        echo "triggered by $userIdFromEmail from env.LATEST_GIT_AUTHOR"
        return userIdFromEmail
    }

    echo "Unknown triggerer, returning null"
    return null
}

static String getUserIdFromEmail(String email) {
    return email.split("@")[0]
}

void sendSlackNotification(String colour, String channel, String message) {
    if (channel == 'ar' || channel == 'mx' || channel == 'sg' || channel == 'za') {
        channel = 'bolt'
    }
    sendToSlack(colour, channel, message)
}

void sendToSlackJenkinsChannel(String colour, String message) {
    sendToSlack(colour, "jenkins", message)
}

void sendToSlackProdChannel(String colour, String message) {
    sendToSlack(colour, "production", message)
}

// Channel will be prepended with 'ecg-comaas-', for your convenience
private void sendToSlack(String colour, String channel, String message) {
    String triggerer = getTriggerer()
    if (triggerer != null) {
        if (triggerer.startsWith('@')) {
            triggerer = triggerer.substring(1)
        }
        message = "@$triggerer " + message
    }

    if (channel == null) {
        // this sends the message to ecg-comaas-jenkins instead of the tenant channel
        channel = 'jenkins'
    }

    slackSend([
            botUser          : true,
            color            : "$colour",
            channel          : "ecg-comaas-$channel",
            teamDomain       : 'ebayclassifiedsgroup',
            tokenCredentialId: 'SlackToken',
            message          : "$message"
    ])
}

def loginToDocker() {
    echo "Logging in to Docker repository"
    def secrets = [
            [
                    $class: 'VaultSecret',
                    path: 'secret/nomad/dock.es.ecg.tools/comaas+robot_upload',
                    secretValues: [
                            [$class: 'VaultSecretValue', envVar: 'DOCKER_USERNAME', vaultKey: 'username'],
                            [$class: 'VaultSecretValue', envVar: 'DOCKER_PASSWORD', vaultKey: 'password']
                    ],
            ],
    ]
    wrap([$class: 'VaultBuildWrapper', vaultSecrets: secrets]) {
        sh "docker login -u=\"${DOCKER_USERNAME}\" -p=\"${DOCKER_PASSWORD}\" dock.es.ecg.tools"
    }
}

String getUID() {
    return sh(
            script: "echo \$UID",
            returnStdout: true
    ).trim()
}

static Integer toIntOrNull(String value) {
    if (value != null) {
        return value as Integer
    }
    return null
}

boolean isDirPresent(String location, String dirname) {
    sh "mkdir -p $location"
    String dirsCount = sh(
            script: "cd $location; find . -maxdepth 1 -type d -name $dirname | wc -l",
            returnStdout: true
    ).trim()
    return 1 == toIntOrNull(dirsCount)
}

void sendGrafanaMetric(String tenant, String dc, String environment, String event, String message) {
    String payload = JsonOutput.toJson(["text": message, "tags": ["comaas", event, tenant]])
    sh "curl -s -H 'Content-Type: application/json' -H 'Authorization: Bearer eyJrIjoiTmdhSUxKZGxvalp6c3BxZTU4NEFvaTNxWkdKSndma2QiLCJuIjoiamVua2lucyIsImlkIjoxfQ==' -X POST 'https://grafana.comaas-${environment}.${dc}.cloud/api/annotations/' -d '$payload'"
}

void forcefullyRemoveStaleDockerContainers() {
    sh "docker ps --all --format {{.Names}} | grep comaas | xargs --no-run-if-empty docker rm --force --volumes; docker system prune --force"
}

void tagWithVersion(String gitHash, String version) {
    def date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date())

    String data = '{"tag": "' + version + '", ' +
            '"message": "version ' + version + '", ' +
            '"object": "' + gitHash + '", ' +
            '"type": "commit", ' +
            '"tagger": { ' +
            '   "name": "GITHUBBOT", ' +
            '   "date": "' + date + '", ' +
            '   "email": "DL-eCG-comaas-dev@ebay.com" ' +
            '}}'
    withCredentials([string(credentialsId: 'GITHUBBOT-COMAAS-PAT', variable: 'TOKEN')]) {
        final String tagJson = sh(
                script: "curl --silent --location --fail --request POST --header 'Content-Type: application/json' --header 'Authorization: token $TOKEN' --data '$data' https://github.corp.ebay.com/api/v3/repos/ecg-comaas/central/git/tags" +
                        " | jq '{sha: .sha, ref: (\"refs/tags/\" + .tag)}'",
                returnStdout: true
        )
        sh "curl --silent --location --fail --request POST --header 'Content-Type: application/json' --header 'Authorization: token $TOKEN' --data '$tagJson' https://github.corp.ebay.com/api/v3/repos/ecg-comaas/central/git/refs"
    }
}

// Gets the SHA / git hash of the commit that 'master' is pointing to and puts it in env.MASTER_SHA
String getMasterSha() {
    withCredentials([string(credentialsId: 'GITHUBBOT-COMAAS-PAT', variable: 'TOKEN')]) {
        final String masterSha = sh(
                script: "curl --silent --location --fail --header \"Authorization: token $TOKEN\" https://github.corp.ebay.com/api/v3/repos/ecg-comaas/central/branches/master" +
                        " | jq -r .commit.sha",
                returnStdout: true
        ).trim()
        env.MASTER_SHA = masterSha
    }
    return env.MASTER_SHA
}

String getGitHashForTag(String tag) {
    withCredentials([string(credentialsId: 'GITHUBBOT-COMAAS-PAT', variable: 'TOKEN')]) {
        final String tagObj = sh(
                script: "curl --silent --location --fail --header \"Authorization: token $TOKEN\" https://github.corp.ebay.com/api/v3/repos/ecg-comaas/central/git/refs/tags/$tag" +
                        " | jq -r .object",
                returnStdout: true
        ).trim()

        def tagObjJson = new JsonSlurper().parseText(tagObj)
        final String type = tagObjJson.type
        final String sha = tagObjJson.sha
        final String url = tagObjJson.url
        // Yes, really. Jenkins/Groovy cannot serialize LazyMap (output of JsonSlurper().parseText())
        // When entering the sh() closure below, Jenkins(?) will try to serialize the current context, which fails.
        tagObjJson = null
        if (type == 'commit') {
            env.TEMP = sha
        }
        if (type == 'tag') {
            env.TEMP = sh(
                    script: "curl --silent --location --fail --header \"Authorization: token $TOKEN\" $url" +
                            " | jq -r .object.sha",
                    returnStdout: true
            ).trim()
        }
    }

    return env.TEMP
}

Closure wrapWithStepDefinition(param, closure, String commit, String stageName) {
    return {
        stage("$stageName") {
            node('qa') {
                try {
                    checkoutComaasCentralRepo(commit)
                    loginToDocker()

                    if (param == null) {
                        closure()
                    } else {
                        closure(param)
                    }
                } catch (ex) {
                    if (env.FAILED_BUILD == null || env.FAILED_BUILD == false) {
                        env.FAILED_BUILD = true
                        exMessage = ex.getMessage()
                        slackMessage = """
                            |Stage `$stageName` produced `$exMessage`
                            |Builder - `$env.NODE_NAME`, (<$BUILD_URL|Build>)
                        """.stripMargin()
                        sendToSlackJenkinsChannel("#FF0000", "$slackMessage")
                    }
                    error "Stage $stageName failed with $exMessage"
                } finally {
                    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true])
                    publishHTML(target: [
                            allowMissing         : true,
                            alwaysLinkToLastBuild: false,
                            keepAll              : true,
                            reportDir            : 'coverage',
                            reportFiles          : 'index.html',
                            reportName           : "Junit Report"
                    ])
                    cleanWs notFailBuild: true
                }
            }
        }
    }
}
