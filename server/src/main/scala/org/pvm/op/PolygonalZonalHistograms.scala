package org.pvm.op

import geotrellis._
import geotrellis.geometry._
import geotrellis.geometry.rasterizer.Rasterizer
import geotrellis.data._
import geotrellis.statistics._

import scala.math.{max,min}

/**
 * Given a raster and an array of polygons, return a histogram summary of the cells
 * within each polygon.
 */
case class PolygonalZonalCount(ps:Array[Polygon], r:Op[Raster]) extends Op[Map[Int,Int]] {
  def _run(context:Context) = runAsync(r :: ps.toList)

  val nextSteps:Steps = {
    case raster :: polygons => {
      step2(raster.asInstanceOf[Raster], polygons.asInstanceOf[List[Polygon]])
    }
  }

  def step2(raster:Raster, polygons:List[Polygon]) = {
    // build our map to hold results
    // secretly build array
    var pmax = polygons.map(_.value).reduceLeft(_ max _)
    var histmap = Array.ofDim[Int](pmax+1)

    // dereference some useful variables
    val geo   = raster.rasterExtent
    val rdata = raster.data.asArray.getOrElse(sys.error("need array"))

    val p0    = polygons(0)
    val rows  = geo.rows
    val cols  = geo.cols

    // calculate the bounding box
    var xmin = p0.xmin
    var ymin = p0.ymin
    var xmax = p0.xmax
    var ymax = p0.ymax
    polygons.tail.foreach {
      p => {
        xmin = min(xmin, p.xmin)
        ymin = min(ymin, p.ymin)
        xmax = max(xmax, p.xmax)
        ymax = max(ymax, p.ymax)
      }
    }

    // save the bounding box as grid coordinates
    val (col1, row1) = geo.mapToGrid(xmin, ymax)
    val (col2, row2) = geo.mapToGrid(xmax, ymin)

    // burn our polygons onto a raster
    val zones = Raster.empty(geo)
    val zdata = zones.data.asArray.getOrElse(sys.error("need array"))
    Rasterizer.rasterize(zones, polygons.toArray)

    println(xmin + " | " + ymin + " | " + xmax + " | " + ymax)
    println(col1 + " | " + row1 + " | " + col2 + " | " + row2)

    // iterate over the cells in our bounding box; determine its zone, then
    // looking in the raster for a value to add to the zonal histogram.
    var row = row1
    while (row < row2) {
      var col = col1
      while (col < col2) {
        val i     = row * cols + col
        val value = rdata(i)
        if (value != NODATA) {
          val zone  = zdata(i)
          if (zone != NODATA) {
            histmap(zone) += value
          }
        }
        col += 1
      }
      row += 1
    }

    // return an immutable mapping
    Result(polygons.foldLeft(Map[Int,Int]()) { 
       (m, pgon) => m + (pgon.value -> histmap(pgon.value))
    })
  }

}
