#!/usr/bin/env groovy
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

static def allTenants() { ["ebayk", "gtau", "gtuk", "it", "kjca", "mde", "mp", "ar", "mx", "sg", "za"] }

static def activeTenants(List disabled_tenants) {
    def all = allTenants()
    all.removeAll(disabled_tenants)
    return all
}

static def tenantAliases() {
    [
            'mp'   : 'mp',
            'ebayk': 'ek',
            'it'   : 'it',
            'kjca' : 'ca',
            'mde'  : 'mo',
            'gtuk' : 'uk',
            'gtau' : 'au',
            'ar'   : 'ar',
            'mx'   : 'mx',
            'sg'   : 'sg',
            'za'   : 'za',
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
            extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: relativeDir]],
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
    String version = sh(
            script: "curl -s ${dc}.${tenant}.${environmentShort}.comaas.cloud/health | jq -r .version",
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
    if (ip.startsWith("10.32.24.")) {
        dc = "ams1"
    }
    if (ip.startsWith("10.32.56.")) {
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
    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'docker-registry',
                      usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS']]) {
        sh "docker login -u $DOCKER_USER -p $DOCKER_PASS docker-registry.ecg.so"
    }
}

String getUID() {
    return sh(
            script: "echo \$UID",
            returnStdout: true
    ).trim()
}

def uploadToSwift(String tenant, String version, String pwd) {
    echo "Uploading Comaas for $tenant with version $version to Swift (from $pwd)"

    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'swift',
                      usernameVariable: 'SWIFT_USER', passwordVariable: 'SWIFT_PASS']]) {
        final String userId = getUID()

        final String file = "comaas-${tenant}_${version}.tar.gz"
        final String target = "${tenant}/builds/${file}"

        echo sh(
                script: "docker run --rm " +
                        "-v ${pwd}:/objects " +
                        "--network host " +
                        "-u $userId " +
                        "-e OS_AUTH_URL=https://keystone.dus1.cloud.ecg.so/v2.0 " +
                        "-e OS_TENANT_NAME='comaas-qa' " +
                        "-e OS_USERNAME='$SWIFT_USER' " +
                        "-e OS_PASSWORD='$SWIFT_PASS' " +
                        "-e OS_PROJECT_NAME='comaas-control-prod' " +
                        "-e OS_REGION_NAME='dus1' " +
                        "ebayclassifiedsgroup/python-swiftclient:3.5.0 " +
                        "swift upload --skip-identical --object-name $target comaas /objects/$file",
                returnStdout: true
        )
    }
}

boolean isVersionPresentInSwift(String tenant, String version) {
    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'swift',
                      usernameVariable: 'SWIFT_USER', passwordVariable: 'SWIFT_PASS']]) {

        String userId = getUID()

        String requiredFilesFoundInSwift = sh(
                script: "[ 1 == \$( docker run --rm " +
                        "--network host " +
                        "-u $userId " +
                        "-e OS_AUTH_URL=https://keystone.dus1.cloud.ecg.so/v2.0 " +
                        "-e OS_TENANT_NAME='comaas-qa' " +
                        "-e OS_USERNAME='$SWIFT_USER' " +
                        "-e OS_PASSWORD='$SWIFT_PASS' " +
                        "-e OS_PROJECT_NAME='comaas-control-prod' " +
                        "-e OS_REGION_NAME='dus1' " +
                        "ebayclassifiedsgroup/python-swiftclient:3.5.0 swift list --prefix $tenant comaas " +
                        "| grep -c $version ) ]",
                returnStatus: true
        )

        return requiredFilesFoundInSwift == "0"
    }
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

def createGraphitePayload(String event, String tenant, String message) {
    // this expects the tenant alias aka short name
    return JsonOutput.toJson(["what": "Comaas - deploy", "tags": ["comaas", "$event", "$tenant"], "data": "$message"])
}

void sendGraphiteMetric(String dc, String envName, String jsonPayload) {
    sh "curl -s -X POST 'https://graphite.${dc}.cloud.ops.${envName}.comaas.ecg.so/events/' -d '$jsonPayload'"
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
