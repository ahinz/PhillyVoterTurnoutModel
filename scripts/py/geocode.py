#!/usr/bin/python
import psycopg2
import sys
import urllib2
import json
from urllib import quote
 

url = "http://services.phila.gov/ULRS311/Data/Location/%s"


def geocode(house, street, apt, address2, city, state, zipcode):
    address = "%s %s\n%s %s\n%s, %s %s" % (house, street, apt, address2, city, state, zipcode)
    address = quote(address)
    try:
        handle = urllib2.open(url % address)
        response = handle.read()
        handle.close()
        d = json.loads(response)
        locs = d["Locations"]
        if not locs or len(locs) == 0:
            return None
        l = locs[0]
        x = l["XCoord"]
        y = l["YCoord"]
        return { "x": x, "y": y }
    except:
        return None


def main():
	#Define our connection string
	conn_string = "host='localhost' dbname='philly_voters' user='postgres' password='postgres'"
	print "Connecting to database\n	->%s" % (conn_string)
 
	# get a connection, if a connect cannot be made an exception will be raised here
	conn = psycopg2.connect(conn_string)
 
	# conn.cursor will return a cursor object, you can use this cursor to perform queries
	cursor = conn.cursor()
	print "Connected!\n"

    cursor.execute("SELECT pk, House, HouseNoSuffix, StreetNameComplete, Apt, Address_Line_2, City, State, Zip_Code " + 
                   "FROM voters_scrubbed LIMIT 10")
    adds = cursor.fetchall()

    for a in adds:
        pk = a[0]
        args = a[1:]
        address = geocode(*args)
        if address:
            cursor.execute("UPDATE voters_scrubbed SET loc = ST_GeomFromText('POINT(%s %s)') WHERE pk = %s" %
                           (address["x"], address["y"], pk))

 
if __name__ == "__main__":
	main()

