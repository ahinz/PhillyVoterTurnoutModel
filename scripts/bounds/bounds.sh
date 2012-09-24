shp2pgsql -s 102729 -d -S PhiladelphiaPoliticalWards201209.shp wards > wards.sql

SELECT UpdateGeometrySRID('wards', 'the_geom', 4326);
