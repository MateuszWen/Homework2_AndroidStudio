package com.mw.homework2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap; //przechowuje aktualnie załadowaną mapę
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationClient;  // dostarcza dostęp do serwisu lokalizacji
    private LocationRequest mLocationRequest;  // dostarcza informacji o aktualizacji seriwsu lokalizacji (czyli chyba tego co wyżej)
    private LocationCallback locationCallback; // przechowuje aktualizacje tego co ogranął wyżej (tego mLocationRequest)
    Marker gpsMarker = null; //pokazuje aktualną pozycję urządzenia

    private final String POSITIONS_JSON_FILE = "positions5.json"; //positions saved in Json format


    private List<MarkerJson> markerList;
    private List<MarkerJsonString> markerListString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); //obiekt GoogleMap jest inicjalizowany za pomoca getMapAsync ... "that will trigger a onMapReady call when the Map is ready"

        //Create an instance of FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //jest tworzony by uzyskać dostęp do LocationServices.

        markerList = new ArrayList<>(); //Initialize markerList
        markerListString = new ArrayList<>(); //Initialize markerList


        // Log.d("ROZMIAR", String.valueOf(markerList.size()));

        restoreFromJson();

        //Log.d("HEJ", String.valueOf(markerList.size()));

        //if(markerList.size() != 0) {
            for (MarkerJsonString ml : markerListString) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(Double.parseDouble(ml.X), Double.parseDouble(ml.Y)))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                        .alpha(0.8f)
                        .title(String.format("Position:(%.2f, %.2f)", Double.parseDouble(ml.X), Double.parseDouble(ml.Y))));
            }
        //}
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
        //poniższe linie ustawiają, że MapsActivity będzie handlerem map loader, marker client i map long click events
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // ustawia interwał z jakim mapa będzie się aktualizowała
        mLocationRequest.setFastestInterval(5000); // ustawia najszybszy możliwy interwał dla apliakcji, jest potrzebne ze względu na to, że w tym
                                                   // samym czasie inne apliakcje moga korzystać z GoogleMaps i mogłyby się kłócić
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationsUpdates(){
        //Request location updates with mLocationRequest and locationCallback
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null); //requestLocationUpdates() uzyskuje informacje o położeniu od fusedLocationClient
                                                                                                      //z ustawieniami mLocationRequest, zaktualozowana loaklizacja jest zapisywana w locationCallback
    }

    private void createLocationCallback(){
        //create the LocationCallback
        locationCallback = new LocationCallback(){
            @Override
            //jest odpowiedzialny za aktualizowanie informacji o lokalizacji na urządzeniu, zminy lokalizacji sa przechowywane w MapsActivity, poprzez ustawienie markera na mapie
            public void onLocationResult(LocationResult locationResult){
                //Code executed when user's location changes
                if(locationResult != null){
                    //remove the last reported location
                    if(gpsMarker != null){
                        gpsMarker.remove();
                        //Add a custom marker to the map for location from the locationResult
                        Location location = locationResult.getLastLocation();
                        gpsMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(),location.getLongitude()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                        .alpha(0.8f)
                        .title("Current Location"));
                    }
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {

        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){ //if sprawdza czy program ma dostęp do uzsyskiwania lokalizacji
            //Request the missing permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation(); //pobierana jest ostatnio zapisana lokalizacjia

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //Add a marker on the last known location
                if(location != null && mMap != null){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });

        createLocationRequest();
        createLocationCallback();
        startLocationsUpdates();
    }

    //onPause() uruchamiane jest kiedy użytkownik przełączy naszą aplikację na jakąś inną, ale ta będzie działała w tle
    @Override
    protected void onPause(){
        super.onPause();
        stopLocationUpdates();
    }

    //poniższa metoda z tego co rozumiem zatrzymuje dalsze aktualizaowanie położenia
    private void stopLocationUpdates(){
        if(locationCallback != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }


    //metoda pozwala na dodawanie znaczników na mapie po dłuższym przytrzymaniu palca w danym miejscu na mapie,
    @Override
    public void onMapLongClick(LatLng latLng) {
        //float distance = 0f;

        if(markerList.size() > 0){
            //If the markerList is not empty, calculate the distance between long click position and the last element of the marker list
            //Marker lastMarker = markerList.get(markerList.size() - 1);
            //float [] tmpDis = new float[3];
            //Calculate the distance between two points
            //Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude, latLng.latitude, latLng.longitude, tmpDis);
            //the distance is provided at index 0 of the tmpDis array
            //distance = tmpDis[0];

            //Create a blue line between the two points
            /*PolylineOptions rectOptions = new PolylineOptions()
                    .add(lastMarker.getPosition())
                    .add(latLng)
                    .width(10)
                    .color(Color.BLUE);
            mMap.addPolyline(rectOptions);*/
        }
        //Add a custom marker at the position of the long click
        Marker marker = mMap.addMarker(new MarkerOptions()
        .position(new LatLng(latLng.latitude, latLng.longitude))
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
        .alpha(0.8f)
        .title(String.format("Position:(%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        //Add the marker to the list
        markerList.add(new MarkerJson(latLng.latitude, latLng.longitude)); //<-- adding to list for programm
        markerListString.add(new MarkerJsonString(Double.toString(latLng.latitude), Double.toString(latLng.longitude))); // <-- adding to list for Json

        Log.d("HEJ", String.valueOf(markerList.size()));

        savePositionToJson();

        Log.d("HEJ", String.valueOf(markerList.size()));
        Log.d("HEJ", "Wyszedłem z opcji zapisywania do Json - savePositionToJson()");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //Zoom the map on the marker
       /* CameraPosition cameraPos = mMap.getCameraPosition();
        if(cameraPos.zoom < 14f)
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f));*/
        return false;
    }

    public void zoomInClick(View view) {
        //Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        //Zoom out the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    protected void onDestroy() {
        Log.d("zamykam", "TUDU");
        savePositionToJson();
        Log.d("zamykam", "TUDU");
        super.onDestroy();

    }

    //metoda zapisuje dane w pliku w formie pliku JSON
    private void savePositionToJson(){
        Gson gson = new Gson();
        String listJson = gson.toJson(markerListString);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(POSITIONS_JSON_FILE, MODE_APPEND);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //metoda odczytuje dane w formie JSON i zapisuje na listę danych w programie - taka specjalna metoda do odczytywania akurat JSON z pliku
    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 1000;
        Gson gson = new GsonBuilder().setLenient().create();
        String readJson;

        try{
            inputStream = openFileInput(POSITIONS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){


                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<MarkerJsonString>>(){}.getType();
            List<MarkerJsonString> o = gson.fromJson(readJson, collectionType);
            if(o != null){
                markerListString.clear();
                markerList.clear();
                for(MarkerJsonString position : o){
                    markerListString.add(position);
                    markerList.add(new MarkerJson(Double.parseDouble(position.X), Double.parseDouble(position.Y)));
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public class MarkerJson{

        private double X;
        private double Y;

        public MarkerJson(double X, double Y){
            this.X = X;
            this.Y = Y;
        }
    }
    public class MarkerJsonString{

        private String X;
        private String Y;

        public MarkerJsonString(String X, String Y){
            this.X = X;
            this.Y = Y;
        }
    }
}


