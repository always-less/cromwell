task hello {
  command {
    echo 'Hello world!' && sleep 300
  }
  output {
    File response = stdout()
  }
  runtime {
        image: "ml-platform-q6r4bhp2mj9zlw2kf7/pipeline:fastpv0.21.0-v0.0.1"
        flavor: "ml.g1ie.4xlarge"
        resource_queue_id: "q-20220316181239-65gt9"
        sidecar_image: "cr-cn-beijing.volces.com/ml_platform/cloudfs-init:test_sync_3"
        framework: "TensorFlowPS"
    }
}

workflow test {
  call hello
}