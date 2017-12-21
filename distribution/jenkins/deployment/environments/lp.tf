variable tenant { default = "unset" }
variable environment { default = "dev" }
variable region { default = "ams1" }
variable datacenters { type = "list" default = ["zone1", "zone2", "zone3"] }
variable registry_namespace { default = "docker-registry.ecg.so/comaas" }
variable registry_imagename { default = "comaas-gtuk" }
variable docker_username { default = "comaas-docker-registry" }
variable docker_password { default = "unset" }
variable vault_policy { default = "nomad-secret-readonly" }

variable node_class { default = "services" }

variable api_count { default = 3 }
variable api_resources_cpu { default = 4400 }
variable api_resources_mem { default = 3096 }

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
  type: log
- fields.kafka_topic: access_logs
  json.add_error_key: true
  json.keys_under_root: true
  json.overwrite_keys: true
  paths:
  - ${NOMAD_ALLOC_DIR}/logs/api.stdout.*
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
