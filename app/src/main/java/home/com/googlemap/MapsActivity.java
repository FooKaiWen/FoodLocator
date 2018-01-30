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
    public static final String TAG = MapsActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_CODE = 99;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private DBHelper mydb;
    ArrayList<Marker> mapMarkers = new ArrayList<>();
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


        addListenerOnRatingBar();
        setEmptyDetails();

        cuisineSpinner =  findViewById(R.id.cuisineSpinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.cuisine_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cuisineSpinner.setAdapter(adapter);

        priceSpinner = findViewById(R.id.priceSpinner);
        final ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.price_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priceSpinner.setAdapter(adapter2);

        mLayout =  findViewById(R.id.sliding_layout);
        mLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        FloatingActionButton randomFill =  findViewById(R.id.randomFilters);
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
                    getLatLngCallNo(rs);
                    Toast.makeText(getApplicationContext(), "We randomly chose for you!", Toast.LENGTH_LONG).show();
                    anchorThePane();
                }
            }
        });

        ImageButton callB =  findViewById(R.id.callButton);
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

        ImageButton uberB =  findViewById(R.id.transportButton);
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
                    Toast.makeText(getApplicationContext(),"SCROLL UP FOR MORE",Toast.LENGTH_LONG).show();
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
        Marker currentLocationMarker = mMap.addMarker(markerOptions);
        mapMarkers.add(mMap.addMarker(markerOptions));
    }

    public void getSpinnerData(){
        mMap.clear();
        cuisineSpinner = findViewById(R.id.cuisineSpinner);
        String cuisinetext = cuisineSpinner.getSelectedItem().toString();
        priceSpinner = findViewById(R.id.priceSpinner);
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
        rn =  findViewById(R.id.restaurantName);
        ri =  findViewById(R.id.restaurantImage);
        distance =  findViewById(R.id.restaurantDistance);
        operatingDaysStart = findViewById(R.id.operatingDaysField_Start);
       operatingHoursStart =  findViewById(R.id.operatingHoursField_Start);
        address =  findViewById(R.id.address);
        contact =  findViewById(R.id.contact);
        price =  findViewById(R.id.price);
        cuisine =  findViewById(R.id.cuisine);
        foodtype = findViewById(R.id.foodType);
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
        rn =  findViewById(R.id.restaurantName);
        ri =  findViewById(R.id.restaurantImage);
        distance =  findViewById(R.id.restaurantDistance);
        operatingDaysStart =  findViewById(R.id.operatingDaysField_Start);
        operatingHoursStart =  findViewById(R.id.operatingHoursField_Start);
        address =  findViewById(R.id.address);
        contact =  findViewById(R.id.contact);
        price =  findViewById(R.id.price);
        cuisine =  findViewById(R.id.cuisine);
        foodtype = findViewById(R.id.foodType);


        String uri = "@drawable/restaurant";
        int imageResource = getResources().getIdentifier(uri, null, getPackageName());
        Drawable res = getResources().getDrawable(imageResource);
        ri.setImageDrawable(res);
        rn.setText(R.string.clickAnyRestaurant);
        operatingDaysStart.setText("");
        operatingHoursStart.setText("");
        address.setText("");
        contact.setText("");
        price.setText("");
        cuisine.setText("");
        distance.setText("");
        foodtype.setText("");

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

        ratingBar =  findViewById(R.id.ratingBar);

        //if rating value is changed,
        //display the current rating value in the result (textview) automatically
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                TextView rn =  findViewById(R.id.restaurantName);
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

        mydb.insertRestaurant("Bumbledees at 1938", "Western", "0.8 km",
                "MON - FRI", "9:00 - 20:00", "$$", "+6019-4733777",
                "BumbleDee's Cafe, 11800 George Town, Penang, Malaysia",
                "5.362104", "100.306944", "bumbledee", "Halal","0");

        mydb.insertRestaurant("The Kapits", "Western", "0.6 km",
                "MON-SUN", "10:00 - 0:00", "$", "+6013-4999507",
                "8, Jalan Gemilang, 11800 Gelugor, Pulau Pinang",
                "5.360454", "100.303527", "the_kapits", "Halal","0");

        mydb.insertRestaurant("Chicos cafe", "Western", "0.45 km",
                "MON - FRI", "8:00 - 17:00", "$", "+6012-4819061",
                "D01, Anjung Budi, Jalan Ilmu, 11800 Gelugor, Pulau Pinang",
                "5.357409","100.306151", "chico_cafe", "Halal","0");

        mydb.insertRestaurant("Subaidah USM", "Indian", "0.22 km",
                "MON - SUN", "7:00 - 22:00", "$", "N/A",
                "17, Jalan Universiti, 11800 Gelugor, Pulau Pinang",
                "5.356727", "100.304263", "subaidah", "Halal","0");

        mydb.insertRestaurant("Golden Phoenix Restaurant", "Chinese", "0.7 km",
                "MON - SUN", "7:00 - 1:00", "$", "+6012-4206888",
                "2-G-2, Jalan Sungai Dua, Taman Pekaka Desa University, 11700 Gelugor, Pulau Pinang",
                "5.352714", "100.300135", "phoenix", "Non-halal","0");

        mydb.insertRestaurant("Uview Cafe", "Multi-Cuisine", "0.24 km",
                "MON - SAT", "OPEN FOR 24 HOURS", "$", "+6019-5652249",
                "Jalan Universiti, 11800 Gelugor, Pulau Pinang",
                "5.35877", "100.308409", "uview", "Halal","0");

        mydb.insertRestaurant("Campus Cafe USM","Multi-Cuisine", "0.8 km",
                "MON -FRI", "10:30 - 17:00", "$ ","+6012-4482725",
                "Bangunan Canselori, 11700 Gelugor, Pulau Pinang", "5.358079",
                "100.305627", "campus_cafe", "Halal","0");

        mydb.insertRestaurant("43 Cafe","Cafe","0.55 km",
                "MON - SUN", "17:30 - 22:00", "$$","+6016-430 7009",
                "43, Jalan Sungai Dua, Kampung Dua Bukit, 11700 Gelugor, Pulau Pinang",
                "5.3532864","100.303787", "cafe_43", "Non-halal","0");

        mydb.insertRestaurant("Riszona Restaurant","African","0.55 km",
                "MON - SUN","12:00 - 0:00","$","010-774 4948",
                "77B-1-14, Jalan Sungai Dua, Kampung Dua Bukit, 11700 Gelugor, Pulau Pinang",
                "5.352696","100.302292","riszona","Non-halal","0");

        mydb.insertRestaurant("Kopitan Classic","Cafe","0.6 km",
                "MON - SUN","11:00 - 23:00","$$","04-537 5697",
                "4, Lorong Cempedak 2, Kampung Dua Bukit, 14000 Gelugor, Pulau Pinang",
                "5.352553","100.301864", "kopitan_classic_sungai_dua","Halal","0");

        mydb.insertRestaurant("KFC","Fast Food","0.7 km",
                "MON - SUN","0:00 - 0:00","$","04-656 8539",
                "559 & 559A, Jalan Taman Sri Saujana,, Sungai Dua, 11700 George Town, Pulau Pinang",
                "5.3524329","100.301435","kfc","Halal","0");

        mydb.insertRestaurant("Man Burger Stall","Hawker Stall","0.75 km",
                "MON - SUN","19:00 - 1:00","$","017-303 6067",
                "Desa University Comm. Complex, 6, Jalan Sungai Dua, 11700 Gelugor, Pulau Pinang",
                "5.352384","100.299587","man_burger","Halal","0");

        mydb.insertRestaurant("McDonalds", "Fast Food", "0.8 km",
                "MON-SUN","OPEN FOR 24 HOURS", "$", "+6 04-659 6346",
                "4 B-C-D, Jalan Sungai Dua, 11700 Gelugor, Pulau Pinang", "5.352578",
                "100.299404","mcdonalds_sungai_dua", "Halal","0");

        mydb.insertRestaurant("Plus 2 Restaurant", "Chinese", "0.8 km",
                "MON - SUN", "11:00 - 19:30", "$", "04-656 7118",
                "4K, Jalan Sungai Dua, Taman Pekaka Desa University, 11700 Gelugor, Pulau Pinang",
                "5.352478", "100.299189", "plus_2", "Non-halal","0");

        mydb.insertRestaurant("Pizza Hut", "Fast Food", "0.8 km",
                "MON-SUN", "11:00 - 23:00","$", "1-300-88-2525",
                "Desa University, 4, Jalan Sungai Dua, 11700, Pulau Pinang",
                "5.352470", "100.299102", "pizza_hut_sungai_dua", "Halal","0");

        mydb.insertRestaurant("Nasi Kandar Pelita","Malay","0.9 km",
                "MON - SUN","0:00 - 0:00","$","04-656 4602",
                "723l-g, Jalan Sungai Dua, Desa Permai Indah, 11700 Gelugor, Pulau Pinang",
                "5.352024","100.299324","nasi_kandar_pelita","Halal","0");

        mydb.insertRestaurant("Hidden Recipe Cafe", "Cafe","0.9 km",
                "TUE - SUN","11:30 - 22:00","$$", "012-534 5496",
                "723-G,Yellow House,Vanda Buisness Park,, Jalan Sungai Dua,Sungai Dua, 11700 George Town, Penang",
                "5.351739","100.298909","hidden_recipe", "Halal","0");

        mydb.insertRestaurant("Restaurant Kim Hin", "Chinese", "0.95 km",
                "MON - SUN", "16:30 - 22:00", "$", "+60 16-551 9152",
                "MK 13, 674C, Jalan Sungai Dua, Taman Pekaka, 11700 Gelugor, Pulau Pinang",
                "5.351722", "100.298206", "kim_him_seafood", "Non-halal","0");

        mydb.insertRestaurant("Restaurant Heng Leong Seafood","Chinese","1.0 km",
                "MON - SUN","16:30 - 22:00", "$","016-484 1383",
                "Taman Pekaka, 11700 Sungai Dua, Penang","5.351511","100.298093",
                "heng_leong","Non-halal","0");

        mydb.insertRestaurant("Restaurant Hutton Lane	Chinese	","Chinese", "1.0 km",
                "MON - SUNDAY","11:00 - 22:30","$", "04-656 6586",
                "727F, Jalan Sungai Dua, Desa Permai Indah, 11700 Gelugor, Pulau Pinang	",
                "5.351233","100.298514","hutton_lane","Non-halal","0");

        mydb.insertRestaurant("Restoran Khaleel","Indian","1.5 km",
                "MON - SUN", "6:00 - 2:00", "$","04-659 3314",
                "1-1-11,12,12A, Plaza Ivory, Halaman Bukit Gambir, Sunway Bukit Gambier, 11700 Gelugor, Pulau Pinang",
        "5.356694","100.292615", "khaleel","Halal","0");

        mydb.insertRestaurant("Kafe RotiBakar University Place","Cafe","1.8 km",
                "MON - SUN","8:00 - 22:00", "$$","04-655 2989",
                "1, Halaman Bukit Gambir 3, Sunway Bukit Gambier, 11700 Gelugor, Pulau Pinang",
                "5.357133","100.292596", "roti_bakar", "Halal", "0");

        mydb.insertRestaurant("Korean Cafe Gil","Korean","1.7 km","MON - SAT",
                "SUN","$$", "04-656 2558",
                "Plaza Ivory, 1, Halaman Bukit Gambir, Sunway Bukit Gambier, 11700 Gelugor, Pulau Pinang",
                "5.357856","100.292489","korean_gil", "Halal","0");

        mydb.insertRestaurant("LK Western Cafe", "Cafe","1.7 km",
                "MON - SUN","12:30 - 23:00", "$$","04-659 1637",
                "2-1-11, Plaza Ivory, Persiaran Bukit Gambir, Sunway Bukit Gambier, 11700 Gelugor, Pulau Pinang",
                "5.358259","100.292554", "lk_western", "Halal", "0");

        mydb.insertRestaurant("Al Shami Restaurant","Arabic","1.7 km",
                "MON - SUN", "15:30 - 24:00", "$$","019-232 0121",
                "2-1-4, Plaza Ivory, Jalan Bukit Gambir, Sunway Bukit Gambier, 11700 Gelugor, Pulau Pinang",
                "5.358177","100.292543", "alshami","Halal", "0");

        mydb.insertRestaurant("Falafel Syria", "Hawker Stall","1.8 km",
                "MON - SUN","18:00 - 24:00", "$","N/A",
                "Sunway Bukit Gambier, 11700 Gelugor, Penang","5.358693",
                "100.292235", "falafel","Halal","0");

        mydb.insertRestaurant("Spades Burger","Fast Food","3.0 km",
                "MON - SUN", "11:30 - 22:00", "$$$", "04-638 4848",
                "Medan Kampung Relau 1, Bayan Lepas, 11900 Bayan Lepas, Pulau Pinang",
                "5.332584", "100.293021", "spade_burger", "Non-halal", "0");

        mydb.insertRestaurant("Tsuruya Japanese Restaurant","Japanese","3.4 km",
                "MON - SUN",  "11:30 - 22:00", "$$", "04-641 0828",
                "i-, 1-1-38/39, Medan Kampung Relau 1, Bayan Lepas, 11900 Bayan Lepas, Penang",
                "5.333247","100.293239", "tsuruya","Non-halal", "0");

        mydb.insertRestaurant("Sushi-Burito Spice Canopy", "Multi-cuisine","4.0 km",
                "MON - SUN","11:00 - 22:00", "$$$", "04-642 2855",
                "Bayan Baru, 11900 Bayan Lepas, Penang", "5.328793", "100.278882",
                "sushi_burito","Halal", "0");

        mydb.insertRestaurant("Restaurants Red Chopstick","Nyonya","3.8 km",
                "MON - SUN", "12:00 - 21:30", "$$", "016-440 3938",
                "1-1-5, Ideal Avenue, Jalan Tun Dr Awang, Kampung Seberang Paya, 11900 Bayan Lepas, Pulau Pinang",
                "5.332515","100.29383","redchopstick","Non-halal","0");
    }

}
