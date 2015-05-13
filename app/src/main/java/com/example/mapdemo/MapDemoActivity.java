package com.example.mapdemo;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MapDemoActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        LoaderCallbacks<Cursor>{

    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 secs */


    private double latitude;
    private double longitude;
    private float accuracy;

    //Test synchro github

    private Marker markerSearch = null;

    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_demo_activity);


        // We create the map fragment
        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    loadMap(map);
                }
            });
        } else {
            Toast.makeText(this, "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
        }

        handleIntent(getIntent());

    }

    public String[] loadMap(GoogleMap googleMap) {

        // We create the map and load it
        map = googleMap;
        String tokens[] = null;

        if (map != null) {
            // Map is ready
            Toast.makeText(this, "Map Fragment was loaded properly!", Toast.LENGTH_SHORT).show();
            map.setMyLocationEnabled(true);

            // Now that map has loaded, let's get our location!
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();

            connectClient();
        } else {
            Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
        }


        // Here we set the kml file we want to parse
        try {
            AssetManager mng = this.getAssets();
            InputStream str = mng.open("doc.kml");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(str);

            doc.getDocumentElement().normalize();

            // We parse the KML file, we get every element with the tag name "Placemark" and put them into a node list

            NodeList nList = doc.getElementsByTagName("Placemark");
            int temp;
            for (temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                // We look for the description content between "description" tag in each Placemark node

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    // We take the content from the "description" tag and put in into a Spanned, we also remove the html tags into the content

                    Spanned desc2 = Html.fromHtml(eElement.getElementsByTagName("description").item(0)
                            .getTextContent());

                    // We split the string with the content we need to use it in a nice layout after
                    // We are using a Pattern to do it

                    String split = "VOIE|LOCALISATION|PLACE_ELARGIE|ETAT|DUREE|NOMBRE_PLACES|CODE";
                    Pattern p = Pattern.compile(split);
                    tokens = p.split(desc2);

                    // Sometimes informations are empty or "Null" we replace it by "Illimité" because when
                    // the duration time on a parking slot it not provided it means it's unlimited (verified with SITG)

                    if (tokens[5].equals("") || (tokens[5].equals("<Null>"))) {

                        tokens[5] = "Illimité";
                    }

                    // We take the coordinates into the "coordinates" tag and put them into a string

                    String coordinates = eElement.getElementsByTagName("coordinates")
                            .item(0).getTextContent();

                    // The coordinates are not well formated into the kml file so we have to reformat them like this we can create a LatLng variable
                    // We are using a StringTokenizer to do it

                    StringTokenizer tokens1 = new StringTokenizer(coordinates, ",");
                    String lon = tokens1.nextToken();
                    String lat = tokens1.nextToken();
                    double lati = Double.parseDouble(lat);
                    double lngi = Double.parseDouble(lon);

                    LatLng LOCATION = new LatLng(lati, lngi);

                    // We create the snippet for the marker on the map
                    //The snippet contains all the informations about the parking slot

                    String snippet = "Voie : " +tokens[1] + System.getProperty("line.separator") + "Localisation : "+tokens[2] + System.getProperty("line.separator") + "Place élargie : "+tokens[3] + System.getProperty("line.separator") + "Durée maximum : " +tokens[5] + System.getProperty("line.separator") + "Nombre de places : "+tokens[6];

                    // We create the marker with all the informatations that we took before (Location, title, snippet)
                    // We put a custom icon for the marker on the map and also put some transparency on these icons

                    map.addMarker(new MarkerOptions()
                            .position(LOCATION)
                            .title(tokens[1])
                            .alpha(0.5f)
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.drawable.symbol)));

                    // We create a OnInfoWindowClickListener like this when the user touch on the marker he gets the informations

                    map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick(com.google.android.gms.maps.model.Marker marker) {
                            marker.getSnippet();
                        }
                    });


                    map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                        View contents = getLayoutInflater().inflate(R.layout.info_window_layout, null);

                        @Override

                        public View getInfoWindow(Marker marker) {
                            return null;
                        }

                        @Override

                        public View getInfoContents(final Marker marker) {


                            TextView txtTitle = ((TextView) contents.findViewById(R.id.titre));

                            if (marker.getTitle() != null) {

                                // Spannable string allows us to edit the formatting of the text.

                                SpannableString titleText = new SpannableString(marker.getTitle());

                                titleText.setSpan(new ForegroundColorSpan(Color.BLUE), 0, titleText.length(), 0);

                                txtTitle.setText(marker.getTitle());

                            } else {

                                txtTitle.setText("");

                            }

                            TextView txtSnippet = ((TextView) contents.findViewById(R.id.snippet));

                            txtSnippet.setText(marker.getSnippet());

                            return contents;

                        }

                    });


                }

            }

            //showLocation();

        } catch (Exception e) {
            Log.e("Buildmap Error", e.getMessage());
        }
        return new String[0];
    }


    protected void connectClient() {
        // Connect the client.
        if (isGooglePlayServicesAvailable() && mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    /*
     * Called when the Activity becomes visible.
    */
    @Override
    protected void onStart() {
        super.onStart();
        connectClient();
    }

    /*
	 * Called when the Activity is no longer visible.
	 */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /*
     * Handle results returned to the FragmentActivity by Google Play services
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mGoogleApiClient.connect();
                        break;
                }

        }
    }

    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Location Updates");
            }

            return false;
        }
    }

    /*
     * Called by Location Services when the request to connect the client
     * finishes successfully. At this point, you can request the current
     * location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            Toast.makeText(this, "GPS location was found!", Toast.LENGTH_SHORT).show();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            map.animateCamera(cameraUpdate);
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Current location was null, enable GPS on emulator!", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        accuracy = location.getAccuracy();

    }

    /*
     * Called by Location Services if the connection to the location client
     * drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Called by Location Services if the attempt to Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Sorry. Location services not available to you", Toast.LENGTH_LONG).show();
        }
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private void handleIntent(Intent intent){
        if(intent.getAction().equals(Intent.ACTION_SEARCH)){
            doSearch(intent.getStringExtra(SearchManager.QUERY));
        }else if(intent.getAction().equals(Intent.ACTION_VIEW)){
            getPlace(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void doSearch(String query){
        Bundle data = new Bundle();
        data.putString("query", query);
        getSupportLoaderManager().restartLoader(0, data, this);
    }

    private void getPlace(String query){
        Bundle data = new Bundle();
        data.putString("query", query);
        getSupportLoaderManager().restartLoader(1, data, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_search:
                onSearchRequested();
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle query) {
        CursorLoader cLoader = null;
        if(arg0==0)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.SEARCH_URI, null, null, new String[]{ query.getString("query") }, null);
        else if(arg0==1)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.DETAILS_URI, null, null, new String[]{ query.getString("query") }, null);
        return cLoader;
    }

    public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
        showLocations(c, map);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
    }

    private void showLocations(Cursor c, GoogleMap map){


        MarkerOptions markerOptions = null;
        LatLng position = null;
        while(c.moveToNext()){
            markerOptions = new MarkerOptions();
            position = new LatLng(Double.parseDouble(c.getString(1)),Double.parseDouble(c.getString(2)));
            markerOptions.position(position);
            markerOptions.title(c.getString(0));

            markerSearch = map.addMarker(markerOptions);


        }
        if(position!=null){
            CameraUpdate cameraPosition = CameraUpdateFactory.newLatLng(position);
            map.animateCamera(cameraPosition);
        }

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                markerSearch.remove();
            }
                });

            }


        }
