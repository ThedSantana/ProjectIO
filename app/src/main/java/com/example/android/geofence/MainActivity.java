package com.example.android.geofence;

import android.*;
import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity
    implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        ResultCallback<Status>

{

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView textLat, textLong;
    private MapFragment mapFragment;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private FloatingActionButton fab;

    private static final String NOTIFICATION_MSG = "NOTIFICATION_MSG";
    //Create an intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg){
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_MSG, msg);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textLat = (TextView) findViewById(R.id.lat);
        textLong = (TextView) findViewById(R.id.lon);
        fab = (FloatingActionButton) findViewById(R.id.fab);


        //Initialize GoogleMaps
        initGMaps();

        //Create GoogleApiClient instance
        createGoogleApi();

        //Set a click listener for the floating action button to create a Geofence
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(geoFenceMarker != null){
                    startGeofence();


                }else
                    Snackbar.make(v,"Tap on the map to create a Geofence marker",Snackbar.LENGTH_SHORT)
                    .setAction("Action",null).show();


            }
        });



    }

    //Method to create a GoogleApiClient instance
    private void createGoogleApi(){
        Log.d(TAG, "createGoogleApi()");
        if( googleApiClient == null){
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }

    }


    @Override
    protected void onStart() {
        super.onStart();

        //Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG,  "onConnectionFailed");

    }

    //Callback when the GoogleApiClient is connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");

        getLastKnownLocation();
    }

    //Method to get the last known location
    private void getLastKnownLocation(){
        Log.d(TAG, "getLastKnownLocation()");

        if(checkPermission()){

            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if(lastLocation != null ){
                Log.i(TAG, "LastKnown Location. " +
                        "Long: " + lastLocation.getLongitude() +
                        "| Lat: " + lastLocation.getLatitude());

                writeLastLocation();
                startLocationUpdates();
            }else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }

        }
        else askPermission();

    }

    private LocationRequest locationRequest;
    //Defined in milliseconds
    private final int UPDATE_INTERVAL = 10000;
    private final int FASTEST_INTERVAL = 900;
    private final int REQ_PERMISSION = 1;

    //Method to start location updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");

        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if(checkPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this);

            //Show your current location on the map (blue dot)
            map.setMyLocationEnabled(true);
    }

    //Callback when location is changed
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");

        lastLocation = location;
        writeActualLocation(location);
    }

    //Method to write location coordinates on UI
    private void writeActualLocation(Location location){
        textLat.setText("Lat: " + location.getLatitude());
        textLong.setText("Long " + location.getLongitude());

        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    //Method to write last location coordinates
    private void writeLastLocation(){
        writeActualLocation(lastLocation);
    }

    //Check for permission to access Location
    private boolean checkPermission(){
        Log.d(TAG, "checkPermission()");

        //Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    //Ask for permission to access location
    private void askPermission(){
        Log.d(TAG, "askPermission()");

        ActivityCompat.requestPermissions(
                this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

    //Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){

            case REQ_PERMISSION: {
                if (grantResults.length> 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //Permission granted
                    getLastKnownLocation();
                } else{

                    //Permission denied
                    permissionDenied();
                }
                break;

            }

        }
    }

    //App cannot work without the permissions
    private void permissionDenied(){

        Toast.makeText(this, "permissionsDenied()", Toast.LENGTH_LONG).show();
    }



    //Method to initialize GoogleMaps
    private void initGMaps(){
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;

        //Sets a callback that is invoked when the map is touched
        map.setOnMapClickListener(this);

        //Sets a callback that is invoked when the marker is touched
        map.setOnMarkerClickListener(this);


    }

    //Callback called when the Map is touched
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick("+latLng+")");

        //Create a Geofence location using a marker
        markerForGeofence(latLng);
    }

    private Marker locationMarker;

    //Create a Location Marker
    private void markerLocation(LatLng latLng){
        Log.i(TAG, "markerLocation(" + latLng+ ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                .position(latLng)
                .title("My Current Location")
                .snippet(title);



        if(map!=null){

            //Remove the earlier marker
            if( locationMarker != null)
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);

        }

    }

    private Marker geoFenceMarker;

    //Create a Geofence Creation Marker

    private void markerForGeofence(LatLng latLng){
        Log.i(TAG, "markerForGeofence(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        //Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                .title("My Geofence")
                .snippet(title);


        if(map!=null){

            //Remove last geoFenceMarker
            if(geoFenceMarker != null){
                geoFenceMarker.remove();

            }


            geoFenceMarker = map.addMarker(markerOptions);


        }

    }



    //Callback called when Marker is touched

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition());

      if(geoFenceMarker!= null)
        if(marker.getTitle().equals(geoFenceMarker.getTitle())){


          //Show alert dialog
          showAlertDialog(marker);

          return true;


      }

        return false;
    }

    //Method to show an alert dialog when a Geofence marker is clicked
    private void showAlertDialog(final Marker marker){



        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //User clicked Save button
                    }
                })
                .setNegativeButton(R.string.dialog_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //remove the marker
                        marker.remove();
                    }
                })
                .setNeutralButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //User clicked the Cancel Button
                    }
                })
                .create();

        dialog.show();

    }


    //Create a Geofence
    private static final long GEO_DURATION = 60*60*1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static float GEOFENCE_RADIUS = 100.0f; // in meters

    //Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius){
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }


    //Create a GeofenceRequest object
    private GeofencingRequest createGeofenceRequest (Geofence geofence){
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }


    //Create a PendingIntent object to call an IntentService that will handle the GeofenceEvent
    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent(){
        Log.d(TAG, "createGeofencePendingIntent");
        if(geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionService.class);

        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

    }

    //Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request){
        Log.d(TAG, "addGeofence");

        if(checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }


    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if(status.isSuccess()) {

            drawGeofence();
        }else{

            //inform about fail
        }


    }

    //Draw Goefence circle on GoogleMap
    private Circle geoFenceLimits;
    private void drawGeofence(){

        Log.d(TAG, "drawGeofence()");

        if(geoFenceLimits != null)
            geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center(geoFenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70, 70,70))
                .fillColor(Color.argb(100,150,150,150))
                .radius(GEOFENCE_RADIUS);

        geoFenceLimits = map.addCircle(circleOptions);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);

        MenuItem spinnerItem = menu.findItem(R.id.menu_spinner);

        Spinner spinner = (Spinner) MenuItemCompat.getActionView(spinnerItem);

        //Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.geofence_radius_options,
                android.R.layout.simple_spinner_dropdown_item);

        //Attach the adapter to the spinner
        spinner.setAdapter(mSpinnerAdapter);

        //Handle user selections
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                //Dynamically change the Geofence Radius based on the user's choice

                switch (position){

                    case 0:
                        //Set Geofence radius to 100 meters
                        GEOFENCE_RADIUS = 100.0f;
                        break;
                    case 1:
                        //Set Geofence radius to 200 meters
                        GEOFENCE_RADIUS = 200.0f;
                        break;
                    case 2:
                        //Set Geofence radius to 300 meters
                        GEOFENCE_RADIUS = 300.0f;
                        break;
                    case 3:
                        //Set Geofence radius to 500 meters
                        GEOFENCE_RADIUS = 500.0f;
                        break;
                    case 4:
                        //Set Geofence radius to 1 kilometers
                        GEOFENCE_RADIUS = 1000.0f;
                        break;
                    default:
                        //default radius 100 meters
                        GEOFENCE_RADIUS = 100.0f;



                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        return super.onCreateOptionsMenu(menu);
    }

    //PlaceAutocomplete request code
    private final  int PLACE_AUTOCOMPLETE_REQUEST_CODE =1;

    //Filter results by country
    AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
            .setCountry("PH")
            .build();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()){

            case R.id.action_search:{

                try{

                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                             .setFilter(typeFilter)
                            .build(this);

                    startActivityForResult(intent,PLACE_AUTOCOMPLETE_REQUEST_CODE);

                }catch (GooglePlayServicesRepairableException e){

                    e.printStackTrace();

                }catch (GooglePlayServicesNotAvailableException e){

                    e.printStackTrace();
                }


            }

            case R.id.geofence: {

                startGeofence();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Start the Geofence creation process
    private  void startGeofence(){
        Log.i(TAG, "startGeofence()");

        if(geoFenceMarker != null){
            Geofence geofence = createGeofence(geoFenceMarker.getPosition(), GEOFENCE_RADIUS);
            GeofencingRequest geofencingRequest = createGeofenceRequest(geofence);
            addGeofence(geofencingRequest);

        }else{
            Log.e(TAG, "Geofence marker is null");
        }

    }

    //Handle the selected place from the Autocomplete widget
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE)
            if(resultCode == RESULT_OK){

                Place place = PlaceAutocomplete.getPlace(this,data);
                Log.i(TAG, "Place: " + place.getName());

                //Create a Geofence Marker on the selected location
                markerForGeofence(place.getLatLng());


            }else if (resultCode == PlaceAutocomplete.RESULT_ERROR){
                Status status = PlaceAutocomplete.getStatus(this, data);

                Log.i(TAG, status.getStatusMessage());
            }else if(requestCode == RESULT_CANCELED){

                //The user canceled the operation.
            }
    }
}
