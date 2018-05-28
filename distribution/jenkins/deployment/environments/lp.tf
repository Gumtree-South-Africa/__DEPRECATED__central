variable tenant { default = "unset" }
variable tenant_short { default = "unset" }
variable environment { default = "dev" }
variable region { default = "ams1" }
variable registry_namespace { default = "dock.es.ecg.tools/comaas" }
variable docker_username { default = "unset" }
variable docker_password { default = "unset" }
variable vault_policy { default = "nomad-secret-readonly" }

variable api_count { default = 2 }
variable api_resources_cpu { default = 1100 }
variable api_resources_mem { default = 3096 }

variable newmsg_count { default = 2 }
variable newmsg_resources_cpu { default = 1100 }
variable newmsg_resources_mem { default = 3096 }

variable cronjob_resources_cpu { default = 100 }
variable cronjob_resources_mem { default = 1280 }

variable restart_jenkins_job_nr { default = "none" }

variable comaas_heap_size { default = "2G" }
variable cronjob_heap_size { default = "768M" }

variable filebeat_config {
    type = "string"
    default = <<EOF
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
  - ${NOMAD_ALLOC_DIR}/logs/api.stderr.*
  - ${NOMAD_ALLOC_DIR}/logs/cronjob.stderr.*
  type: log
- fields.kafka_topic: access_logs
  json.add_error_key: true
  json.keys_under_root: true
  json.overwrite_keys: true
  paths:
  - ${NOMAD_ALLOC_DIR}/logs/api.stdout.*
  - ${NOMAD_ALLOC_DIR}/logs/cronjob.stdout.*
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
EOF
}
