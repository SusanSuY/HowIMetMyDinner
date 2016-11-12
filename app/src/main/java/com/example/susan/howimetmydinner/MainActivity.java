package com.example.susan.howimetmydinner;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.MenuInflater;
import android.view.Menu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import java.util.List;

public class MainActivity extends AppCompatActivity {
//declerations
    MapView mMapView;
    GraphicsLayer mLocationLayer;
    Point mLocationLayerPoint;
    String mLocationLayerPointString;
    //ArcGISTiledMapServiceLayer mBaseMap;
    EditText mSearchEditText;
    LocationDisplayManager lDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {// creates main space
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.map);     //mapview class
        mLocationLayer = new GraphicsLayer();            //location instance
        mMapView.addLayer(mLocationLayer);               //add layer
        //mBaseMap = new ArcGISTiledMapServiceLayer(getResources().getString(R.string.basemap_url));

        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {  //
            public void onStatusChanged(Object source, STATUS status) {
                if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
                    boolean mIsMapLoaded = true;
                }
            }
        });
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMapView && status == STATUS.INITIALIZED) {
                    lDisplayManager = mMapView.getLocationDisplayManager();
                    lDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
                    lDisplayManager.setLocationListener(new LocationListener() {

                        boolean locationChanged = false;

                        // Zooms to the current location when first GPS fix arrives.
                        @Override
                        public void onLocationChanged(Location loc) {
                            if (!locationChanged) {
                                locationChanged = true;
                                double locy = loc.getLatitude();
                                double locx = loc.getLongitude();
                                Point wgspoint = new Point(locx, locy);
                                Point mapPoint = (Point) GeometryEngine
                                        .project(wgspoint,
                                                SpatialReference.create(4326),
                                                mMapView.getSpatialReference());

                                Unit mapUnit = mMapView.getSpatialReference()
                                        .getUnit();
                                double zoomWidth = Unit.convertUnits(
                                        SEARCH_RADIUS,
                                        Unit.create(LinearUnit.Code.MILE_US),
                                        mapUnit);
                                Envelope zoomExtent = new Envelope(mapPoint,
                                        zoomWidth, zoomWidth);
                                mMapView.setExtent(zoomExtent);

                            }

                        }

                        @Override
                        public void onProviderDisabled(String arg0) {

                        }

                        @Override
                        public void onProviderEnabled(String arg0) {
                        }

                        @Override
                        public void onStatusChanged(String arg0, int arg1,
                                                    Bundle arg2) {

                        }
                    });
                    lDisplayManager.start();

                }

            }

            @Override
            public void onSingleTap(float x, float y) {

                callout.hide();

                // Handles the tapping on Graphic

                int[] graphicIDs = graphicsLayer.getGraphicIDs(x, y, 25);
                if (graphicIDs != null && graphicIDs.length > 0) {
                    Graphic gr = graphicsLayer.getGraphic(graphicIDs[0]);
                    updateContent((String) gr.getAttributeValue("Rating"),
                            (String) gr.getAttributeValue("Title"));
                    Point location = (Point) gr.getGeometry();
                    callout.setOffset(0, -15);
                    callout.show(location, content);
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        View searchRef = menu.findItem(R.id.action_search).getActionView();
        mSearchEditText = (EditText) searchRef.findViewById(R.id.searchText);
        mSearchEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onSearchButtonClicked(mSearchEditText);
                    return true;
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSearchButtonClicked(View view) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        String address = mSearchEditText.getText().toString();
        executeLocatorTask(address);
    }

    private void executeLocatorTask(String address) {
        //Create Locator parameters from single line address string
        LocatorFindParameters findParams = new LocatorFindParameters(address);
        //Use the centre of the current map extent as the find location point
        findParams.setLocation(mMapView.getCenter(), mMapView.getSpatialReference());
        // Calculate distance for find operation
        Envelope mapExtent = new Envelope();
        mMapView.getExtent().queryEnvelope(mapExtent);
        // assume map is in metres, other units wont work, double current envelope
        double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent.getWidth() * 2 : 10000;
        findParams.setDistance(distance);
        findParams.setMaxLocations(2);
        // Set address spatial reference to match map
        findParams.setOutSR(mMapView.getSpatialReference());
        // Execute async task to find the address
        new LocatorAsyncTask().execute(findParams);
        mLocationLayerPointString = address;
    }

    private class LocatorAsyncTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        @Override
        protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... params) {
            mException = null;
            List<LocatorGeocodeResult> results = null;
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            if (mException != null) {
                Log.w("PlaceSearch", "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MainActivity.this, getString(R.string.addressSearchFailed), Toast.LENGTH_LONG).show();
                return;
            }
            if (result.size() == 0) {
                Toast.makeText(MainActivity.this, getString(R.string.noResultsFound), Toast.LENGTH_LONG).show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);
                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();
                // create marker symbol to represent location
                SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
                // create graphic object for resulting location
                Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
                // add graphic to location layer
                mLocationLayer.addGraphic(resultLocGraphic);
                // create text symbol for return address
                String address = geocodeResult.getAddress();
                TextSymbol resultAddress = new TextSymbol(20, address, Color.BLACK);
                // create offset for text
                resultAddress.setOffsetX(-4 * address.length());
                resultAddress.setOffsetY(10);
                // create a graphic object for address text
                Graphic resultText = new Graphic(resultPoint, resultAddress);
                // add address text graphic to location graphics layer
                mLocationLayer.addGraphic(resultText);
                mLocationLayerPoint = resultPoint;
                // Zoom map to geocode result location
                mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
            }
        }
    }
}
