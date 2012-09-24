--
-- This script will install and configure the spatial database for
-- use with the ecep web application.
--

-- create database template_postgis with encoding='UTF8';

-- \c template_postgis;
-- \i /usr/share/postgresql/9.1/contrib/postgis-1.5/postgis.sql;
-- \i /usr/share/postgresql/9.1/contrib/postgis-1.5/spatial_ref_sys.sql;

-- \c postgres;
-- update pg_database set datistemplate='t', datallowconn='f' where datname='template_postgis';
-- create role pv with login password 'philly_voters';
-- create database philly_voters with encoding='UTF8' owner=pv;
-- create database philly_voters with encoding='UTF8' owner=pv template=template_postgis;

-- \c philly_voters
-- alter table spatial_ref_sys owner to pv;
-- alter table geometry_columns owner to pv;
-- alter view geography_columns owner to pv;
-- create schema pv authorization pv;



-- ID_Number	Title	Last_Name	First_Name	Middle_Name	Suffix	Sex	Date_Of_Birth	Date_Registered	Voter_Status	StatusChangeDate	Political_Party	House__	HouseNoSuffix	StreetNameComplete	Apt__	Address_Line_2	City	State	Zip_Code	MAddress_Line_1	MAddress_Line_2	MCity	MState	MZip_Code	PollingPlaceDescript	PollPlaceAdd	PollPlaceCSZ	Last_Date_Voted	Dist1	Dist2	Dist3	Dist4	Dist5	Dist6	Dist7	Dist8	Dist9	Dist10	Dist11	Dist12	Dist13	Dist14	Dist15	CustomData1	Date_Last_Changed	PR_04_24_12	PR_04_24_12_VM	GN_11_08_11	GN_11_08_11_VM	PR_05_17_11	PR_05_17_11_VM	SP_02_01_11	SP_02_01_11_VM	GN_11_02_10	GN_11_02_10_VM	PR_05_18_10	PR_05_18_10_VM	GN_11_03_09	GN_11_03_09_VM	PR_05_19_09	PR_05_19_09_VM	GN_11_04_08	GN_11_04_08_VM	PR_04_22_08	PR_04_22_08_VM

-- "015268548-51"			"SAPHFIRE"			"F"	"7/22/1950"	"7/18/2000"	"A"	"7/18/2000 0:0:0"	"D"	"1841"		"S YEWDALL ST"			"PHILADELPHIA"	"PA"	"19143"						"51/24 MITCHELL SCHOOL"	" 56TH & KINGSESSING AVE "	"PHILADELPHIA, PA  "	"4/24/2012"	"5124"	"WD51"		"MN01"		"LG191"	"SN08"	"CG02"	"CTC03"	"CPR"	"CPD"	"RCD02"	"5124-1"			"06103279805"	"6/22/2012 10:2:12"	"D"	"AP"	"D"	"AP"	"D"	"AP"					"D"	"P"					"D"	"AP"	"D"	"AP"

CREATE TABLE Voters
(
    ID_Number varchar(50) PRIMARY KEY UNIQUE NOT NULL,
    Title TEXT,
    Last_Name TEXT,
    First_Name TEXT,
    Middle_Name TEXT,
    Suffix TEXT,
    Sex VARCHAR(10),
    Date_Of_Birth DATE,
    Date_Registered DATE,
    Voter_Status VARCHAR(8),
    StatusChangeDate TIMESTAMP,
    Political_Party VARCHAR(16),
    House TEXT,
    HouseNoSuffix TEXT,
    StreetNameComplete TEXT,
    Apt TEXT,
    Address_Line_2 TEXT,
    City TEXT,
    State VARCHAR(8),
    Zip_Code TEXT,
    MAddress_Line_1 TEXT,
    MAddress_Line_2 TEXT,
    MCity TEXT,
    MState TEXT,
    MZip_Code TEXT,
    PollingPlaceDescript TEXT,
    PollPlaceAdd TEXT,
    PollPlaceCSZ TEXT,
    Last_Date_Voted DATE,
    Dist1 TEXT,
    Dist2 TEXT,
    Dist3 TEXT,
    Dist4 TEXT,
    Dist5 TEXT,
    Dist6 TEXT,
    Dist7 TEXT,
    Dist8 TEXT,
    Dist9 TEXT,
    Dist10 TEXT,
    Dist11 TEXT,
    Dist12 TEXT,
    Dist13 TEXT,
    Dist14 TEXT,
    Dist15 TEXT,
    CustomData1 TEXT,
    Date_Last_Changed DATE,
    PR_04_24_12 TEXT,
    PR_04_24_12_VM TEXT,
    GN_11_08_11 TEXT,
    GN_11_08_11_VM TEXT,
    PR_05_17_11 TEXT,
    PR_05_17_11_VM TEXT,
    SP_02_01_11 TEXT,
    SP_02_01_11_VM TEXT,
    GN_11_02_10 TEXT,
    GN_11_02_10_VM TEXT,
    PR_05_18_10 TEXT,
    PR_05_18_10_VM TEXT,
    GN_11_03_09 TEXT,
    GN_11_03_09_VM TEXT,
    PR_05_19_09 TEXT,
    PR_05_19_09_VM TEXT,
    GN_11_04_08 TEXT,
    GN_11_04_08_VM TEXT,
    PR_04_22_08 TEXT,
    PR_04_22_08_VM TEXT
);

