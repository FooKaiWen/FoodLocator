package home.com.googlemap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
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
    private Marker currentLocationMarker;
    public static final String TAG = MapsActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_CODE = 99;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private DBHelper mydb;
    ArrayList<Marker> mapMarkers = new ArrayList<Marker>();

    double longitude = 0;
    double latitude = 0;
    String callNo = "";
    Spinner spinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mydb = new DBHelper(this);
        if (mydb.numberOfRows() < 1) {
            insertData();
        }

        setEmptyDetails();

        spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cuisine_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        FloatingActionButton randomFill = (FloatingActionButton) findViewById(R.id.randomFilters);
        randomFill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int randomNum = new Random().nextInt(mydb.numberOfRows());
                // retrieve the name of the marker and match it in the database
                // display it all out in the
                if (randomNum == 0) {
                    randomNum++;
                }

                Cursor rs = mydb.getData(randomNum);
                rs.moveToFirst();
                String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
                Marker found = null;

                int i;
                for (i = 0; i < mapMarkers.size(); i++) {
                    if (getName == mapMarkers.get(i).getTitle()) {
                        Toast.makeText(getApplicationContext(),Integer.toString(i),Toast.LENGTH_LONG).show();
                        break;
                    }
                }

                if (i <= mydb.numberOfRows()) {
                    Toast.makeText(getApplicationContext(), "Scroll Up For More", Toast.LENGTH_LONG).show();
                    Cursor rs1 = mydb.getData(i);
                    rs1.moveToFirst();
//                    setDetails(rs1);
//                    getLatLngCallNo(rs1);
                    // change marker colour
                    // get id then
                    // getData(name, time, address, price)
                    // create the function above
                }

