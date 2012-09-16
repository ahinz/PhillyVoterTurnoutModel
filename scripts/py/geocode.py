#!/usr/bin/python
import psycopg2
import sys
import urllib2
import json
from urllib import quote
 

url = "http://services.phila.gov/ULRS311/Data/Location/%s"


def geocode(house, street, apt, address2, city, state, zipcode):
    if house is None:
        house = ""
    if street is None:
        street = ""
    if apt is None:
        apt = ""
    if address2 is None:
        address2 = ""
    if city is None:
        city = ""
    if state is None:
        state = ""
    if zipcode is None:
        zipcode = ""

    address = "%s %s %s %s %s, %s %s" % (house, street, apt, address2, city, state, zipcode)
    address = quote(address)
    try:
        #print url % address
        handle = urllib2.urlopen(url % address)
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
    conn_string = "host='localhost' dbname='phillyvote' user='phillyvote' password='phillyvote'"
    print "Connecting to database\n	->%s" % (conn_string)
 
    # get a connection, if a connect cannot be made an exception will be raised here
    conn = psycopg2.connect(conn_string)
 
    # conn.cursor will return a cursor object, you can use this cursor to perform queries
    cursor = conn.cursor()
    print "Connected!\n"

    offset = 0
    if len(sys.argv) > 1:
        offset = sys.argv[1]

    cursor.execute("SELECT pk, House, StreetNameComplete, Apt, Address_Line_2, City, State, Zip_Code "\
                   "FROM voters_scrubbed where loc is null and (geocode_attempted=false or geocode_attempted is null) LIMIT 100000 OFFSET %s" % offset)
    adds = cursor.fetchall()
    writes = 0

    for a in adds:
        pk = a[0]
        args = a[1:]
        address = geocode(*args)
        #print "---> %s" % address
        if address:
            cursor.execute("UPDATE voters_scrubbed SET loc = ST_SetSRID(ST_GeomFromText('POINT(%s %s)'), 4326), geocode_attempted=true WHERE pk = %s" %
                           (address["x"], address["y"], pk))
        else:
            cursor.execute("UPDATE voters_scrubbed SET geocode_attempted=true WHERE pk = %s" % pk)

        writes += 1
        if writes > 5:
            conn.commit()
            writes = 0

    conn.commit()

 
if __name__ == "__main__":
	main()

