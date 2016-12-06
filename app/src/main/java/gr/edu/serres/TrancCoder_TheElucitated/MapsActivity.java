package gr.edu.serres.TrancCoder_TheElucitated;

/**
 * Edited by Damian on 11/19/2016.
 */

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import gr.edu.serres.TrancCoder_TheElucitated.Services.BackgroundSoundService;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnInfoWindowClickListener
{

    boolean mapReady,musicOn;
    Intent backgroundMusic;
    private GoogleMap mMap;
    Button  mapTypeBtn;
    ToggleButton toggleMusic;
    float MapZoom = 16.5f;
    PopupMenu mapTypeMenu;
    MenuInflater inflater;
    HashMap<String,IMarker> markerHashMap;
    List<IMarker> itemList;
    String stateName;
    /*
     * Location variables
     */
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    /*
     * Proximity variables
     */
    private final String PROX_ALERT = "app.test.PROXIMITY_ALERT";
    private ProximityReceiver proximityReceiver = null;
    private LocationManager locationManager = null;
    private ProximityController proximityController;

    /* Permissions variables
     * used in Permission functions
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;
    private int exp;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (isGooglePlayServicesAvailable(this)) {
            buildGoogleApiClient();
        }

        mapReady = false;

        //Create background music -by default music is off
        createBackgroundMusic();
        //create intent for picking item
        mapFragment.getMapAsync(this);
        // \Proximity
        /////////////////////////////////////

        markerHashMap = new HashMap<>();
        itemList = new ArrayList<>();

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        Button tiltBtn = (Button) findViewById(R.id.tilt_btn);
        tiltBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCameraOptions();
            }
        });


    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(musicOn) startService(backgroundMusic);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onStop();
        if(musicOn) stopService(backgroundMusic);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        unregisterReceiver(proximityReceiver);
        if(checkPermission()) {
            for(ProximityPoint proximityPoint:proximityController.getProximityPointList()){
                locationManager.removeProximityAlert(proximityPoint.getPendingIntent());
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);

        if(checkPermission()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        if (mLastLocation != null) {
            List<Address> addresses;
            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                stateName = addresses.get(0).getLocality();
                Toast.makeText(this, " County = " + stateName, Toast.LENGTH_SHORT).show();
                createMarkersForCurrentCounty();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                Toast.makeText(this, "There is a problem with the markers ", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void createMarkersForCurrentCounty() {
        switch (stateName){
            case "Serres":
                createSerresQuestMarkers();
                createSerresItemMarkers();
                break;
        }

    }


    private void createSerresQuestMarkers(){
        MyMarker victimsBar = new MyMarker("Chocolate","quest",mMap,new LatLng(41.087022,23.547429),R.mipmap.quest,"Victims_bar");
        victimsBar.add();
        victimsBar.show();

        MyMarker home = new MyMarker("Victim's Home","quest",mMap,new LatLng(41.090270,23.54963),R.mipmap.quest,"Victims_home");
        home.add();
        markerHashMap.put(home.getTag(),home);

        MyMarker accountant = new MyMarker("Accountant","quest",mMap,new LatLng(41.085631,23.544688),R.mipmap.quest,"Accountant");
        accountant.add();
        markerHashMap.put(accountant.getTag(),accountant);

        MyMarker queenJackClub = new MyMarker("Queen Jack Club","quest",mMap,new LatLng(41.092082,23.558603),R.mipmap.quest,"Queen_jack_club");
        queenJackClub.add();
        markerHashMap.put(queenJackClub.getTag(),queenJackClub);
    }

    private void createSerresItemMarkers(){
        Resources res = getResources();
        TypedArray itemName = res.obtainTypedArray(R.array.serres_items);
        TypedArray itemExp =res.obtainTypedArray(R.array.serres_items_experience);
        TypedArray itemLatitude = res.obtainTypedArray(R.array.serres_items_latitude);
        TypedArray itemLongitude = res.obtainTypedArray(R.array.serres_items_longitude);

        for(int i=0;i<itemName.length();i++){
            MyMarker marker = new MyMarker(itemName.getString(i),"item",mMap,new LatLng(Double.parseDouble(itemLatitude.getString(i)),Double.parseDouble(itemLongitude.getString(i))),itemName.getString(i)+".png",itemExp.getString(i));
            marker.add();
            itemList.add(marker);
        }

        proximityReceiver = new ProximityReceiver(mMap);
        IntentFilter intentFilter = new IntentFilter(PROX_ALERT);
        intentFilter.addDataScheme("geo");
        registerReceiver(proximityReceiver, intentFilter);
        proximityController = new ProximityController(getApplicationContext(),locationManager);

        try {
            for (IMarker item : itemList) {
                proximityReceiver.addMarker(item);
                proximityController.createProximityPoint(item);
                proximityController.addProximityAlert(item);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"List is empty",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        changeMapStyle();// Change Map style to Night mode

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();//My Location and localize button

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        //mMap.animateCamera(CameraUpdateFactory.zoomIn());


        //Get Camera Zoom when Camera changes
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                MapZoom =  mMap.getCameraPosition().zoom;
            }
        });

        //Select Map Type
        createPopupMenuForMapType();

        mMap.setOnInfoWindowClickListener(this);
        mapReady = true;
    }



    @Override
    public void onInfoWindowClick(final Marker marker) {
        String type = marker.getSnippet();
        if(type.equals("quest")){
            Toast.makeText(getApplicationContext(), (String) marker.getTag(),Toast.LENGTH_SHORT).show();
            String questTag = (String) marker.getTag();
            switch (questTag){
                case "Victims_bar":
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.quest_done));
                    markerHashMap.get("Victims_home").show();
                    break;
                case "Victims_home":
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.quest_done));
                    markerHashMap.get("Accountant").show();
                    break;
                case "Accountant":
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.quest_done));
                    markerHashMap.get("Queen_jack_club").show();
                    break;
                case "Queen_jack_club":
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.quest_done));
                    break;
            }
        }else if(type.equals("item")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You want to pick up an "+marker.getTitle()+" ?")
                    .setTitle(marker.getTitle());
            builder.setPositiveButton("OK",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(),"OK button clicked.You got " + marker.getTag()+" Experience",Toast.LENGTH_SHORT).show();
                    for(IMarker item:itemList){
                        if(marker.getTitle().equals(item.getName())){
                            proximityController.removePendingIntent(item);
                            item.delete();
                        }
                    }
                }
            });
            builder.setNegativeButton("Leave item",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(),"You leaved "+marker.getTitle(),Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void setCameraOptions(){
        LatLng location = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)
                .zoom(17)
                //.bearing(90)
                .tilt(75)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void changeMapStyle(){
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e("MapsActivityRaw", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivityRaw", "Can't find style.", e);
        }
    }

    public void enableMyLocation(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    /* Shows a Toast with text when
     * MyLocation Button is clicked
     */
    @Override
    public boolean onMyLocationButtonClick(){
        Toast.makeText(this, " Finding your location  . . . ", Toast.LENGTH_SHORT).show();
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        return false;
    }

    /**
     * When invoked check permission for FINE or COARSE location
     * @return true if permission granted
     */
    protected boolean checkPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                /*&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED*/) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        if(requestCode != LOCATION_PERMISSION_REQUEST_CODE){
            return;
        }

        if(PermissionUtils.isPermissionGranted(permissions, grantResults,Manifest.permission.ACCESS_FINE_LOCATION)){
            enableMyLocation();
        }else{
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments(){
        super.onResumeFragments();
        if(mPermissionDenied){
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    private void showMissingPermissionError(){
        PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(),"dialog");
    }

    /**
     * Checks if Google Play services are installed and available
     * @param activity
     * @return true if are installed and available
     */
    public boolean isGooglePlayServicesAvailable(Activity activity){
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(resultCode == ConnectionResult.SUCCESS){
            return true;
        }else{
            return false;
        }
    }


    void createBackgroundMusic(){
        toggleMusic = (ToggleButton)findViewById(R.id.toggle_music) ;

        musicOn = false;
        backgroundMusic=new Intent(this, BackgroundSoundService.class);
        toggleMusic.setText("");
        toggleMusic.setTextOn("");
        toggleMusic.setTextOff("");
        toggleMusic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startService(backgroundMusic);
                    toggleMusic.setBackgroundResource(R.mipmap.ic_pause_circle_outline);
                    musicOn=true;
                } else {
                    stopService(backgroundMusic);
                    toggleMusic.setBackgroundResource(R.mipmap.ic_play_circle_outline);
                    musicOn=false;
                }
            }
        });
    }

    /*void createStoryItems(MapItems items){
        //Set up some items on the map
       // DummyItem magnifier = new DummyItem("magnifier","A very clean magnifier. Find clues better",new LatLng(41.069131,23.55389),R.mipmap.ic_launcher);
      //  DummyItem glasses = new DummyItem("glasses","You can't look very far away without them",new LatLng(41.07902,23.553690),R.mipmap.ic_launcher);
      //  DummyItem handcuffs = new DummyItem("handcuffs","You need these in order to catch the criminal",new LatLng(41.07510,23.552997),R.mipmap.ic_launcher);
      //  DummyItem chocolate = new DummyItem("Chocolate","Chocolate",new LatLng(41.087022,23.547429),R.mipmap.ic_launcher);

        //DummyItem home = new DummyItem("Home","Home",new LatLng(41.080722,23.547429),R.mipmap.ic_launcher);
        MyMarker home = new MyMarker("Crime Scene","quest",mMap,new LatLng(41.080722,23.547429),R.mipmap.ic_launcher,"Crime_Scene");

        //home.setDialogue(getResources().getStringArray(R.array.Victims_home));
       //DummyItem accountant = new DummyItem("Accountant","Accountant",new LatLng(41.085631,23.544688),R.mipmap.ic_launcher);
        //accountant.setDialogue(getResources().getStringArray(R.array.Accountant));
       //DummyItem queen_jack_club = new DummyItem("Queen Jack Club","Queen Jack Club",new LatLng(41.092082,23.558603),R.mipmap.ic_launcher);
       // queen_jack_club.setDialogue(getResources().getStringArray(R.array.Queen_jack_club));


        // DummyItem university = new DummyItem("University","University",new LatLng(40.677737,22.925500),R.mipmap.ic_launcher);
      //  DummyItem home_thessaloniki = new DummyItem("Home","Home",new LatLng(40.630389,22.943000),R.mipmap.ic_launcher);
       // DummyItem hospital = new DummyItem("Hospital","Hospital",new LatLng(40.640063,22.94419),R.mipmap.ic_launcher);
        //items.addItems(magnifier,glasses,handcuffs,chocolate,home,accountant,queen_jack_club,university,home_thessaloniki,hospital);


    }*/

    /*void onItemClickListener(){
        //On Item Click Function
        mMap.setOnGroundOverlayClickListener(new GoogleMap.OnGroundOverlayClickListener() {
            @Override
            public void onGroundOverlayClick(GroundOverlay groundOverlay) {
                try {
                    DummyItem itemFound = inventoryMap.getItemByIconId(groundOverlay.getId());
                    if(itemFound.hasDialogue()){
                        Intent intent = new Intent(MapsActivity.this,DialogsActivity.class);
                        intent.putExtra("dialogue",itemFound.getDialogue());
                        MapsActivity.this.startActivity(intent);
                    }else{
                        Toast.makeText(getApplicationContext(),itemFound.getDescription(),Toast.LENGTH_SHORT).show();
                    }

                } catch (ItemNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }*/

    /*void createSpinnerForFindItemInMap(){
        //pickItemSpinner = (Spinner)findViewById(R.id.pick_item_spinnerr);
        ArrayAdapter<String> pickItemAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,inventoryMap.getNamesOfAllItemsInInventory());
        pickItemSpinner.setAdapter(pickItemAdapter);
        pickItemSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //TODO
                itemSelected = String.valueOf(adapterView.getItemAtPosition(i));
                if(!(inventoryMap.getNamesOfAllItemsInInventory().isEmpty())) {
                    try {
                        itemSelectedLocation = inventoryMap.getItem(itemSelected).getLocation();
                        mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(itemSelectedLocation ,MapZoom));
                    } catch (ItemNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }*/

    void createPopupMenuForMapType(){
        mapTypeBtn = (Button)findViewById(R.id.options_btn);
        mapTypeMenu = new PopupMenu(MapsActivity.this,mapTypeBtn);
        inflater = mapTypeMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_map_type, mapTypeMenu.getMenu());
        mapTypeMenu.getMenu().findItem(R.id.normal).setChecked(true);
        mapTypeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mapTypeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.satellite:
                                if(item.isChecked()){
                                    item.setChecked(false);
                                }else{
                                    item.setChecked(true);
                                    if(mapReady)mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                }
                                return true;
                            case R.id.hybrid:
                                if(item.isChecked()){
                                    item.setChecked(false);
                                }else{
                                    item.setChecked(true);
                                    if(mapReady)mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                }
                                return true;
                            case R.id.terrain:
                                if(item.isChecked()){
                                    item.setChecked(false);
                                }else{
                                    item.setChecked(true);
                                    if(mapReady)mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                }
                                return true;
                            case R.id.normal:
                                if(item.isChecked()){
                                    item.setChecked(false);
                                }else{
                                    item.setChecked(true);
                                    if(mapReady)mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                }
                            default:
                                return MapsActivity.super.onOptionsItemSelected(item);
                        }
                    }
                });
                mapTypeMenu.show();
            }
        });
    }



}
