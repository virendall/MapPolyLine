package com.example.ds.mappolyline;

import android.graphics.Color;
import android.icu.lang.UCharacter;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.android.gms.maps.model.RoundCap;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        getPolyLine("26.8334126", "75.8015951", "26.8940826", "75.7921792");
    }

    public void getPolyLine(String originLat, String originLon, String destinationLat, String destinationLon) {
        AsyncHttpClient client = getClient();
        client.get(getUrl(originLat, originLon, destinationLat, destinationLon), null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONObject json = getJson(new String(responseBody));
                Log.e("Status", json.optString("status"));
                /*            for (NSDictionary *routes in response[@"routes"]) {
                GMSPath *path = [GMSPath pathFromEncodedPath:routes[@"overview_polyline"][@"points"]];
*/
                if (json.optString("status").equals("OK")) {
                    JSONArray jsonArray = json.optJSONArray("routes");
                    String polylineString = jsonArray.optJSONObject(0).optJSONObject("overview_polyline").optString("points");
                    System.out.print(polylineString);
                    final List<LatLng> result = decodePoly(polylineString);
                    System.out.print(result.size());

                    final float red = 244.0f;
                    final float green = 201.0f;
                    final float yellow = 19.0f;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float redSteps = (red / result.size());
                            float greenSteps = (green / result.size());
                            float yellowSteps = (yellow / result.size());
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(result.get(0));
                            for (int i = 1; i < result.size(); i++) {
                                builder.include(result.get(i));
                                int redColor = (int) (red - (redSteps * i));
                                int greenColor = (int) (green - (greenSteps * i));
                                int yellowColor = (int) (yellow - (yellowSteps * i));
                                Log.e("Color", "" + redColor);
                                int color = Color.rgb(redColor, greenColor, yellowColor);

                                PolylineOptions options = new PolylineOptions().width(8).color(color).geodesic(true);
                                options.add(result.get(i - 1));
                                options.add(result.get(i));
                                Polyline line = mMap.addPolyline(options);
                                line.setEndCap(new RoundCap());
                            }
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
                            mMap.animateCamera(cu);
                        }
                    });
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });


    }

    public String getUrl(String originLat, String originLon, String destinationLat, String destinationLon) {
        String DIRECTION_API = "https://maps.googleapis.com/maps/api/directions/json?origin=";
        return DIRECTION_API + originLat + "," + originLon + "&destination=" + destinationLat + "," + destinationLon;// + "&key=" + API_KEY;
    }

    public static AsyncHttpClient getClient() {
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory socketFactory = new MySSLSocketFactory(trustStore);
            socketFactory.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(60 * 1000);
            client.setSSLSocketFactory(socketFactory);
            return client;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AsyncHttpClient();
    }


    public static JSONObject getJson(String jsonString) {
        JSONObject jsonObject = new JSONObject();
        try {
            System.out.println("JSON:" + jsonString);
            jsonObject = new JSONObject(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonObject == null) {
            try {
                jsonObject.put("status", "FALSE");
                jsonObject.put("msg", "Invalid Json Response");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return jsonObject;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
