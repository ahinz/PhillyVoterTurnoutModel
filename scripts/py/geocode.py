#!/usr/bin/python
import psycopg2
import sys
import urllib2
import json
from urllib import quote
 

url = "http://services.phila.gov/ULRS311/Data/Location/%s"


def geocode(house, street, apt, address2, city, state, zipcode):
    house = house or ""
    street = street or ""
    apt = apt or ""
    address2 = address2 or ""
    city = city or ""
    state = state or ""
    zipcode = zipcode or ""

    # ULRS doesn't like apt and address2, so we omit them
    address = "%s %s %s, %s %s" % (house, street, city, state, zipcode)
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
    conn_string = "host='localhost' dbname='phillyvotes' user='phillyvotes' password='phillyvotes'"
    print "Connecting to database\n	->%s" % (conn_string)
    conn = psycopg2.connect(conn_string)
    cursor = conn.cursor()
    print "Connected!\n"

    n = 50
    offset = 0
    if len(sys.argv) > 1:
        offset = int(sys.argv[1])

    if len(sys.argv) > 2:
        n = int(sys.argv[2])

    print "Limit %s Offset %s" % (n, n *offset)

    cursor.execute("SELECT pk, House, StreetNameComplete, Apt, Address_Line_2, City, State, Zip_Code "
                   "FROM voters_scrubbed where loc is null and (geocode_attempted=false or geocode_attempted is null) "
                   "LIMIT %s OFFSET %s" % (n, n *offset))
    adds = cursor.fetchall()
    writes = 0
    count = 0
    failed = 0

    for a in adds:
        pk = a[0]
        args = a[1:]
        address = geocode(*args)
        if address:
            cursor.execute("UPDATE voters_scrubbed "
                           "SET loc = ST_SetSRID(ST_GeomFromText('POINT(%s %s)'), 4326), "
                           "geocode_attempted=true WHERE pk = %s" %
                               (address["x"], address["y"], pk))
        else:
            failed += 1
            cursor.execute("UPDATE voters_scrubbed SET geocode_attempted=true WHERE pk = %s" % pk)

        count += 1
        writes += 1
        if writes > 50:
            print "Commit (%d)" % count
            conn.commit()
            writes = 0

    conn.commit()
    print("%d rows processed with %d failures" % (count, failed))

 
if __name__ == "__main__":
	main()

