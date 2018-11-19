package com.citymo.androidapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationProvider;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlPolygon;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final double Bogota_lat = 4.59354, Bogota_lng = -74.26964;
    private static final float zoom = 10;
    private static final int scroll_list = 5;

    private GoogleMap mMap;

    private Marker marker;

    private GoogleApiClient mLocationClient;
    private LocationListener mListener;
    private FusedLocationProviderClient mFusedLocationClient;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 14;


    protected int getLayoutId() {
        return R.layout.activity_maps;
    }
    // private KmlLayer kmlLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        setUpMap();
        setUpLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMap();
        setUpLocation();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) {
            return;
        }
        mMap = googleMap;
        if (!checkPermissions()){
            requestPermissions();
        } else
        gotoCurrentLocation();
        kmlMap();
        //gotoLocaton(Bogota_lat, Bogota_lng, zoom);
        //Log.d("myTag", "Prueba del mapa");
    }

    private void setUpMap() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    private void setUpLocation() {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void gotoLocaton(double lat, double lng, float zoom) {
        LatLng latlng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, zoom);
        mMap.moveCamera(update);
        //TODO Considerar hashmap o collections para poner múltiples markers (?)
        //TODO Aplcar sqllite para guardar info
        if (marker != null){
            marker.remove();
        }
        addMarker(lat,lng);
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void geoLocate(View view) throws IOException {
        hideSoftKeyboard(view);
        TextView tv = findViewById(R.id.editText1);
        String searchString = tv.getText().toString();

        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(searchString, scroll_list);

        if (list.size() > 0) {
            Address adr = list.get(0);
            String locality = adr.getLocality();
            Toast.makeText(this, "Found " + locality, Toast.LENGTH_LONG).show();

            double lat = adr.getLatitude();
            double lng = adr.getLongitude();
            gotoLocaton(lat, lng, 15);
        }
    }

    public void gotoCurrentLocation() {

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            gotoLocaton(location.getLatitude(), location.getLongitude(), zoom);
                        }
                    }
                });

    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    public void showCurrentLocation(MenuItem item) {
        gotoCurrentLocation();
    }

    private void kmlMap() {
        // TODO optimizar toma mucho tiempo!
        try {
            KmlLayer kmlLayer = new KmlLayer(mMap, R.raw.ciclorruta, getApplicationContext());
            kmlLayer.addLayerToMap();
            //       moveCameraToKml(kmlLayer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void addMarker(double lat, double lng) {
        MarkerOptions options = new MarkerOptions()
                .title("CITYMO USER")
                .position(new LatLng(lat,lng))
                //TODO Ajustar color con paleta CITYMO
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        marker = mMap.addMarker(options);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "Welcome to CITYMO Map", Toast.LENGTH_LONG).show();
        mListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                gotoLocaton(location.getLatitude(), location.getLongitude(),zoom);
            }
        };
        //TODO implementar esta parte -- falla por getFusedLocationProviderClient
/*
        LocationRequest lRequest = LocationRequest.create();
        //TODO cambiar parámetros por unos que no consuman tanta batería
        lRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        lRequest.setInterval(5000);
        lRequest.setFastestInterval(1000);
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                mLocationClient, lRequest, mListener
        );
        */
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        mLocationClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO implementar esta parte  -- falla por getFusedLocationProviderClient
     /*   LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(
                mLocationClient, mListener);
                */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gotoCurrentLocation();
                kmlMap();
            }
            //TODO implementar excepciones
        }
    }
}
