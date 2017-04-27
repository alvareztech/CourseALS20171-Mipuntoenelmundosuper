package tech.alvarez.mipuntoenelmundosuper;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleApiClient googleApiClient;

    private TextView latitudTextView;
    private TextView longitudTextView;

    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudTextView = (TextView) findViewById(R.id.latitudTextView);
        longitudTextView = (TextView) findViewById(R.id.longitudTextView);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();

        crearSolicitudLocalizacion();
        crearSolicitudConfiguracion();
        verificarConfiguracionParaLocalizacion();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            detenerObtencionUbicaciones();
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        iniciarObtenerUbicaciones();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("MIAPP", "Localización: " + location.getLatitude() + ", " + location.getLatitude());
        latitudTextView.setText(String.valueOf(location.getLatitude()));
        longitudTextView.setText(String.valueOf(location.getLongitude()));
    }

    // Métodos crear

    private void crearSolicitudLocalizacion() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void crearSolicitudConfiguracion() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        locationSettingsRequest = builder.build();
    }

    // Iniciar y detener el proceso

    private void iniciarObtenerUbicaciones() {
        if (tenemosPermisoUbicacion()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        } else {
            manejarPermisoDenegado();
        }
    }

    private void detenerObtencionUbicaciones() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }


    // Configuraciones de localización

    private void verificarConfiguracionParaLocalizacion() {
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                Status status = result.getStatus();

                if (status.getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                    Log.d("MIAPP", "Todo configurado para obtener ubicaciones");
                    iniciarObtenerUbicaciones();
                } else if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.d("MIAPP", "No satisface lo necesario, abrimos diálogo");
                        status.startResolutionForResult(MainActivity.this, 999);
                    } catch (IntentSender.SendIntentException e) {
                        Log.d("MIAPP", "No podemos abrir díalogo");
                    }
                } else if (status.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                    Log.d("MIAPP", "No podemos abrir de nuevo el diálogo de configuraciones, activalo manualmente");
                    Toast.makeText(MainActivity.this, "No podemos abrir de nuevo el diálogo de configuraciones, activalo manualmente", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 999) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("MIAPP", "Permitió el cambio de configuraciones de ubicación");
                iniciarObtenerUbicaciones();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("MIAPP", "No permitió el cambio de configuraciones de ubicación");
                Toast.makeText(this, "No permitió el cambio de configuraciones de ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Permisos de ubicación

    private boolean tenemosPermisoUbicacion() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void manejarPermisoDenegado() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Ya rechazaste anteriormente la solictud, debes activar en configuraciones", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 666);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 666) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarObtenerUbicaciones();
            } else {
                Toast.makeText(this, "Permisos no cedidos", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
