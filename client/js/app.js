(function(L) {
    var map = L.map('map').setView([51.505, -0.09], 13);

    var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    var osmAttrib='Map data © OpenStreetMap contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 8, maxZoom: 12, attribution: osmAttrib});

    map.setView(new L.LatLng(40.00, -75.20), 11);
    map.addLayer(osm);
})(L);