-- Change this path to the correct location
copy voters from '/vagrant/VOTERSCITYWIDE.TXT' WITH CSV HEADER DELIMITER AS E'\t';


CREATE TABLE voters_scrubbed
(
    pk SERIAL PRIMARY KEY,
    ID_Number varchar(50) UNIQUE NOT NULL,
    Sex VARCHAR(10),
    Voter_Status VARCHAR(8),
    Political_Party VARCHAR(16),
    House TEXT,
    HouseNoSuffix TEXT,
    StreetNameComplete TEXT,
    Apt TEXT,
    Address_Line_2 TEXT,
    City TEXT,
    State VARCHAR(8),
    Zip_Code TEXT,
    MAddress_Line_1 TEXT,
    MAddress_Line_2 TEXT,
    MCity TEXT,
    MState TEXT,
    MZip_Code TEXT,
    Last_Date_Voted DATE
);

ALTER TABLE voters_scrubbed
    ADD COLUMN geocode_attempted BOOLEAN;

SELECT AddGeometryColumn('public', 'voters_scrubbed', 'loc', 4326, 'POINT', 2);
SELECT AddGeometryColumn('public', 'voters_scrubbed', 'mloc', 4326, 'POINT', 2);

INSERT INTO voters_scrubbed (
    ID_Number,
    Sex,
    Voter_Status,
    Political_Party,
    House,
    HouseNoSuffix,
    StreetNameComplete,
    Apt,
    Address_Line_2,
    City,
    State,
    Zip_Code,
    MAddress_Line_1,
    MAddress_Line_2,
    MCity,
    MState,
    MZip_Code,
    Last_Date_Voted)
    SELECT 
        ID_Number,
        Sex,
        Voter_Status,
        Political_Party,
        House,
        HouseNoSuffix,
        StreetNameComplete,
        Apt,
        Address_Line_2,
        City,
        State,
        Zip_Code,
        MAddress_Line_1,
        MAddress_Line_2,
        MCity,
        MState,
        MZip_Code,
        Last_Date_Voted
        FROM voters;

-- ULRS Geocoder doesn't work well with 'ST' instead of 'STREET', so convert them
UPDATE voters_scrubbed
    SET StreetNameComplete = REPLACE(StreetNameComplete, ' ST', ' STREET')
    WHERE StreetNameComplete LIKE '% ST%';

INSERT into spatial_ref_sys (srid, auth_name, auth_srid, proj4text, srtext) values ( 102729, 'esri', 102729, '+proj=lcc +lat_1=39.93333333333333 +lat_2=40.96666666666667 +lat_0=39.33333333333334 +lon_0=-77.75 +x_0=600000.0000000001 +y_0=0 +ellps=GRS80 +datum=NAD83 +to_meter=0.3048006096012192 +no_defs ', 'PROJCS["NAD_1983_StatePlane_Pennsylvania_South_FIPS_3702_Feet",GEOGCS["GCS_North_American_1983",DATUM["North_American_Datum_1983",SPHEROID["GRS_1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]],PROJECTION["Lambert_Conformal_Conic_2SP"],PARAMETER["False_Easting",1968500],PARAMETER["False_Northing",0],PARAMETER["Central_Meridian",-77.75],PARAMETER["Standard_Parallel_1",39.93333333333333],PARAMETER["Standard_Parallel_2",40.96666666666667],PARAMETER["Latitude_Of_Origin",39.33333333333334],UNIT["Foot_US",0.30480060960121924],AUTHORITY["EPSG","102729"]]');
