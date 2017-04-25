name := "zhidao_baidu.scan_urls_not_in_offsum"

version := "1.0"

scalaVersion := "2.10.6" // use your scala version instead

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.1",  // use your spark version instead
  "org.apache.hadoop" % "hadoop-core" % "1.2.1",
  "org.apache.hbase" % "hbase" % "1.2.4",
  "org.apache.hbase" % "hbase-client" % "1.2.4",
  "org.apache.hbase" % "hbase-common" % "1.2.4",
  "org.apache.hbase" % "hbase-server" % "1.2.4"
)
