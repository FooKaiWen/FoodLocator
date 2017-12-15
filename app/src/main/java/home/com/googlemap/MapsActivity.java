package home.com.googlemap;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Random;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private SlidingUpPanelLayout mLayout;

    private GoogleMap mMap;
    private GoogleApiClient client;
    private Location lastLocation;
    private Marker currentLocationMarker;
    public static final String TAG = MapsActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_CODE = 99;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private DBHelper mydb;
    ArrayList<Marker> mapMarkers = new ArrayList<Marker>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_layout);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton randomFill = (FloatingActionButton) findViewById(R.id.randomFilters);
        randomFill.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                int randomNum = new Random().nextInt(mydb.numberOfRows());
                // retrieve the name of the marker and match it in the database
                // display it all out in the
                Cursor rs = mydb.getData(randomNum);
                rs.moveToFirst();
                String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));

                Marker found = null;
                for(Marker m : mapMarkers){
                    if(m.getTitle() == getName){
                        found = m;
                    }
                }
                if(found!=null){
                   mMap.moveCamera(CameraUpdateFactory.newLatLng(found.getPosition()));
//                    mMap.animateCamera(CameraUpdateFactory.zoomBy(14));
//                    found.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                }
                setDetails(getName);

            }
        });

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        Button callB = (Button) findViewById(R.id.callButton);
        callB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                // example
                String phoneNo = "017-5818160";
                if(!phoneNo.isEmpty()){
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + phoneNo)); // get the phone number from the phone number area.

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    startActivity(callIntent);
                } else if (phoneNo.isEmpty()){ // reject the user from calling without phone number input.
                    Toast.makeText(getApplicationContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button uberB = (Button) findViewById(R.id.transportButton);
        uberB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0){
                PackageManager pm = getPackageManager();
                try {
                    pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
                    // example
                    double longitude = 100.302518;
                    double latitude = 5.355934;
                    String uri = "uber://?client_id=<CLIENT_ID>" +
                            "&action=setPickup&pickup=my_location&pickup[nickname]=You" +
                            "&dropoff[latitude]="+latitude+"&dropoff[longitude]="+longitude+"&dropoff[nickname]=Destination" +
                            "&product_id=a1111c8c-c720-46c3-8534-2fcdd730040d" +
                            "&link_text=View%20team%20roster" +
                            "&partner_deeplink=partner%3A%2F%2Fteam%2F9383";
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(uri));
                    startActivity(intent);
                } catch (PackageManager.NameNotFoundException e) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ubercab")));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.ubercab")));
                    }
                }
            }
        });

    }

    public void setDetails(String name){
        //Text views set to defaults for design purposes
        TextView rn = (TextView) findViewById(R.id.restaurantName);
        rn.setText(name);
        ImageView ri = (ImageView)findViewById(R.id.restaurantImage);

        TextView operatingDaysStart = (TextView) findViewById(R.id.operatingDaysField_Start);
        operatingDaysStart.setText("Wednesday");
        TextView operatingDaysEnd = (TextView) findViewById(R.id.operatingDaysField_End);
        operatingDaysEnd.setText("Wednesday");
        TextView operatingHoursStart = (TextView) findViewById(R.id.operatingHoursField_Start);
        operatingHoursStart.setText("88:88");
        TextView operatingHoursEnd = (TextView) findViewById(R.id.operatingHoursField_End);
        operatingHoursEnd.setText("88:88");
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case REQUEST_PERMISSION_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted
                    if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                        if(client == null){
                            buildingGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else { // permission denied

                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show();
                }
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        LatLngBounds USM = new LatLngBounds(new LatLng(5.326039,100.272603),new LatLng(5.370049,100.317664));
        mMap.setLatLngBoundsForCameraTarget(USM);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USM.getCenter(), 10));
        mMap.setMinZoomPreference(14.0f);
        mMap.setMaxZoomPreference(20.0f);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                buildingGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildingGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        setUpMap();

        mydb = new DBHelper(this);

        mydb.insertRestaurant("Bumbledee's at 1938", "Western", "0.8", "MON - FRI", "SAT - SUN", "9:00 - 20:00","$$", "+60 19-473 3777",
                "B07 Rumah Tetamu,Universiti Sains Malaysia, Penang Island 11700, Malaysia","5.3620769", " 100.306943888888");


        for(int i=1;i<=mydb.numberOfRows();i++){
            Cursor rs = mydb.getData(i);
            rs.moveToFirst();
            String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
            String getCuisine = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CUISINE));
            String getDistance = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_DISTANCE));
            String getWork = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_WORK));
            String getRest = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_REST));
            String getTime = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_TIME));
            String getPrice = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_PRICE));
            String getContact = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CONTACT));
            String getAddress = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_ADRRESS));
            String getLat = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE));
            String getLongi = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE));
            if (!rs.isClosed())  {
                rs.close();
            }
            addMarker(i,getName,getLat,getLongi);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener(){
            @Override
            public boolean onMarkerClick(Marker currentM) {
                Toast.makeText(getApplicationContext(),currentM.getTag().toString(),Toast.LENGTH_LONG).show();
                currentM.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                Cursor rs = mydb.getData(Integer.parseInt(currentM.getTag().toString()));
                rs.moveToFirst();
                String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
                setDetails(getName);
                // change marker colour
                // get id then
                // getData(name, time, address, price)
                // create the function above
                return false;
            }
        }
        );

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setDetails("");
                // there is orange, red, yellow marker,
                // then change all to default colour.
            }
        });

