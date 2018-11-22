job "comaas-[[ .tenant_short ]]" {
  region = "[[ .region ]]"
  datacenters = ["zone1", "zone2", "zone3", "zone4"]

  type = "service"

  meta {
    docker_image = "[[ .docker_namespace ]]/comaas:[[ .version ]]"
    version = "[[ .version ]]"
  }

  update {
    max_parallel = 1
    auto_revert = "true"
    health_check = "checks"
    min_healthy_time = "10s"
    healthy_deadline = "90s"
    stagger = "1m"
  }

  group "newmsg" {

    count = [[ .newmsg.count ]]

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    update {
      max_parallel = [[ .newmsg.max_parallel ]]
    }

    task "newmsg" {
      driver = "docker"

      config {
        image = "[[ .docker_namespace ]]/comaas:[[ .version ]]"
        network_mode = "host"

        auth {
          username = "[[ .docker_username ]]"
          password = "[[ .docker_password ]]"
        }
      }

      env {
        HEAP_SIZE = "[[ .newmsg.heap ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        MAIL_PROVIDER_STRATEGY = "kafka"
        HAZELCAST_GROUP_NAME = "[[ .hazelcast_group_name ]]"
        SWIFT_USERNAME = "[[ .swift_username ]]"
        SWIFT_PASSWORD = "[[ .swift_password ]]"
        SWIFT_KEYSTONE = "https://keystone.[[ .region ]].cloud.ecg.so/v2.0"
        ESAAS_USERNAME = "[[ .esaas_username ]]"
        ESAAS_PASSWORD = "[[ .esaas_password ]]"
        VERSION = "[[ .version ]]"
      }

      service {
        name = "${JOB}"
        tags = [
          "version-[[ .version ]]",
          "newmsg"
        ]
      }

      service {
        name = "${JOB}"
        port = "hazelcast"
        tags = [
          "hazelcast"
        ]
        check {
          type = "http"
          path = "/hazelcast/health"
          interval = "30s"
          timeout = "5s"
        }
      }

      service {
        name = "${JOB}"
        port = "prometheus"
        tags = [
          "prometheus",
          "alloc_id-${NOMAD_ALLOC_ID}"
        ]
      }

      resources {
        cpu    = [[ .newmsg.cpu ]]
        memory = [[ .newmsg.memory ]]
        network {
          mbits = 100

          // keep this, Comaas needs it to start up
          port "http" {}
          port "hazelcast" {}
          port "prometheus" {}
        }
      }
    }
  }

  group "http" {
    count = [[ .http.count ]]

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    update {
      max_parallel = [[ .http.max_parallel ]]
    }

    task "http" {
      driver = "docker"

      config {
        image = "[[ .docker_namespace ]]/comaas:[[ .version ]]"
        network_mode = "host"

        auth {
            username = "[[ .docker_username ]]"
            password = "[[ .docker_password ]]"
        }
      }

      env {
        HEAP_SIZE = "[[ .http.heap ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        HAZELCAST_GROUP_NAME = "[[.hazelcast_group_name]]"
        SWIFT_USERNAME = "[[ .swift_username ]]"
        SWIFT_PASSWORD = "[[ .swift_password ]]"
        SWIFT_KEYSTONE = "https://keystone.[[ .region ]].cloud.ecg.so/v2.0"
        ESAAS_USERNAME = "[[ .esaas_username ]]"
        ESAAS_PASSWORD = "[[ .esaas_password ]]"
      }

      service {
        name = "${JOB}"
        port = "http"
        tags = [
          "version-[[.version]]",
          "http",
          "traefik.enable=true",
          "traefik.frontend.rule=Host:[[ .tenant_short ]].[[ .environment ]].comaas.cloud",
          "urlprefix-[[ .tenant_short ]].[[ .environment ]].comaas.cloud/",
          "urlprefix-[[ .region ]].[[ .tenant_short ]].[[ .environment ]].comaas.cloud/"
          [[ .urlprefixes ]]
        ]
        check {
          type     = "http"
          path     = "/health"
          interval = "5s"
          timeout  = "2s"
        }
      }

      service {
        name = "${JOB}"
        port = "hazelcast"
        tags = [
          "hazelcast"
        ]
        check {
          type = "http"
          path = "/hazelcast/health"
          interval = "5s"
          timeout  = "2s"
        }
      }

      service {
        name = "${JOB}"
        port = "prometheus"
        tags = [
          "prometheus",
          "alloc_id-${NOMAD_ALLOC_ID}"
        ]
      }

      resources {
        cpu    = [[ .http.cpu ]]
        memory = [[ .http.memory ]]
        network {
            mbits = 100
            port "http" {}
            port "hazelcast" {}
            port "prometheus" {}
        }
      }
    }
  }

  group "cronjob" {
    count = 1

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    task "cronjob" {
      driver = "docker"

      config {
        image = "[[ .docker_namespace ]]/comaas:[[ .version ]]"
        network_mode = "host"

        auth {
          username = "[[ .docker_username ]]"
          password = "[[ .docker_password ]]"
        }
      }

      env {
        HEAP_SIZE = "[[ .cronjob.heap ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        COMAAS_RUN_CRON_JOBS = "true"
        HAZELCAST_GROUP_NAME = "[[ .hazelcast_group_name ]]"
        SWIFT_USERNAME = "[[ .swift_username ]]"
        SWIFT_PASSWORD = "[[ .swift_password ]]"
        SWIFT_KEYSTONE = "https://keystone.[[ .region ]].cloud.ecg.so/v2.0"
        ESAAS_USERNAME = "[[ .esaas_username ]]"
        ESAAS_PASSWORD = "[[ .esaas_password ]]"
      }

      service {
        name = "${JOB}"
        port = "http"
        tags = [
          "version-[[.version]]",
          "cronjob"
        ]
      }

      service {
        name = "${JOB}"
        port = "prometheus"
        tags = [
          "prometheus",
          "alloc_id-${NOMAD_ALLOC_ID}"
        ]
      }

      resources {
        cpu    = [[ .cronjob.cpu ]]
        memory = [[ .cronjob.memory ]]
        network {
          mbits = 100
          // keep this, Comaas needs it to start up
          port "http" {}
          port "hazelcast" {}
          port "prometheus" {}
        }
      }
    }
  }
}
