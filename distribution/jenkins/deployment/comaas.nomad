job "comaas-[[ .tenant_short ]]" {
  region = "[[ .region ]]"
  datacenters = ["zone1", "zone2", "zone3", "zone4"]

  type = "service"

  meta {
    docker_image = "[[.registry_namespace]]/comaas-[[ .tenant ]]:[[.version]]"
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

    update {
      max_parallel = [[.newmsg_max_parallel]]
    }

    task "newmsg" {
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
        HEAP_SIZE = "[[ .newmsg_heap_size ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        MAIL_PROVIDER_STRATEGY = "kafka"
        HAZELCAST_GROUP_NAME = "[[.hazelcast_group_name]]"
      }

      service {
        name = "${JOB}"
        tags = [
          "version-[[.version]]",
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
        image = "ebayclassifiedsgroup/filebeat:5.6.4"
        args = [
          "-c", "/local/config/filebeat.yml"
        ]

        network_mode = "host"
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

    update {
      max_parallel = [[.api_max_parallel]]
    }

    task "http" {
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
        HEAP_SIZE = "[[ .api_heap_size ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        HAZELCAST_GROUP_NAME = "[[.hazelcast_group_name]]"
      }

      service {
        name = "${JOB}"
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
        image = "ebayclassifiedsgroup/filebeat:5.6.4"
        args = [
          "-c", "/local/config/filebeat.yml"
        ]

        network_mode = "host"
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

  group "cronjob" {

    count = 1

    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    task "cronjob" {
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
        HEAP_SIZE = "[[ .cronjob_heap_size ]]"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        COMAAS_RUN_CRON_JOBS = "true"
        HAZELCAST_GROUP_NAME = "[[.hazelcast_group_name]]"
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
          "prometheus"
        ]
      }

      resources {
        cpu    = [[.cronjob_resources_cpu]]
        memory = [[.cronjob_resources_mem]]
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
        image = "ebayclassifiedsgroup/filebeat:5.6.4"
        args = [
          "-c", "/local/config/filebeat.yml"
        ]

        network_mode = "host"
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
