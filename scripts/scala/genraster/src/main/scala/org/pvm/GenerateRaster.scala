package org.pvm

// http://spatialreference.org/ref/esri/102729/postgis/

import geotrellis.geometry._
import geotrellis.logic._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.logic.applicative._
import geotrellis.logic.applicative.Implicits._
import geotrellis.rest.op.string._
import geotrellis.io._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.raster.op.transform._
import geotrellis.op._
import geotrellis.vector.op.data._
import geotrellis.process._
import geotrellis._

object AnApp {
  lazy val server = Server("myapp", "src/main/resources/myapp-catalog.json")
  lazy val cxn = Postgres.connect("jdbc:postgresql://localhost/phillyvotes","phillyvotes","phillyvotes")
  
  def postgresReader = new PostgresReader(cxn)
}

object GenerateRaster extends App {

  val extent = Extent(-8383359, 4845908, -8339609, 4890678)
  val rasterExtent = RasterExtent(extent, 5, 5, 
                                  ((extent.xmax - extent.xmin)/5).toInt,
                                  ((extent.ymax - extent.ymin)/5).toInt)
  
  val mutableRasterData:MutableRasterData = new IntArrayRasterData(Array.ofDim(rasterExtent.rows * rasterExtent.cols), rasterExtent.cols, rasterExtent.rows)

  val tbl = "voters_scrubbed"
  val typ = "case political_party when 'D' then 1 when 'R' then 2 else '3' end"
  val sql = "SELECT ST_Transform(ST_SetSRID(loc, 4326), 900913),%s from %s where loc is not null" format (typ,tbl)

  println("Executing sql %s" format sql)
  val features = AnApp.postgresReader.getFeatures(sql, 0, 1).map(_.asInstanceOf[Point]).map(p => (rasterExtent.mapToGrid(p.x,p.y),p.value))
  val NODATA = Integer.MIN_VALUE
  for(((x,y), counttype) <- features) {
    val vnd = mutableRasterData.get(x,y)
    val v = if (vnd == NODATA) 0 else vnd

    val n = counttype match { // IIII IIII RRRR RRRR RRRR DDDD DDDD DDDD
      case 1 => 1
      case 2 => (1 << 12)
      case 3 => (1 << 24)
      case a => sys.error("invalid value %d" format a)
    }
    mutableRasterData.set(x, y, n + v)
  }

  val completedRaster = new Raster(mutableRasterData, rasterExtent)
  // val k = Kernel(101, Kernel.Function.gaussian(100, 30, 30))
  // val features:Array[Point] = AnApp.postgresReader.getFeatures(sql, 0).map(_.asInstanceOf[Point])
  // val completedRaster = AnApp.server.run(KernelDensity(rasterExtent, k, features))

  println("Min/max: " + completedRaster.findMinMax)
  
  val w = new ArgWriter(TypeInt)
  w.write("/tmp/voter/voter_count.arg", completedRaster, "voter_count")
  
  println("Done Writing")
  sys.exit(0)
}
