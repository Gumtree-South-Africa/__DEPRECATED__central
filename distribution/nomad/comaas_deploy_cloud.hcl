job "comaas-%TENANT%" {

  region = "global"

  name = "comaas-%TENANT%"
  type = "service"
  priority = 50
  all_at_once = false

  datacenters = [
    "ams1"
  ]

  constraint
  {
    attribute = "${node.class}"
    value = "%TENANT%"
    distinct_hosts = true
  }
  update {
    max_parallel = 1
    stagger = "30s"
  }

  group "server" {
    count = "%COUNT%"

    task "comaas" {
      driver = "raw_exec"
      config = {
        command = "bin/comaas"
      }

      env = {
        COMAAS_HTTP_PORT = "${NOMAD_PORT_comaas}"
      }
      service "comaas-%TENANT%" {
        tags = [
          "urlprefix-%TENANT_SHORT%.%ENVIRONMENT%.comaas.ecg.so/",
          "git-%GIT_HASH%"
        ]
        port = "comaas"

        check {
          type = "http"
          name = "health-endpoint"
          path = "/health"
          port = "comaas"

          interval = "10s"
          timeout = "20s"
        }
      }
      resources {
        cpu = 200
        memory = 1024
        network {
          mbits = 10
          port "comaas" {
          }
        }
      }

      kill_timeout = "5s"
      logs {
        max_files = 10
        max_file_size = 10
      }
      artifact {
        source = "%ARTIFACT%"
        destination = "/"
        # TODO add checksum
      }
    }
    restart {
      interval = "60s"
      attempts = 2
      delay = "15s"
      mode = "delay"
    }
  }
}
