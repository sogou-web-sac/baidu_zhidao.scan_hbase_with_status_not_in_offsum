import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.protobuf.ProtobufUtil
import org.apache.hadoop.hbase.util.{Base64, Bytes}
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.Cell
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object SparkOnHBase {

  def convertScanToString(scan: Scan) = {
    val proto = ProtobufUtil.toScan(scan)
    Base64.encodeBytes(proto.toByteArray)
  }

  def m_map(x:(ImmutableBytesWritable, Result)) : (String, String, Int, Long, String) = {
    // please look at http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/client/Result.html to understand what "Result" is.
    var result = x._2
    var key = Bytes.toString( result.getRow() )
    var url = Bytes.toString( result.getValue("u".getBytes, "u".getBytes) )
    var value = Bytes.toString( result.getValue("i".getBytes, "status".getBytes) )
    var versions = result.getColumnCells("i".getBytes, "status".getBytes)
    var ts_last_fetch = 0L
    if (versions.size >= 1) {
      ts_last_fetch = versions.get(0).getTimestamp() // ts_last_fetch is 12bit
    }
    // Test
    //var url = Bytes.toString( result.getRow() )
    //var value = Bytes.toString( result.getValue("info".getBytes, "status".getBytes) )
    //var versions = result.getColumnCells("info".getBytes, "status".getBytes)
    //var ts_last_fetch = 0L
    //if (versions.size >= 1) {
    //  ts_last_fetch = versions.get(0).getTimestamp() // ts_last_fetch is 12bit
    //}
    return (url, value, versions.size, ts_last_fetch, key)
  }
 
  def prob_select(x: (String, String, Int, Long, String)) : Boolean = {
    // If fetching time <= 2, just select it
    // othervise, do probability selecting
    if (x._3 <= 3) {
      return true
    }
    val rand = scala.util.Random
    val r = rand.nextInt(x._3-2)
    if (r == 0) {
      return true
    }
    return false
  }
  
  def rm_recent_fetched(x: (String, String, Int, Long, String)) : Boolean = {
    // rm last status is within 5 hours
    var ts_cur = System.currentTimeMillis // ts_cur is 12bit
    if (ts_cur - x._4 > 5*3600*1000)
      return true
    return false
  }

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("zhidao_baidu.scan_urls_not_in_offsum")
    val sc = new SparkContext(sparkConf)

    var input_hbase_table = args(0)
    var output = args(1)
    var max_select_num = args(2).toInt

    val conf = HBaseConfiguration.create()
    conf.set(TableInputFormat.INPUT_TABLE, input_hbase_table)

    var scan = new Scan()
    scan.setMaxVersions(365)
    conf.set(TableInputFormat.SCAN, convertScanToString(scan))

    val hbase_rdd = sc.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[ImmutableBytesWritable],
      classOf[Result])

    // input_rdd is a array of tuple: [(ImmutableBytesWritable, Result), (ImmutableBytesWritable, Result), ...]
    // TODO
    var rdd = hbase_rdd.map(m_map).filter(p => (p._2 != null) && (p._2 == "N"))
    rdd.filter(prob_select).filter(rm_recent_fetched).repartition(10).saveAsTextFile(output)
    
    // Test
    //var rdd = hbase_rdd.map(m_map).filter(p => (p._2 != null) && (p._2 == "open"))
    //var l = rdd.filter(rm_recent_fetched).collect()
    //for (e <- l) {
    //  println(e)
    //}
  }
}
