package com.mw.homework2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, SensorEventListener {

    private GoogleMap mMap; //przechowuje aktualnie załadowaną mapę (tak, tę która widzimy na ekranie)
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationClient;  // dostarcza dostęp do serwisu lokalizacji
    private LocationRequest mLocationRequest;  // dostarcza informacji o aktualizacji seriwsu lokalizacji (czyli chyba tego co wyżej)
    private LocationCallback locationCallback; // przechowuje aktualizacje tego co ogranął wyżej (tego mLocationRequest)
    Marker gpsMarker = null; //pokazuje aktualną pozycję urządzenia

    static public SensorManager mSensorManager; // <-- manager do obsługi Sensorów
    static Sensor sensor; // <-- tej wartości będzie przypisany Akcelerometr

    private final String POSITIONS_JSON_FILE = "positions.json"; //positions saved in Json format (Json file)

    private List<LatLng> markerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Code above hide 2 floatButtons and textView
        setContentView(R.layout.activity_main);
        View but1 = findViewById(R.id.floatingActionButton_startAccelerometer);
        but1.setVisibility(View.GONE);
        View but2 = findViewById(R.id.floatingActionButton_hideAccelerometer2);
        but2.setVisibility(View.GONE);
        View textView = findViewById(R.id.textView);
        textView.setVisibility(View.GONE);

        //Inicjalization sensor - accelerometr
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); //obiekt GoogleMap jest inicjalizowany za pomoca getMapAsync ... "that will trigger a onMapReady call when the Map is ready"

        //Create an instance of FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //jest tworzony by uzyskać dostęp do LocationServices.

        markerList = new ArrayList<>(); //Initialize markerList
    }

    @Override
    //metoda wywoływana jest za każdym razem kiedy Sensor (a w tym konkretnym programie Akcelerometr zarejestruje jakąkolwiek zmianę)
    public void onSensorChanged(SensorEvent event){
        if(startStop == true){
            TextView textView = findViewById(R.id.textView);
            textView.setTextColor(Color.WHITE);
            //Create display information
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Accelerometr:\n");
            stringBuilder.append(String.format("X: %4f\r\r\r", event.values[0]));
            stringBuilder.append(String.format("Y: %4f\n", event.values[1]));
            String text = stringBuilder.toString();
            textView.setGravity(Gravity.CENTER);
            textView.setText(text);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
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
    //inicjuje możliwość używania wszelkich metod (w tym np. onMapLongClick), dodaje tzw. "listenery" czyli słuchacze, które kiedy stanie się określona dla nich czynnośc na ekranie mapy - zostaną wywołane
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //poniższe linie ustawiają, że MapsActivity będzie handlerem map loader, marker client i map long click events
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);

        //stawianie znaczników odczytanych z pliku Json nie może odbywac się w onCreate, ponieważ obiekt mapy jest inicjalizowany dopiero w TEJ metodzie za pomoca polecenia " mMap = googleMap "
        restoreFromJson();
        for (LatLng ml : markerList) {
            mMap.addMarker(new MarkerOptions()
                    .position(ml)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                    .alpha(0.8f)
                    .title(String.format("Position:(%.2f, %.2f)", ml.latitude, ml.latitude)));
        }
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
        mSensorManager.unregisterListener(this); //<- odrejestrowanie sensor managera
    }

    //poniższa metoda z tego co rozumiem zatrzymuje dalsze aktualizaowanie położenia (połozenia urządzenia)
    private void stopLocationUpdates(){
        if(locationCallback != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    //metoda obsługująca kliknięcie w marker
    public boolean onMarkerClick(Marker marker) {
        //ustawia na widoczne przyciski floatButton1
        View but1 = findViewById(R.id.floatingActionButton_startAccelerometer);
        Animation animationBut1 = AnimationUtils.loadAnimation(this, R.anim.blink);
        but1.startAnimation(animationBut1);
        but1.setVisibility(View.VISIBLE);

        //ustawia na widoczne przyciski floatButton2
        View but2 = findViewById(R.id.floatingActionButton_hideAccelerometer2);
        Animation animationBut2 = AnimationUtils.loadAnimation(this, R.anim.blink);
        but2.startAnimation(animationBut2);
        but2.setVisibility(View.VISIBLE);

        return false;
    }

    //przybliżenie widoku na mapie za pomocą "lupy"
    public void zoomInClick(View view) {
        //Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    //oddalenie widoku na mapie za pomocą "lupy"
    public void zoomOutClick(View view) {
        //Zoom out the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //metoda zapisuje dane w pliku w formie pliku JSON
    private void savePositionToJson(){
        Gson gson = new Gson();
        String listJson = gson.toJson(markerList);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(POSITIONS_JSON_FILE, MODE_PRIVATE);
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
            Type collectionType = new TypeToken<List<LatLng>>(){}.getType();
            List<LatLng> o = gson.fromJson(readJson, collectionType);
            if(o != null){
                markerList.clear();
                for(LatLng position : o){
                    markerList.add(new LatLng(position.latitude, position.longitude));
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    //metoda pozwala na dodawanie znaczników na mapie po dłuższym przytrzymaniu palca w danym miejscu na mapie,
    public void onMapLongClick(LatLng latLng) {

        //Add a custom marker at the position of the long click
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
                .alpha(0.8f)
                .title(String.format("Position:(%.2f, %.2f)", latLng.latitude, latLng.longitude)));

        markerList.add(latLng); //<-- adding to list for programm

        savePositionToJson(); // <--zapisywanie nowego znacznika do pliku Json
    }

    //metoda obsługująca przycisk "Clear Memory"
    public void onClickClearMemoryButton(View view) {
        markerList.clear(); //<-- czyszczenie markerList
        savePositionToJson(); //<-- metoda nadpisze plik Json uprzednio wyczyszczoną listą
        mMap.clear(); //<-- mMap jest obiektem reprezentującym widoczną dla użytkownika mapę, mMap.clear() czyści mapę z znaczników

        View but1 = findViewById(R.id.floatingActionButton_startAccelerometer);
        View but2 = findViewById(R.id.floatingActionButton_hideAccelerometer2);
        View textView = findViewById(R.id.textView);
        //jeżeli któryś z floatButton lub textView był widoczny podczas wywołania metody onClickClearMemoryButton, zposzczególny if go schowa z animacją
        if(but1.getVisibility() == View.VISIBLE) {
            Animation animationBut1 = AnimationUtils.loadAnimation(this, R.anim.blink2);
            but1.startAnimation(animationBut1);
            but1.setVisibility(View.GONE);
        }
        if(but2.getVisibility() == View.VISIBLE) {
            Animation animationBut2 = AnimationUtils.loadAnimation(this, R.anim.blink2);
            but2.startAnimation(animationBut2);
            but2.setVisibility(View.GONE);
        }
        if(textView.getVisibility() == View.VISIBLE){
            Animation animationTextView = AnimationUtils.loadAnimation(this, R.anim.blink2);
            textView.startAnimation(animationTextView);
            textView.setVisibility(View.GONE);
        }

        onMapReady(mMap); //<-- metoda ma za zadanie inicjowanie mapy na ekranie, jeżeli wyczyścimy uprzednio mapę i prekażemy ją do metody, wtedy stara mapa z znacznikami zostanie "zastąpiona" nową bez znaczników
    }

    //service floatingButtonStartAccelerometer
    boolean startStop = false;
    public void onClickStartAccelerometer(View view) {
        if(startStop == false){
            startStop = true;
            View textView = findViewById(R.id.textView);
            Animation animationTextView = AnimationUtils.loadAnimation(this, R.anim.blink);
            textView.startAnimation(animationTextView);
            textView.setVisibility(View.VISIBLE);
        }else{
            startStop = false;
            View textView = findViewById(R.id.textView);
            Animation animationTextView = AnimationUtils.loadAnimation(this, R.anim.blink2);
            textView.startAnimation(animationTextView);
            textView.setVisibility(View.GONE);
        }
    }

    //obsługa floatingButton chowania przycisków i textView
    public void onClickHideAccelerometer(View view) {
        View but1 = findViewById(R.id.floatingActionButton_startAccelerometer);
        Animation animationBut1 = AnimationUtils.loadAnimation(this, R.anim.blink2);
        but1.startAnimation(animationBut1);
        but1.setVisibility(View.GONE);

        View but2 = findViewById(R.id.floatingActionButton_hideAccelerometer2);
        Animation animationBut2 = AnimationUtils.loadAnimation(this, R.anim.blink2);
        but2.startAnimation(animationBut2);
        but2.setVisibility(View.GONE);

        View textView = findViewById(R.id.textView);
        //textView może być akurat widoczny lub niewidoczny (zależy czy w trakcie zamykanie był robiony pomiar), jeżeli widoczny to również zostanie zamknięty z pomocą animacji
        if(textView.getVisibility() == View.VISIBLE) {
            Animation animationTextView = AnimationUtils.loadAnimation(this, R.anim.blink2);
            textView.startAnimation(animationTextView);
            textView.setVisibility(View.GONE);
        }
    }
}


