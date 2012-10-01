package org.pvm.rest

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

@Path("/geometry")
class GeomService {
  /**
   * Perform a zonal histogram over the voter population
   * raster given a boundary layer
   *
   * The boundary layer must be a table in postgres
   * with a 'the_geom' column containing simple polygons
   */
  @GET
  @Path("/boundary")
  def get(@QueryParam("layer") layer: String, 
          @QueryParam("lat") lat: String,
          @QueryParam("lng") lng: String) = 
  {
    val pt = "ST_GeomFromText('POINT(%s %s)',4326)" format (lng, lat)
    val sql = "SELECT ward_num FROM %s WHERE ST_Contains(the_geom, %s)" format(layer,pt)
    
    val rs = AnApp.postgresReader.run(sql)
    rs.next()

    implicit val formats = Serialization.formats(NoTypeHints)              
    val m = Map("id" -> rs.getString(1))
    AnApp.response("application/json")(write(m))
  }              
}