//                if(found!=null){
//                    currentLocationMarker = found;
//                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocationMarker.getPosition()));
//                    mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocationMarker.getPosition()));
//                    found.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
//                }
                setDetails(rs);

                if (mLayout != null) {
                    mLayout.setAnchorPoint(0.7f);
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                }
                Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
            }
        });

        Button callB = (Button) findViewById(R.id.callButton);
        callB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                if (!callNo.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + callNo)); // get the phone number from the phone number area.

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    startActivity(callIntent);
                } else if (callNo.isEmpty()) { // reject the user from calling without phone number input.
                    Toast.makeText(getApplicationContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button uberB = (Button) findViewById(R.id.transportButton);
        uberB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                PackageManager pm = getPackageManager();
                if (latitude != 0 && longitude != 0) {
                    try {
                        pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
                        // example
                        String uri = "uber://?client_id=<CLIENT_ID>" +
                                "&action=setPickup&pickup=my_location&pickup[nickname]=You" +
                                "&dropoff[latitude]=" + latitude + "&dropoff[longitude]=" + longitude + "&dropoff[nickname]=Destination" +
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
                } else if (latitude == 0 && longitude == 0) {
                    Toast.makeText(getApplicationContext(), "Invalid Location", Toast.LENGTH_SHORT).show();
                }
            }
        });

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

        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (client == null) {
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

        LatLngBounds USM = new LatLngBounds(new LatLng(5.326039, 100.272603), new LatLng(5.370049, 100.317664));
        mMap.setLatLngBoundsForCameraTarget(USM);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(USM.getCenter(), 10));
        mMap.setMinZoomPreference(14.0f);
        mMap.setMaxZoomPreference(20.0f);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                buildingGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildingGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        setUpMap();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!adapterView.getSelectedItem().toString().matches("All")) {
                    mMap.clear();
                    mapMarkers.clear();
                    for (int p = 1; p <= mydb.numberOfRows(); p++) {
                        Cursor rs = mydb.getData(p);
                        rs.moveToFirst();
                        if(rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CUISINE)).matches(adapterView.getSelectedItem().toString())) {
                            String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
                            String getLat = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE));
                            String getLongi = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE));
                            String getType = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_FOODTYPE));
                            if (!rs.isClosed()) {
                                rs.close();
                            }
                            addMarker(p, getName, getLat, getLongi, getType);
                        }
                    }
                } else {
                    mMap.clear();
                    mapMarkers.clear();
                    for (int k = 1; k <= mydb.numberOfRows(); k++) {
                        Cursor rs = mydb.getData(k);
                        rs.moveToFirst();
                        String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
                        String getLat = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE));
                        String getLongi = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE));
                        String getType = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_FOODTYPE));
                        if (!rs.isClosed()) {
                            rs.close();
                        }
                        addMarker(k, getName, getLat, getLongi, getType);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        for (int i = 1; i <= mydb.numberOfRows(); i++) {
            Cursor rs = mydb.getData(i);
            rs.moveToFirst();
            String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
            String getLat = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE));
            String getLongi = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE));
            String getType = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_FOODTYPE));
            if (!rs.isClosed()) {
                rs.close();
            }
            addMarker(i, getName, getLat, getLongi, getType);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker currentM) {
                    if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED
                            || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        mLayout.setAnchorPoint(1.0f);
                        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    }
                    int i;
                    for (i = 0; i < mapMarkers.size(); i++) {
                        if (currentM.getTitle().equals(mapMarkers.get(i).getTitle())){
                            break;
                        }
                    }
                    i++;
                    if (i <= mydb.numberOfRows()) {
                        Toast.makeText(getApplicationContext(), Integer.toString(i), Toast.LENGTH_LONG).show();
                        currentM.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        Cursor rs = mydb.getData(i);
                        rs.moveToFirst();
                        setDetails(rs);
                        getLatLngCallNo(rs);
                        // change marker colour
                        // get id then
                        // getData(name, time, address, price)
                        // create the function above
                    }
                    return false;
                }
            }
        );

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED|| mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mLayout.setAnchorPoint(1.0f);
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
                setEmptyDetails();
                // there is orange, red, yellow marker,
                // then change all to default colour.
            }
        });
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

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title("You are here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }

    public void handleNewLocation(Location location) {

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title("You are here!");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        currentLocationMarker = mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create();

        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
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

    public boolean checkLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
            }
            return false;
        } else
            return true;
    }

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
                (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
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

    public void getLatLngCallNo(Cursor restaurant) {

        latitude = Double.parseDouble(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE)));
        longitude = Double.parseDouble(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE)));
        callNo = restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CONTACT));

    }

    public void addMarker(int id, String name, String lat, String longi, String type) {
        // need latitude, longitude, id

        LatLng latlng = new LatLng(Double.parseDouble(lat), Double.parseDouble(longi));
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title(name);

        if (type.matches("Halal")) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        } else if (type.matches("Non-halal")) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        } else
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currentLocationMarker = mMap.addMarker(markerOptions);
        mapMarkers.add(mMap.addMarker(markerOptions));
    }

    public void setDetails(Cursor restaurant) {
        //Text views set to defaults for design purposes
        TextView rn = (TextView) findViewById(R.id.restaurantName);
        rn.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME)));

        String uri = "@drawable/" + restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_IMAGENAME));
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        ImageView ri = (ImageView) findViewById(R.id.restaurantImage);
        ri.setImageDrawable(res);

        TextView operatingDaysStart = (TextView) findViewById(R.id.operatingDaysField_Start);
        operatingDaysStart.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_WORK)));

//        TextView operatingDaysEnd = (TextView) findViewById(R.id.operatingDaysField_End);
//        operatingDaysEnd.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_REST)));

        TextView operatingHoursStart = (TextView) findViewById(R.id.operatingHoursField_Start);
        operatingHoursStart.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_TIME)));

