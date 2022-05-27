workflow myWorkflow {
    call myTask as task1
    call myTask as task2
}

task myTask {
    command {
        echo "hello volc cromwell"
        sleep 30
        echo end6
    }
    output {
        String out = read_string(stdout())
    }
    runtime {
        image: "ml_platform/pytorch:1.7"
        flavor: "ml.g1ie.large"
        resource_queue_id: "q-20220526181811-2s7d8"
        sidecar_image: "cr-cn-guilin-boe.volces.com/ml_platform/cloudfs-pub:ea5898c3f3c9d92de1ea88a7b3dbcb89"

    }



}