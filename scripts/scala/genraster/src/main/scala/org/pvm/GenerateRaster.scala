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
  lazy val cxn = Postgres.connect("jdbc:postgresql://localhost/phillyvote","phillyvote","phillyvote")
  
  def postgresReader = new PostgresReader(cxn)
}

object GenerateRaster extends App {

  def genraster(party: String, output: String) = {
    val extent = Extent(-8383359, 4845908, -8339609, 4890678)
    val rasterExtent = RasterExtent(extent, 5, 5, 
                                    ((extent.xmax - extent.xmin)/5).toInt,
                                    ((extent.ymax - extent.ymin)/5).toInt)
    
    val mutableRasterData:MutableRasterData = new IntArrayRasterData(Array.ofDim(rasterExtent.rows * rasterExtent.cols), rasterExtent.cols, rasterExtent.rows)

    val tbl = "voters_scrubbed"
    val sql = "SELECT ST_Transform(ST_SetSRID(loc, 4326), 900913) from %s where loc is not null and political_party = '%s'" format (tbl,party)

    // val features = AnApp.postgresReader.getFeatures(sql, 0).map(_.asInstanceOf[Point]).map(p => rasterExtent.mapToGrid(p.x,p.y))
    // val NODATA = Integer.MIN_VALUE
    // for(ft <- features) {
    //   val v = mutableRasterData.get(ft._1, ft._2)
    //   val n = if (v == NODATA) 1 else v + 1
    //   mutableRasterData.set(ft._1, ft._2, n)
    // }

    val k = Kernel(101, Kernel.Function.gaussian(100, 30, 30))
    val features:Array[Point] = AnApp.postgresReader.getFeatures(sql, 0).map(_.asInstanceOf[Point])
    val completedRaster = AnApp.server.run(KernelDensity(rasterExtent, k, features))

    println("Min/max: " + completedRaster.findMinMax)
    
    val w = new ArgWriter(TypeInt)
    w.write("/tmp/voter/voter_density_%s.arg" format output, completedRaster, "voter_density_%s" format output)
  }

  genraster("R","r")
  println("Done Writing (r)")

  genraster("D","d")
  
  println("Done Writing (d)")
  sys.exit(0)
}
