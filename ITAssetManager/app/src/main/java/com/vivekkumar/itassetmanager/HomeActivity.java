package com.vivekkumar.itassetmanager;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class HomeActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST = 200;
    private static final String TAG = "HomeActivity";
    private static final String URL_FOR_RETURNING_ASSET = "http://10.0.2.2:8080/ITAssetManager/returnAsset";
    ProgressDialog progressDialog;
    //private TextView greetingTextView;
    private Button btnAddUsr, btnLogOut, btnScan, btnReturn;
    private boolean scanDeallocate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //Bundle bundle = getIntent().getExtras();
        //String user = bundle.getString("username");
        //greetingTextView = (TextView) findViewById(R.id.greeting_text_view);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        btnAddUsr = (Button) findViewById(R.id.btn_home_add_user);
        btnScan = (Button) findViewById(R.id.btn_alloc);
        btnReturn = (Button) findViewById(R.id.btn_ret);
        //greetingTextView.setText("Hello "+ user);
        // Progress dialog
        /*btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i);
            }
        });*/
        btnAddUsr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AddUserActivity.class);
                startActivity(i);
            }
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDeallocate = false;
                Intent intent = new Intent(HomeActivity.this, ScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDeallocate = true;
                Intent intent = new Intent(HomeActivity.this, ScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                final Barcode barcode = data.getParcelableExtra("barcode");
                if (!scanDeallocate) {
                    Intent intent = new Intent(
                            HomeActivity.this,
                            AllocateAssetActivity.class);
                    intent.putExtra("assetId", barcode.displayValue);
                    startActivity(intent);
                    finish();
                } else {
                    submitForm(barcode.displayValue);
                }

            }
        }
    }


    private void submitForm(String assetId) {

        deallocateAsset(assetId);
    }

    private void deallocateAsset(final String assetId) {
        // Tag used to cancel the request
        String cancel_req_tag = "DEAllocate";

        progressDialog.setMessage("Returning asset ...");
        showDialog();

        HashMap<String, Object> params = new HashMap<>();
        params.put("assetId", assetId);

        JsonObjectRequest request_json = new JsonObjectRequest(Request.Method.PUT, URL_FOR_RETURNING_ASSET, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Add Response: " + response.toString());
                        hideDialog();

                        try {
                            boolean error = response.getBoolean("hasError");

                            if (!error) {
                                String user = response.getJSONObject("user").getString("firstName");
                                String assetType = response.getString("assetType");
                                Toast.makeText(getApplicationContext(), assetType + " with " + assetId + "is returned by " + user + "!", Toast.LENGTH_SHORT).show();

                            } else {

                                String errorMsg = response.getString("error");
                                Toast.makeText(getApplicationContext(),
                                        errorMsg, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Asset Deallocation: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        });
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(request_json, cancel_req_tag);
    }

    private void showDialog() {
        if (!progressDialog.isShowing())
            progressDialog.show();
    }

    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

    private void logout() {
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
    }
}


