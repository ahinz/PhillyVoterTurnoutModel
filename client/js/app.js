var B = {};


(function(L) {
    var map = L.map('map').setView([51.505, -0.09], 13);

    var osmUrl='http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
    var osmAttrib='Map data © OpenStreetMap contributors';
    var osm = new L.TileLayer(osmUrl, {minZoom: 8, maxZoom: 18, attribution: osmAttrib});

    var wards = L.tileLayer.wms("geoserver/wms", {
        layers: 'voters:wards',
        format: 'image/png',
        transparent: true
    });

    var dems = L.tileLayer.wms("http://localhost:8888/wms/voter_density", {
        layers: 'nexrad-n0r-900913',
        format: 'image/png',
        party: 'd',
        ds: "35000",
        rs: "20000",
        transparent: true,
        attribution: "Weather data © 2012 IEM Nexrad"
    });


    var reps = L.tileLayer.wms("http://localhost:8888/wms/voter_density", {
        layers: 'nexrad-n0r-900913',
        format: 'image/png',
        party: 'r',
        rs: "10000",
        ds: "35000",
        transparent: true,
        attribution: "Weather data © 2012 IEM Nexrad"
    });

    var diff = L.tileLayer.wms("http://localhost:8888/wms/voter_density", {
        layers: 'nexrad-n0r-900913',
        format: 'image/png',
        party: 'diff',
        rs: "35000",
        ds: "20000",
        transparent: true,
        attribution: "Weather data © 2012 IEM Nexrad"
    });

    var skew = L.tileLayer.wms("http://localhost:8888/wms/voter_density", {
        layers: 'nexrad-n0r-900913',
        format: 'image/png',
        party: 'skew',
        rs: "35000",
        ds: "20000",
        transparent: true,
        attribution: "Weather data © 2012 IEM Nexrad"
    });

    map.setView(new L.LatLng(40.00, -75.20), 11);
    map.addLayer(osm);
    map.addLayer(wards);

    map.addEventListener('click', onMapClick);

    var popup = new L.Popup({
        maxWidth: 400
    });

    B.allData = {};

    function voterCounts() {
        $.ajax({
            url: "/trellis/zones?layer=wards&simpl=5",
            dataType: "json",
            type: "GET",
            success: function(d) {
                B.allData = d;
            }
        });
    }

    voterCounts();

    function onMapClick(e) {
        var lng = "lng=" + e.latlng.lng.toFixed(3)
        var lat = "lat=" + e.latlng.lat.toFixed(3)
        var req = "/trellis/geometry/boundary?layer=wards&" + lng + "&" + lat
	$.ajax({
	    url: req,
	    dataType: "json",
	    type: "GET",
	    //async: false,
	    success: function(data) {
		popup.setContent("Voters: " + JSON.stringify(B.allData[data.id]));
		popup.setLatLng(e.latlng);
		map.openPopup(popup);
	    }
	});


    }

    B.layers = {};
    B.layers.dems = dems;
    B.layers.reps = reps;
    B.layers.diff = diff;
    B.layers.skew = skew;
    B.map = map;
//    map.addLayer(dems);
//    map.addLayer(reps);
//    map.addLayer(diff);
})(L,B);

$(function() {
    function chg() {
        B.map.removeLayer(B.layers.dems);
        B.map.removeLayer(B.layers.reps);
        B.map.removeLayer(B.layers.diff);
        B.map.removeLayer(B.layers.skew);

        if ($("#dems").is(':checked')) {
            B.map.addLayer(B.layers.dems);
        } else if ($("#reps").is(':checked')) {
            B.map.addLayer(B.layers.reps);
        } else if ($("#diff").is(':checked')) {
            B.map.addLayer(B.layers.diff);
        } else if ($("#skew").is(':checked')) {
            B.map.addLayer(B.layers.skew);
        } else if ($("#both").is(':checked')) {
            B.map.addLayer(B.layers.dems);
            B.map.addLayer(B.layers.reps);
        }
    }    

    B.update = chg;

    $("#dems").change(chg);
    $("#reps").change(chg);
    $("#diff").change(chg);
    $("#both").change(chg);
    $("#skew").change(chg);

});