//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//
//            buildingGoogleApiClient();
//            mMap.setMyLocationEnabled(true);
//
//        }

        }

    public void addMarker(int id, String name, String lat, String longi){
        // need latitude, longitude, id

        LatLng latlng = new LatLng(Double.parseDouble(lat),Double.parseDouble(longi));
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title(name);

        String cuisineType = "-";
        if(cuisineType.matches("Halal")){
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        } else if(cuisineType.matches("Non-halal")){
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        } else
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        currentLocationMarker = mMap.addMarker(markerOptions);
        currentLocationMarker.setTag(id);
        mapMarkers.add(mMap.addMarker(markerOptions));
    }

    protected synchronized void buildingGoogleApiClient() {

        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        //handleNewLocation(location);
        lastLocation = location;

        if(currentLocationMarker!= null){
            currentLocationMarker.remove();
        }

        LatLng latlng = new LatLng(location.getLatitude(),location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title("You are here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if(client!= null){
            LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        }
    }

    public void handleNewLocation(Location location){
        lastLocation = location;

        if(currentLocationMarker!= null){
            currentLocationMarker.remove();
        }

        LatLng latlng = new LatLng(location.getLatitude(),location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title("You are here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if(client!= null){
            LocationServices.FusedLocationApi.removeLocationUpdates(client,this);
        }
    }

    /*public void onClick(View view){

        switch (view.getId()){
            case R.id.searchButton:
                EditText editSearch = findViewById(R.id.editSearch);
                String location = editSearch.getText().toString();

                List<Address> addressList;

                if(!location.equals("")){
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        addressList = geocoder.getFromLocationName(location,10);

                        for(int i = 0; i < addressList.size() ; i++){

                            LatLng latLng = new LatLng(addressList.get(i).getLatitude() , addressList.get(i).getLongitude());

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);
                            markerOptions.title(location);
                            mMap.addMarker(markerOptions);
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
        }

    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create();

        locationRequest.setInterval(30*1000);
        locationRequest.setFastestInterval(5*1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(client, builder.build());

        if (result != null) {
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult locationSettingsResult) {
                    final Status status = locationSettingsResult.getStatus();

                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location
                            // requests here.

                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user
                            // a optionsDialog.
                            try {
                                // Show the optionsDialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                if (status.hasResolution()) {
                                    status.startResolutionForResult(MapsActivity.this, 1000);
                                }
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the optionsDialog.
                            break;
                    }
                }
            });
        }
//        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//
//          LocationServices.FusedLocationApi.requestLocationUpdates(client,locationRequest,this);
//          LocationServices.getFusedLocationProviderClient(this).getLastLocation();
//
//        }
//
//        Log.i(TAG, "Location services connected.");
    }

    public boolean checkLocationPermission(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
            }
            return false;
        }else
            return true;
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//      //  client.connect();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
            client.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to exit?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MapsActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void setUpMap() {
        if (checkLocationPermission())
        {
            UiSettings mUiSettings = mMap.getUiSettings();
            mMap.setMyLocationEnabled(true);
            mUiSettings.setMyLocationButtonEnabled(true);
            mUiSettings.setTiltGesturesEnabled(true);
            mUiSettings.setRotateGesturesEnabled(false);
        }
    }

}
