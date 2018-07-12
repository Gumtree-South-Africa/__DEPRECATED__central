job "comaas-indexer-[[ .tenant_short ]]" {
  region = "ams1"
  datacenters = ["zone1", "zone2", "zone3", "zone4"]

  type = "batch"

  meta {
    docker_image = "dock.es.ecg.tools/comaas/comaas-[[ .tenant ]]:[[.version]]"
    version = "[[.version]]"
    esaas_username = "[[ .esaas_username ]]"
    esaas_password = "[[ .esaas_password ]]"
  }

  group "indexer" {
    constraint {
      attribute = "${node.class}"
      value = "services"
    }

    task "indexer" {
      driver = "docker"

      config {
        image = "dock.es.ecg.tools/comaas/comaas-[[ .tenant ]]:[[.version]]"
        network_mode = "host"

        auth {
          username = "[[.docker_username]]"
          password = "[[.docker_password]]"
        }
      }

      env {
        HEAP_SIZE = "6G"
        JAVA_OPTS = ""
        TENANT = "[[ .tenant ]]"
        IS_INDEXER_MODE = "true"
        INDEXER_DAYS_SINCE = "[[ .days_since ]]"
      }

      resources {
        cpu = 4000
        memory = 8192
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
          "-c",
          "/local/config/filebeat.yml"
        ]

        network_mode = "host"
      }

      template {
        data = <<EOH
---
fields_under_root: true
fields:
  allocation_id: ${NOMAD_ALLOC_ID}
  allocation_name: ${NOMAD_ALLOC_NAME}
  job_name: ${NOMAD_JOB_NAME}
  region: ${NOMAD_REGION}
filebeat.prospectors:
- fields.kafka_topic: logs
  json.add_error_key: true
  json.keys_under_root: true
  json.message_key: message
  json.overwrite_keys: true
  paths:
  - ${NOMAD_ALLOC_DIR}/logs/*.stderr.*
  type: log
logging.to_files: false
output.kafka:
  hosts: ["kafkalog.service.consul:9092"]
  topic: '%{[fields.kafka_topic]}'
  partition.round_robin:
    reachable_only: false
  required_acks: 1
  compression: gzip
  max_message_bytes: 1000000
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
