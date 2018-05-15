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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.vision.barcode.Barcode;
import com.vivekkumar.itassetmanager.constant.AssetManagerConstant;
import com.vivekkumar.itassetmanager.sessionutil.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class HomeActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST = 200;
    private static final String TAG = "HomeActivity";
    private static final String URL_FOR_RETURNING_ASSET = AssetManagerConstant.DNS_URL + "ITAssetManager/returnAsset";
    private static final String URL_FOR_GENERATING_REPORT = AssetManagerConstant.DNS_URL + "ITAssetManager/generateExcelReport";
    ProgressDialog progressDialog;
    private Button btnAddUsr, btnLogOut, btnScan, btnReturn, btnSendReport;
    private boolean scanDeallocate = false;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        session = new SessionManager(getApplicationContext());
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        btnAddUsr = (Button) findViewById(R.id.btn_home_add_user);
        btnScan = (Button) findViewById(R.id.btn_alloc);
        btnReturn = (Button) findViewById(R.id.btn_ret);
        btnSendReport = (Button) findViewById(R.id.btn_send_report);
        session.checkLogin();
        btnAddUsr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AddUserActivity.class);
                startActivity(i);
                finish();
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

        btnSendReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendReport(session.getUserDetails().get("email"));
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
        request_json.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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

    private void sendReport(final String emailId) {
        // Tag used to cancel the request
        String cancel_req_tag = "SendReport";

        progressDialog.setMessage("Generating Report ...");
        showDialog();
        StringRequest strReq = new StringRequest(Request.Method.GET,
                URL_FOR_GENERATING_REPORT + "?email="+emailId, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Report Response: " + response.toString());
                hideDialog();

                    boolean error = Boolean.valueOf(response);
                    String msg = null;
                    if (!error) {
                        msg = "Report generated and has been mailed to you.";
                    } else {
                        msg = "No Data to generate report";
                    }
                    Toast.makeText(getApplicationContext(),
                            msg, Toast.LENGTH_LONG).show();


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Report Generation error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) ;
        strReq.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Adding request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq, cancel_req_tag);


    }

    private void logout() {
        session.logoutUser();
    }
}