//        TextView operatingHoursEnd = (TextView) findViewById(R.id.operatingHoursField_End);
//        operatingHoursEnd.setText("88:88");

        TextView address = (TextView) findViewById(R.id.address);
        address.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_ADDRESS)));

        TextView contact = (TextView) findViewById(R.id.contact);
        contact.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CONTACT)));

        TextView price = (TextView) findViewById(R.id.price);
        price.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_PRICE)));
    }

    public void setEmptyDetails() {
        TextView rn = (TextView) findViewById(R.id.restaurantName);
        rn.setText("Restaurant Name");

        ImageView ri = (ImageView) findViewById(R.id.restaurantImage);
        ri.setImageDrawable(null);

        TextView operatingDaysStart = (TextView) findViewById(R.id.operatingDaysField_Start);
        operatingDaysStart.setText("N/A");

//        TextView operatingDaysEnd = (TextView) findViewById(R.id.operatingDaysField_End);
//        operatingDaysEnd.setText("");

        TextView operatingHoursStart = (TextView) findViewById(R.id.operatingHoursField_Start);
        operatingHoursStart.setText("N/A");

//        TextView operatingHoursEnd = (TextView) findViewById(R.id.operatingHoursField_End);
//        operatingHoursEnd.setText("");
        TextView address = (TextView) findViewById(R.id.address);
        address.setText("N/A");

        TextView contact = (TextView) findViewById(R.id.contact);
        contact.setText("N/A");

        TextView price = (TextView) findViewById(R.id.price);
        price.setText("");

        callNo = "";
        latitude = 0;
        longitude = 0;
    }

    private void setUpMap() {
        if (checkLocationPermission()) {
            UiSettings mUiSettings = mMap.getUiSettings();
            mMap.setMyLocationEnabled(true);
            mUiSettings.setMyLocationButtonEnabled(true);
            mUiSettings.setTiltGesturesEnabled(true);
            mUiSettings.setRotateGesturesEnabled(false);
        }
    }

    public void insertData(){
        mydb.insertRestaurant("Bumbledee's at 1938", "Western", "0.8 km",
                "MON - FRI", "SAT - SUN", "9:00 - 20:00", "$$$", "+6019-4733777",
                "BumbleDee's Cafe, 11800 George Town, Penang, Malaysia",
                "5.362104", "100.306944", "bumbledee", "Halal");

        mydb.insertRestaurant("McDonald's", "Fast Food", "0.8 km",
                "MON-SUN","N/A","OPEN FOR 24 HOURS", "$-$$", "+6 04-659 6346",
                "4 B-C-D, Jalan Sungai Dua, 11700 Gelugor, Pulau Pinang",
                "5.352584", "100.299395","mcdonalds_sungai_dua", "Halal");

        mydb.insertRestaurant("Pizza Hut", "Fast Food", "0.8 km",
                "MON-SUN", "N/A", "11:00 - 23:00","$-$$", "1-300-88-2525",
                "Desa University, 4, Jalan Sungai Dua, 11700, Pulau Pinang",
                "5.352470", "100.299102", "pizza_hut_sungai_dua", "Halal");

        mydb.insertRestaurant("The Kapit's", "Western", "0.6 km",
                "MON-SUN", "N/A", "10:00 - 0:00", "$-$$", "+60 13-499 9507",
                "8, Jalan Gemilang, 11800 Gelugor, Pulau Pinang",
                "5.360454", "100.303527", "the_kapit's", "Halal");

        mydb.insertRestaurant("Chico's cafe", "Western", "0.45 km",
                "MON - FRI", " SAT - SUN", "8:00 - 17:00", "$-$$", "+60 12-4819061",
                "D01, Anjung Budi, Jalan Ilmu, 11800 Gelugor, Pulau Pinang",
                "5.357409","100.306151", "chico_cafe", "Halal");

        mydb.insertRestaurant("Restaurant Kim Hin", "Chinese", "0.95 km",
                "MON - SUN", "WED", "16:30 - 22:00", "$-$$", "+60 16-551 9152",
                "MK 13, 674C, Jalan Sungai Dua, Taman Pekaka, 11700 Gelugor, Pulau Pinang",
                "5.351722", "100.298206", "kim_him_seafood", "Non-halal");

    }

}
