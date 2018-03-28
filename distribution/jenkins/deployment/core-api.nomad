job "core-api-[[ .tenant ]]" {
  region = "[[ .region ]]"
  datacenters = [
    [[- range $index, $element := .datacenters -]]
      [[/* if element is not index 0 (bool false) prepend a comma */]]
      [[- if $index ]], [[ end -]]
      "[[- . ]]"
    [[- end -]]
  ]

  type = "service"

  meta {
    wanted_instances = [[.api_count]]
    docker_image = "[[.registry_namespace]]/comaas-[[ .tenant ]]:[[.version]]"
    wanted_instances_per_zone = "[[.api_count_per_zone]]"
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
//  vault {
//    policies    = [ "[[.vault_policy]]" ]
//    change_mode = "noop"
//    env         = "false"
//  }

  group "api" {

    count = [[.api_count]]

    constraint {
      operator = "distinct_property"
      attribute = "${node.datacenter}"
      value = "[[.api_count_per_zone]]"
    }

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
          "version-[[.version]]"
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
//        check {
//          type = "http"
//          path = "/hazelcast/health"
//          interval = "5s"
//          timeout  = "2s"
//          }
      }

      resources {
        cpu    = [[.api_resources_cpu]]
        memory = [[.api_resources_mem]]
        network {
            port "http" {}
            mbits = 100
            port "hazelcast" {}
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
        memory = 64
        network {
          mbits = 1
        }
      }
    }
  }
}
