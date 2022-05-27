## Cromwell on ML Platform

### 项目背景
- [Cromwell(Pipeline) on ML Platform](https://bytedance.feishu.cn/wiki/wikcnAwun8vCaLeI6l3vblm8gXb)
- [ML Platform - 工作流 PRD](https://bytedance.feishu.cn/docx/doxcnzUex7LLfPDVxLt6BGW4LHe)

### Mac本地开发调试
- [cromwell 官方教程](https://cromwell.readthedocs.io/en/stable/tutorials/FiveMinuteIntro/)
- [scala](https://docs.scala-lang.org/getting-started/index.html)
- [akka & actor 快速上手](https://zhuanlan.zhihu.com/p/25598361)

自定义训练backend扩展入口：cromwell.backend.impl.volc.VolcBackendLifecycleActorFactory


[todo] 抽时间配置下docker开发环境
- java 11 https://adoptopenjdk.net/installation.html
- sbt 
- idea
  - -Dconfig.file=volc/cromwell_server.conf
  - idea debug env(看情况修改): CROMWELL_BUILD_CENTAUR_SLICK_PROFILE=slick.jdbc.MySQLProfile$;CROMWELL_BUILD_CENTAUR_JDBC_DRIVER=com.mysql.cj.jdbc.Driver;CROMWELL_BUILD_CENTAUR_JDBC_URL=jdbc:mysql://localhost:3306/cromwell_test?allowPublicKeyRetrieval=true&useSSL=false&rewriteBatchedStatements=true&serverTimezone=UTC&useInformationSchema=true;CROMWELL_BUILD_RESOURCES_DIRECTORY=target/ci/resources;CROMWELL_BUILD_PAPI_JSON_FILE=target/ci/resources/cromwell-centaur-service-account.json;CROMWELL_BUILD_CENTAUR_READ_LINES_LIMIT=128000;CROMWELL_BUILD_CENTAUR_256_BITS_KEY=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=;CROMWELL_TOS_BUCKET=chong-test;CROMWELL_TOS_PREFIX=cromwell-test;CROMWELL_TOS_MOUNT_PATH=/Users/zhangchong/go/src/github.com/always-less/cromwell/cromwell-executions/volc-local-test;VOLC_AK=as;VOLC_SK=sdf;CROMWELL_BUILD_CENTAUR_JDBC_USER=root;CROMWELL_BUILD_CENTAUR_JDBC_PASSWORD=HzVAkwfTbk3^4=zG9vi}L9J

volc命令行工具使用文档：https://www.volcengine.com/docs/6459/72394

### boe环境部署测试

[boe环境配置文档](https://bytedance.feishu.cn/wiki/wikcnfyui3J6Le60XgqczOKnIhh)

- 首先本地构建cromwell server镜像：sh .volc/build.sh -e boe -v 78 -t v0.3 -b
- devbox 上配置kubectl，kubectl apply -f [cromwell-server deployment yaml](./cromwell-server-volc-deploy.yaml)
- 可以在boe vke页面运维查看deploy
  - [boe vke](https://console-stable.volcanicengine.com/vke/region:vke+cn-guilin-boe/cluster/cc705voursfeh841kat5g/pod?filterNamespace=mlplatform-pipeline)
- cromwell 任务提交
  - [boe cromwell service](https://console-stable.volcanicengine.com/vke/region:vke+cn-guilin-boe/cluster/cc705voursfeh841kat5g/service/mlplatform-pipeline/pipeline-cromwell/detail)
  - [boe cromwell ingress](https://console-stable.volcanicengine.com/vke/region:vke+cn-guilin-boe/cluster/cc705voursfeh841kat5g/ingress/mlplatform-pipeline/pipeline-cromwell/detail)
  - java -jar cromwell-78.jar submit -h https://cc705voursfeh841kat5g-ml-platform-cn-guilin-boe.bytedance.net/pipeline-cromwell myWorkflow.wdl
  - widdler [todo]
- 任务运维
  - 元信息保存在 [boe mysql](https://console-stable.volcanicengine.com/db/region:db+cn-guilin-boe/rds-mysql/detail/mysql-3c64a08dc375?tabKey=INSTANCE_INFO)

  

### 线上环境发版
[todo]