package org.pvm

import geotrellis.geometry._
import geotrellis.logic._
import geotrellis.data._
import geotrellis.logic.applicative._
import geotrellis.logic.applicative.Implicits._
import geotrellis.rest.op.string._
import geotrellis.io._
import geotrellis.raster.op.local._
import geotrellis.statistics.op.stat._
import geotrellis.statistics._
import geotrellis.raster.op.transform._
import geotrellis.op._
import geotrellis.vector.op.data._
import geotrellis.process._
import geotrellis._

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

import net.liftweb.json._
import net.liftweb.json.Serialization.{write}

import org.pvm.op._

package object rest {

  object AnApp {
    val server = Server("myapp", "src/main/resources/myapp-catalog.json")
    lazy val cxn = Postgres.connect("jdbc:postgresql://localhost/phillyvotes","phillyvotes","phillyvotes")
    
    def postgresReader = new PostgresReader(cxn)
    def response(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
  }


  sealed trait Party {}
  object Parties {
    val Dem = new Party() {}
    val Rep = new Party() {}
    val Ind = new Party() {}
  }

  /**
   * Voter # data is stored in 32 bytes as:
   * AAAA AAAA BBBB BBBB BBBB CCCC CCCC CCCC
   * Where:
   * A = Independent Voters
   * B = Republican Voters
   * C = Democratic Voters
   *
   * This operation splits the raster based on the party
   */
  case class SplitRaster(combined: Op[Raster], party: Op[Party]) extends Op2[Raster,Party,Raster](combined,party) ({
    (c: Raster, p: Party) => Result(p match {
      case Parties.Dem => c.map(_ & 0xFFF)
      case Parties.Rep => c.map(i => (i >> 12) & 0xFFF)
      case Parties.Ind => c.map(_ >> 24)
    })
  })

  @Path("/zones")
  class SimpleZones {
    /**
     * Perform a zonal histogram over the voter population
     * raster given a boundary layer
     *
     * The boundary layer must be a table in postgres
     * with a 'the_geom' column containing simple polygons
     */
    @GET
    def get(@QueryParam("layer") layer: String, 
            @QueryParam("simpl") s: String) = {
      val voters = LoadRaster("voter_count")

      val sql = "SELECT ST_SimplifyPreserveTopology(ST_Transform(the_geom, 3857), " + s + "), cast(ward_num as int) from %s" format layer
      val features:Array[Polygon] = AnApp.postgresReader.getFeatures(sql, 0, 1).map(_.asInstanceOf[Polygon])

      // val rasters = Map("dems" -> Parties.Dem,
      //                   "reps" -> Parties.Rep,
      //                   "inds" -> Parties.Ind) map {
      //   case (k,v) => AnApp.server.run(GetHistogram(voters)) //reduceZonalHistogram(SplitRaster(voters, v), features))
      // }                       

      val rastersR:Map[Int,Int] = AnApp.server.run(reduceZonalHistogram(SplitRaster(voters, Parties.Dem), features))

      val rasters = rastersR.foldLeft(Map[String,Int]()) { (m,kv) =>
        m + (kv._1.toString -> kv._2)
                                                        }

      implicit val formats = Serialization.formats(NoTypeHints)
              
      AnApp.response("application/json")(write(rasters))
    }

    def reduceZonalHistogram(r: Op[Raster], f: Array[Polygon]) =
      sumHistograms <@> PolygonalZonalHistograms(f, r)
    
    val sumHistograms:Map[Int,Histogram] => Map[Int,Int] = 
      (a: Map[Int, Histogram]) =>
        a.mapValues( v => sumHistogram(v) )

    def sumHistogram(h: Histogram):Int = {
      var sum = 0
      h.foreach {
        case (a,b) => sum += a*b
      }
      sum
    }
    
  }
}


