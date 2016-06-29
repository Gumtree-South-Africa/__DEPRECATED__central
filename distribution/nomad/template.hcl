job "comaas-kjca" {
  priority = 50

  datacenters = ["ams1"]
  region = "comaas-qa"

  type = "service"

  update {
    stagger = "30s"
    max_parallel = 1
  }

  group "comaas-node-kjca" {
    count = 3

    task "comaas-kjca" {
      driver = "raw_exec"

      artifact = {
        source = "http://buildmaster001/job/repo-server/ws/GIT_HASH-TIMESTAMP/ARTIFACT"

        options {
          checksum = "md5:59a4b97500a83274cd5fe21a34e5e113"
        }
      }

      config {
        command = "bin/comaas"
      }

      logs {
        max_files = 10
        max_file_size = 10
      }

      constraint {
        attribute = "${attr.hostname}"
        value = "core*"
        distinct_hosts = true
      }

      resources = {
        memory = 1024
        disk = 2000
        cpu = 200

        network {
          mbits = 100
          port "comaas_kjca" {
            static = 8080
          }
        }
      }

      service {
        tags = ["git-GIT_HASH"]
        port = "comaas_kjca"
        check {
          type = "http"
          name = "tenant_running"
          path = "/health"
          interval = "10s"
          timeout = "5s"
        }
      }
    }
  }
}
