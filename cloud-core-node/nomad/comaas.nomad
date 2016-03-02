# There can only be a single job definition per file.
# Create a job with ID and Name 'example'
job "comaas-core" {
	# Run the job in the global region, which is the default.
	# region = "global"

	# Specify the datacenters within the region this job can run in.
	datacenters = ["ams1"]

	# Service type jobs optimize for long-lived services. This is
	# the default but we can change to batch for short-lived tasks.
	# type = "service"

	# Priority controls our access to resources and scheduling priority.
	# This can be 1 to 100, inclusively, and defaults to 50.
	# priority = 50

	# Restrict our job to only linux. We can specify multiple
	# constraints as needed.
	constraint {
		attribute = "$attr.kernel.name"
		value = "linux"
	}

	# Configure the job to do rolling updates
	update {
		# Stagger updates every 10 seconds
		stagger = "10s"

		# Update a single task at a time
		max_parallel = 1
	}

	# Create a 'cache' group. Each task in the group will be
	# scheduled onto the same machine.
	group "core" {
		# Control the number of instances of this groups.
		# Defaults to 1
		count = 3

		# Restart Policy - This block defines the restart policy for TaskGroups,
		# the attempts value defines the number of restarts Nomad will do if Tasks
		# in this TaskGroup fails in a rolling window of interval duration
		# The delay value makes Nomad wait for that duration to restart after a Task
		# fails or crashes.
		restart {
			interval = "5m"
			attempts = 10
			delay = "25s"
		}

		# Define a task to run
		task "deploy" {
			driver = "exec"

			config {
				artifact_source = "http://core001:8000/com.ecg.replyts.replyts-core_2.19.8-SNAPSHOT.tar.gz"
				command = "tar -xvf $NOMAD_TASK_DIR/com.ecg.replyts.replyts-core_2.19.8-SNAPSHOT.tar.gz; $NOMAD_TASK_DIR/com.ecg.replyts.replyts-core_2.19.8-SNAPSHOT/bin/main"
			}

			service {
				port = "http://localhost:18081"
				check {
					type = "http"
					path = "/"
					interval = "10s"
					timeout = "2s"
				}
			}

			# We must specify the resources required for
			# this task to ensure it runs on a machine with
			# enough capacity.
			resources {
				cpu = 500 # 500 Mhz
				memory = 256 # 256MB
				network {
					mbits = 10
				}
			}
		}
	}
}