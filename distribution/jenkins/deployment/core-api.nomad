job "core-api-[[ .tenant ]]" {
  region = "[[ .region ]]"
  datacenters = ["zone1", "zone2", "zone3", "zone4"]

  type = "service"

  meta {
    wanted_instances_api = [[.api_count]]
    wanted_instances_newmsg  = [[.newmsg_count]]
    docker_image = "[[.registry_namespace]]/comaas-[[ .tenant ]]:[[.version]]"
    deploy_jenkins_job_nr = "[[.deploy_jenkins_job_nr]]"
    restart_jenkins_job_nr = "[[.restart_jenkins_job_nr]]"
    version = "[[.version]]"
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

    count = [[.newmsg_count]]

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    task "api" {
      driver = "docker"

      config {
        image = "[[.registry_namespace]]/comaas-[[ .tenant ]]:[[.version]]"
        network_mode = "host"

        auth {
          username = "[[.docker_username]]"
          password = "[[.docker_password]]"
        }
      }

      env {
        HEAP_SIZE = "2g"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        MAIL_PROVIDER_STRATEGY = "kafka"
      }

      service {
        name = "comaas-core-[[ .tenant ]]"
        tags = [
          "version-[[.version]]",
          "newmsg"
        ]
      }

      service {
        name = "comaas-core-[[ .tenant ]]"
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
        name = "comaas-core-[[ .tenant ]]"
        port = "prometheus"
        tags = [
          "prometheus"
        ]
      }

      resources {
        cpu    = [[.newmsg_resources_cpu]]
        memory = [[.newmsg_resources_mem]]
        network {
          mbits = 100
          // keep this, Comaas needs it to start up
          port "http" {}
          port "hazelcast" {}
          port "prometheus" {}
        }
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image = "docker-registry.ecg.so/comaas/filebeat:5.6.3"
        args = [
          "-c", "/local/config/filebeat.yml"
        ]

        network_mode = "host"
        auth {
          username = "[[.docker_username]]"
          password = "[[.docker_password]]"
        }
      }

      template {
        data = <<EOH
[[ .filebeat_config ]]
EOH
        destination = "local/config/filebeat.yml"
      }

      resources {
        cpu = 100
        memory = 256
        network {
          mbits = 1
        }
      }
    }
  },

  group "http" {

    count = [[.api_count]]

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    task "api" {
      driver = "docker"

      config {
        image = "[[.registry_namespace]]/comaas-[[ .tenant ]]:[[.version]]"
        network_mode = "host"

        auth {
            username = "[[.docker_username]]"
            password = "[[.docker_password]]"
        }
      }

      env {
        HEAP_SIZE = "2g"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
      }

      service {
        name = "comaas-core-[[ .tenant ]]"
        port = "http"
        tags = [
          "version-[[.version]]",
          "http",
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
        name = "comaas-core-[[ .tenant ]]"
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
        name = "comaas-core-[[ .tenant ]]"
        port = "prometheus"
        tags = [
          "prometheus"
        ]
      }

      resources {
        cpu    = [[.api_resources_cpu]]
        memory = [[.api_resources_mem]]
        network {
            mbits = 100
            port "http" {}
            port "hazelcast" {}
            port "prometheus" {}
        }
      }
    }

    task "filebeat" {
      driver = "docker"
      config {
        image = "docker-registry.ecg.so/comaas/filebeat:5.6.3"
        args = [
          "-c", "/local/config/filebeat.yml"
        ]

        network_mode = "host"
        auth {
            username = "[[.docker_username]]"
            password = "[[.docker_password]]"
        }
      }

      template {
        data = <<EOH
[[ .filebeat_config ]]
EOH
        destination = "local/config/filebeat.yml"
      }

      resources {
        cpu = 100
        memory = 256
        network {
          mbits = 1
        }
      }
    }
  }
}
