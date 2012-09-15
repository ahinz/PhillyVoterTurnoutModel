(function(L) {
    var map = L.map('map').setView([51.505, -0.09], 13);

    var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    var osmAttrib='Map data © OpenStreetMap contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 8, maxZoom: 12, attribution: osmAttrib});

    var nexrad = L.tileLayer.wms("http://localhost:8888/wms/boundaries", {
        layers: 'nexrad-n0r-900913',
        format: 'image/png',
        transparent: true,
        attribution: "Weather data © 2012 IEM Nexrad"
    });

    map.setView(new L.LatLng(40.00, -75.20), 11);
    map.addLayer(osm);
    map.addLayer(nexrad);
})(L);
