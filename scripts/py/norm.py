import psycopg2
import sys
import urllib2
import json
from urllib import quote
            
# Normalize Data
def main():
    conn_string = "host='localhost' dbname='phillyvotes2' user='phillyvotes' password='phillyvotes'"
    print "Connecting to database\n	->%s" % (conn_string)
    conn = psycopg2.connect(conn_string)
    scursor = conn.cursor()
    cursor = conn.cursor()
    print "Connected!\n"

    flds = ["house", "StreetNameComplete", "apt",
            "Address_Line_2", "City", "State", "Zip_Code"]

    fldsstr = ', '.join(flds)
    cursor.execute("SELECT pk, %s "
                   "FROM voters_scrubbed "
                   "WHERE address_id is null" % fldsstr)

    writes = 0
    writes_total = 0
    for row in cursor:
        wheref = []
        wherev = []
        pk = row[0]

        for (fld, val) in zip(flds,row[1:]):
            if val:
                wheref.append(fld + "=%s")
                wherev.append(val)

        where = ' AND '.join(wheref)
        
        sel = "SELECT id FROM addresses WHERE %s" % where

        scursor.execute(sel, wherev)
        rows = scursor.fetchall()

        if rows:
            address_id = rows[0][0]
            
            scursor.execute('UPDATE voters_scrubbed '\
                            'SET address_id = %s '\
                            'WHERE pk = %s', (address_id, pk))
            writes += 1

        if writes > 100:
            writes_total += writes
            writes = 0
            conn.commit()
            print "Wrote (%d) rows" % writes_total

    conn.commit()
 
if __name__ == "__main__":
	main()

