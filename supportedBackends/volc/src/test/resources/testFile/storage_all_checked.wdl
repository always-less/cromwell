task hello {
  command {
    sleep 3000 && echo 'Hello world!'
  }
  output {
    File response = stdout()
  }
  runtime {
        image: "ml-platform-q6r4bhp2mj9zlw2kf7/pipeline:fastpv0.21.0-v0.0.1"
        flavor: "ml.g1ie.4xlarge"
        resource_queue_id: "q-20220316181239-65gt9"
        sidecar_image: "cr-cn-beijing.volces.com/ml_platform/cloudfs-init:test_sync_3"
        sidecar_memory_ratio: 0.5
        storages: [
          {
            "type": "Tos",
            "mount_path": "/cjh-cromwell-test/cromwell-data",
            "bucket": "cjh-cromwell-test",
            "prefix": "cromwell-data"
          },
            {
              "type": "Vepfs",
              "mount_path": "/cjh-cromwell-test/vepfs",
            },
        ]
    }
}

workflow test {
  call hello
}