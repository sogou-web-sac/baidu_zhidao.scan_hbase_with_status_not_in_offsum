#!/bin/bash

#INPUT_TABLE="push:test_table"
INPUT_TABLE="web:zhidao_baidu"
OUTPUT="/user/zhufangze/tmp/xxx"
MAX_SELECT_NUM=200000

hadoop fs -rmr ${OUTPUT}
$SPARK_HOME/bin/spark-submit \
  --name "zhidao_baidu.scan_urls_not_in_offsum" \
  --class "SparkOnHBase" \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 200 \
  --executor-cores 2 \
  --executor-memory 4G \
  --driver-memory 6G \
  target/scala-2.10/zhidao_baidu-scan_urls_not_in_offsum_2.10-1.0.jar \
  ${INPUT_TABLE} \
  ${OUTPUT} \
  ${MAX_SELECT_NUM}
