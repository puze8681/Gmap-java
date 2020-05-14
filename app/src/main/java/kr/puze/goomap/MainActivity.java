package kr.puze.goomap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static MapView gMap;
    public static GoogleMap mMap;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        gMap = findViewById(R.id.map);
        gMap.onCreate(savedInstanceState);
        gMap.getMapAsync(MainActivity.this);
        getData();
    }

    private void getData() {
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference().child("map");

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("LOGTAG", "onDataChange"+dataSnapshot.toString());

                for(DataSnapshot data: dataSnapshot.getChildren()){
                    Log.d("LOGTAG", "onDataChange"+data.toString());
                    MapData map = data.getValue(MapData.class);
                    Log.d("LOGTAG", "onDataChange"+map.toString());
                    setPin(map.image, map.title, map.number, map.lat, map.lng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        mDatabase.addValueEventListener(valueEventListener);
    }

    private void setPin(String path, final String title, final String number, final double lat, final double lng) {
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://silmu-7526a.appspot.com");
        StorageReference storageRef = storage.getReference().child("map");
        StorageReference pathReference = storageRef.child(path);

        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d("LOGTAG", "uri "+uri.toString());
                new GetImageFromUrl(title, number, lat, lng).execute(uri.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("LOGTAG", "addOnFailureListener");
            }
        });
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        MapsInitializer.initialize(this);
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15F));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.52487, 126.92723)));

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View view = null;
                try {
                    view = View.inflate(MainActivity.this, R.layout.layout_info_window, null);
                    Log.d("LOGTAG", "marker = "+ marker.getTitle() + marker.getTitle() + Objects.requireNonNull(marker.getTag()).toString());
                    TextView name = view.findViewById(R.id.text_name);
                    TextView number = view.findViewById(R.id.text_number);
                    ImageView image = view.findViewById(R.id.image);
                    name.setText(marker.getTitle());
                    number.setText(marker.getSnippet());
                    image.setImageBitmap((Bitmap)marker.getTag());
                } catch (Exception e) {
                    Log.d("LOGTAG", "get Info Contents Exception = "+e.toString());
                }
                return view;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+ marker.getSnippet())));

            }
        });

        Log.d("LOGTAG", "onMapReady");
    }

    protected void onResume() {
        super.onResume();
        gMap.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        gMap.onDestroy();
    }

    public void onLowMemory() {
        super.onLowMemory();
        gMap.onLowMemory();
    }

    public class GetImageFromUrl extends AsyncTask<String, Void, Bitmap> {
        Bitmap bitmap;
        String title;
        String number;
        Double lat;
        Double lng;

        private GetImageFromUrl(String title, String number, Double lat, Double lng){
            this.title = title;
            this.number = number;
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            String stringUrl = url[0];
            bitmap = null;
            InputStream inputStream;
            try {
                inputStream = new java.net.URL(stringUrl).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap){
            super.onPostExecute(bitmap);
            Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 200, 180, false);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions
                    .position(new LatLng(lat, lng))
                    .title(title)
                    .snippet(number)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            Marker m = mMap.addMarker(markerOptions);
            m.setTag(smallBitmap);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15.0F));
            Log.d("LOGTAG", "onPostExecute");
        }
    }
}