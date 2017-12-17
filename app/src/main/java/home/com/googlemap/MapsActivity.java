package home.com.googlemap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Image;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.maps.android.clustering.ClusterManager;
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
    String callNo = "N/A";
    Spinner cuisineSpinner;
    Spinner priceSpinner;
    int randomNum = 1;
    int lastrandomNum = 1;
    private RatingBar ratingBar;

    TextView rn;
    ImageView ri;
    TextView distance;
    TextView operatingDaysStart;
    TextView operatingHoursStart;
    TextView address;
    TextView contact;
    TextView price;
    TextView cuisine;
    TextView foodtype;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_layout);;

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


        addListenerOnRatingBar();
        setEmptyDetails();

        cuisineSpinner = (Spinner) findViewById(R.id.cuisineSpinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.cuisine_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cuisineSpinner.setAdapter(adapter);

        priceSpinner = (Spinner) findViewById(R.id.priceSpinner);
        final ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.price_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priceSpinner.setAdapter(adapter2);

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
                Cursor rs;
                String randomCuisine;
                String randomPrice;
                String cuisinetext = cuisineSpinner.getSelectedItem().toString();
                String pricetext = priceSpinner.getSelectedItem().toString();

                if (!cuisinetext.equals("All Cuisine") && !pricetext.equals("All Price")) {
                    for (int k = 1; k <= mydb.numberOfRows(); k++) {
                        lastrandomNum = randomNum;
                        do {
                            randomNum = new Random().nextInt(mydb.numberOfRows());
                            if (randomNum == 0) {
                                randomNum++;
                            }
                        } while (randomNum == lastrandomNum);
                        rs = mydb.getData(randomNum);
                        rs.moveToFirst();
                        randomCuisine = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CUISINE));
                        randomPrice = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_PRICE));
                        if (randomCuisine.equals(cuisinetext) && randomPrice.equals(pricetext)) {
                            setDetails(rs);
                            getLatLngCallNo(rs);
                            Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
                            anchorThePane();
                            break;
                        }

                        if(k == mydb.numberOfRows()){
                            Toast.makeText(getApplicationContext(), "No restaurant found!", Toast.LENGTH_LONG).show();
                            break;
                        }
                        rs.close();
                    }
                } else if(!cuisinetext.equals("All Cuisine") && pricetext.equals("All Price")){
                    for (int k = 1; k <= mydb.numberOfRows(); k++) {
                        lastrandomNum = randomNum;
                        do {
                            randomNum = new Random().nextInt(mydb.numberOfRows());
                            if (randomNum == 0) {
                                randomNum++;
                            }
                        } while (randomNum == lastrandomNum);
                        rs = mydb.getData(randomNum);
                        rs.moveToFirst();
                        randomCuisine = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CUISINE));
                        if (randomCuisine.equals(cuisinetext)){
                            setDetails(rs);
                            getLatLngCallNo(rs);
                            Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
                            anchorThePane();
                            break;
                        }

                        if (k == mydb.numberOfRows()) {
                            Toast.makeText(getApplicationContext(), "No restaurant found!", Toast.LENGTH_LONG).show();
                            break;
                        }
                        rs.close();
                    }
                } else if(cuisinetext.equals("All Cuisine") && !pricetext.equals("All Price")){
                        for (int k = 1; k <= mydb.numberOfRows(); k++) {
                            lastrandomNum = randomNum;
                            do {
                                randomNum = new Random().nextInt(mydb.numberOfRows());
                                if (randomNum == 0) {
                                    randomNum++;
                                }
                            } while (randomNum == lastrandomNum);
                            rs = mydb.getData(randomNum);
                            rs.moveToFirst();
                            randomPrice = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_PRICE));
                            if (randomPrice.equals(pricetext)) {
                                setDetails(rs);
                                getLatLngCallNo(rs);
                                Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
                                anchorThePane();
                                break;
                            }

                            if (k == mydb.numberOfRows()) {
                                Toast.makeText(getApplicationContext(), "No restaurant found!", Toast.LENGTH_LONG).show();
                                break;
                            }
                            rs.close();
                        }
                } else{
                    int randomNum = new Random().nextInt(mydb.numberOfRows());
                    if (randomNum == 0) {
                        randomNum++;
                    }
                    rs = mydb.getData(randomNum);
                    rs.moveToFirst();
                    setDetails(rs);
                    Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
                    anchorThePane();
                }
            }
        });

        ImageButton callB = (ImageButton) findViewById(R.id.callButton);
        callB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (!callNo.isEmpty() && !callNo.equals("N/A")) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + callNo)); // get the phone number from the phone number area.
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    startActivity(callIntent);
                } else if (callNo.isEmpty() || callNo.equals("N/A")) { // reject the user from calling without phone number input.
                    Toast.makeText(getApplicationContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton uberB = (ImageButton) findViewById(R.id.transportButton);
        uberB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                PackageManager pm = getPackageManager();
                if (latitude != 0 && longitude != 0) {
                    try {
                        pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
                        // example
                        String uri = "uber://?client_id=<CLIENT_ID>" +
                                "&action=setPickup&pickup=my_location&pickup[nickname]=You" +
                                "&dropoff[latitude]=" + latitude + "&dropoff[longitude]=" + longitude + "&dropoff[nickname]="+ rn.getText().toString() +
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
        mMap.setMaxZoomPreference(19.0f);

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
        addListenerOnSpinner();

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
            addMarker(getName, getLat, getLongi, getType);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker currentM) {
                    if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED
                            || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        mLayout.setAnchorPoint(1.0f);
                        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    }
                    currentM.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    Cursor rs = mydb.getDataUsingName(currentM.getTitle());
                    rs.moveToFirst();
                    setDetails(rs);
                    getLatLngCallNo(rs);
                    return false;
                }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED|| mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mLayout.setAnchorPoint(1.0f);
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
                setEmptyDetails();

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
//        if (client.isConnected()) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
//            client.disconnect();
//        }
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

    public void addMarker(String name, String lat, String longi, String type) {
        // need latitude, longitude, id

        LatLng latlng = new LatLng(Double.parseDouble(lat), Double.parseDouble(longi));
        MarkerOptions markerOptions = new MarkerOptions().position(latlng).title(name);

        if (type.matches("Halal")) {
            markerOptions.snippet("Halal");
        } else if (type.matches("Non-halal")) {
            markerOptions.snippet("Non-halal");
        }
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        currentLocationMarker = mMap.addMarker(markerOptions);
        mapMarkers.add(mMap.addMarker(markerOptions));
    }

    public void getSpinnerData(){
        mMap.clear();
        cuisineSpinner = (Spinner)findViewById(R.id.cuisineSpinner);
        String cuisinetext = cuisineSpinner.getSelectedItem().toString();
        priceSpinner = (Spinner)findViewById(R.id.priceSpinner);
        String pricetext = priceSpinner.getSelectedItem().toString();
        String price;
        String cuisine;
        if(pricetext.equals("All Price")){
            price = "";
        } else {
            price = "where price = '"+pricetext+"'";
        }

        if(cuisinetext.equals("All Cuisine")){
            cuisine = "";
        } else {
            cuisine = "where cuisine = '"+cuisinetext+"'";
        }
        Cursor rs = mydb.getDataforSpinner(cuisine, price);
        if (rs.moveToFirst()){
            do{
                String getName = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME));
                String getLat = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LATITUDE));
                String getLongi = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_LONGITUDE));
                String getType = rs.getString(rs.getColumnIndex(DBHelper.RESTAURANT_COLUMN_FOODTYPE));
                addMarker(getName, getLat, getLongi, getType);

            }while(rs.moveToNext());
        } else {
            Toast.makeText(getApplicationContext(),"No restaurant found!",Toast.LENGTH_SHORT).show();
        }
    }

    public void setDetails(Cursor restaurant) {
        rn = (TextView) findViewById(R.id.restaurantName);
        ri = (ImageView) findViewById(R.id.restaurantImage);
        distance = (TextView) findViewById(R.id.restaurantDistance);
        operatingDaysStart = (TextView) findViewById(R.id.operatingDaysField_Start);
       operatingHoursStart = (TextView) findViewById(R.id.operatingHoursField_Start);
        address = (TextView) findViewById(R.id.address);
        contact = (TextView) findViewById(R.id.contact);
        price = (TextView) findViewById(R.id.price);
        cuisine = (TextView) findViewById(R.id.cuisine);
        foodtype = (TextView) findViewById(R.id.foodType);
        rn.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_NAME)));

        String uri = "@drawable/" + restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_IMAGENAME));
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        ri.setImageDrawable(res);

        distance.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_DISTANCE)));
        operatingDaysStart.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_WORK)));
        operatingHoursStart.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_TIME)));
        address.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_ADDRESS)));
        contact.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CONTACT)));
        price.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_PRICE)));
        cuisine.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_CUISINE)));
        foodtype.setText(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_FOODTYPE)));
        ratingBar.setIsIndicator(false);
        ratingBar.setRating(Float.parseFloat(restaurant.getString(restaurant.getColumnIndex(DBHelper.RESTAURANT_COLUMN_RATING))));
    }

    public void setEmptyDetails() {
        rn = (TextView) findViewById(R.id.restaurantName);
        ri = (ImageView) findViewById(R.id.restaurantImage);
        distance = (TextView) findViewById(R.id.restaurantDistance);
        operatingDaysStart = (TextView) findViewById(R.id.operatingDaysField_Start);
        operatingHoursStart = (TextView) findViewById(R.id.operatingHoursField_Start);
        address = (TextView) findViewById(R.id.address);
        contact = (TextView) findViewById(R.id.contact);
        price = (TextView) findViewById(R.id.price);
        cuisine = (TextView) findViewById(R.id.cuisine);
        foodtype = (TextView) findViewById(R.id.foodType);


        String uri = "@drawable/restaurant";
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        ri.setImageDrawable(res);
        rn.setText("Restaurant Name");
        operatingDaysStart.setText("N/A");
        operatingHoursStart.setText("N/A");
        address.setText("N/A");
        contact.setText("N/A");
        price.setText("");
        cuisine.setText("N/A");
        distance.setText("N/A");
        foodtype.setText("N/A");

        ratingBar.setIsIndicator(true);
        ratingBar.setRating(0);

        callNo = "N/A";
        latitude = 0;
        longitude = 0;
    }

    public void addListenerOnSpinner(){
        cuisineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                getSpinnerData();

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        priceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                getSpinnerData();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    public void addListenerOnRatingBar() {

        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        //if rating value is changed,
        //display the current rating value in the result (textview) automatically
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                TextView rn = (TextView) findViewById(R.id.restaurantName);
                String name = rn.getText().toString();
                if(!name.equals("Restaurant Name")){
                    mydb.insertRating(rating,name);
                }
            }
        });
    }

    private void setUpMap() {
        if (checkLocationPermission()) {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.styles_string));

            if (!success) {
                Toast.makeText(getApplicationContext(),"json not success",Toast.LENGTH_SHORT).show();
            }
            UiSettings mUiSettings = mMap.getUiSettings();
            mMap.setMyLocationEnabled(true);
            mMap.setTrafficEnabled(false);
            mMap.setBuildingsEnabled(false);
            mUiSettings.setMyLocationButtonEnabled(true);
            mUiSettings.setTiltGesturesEnabled(true);
            mUiSettings.setRotateGesturesEnabled(false);

        }
    }

    public void anchorThePane(){
        if (mLayout != null) {
            mLayout.setAnchorPoint(0.7f);
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }
    }

    public void insertData(){

        mydb.insertRestaurant("Bumbledees at 1938", "Western", "0.8",
                "MON - FRI", "SAT - SUN", "9:00 - 20:00", "$$", "+6019-4733777",
                "BumbleDee's Cafe, 11800 George Town, Penang, Malaysia",
                "5.362104", "100.306944", "bumbledee", "Halal","0");

        mydb.insertRestaurant("The Kapits", "Western", "0.6",
                "MON-SUN", "N/A", "10:00 - 0:00", "$", "+6013-4999507",
                "8, Jalan Gemilang, 11800 Gelugor, Pulau Pinang",
                "5.360454", "100.303527", "the_kapits", "Halal","0");

        mydb.insertRestaurant("Chicos cafe", "Western", "0.45",
                "MON - FRI", " SAT - SUN", "8:00 - 17:00", "$", "+6012-4819061",
                "D01, Anjung Budi, Jalan Ilmu, 11800 Gelugor, Pulau Pinang",
                "5.357409","100.306151", "chico_cafe", "Halal","0");

        mydb.insertRestaurant("Subaidah USM", "Indian", "0.22",
                "MON - SUN", "N/A", "7:00 - 22:00", "$", "N/A",
                "17, Jalan Universiti, 11800 Gelugor, Pulau Pinang",
                "5.356727", "100.304263", "subaidah", "Halal","0");

        mydb.insertRestaurant("Golden Phoenix Restaurant", "Chinese", "0.7",
                "MON - SUN", "N/A", "7:00 - 1:00", "$", "+6012-4206888",
                "2-G-2, Jalan Sungai Dua, Taman Pekaka Desa University, 11700 Gelugor, Pulau Pinang",
                "5.352714", "100.300135", "phoenix", "Non-halal","0");

        mydb.insertRestaurant("Uview Cafe", "Multi-Cuisine", "0.24",
                "MON - SAT", "SUN", "OPEN FOR 24 HOURS", "$", "+6019-5652249",
                "Jalan Universiti, 11800 Gelugor, Pulau Pinang",
                "5.35877", "100.308409", "uview", "Halal","0");

        mydb.insertRestaurant("Campus Cafe USM","Multi-Cuisine", "0.8",
                "MON -FRI", "SAT - SUN", "10:30 - 17:00", "$ ","+6012-4482725",
                "Bangunan Canselori, 11700 Gelugor, Pulau Pinang", "5.358079",
                "100.305627", "campus_cafe", "Halal","0");

        mydb.insertRestaurant("43 Cafe","Cafe","0.55",
                "MON - SUN", "N/A","17:30 - 22:00", "$$","+6016-430 7009",
                "43, Jalan Sungai Dua, Kampung Dua Bukit, 11700 Gelugor, Pulau Pinang",
                "5.3532864","100.303787", "cafe_43", "Non-halal","0");

        mydb.insertRestaurant("Riszona Restaurant","African","0.55",
                "MON - SUN","N/A", "12:00 - 0:00","$","010-774 4948",
                "77B-1-14, Jalan Sungai Dua, Kampung Dua Bukit, 11700 Gelugor, Pulau Pinang",
                "5.352696","100.302292","riszona","Non-halal","0");

        mydb.insertRestaurant("Kopitan Classic","Cafe","0.6",
                "MON - SUN","N/A","11:00 - 23:00","$$","04-537 5697",
                "4, Lorong Cempedak 2, Kampung Dua Bukit, 14000 Gelugor, Pulau Pinang",
                "5.352553","100.301864", "kopitan_classic_sungai_dua","Halal","0");

        mydb.insertRestaurant("KFC","Fast Food","0.7",
                "MON - SUN","N/A","0:00 - 0:00","$","04-656 8539",
                "559 & 559A, Jalan Taman Sri Saujana,, Sungai Dua, 11700 George Town, Pulau Pinang",
                "5.3524329","100.301435","kfc","Halal","0");

        mydb.insertRestaurant("Man Burger Stall","Hawker Stall","0.75",
                "MON - SUN","N/A","19:00 - 1:00","$","017-303 6067",
                "Desa University Comm. Complex, 6, Jalan Sungai Dua, 11700 Gelugor, Pulau Pinang",
                "5.352384","100.299587","man_burger","Halal","0");

        mydb.insertRestaurant("McDonalds", "Fast Food", "0.8",
                "MON-SUN","N/A","OPEN FOR 24 HOURS", "$", "+6 04-659 6346",
                "4 B-C-D, Jalan Sungai Dua, 11700 Gelugor, Pulau Pinang", "5.352578",
                "100.299404","mcdonalds_sungai_dua", "Halal","0");

        mydb.insertRestaurant("Plus 2 Restaurant", "Chinese", "0.8",
                "MON - SUN", "TUE", "11:00 - 19:30", "$", "04-656 7118",
                "4K, Jalan Sungai Dua, Taman Pekaka Desa University, 11700 Gelugor, Pulau Pinang",
                "5.352478", "100.299189", "plus_2", "Non-halal","0");

        mydb.insertRestaurant("Pizza Hut", "Fast Food", "0.8",
                "MON-SUN", "N/A", "11:00 - 23:00","$", "1-300-88-2525",
                "Desa University, 4, Jalan Sungai Dua, 11700, Pulau Pinang",
                "5.352470", "100.299102", "pizza_hut_sungai_dua", "Halal","0");

        mydb.insertRestaurant("Nasi Kandar Pelita","Malay","0.9",
                "MON - SUN","N/A","0:00 - 0:00","$","04-656 4602",
                "723l-g, Jalan Sungai Dua, Desa Permai Indah, 11700 Gelugor, Pulau Pinang",
                "5.352024","100.299324","nasi_kandar_pelita","Halal","0");

        mydb.insertRestaurant("Hidden Recipe Cafe", "Cafe","0.9",
                "TUE - SUN","MON","11:30 - 22:00","$$", "012-534 5496",
                "723-G,Yellow House,Vanda Buisness Park,, Jalan Sungai Dua,Sungai Dua, 11700 George Town, Penang",
                "5.351739","100.298909","hidden_recipe", "Halal","0");

        mydb.insertRestaurant("Restaurant Kim Hin", "Chinese", "0.95",
                "MON - SUN", "WED", "16:30 - 22:00", "$", "+60 16-551 9152",
                "MK 13, 674C, Jalan Sungai Dua, Taman Pekaka, 11700 Gelugor, Pulau Pinang",
                "5.351722", "100.298206", "kim_him_seafood", "Non-halal","0");

        mydb.insertRestaurant("Restaurant Heng Leong Seafood","Chinese","1.0",
                "MON - SUN","TUE","16:30 - 22:00", "$","016-484 1383",
                "Taman Pekaka, 11700 Sungai Dua, Penang","5.351511","100.298093",
                "heng_leong","Non-halal","0");

        mydb.insertRestaurant("Restaurant Hutton Lane	Chinese	","Chinese", "1.0",
                "MON - SUNDAY","N/A	","11:00 - 22:30","$", "04-656 6586",
                "727F, Jalan Sungai Dua, Desa Permai Indah, 11700 Gelugor, Pulau Pinang	",
                "5.351233","100.298514","hutton_lane","Non-halal","0");

    }

}
