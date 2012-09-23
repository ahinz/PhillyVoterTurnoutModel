package org.pvm

// http://spatialreference.org/ref/esri/102729/postgis/

import geotrellis.geometry._
import geotrellis.logic._
import geotrellis.data._
import geotrellis.logic.applicative._
import geotrellis.logic.applicative.Implicits._
import geotrellis.rest.op.string._
import geotrellis.io._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.raster.op.transform._
import geotrellis.raster.op.focal._
import geotrellis.op._
import geotrellis.vector.op.data._
import geotrellis.process._
import geotrellis._

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

object AnApp {
  lazy val server = Server("myapp", "src/main/resources/myapp-catalog.json")
  lazy val cxn = Postgres.connect("jdbc:postgresql://localhost/phillyvote","phillyvote","phillyvote")
  
  def postgresReader = new PostgresReader(cxn)
  def response(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}

/**
 * Simple hello world rest service that responds to "/hello"
 */
@Path("/hello")
class TemplateResource {
  @GET
  def hello() = "<h2>Hello GeoTrellis!</h2>"
}


@Path("/wms/")
class Boundaries {

  //v = 0 # 0xFFFFFF00
  //v = M # 0xFF0000FF
  def v2g(v: Int, m: Int, colorMask: Int) = {
    val v2inv = if (v > m) 0 else (m - v)
    val v2 = if (v > m) m else v
    val ch = (255.0 * v2.toDouble / m.toDouble).toInt
    val chinv = 255 - ch
    ((chinv << 24) + (chinv << 16) + (chinv << 8) + ch) | colorMask
  }

  @GET
  @Path("voter_density")
  def getd(@QueryParam("bbox") s1: String,
          @QueryParam("width") s2: String,
          @QueryParam("height") s3: String,
          @QueryParam("party") p: String,
          @QueryParam("rs") rs: String,
          @QueryParam("ds") ds: String,
          @Context req: HttpServletRequest) = {

    val be = ParseExtent(s1)
    val b = ((e:Extent) => RasterExtent(e, 5.0, 5.0, ((e.xmax - e.xmin)/5.0).toInt, ((e.xmax - e.xmin)/5.0).toInt)) <@> be
    println("Loading: " + AnApp.server.run(b))
    
    val maxd = 35000 //Integer.parseInt(ds)
    val maxr = if (p == "skew") 3000 else 15000 // Integer.parseInt(rs)

    val rm = if (p == "diff" || p == "skew") {
      val dems = LoadRaster("voter_density_d", b)
      val reps = LoadRaster("voter_density_r", b)
      val diff = Subtract(dems, reps)
      //-3329
      DoCell(diff, (c:Int) => if (c == Integer.MIN_VALUE) { 0x00 }
             else if (c < 0) { v2g(-c,maxr,0xFF000000) }
             else { v2g(c,maxd,0x0000FF00) })
    } else {

      val r = LoadRaster("voter_density_%s" format p, b)
      val breaks = ColorBreaks(Array((0,0xFF000010), (1,0x00FF0010), (2, 0x0000FF10)))

      val colorMask = 
        if (p == "r") { 0xFF000000 }
        else { 0x0000FF00 }
      

      // render the png
      // val pngOp = RenderPNG(r, breaks, 0, true)
      val maxv = if (p == "d") maxd else maxr
      DoCell(r, (c:Int) => if (c > 0) { v2g(c,maxv, colorMask) } else { 0x00 })
    }

    val pngOp = RenderPngRgba(ResampleRaster(rm, ParseInt(s2), ParseInt(s3)))

    try {
      val img = AnApp.server.run(pngOp)
      println("Done!")
      AnApp.response("image/png")(img)
    } catch {
      case e => AnApp.response("text/plain")(e.toString)
    }
  }


  @GET
  @Path("/boundaries")
  def get(@QueryParam("bbox") s1: String,
          @QueryParam("width") s2: String,
          @QueryParam("height") s3: String,
          @Context req: HttpServletRequest) = {

    val w = Integer.parseInt(s2) 
    val h = Integer.parseInt(s3)
    val b = ParseRasterExtent(s1, w.toString, h.toString)
    val r1 = Raster.empty _ <@> b

    val e = AnApp.server.run(b)
    val p1 = "%s %s" format (e.extent.xmin,e.extent.ymin)
    val p2 = "%s %s" format (e.extent.xmin,e.extent.ymax)
    val p3 = "%s %s" format (e.extent.xmax,e.extent.ymax)
    val p4 = "%s %s" format (e.extent.xmax,e.extent.ymin)

    val tbl = "philadelphiacouncildistricts_2000"

    val bbox = "ST_Transform(ST_SetSRID(ST_GeomFromText('POLYGON ((%s, %s, %s, %s, %s))'), 3857), 102729)" format (p1,p2,p3,p4,p1)
    val sql = "SELECT ST_SimplifyPreserveTopology(ST_Transform(ST_SetSRID(the_geom, 102729), 3857), 3),1 from %s where ST_SetSRID(the_geom, 102729) && %s" format (tbl, bbox)

    val features:Array[Polygon] = AnApp.postgresReader.getFeatures(sql, 0).map(_.asInstanceOf[Polygon])

    val r = RasterizePolygonsWithValue(r1, features, 1)

    val breaks = ColorBreaks(Array((0,0xFF000010), (1,0x00FF0010), (2, 0x0000FF10)))

    // render the png
    //val pngOp = RenderPNG(r, breaks, 0, true)
    val rm = DoCell(r, (c:Int) => if (c > 0) { 0xFF00007F } else { 0x0 })
    val pngOp = RenderPngRgba(rm)

    try {
      val img = AnApp.server.run(pngOp)
      AnApp.response("image/png")(img)
    } catch {
      case e => AnApp.response("text/plain")(e.toString)
    }
  }
}
