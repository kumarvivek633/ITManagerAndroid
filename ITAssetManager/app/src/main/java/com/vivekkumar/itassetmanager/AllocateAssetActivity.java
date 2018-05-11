package com.vivekkumar.itassetmanager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.vivekkumar.itassetmanager.sessionutil.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class AllocateAssetActivity extends AppCompatActivity {


    private static final String TAG = "AddUserActivity";

    private static final String URL_FOR_ALLOCATING_ASSET = "http://ec2-18-219-215-21.us-east-2.compute.amazonaws.com:8080/ITAssetManager/allocateAsset";
    ProgressDialog progressDialog;
    private TextView assetId;
    private EditText empId, assetType;
    private Button btnAll, btnLogOut;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocate_asset);
        session = new SessionManager(getApplicationContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        Bundle bundle = getIntent().getExtras();
        String asset = bundle.getString("assetId");
        assetId = (TextView) findViewById(R.id.asset_id);
        empId = (EditText) findViewById(R.id.all_emp_id);
        assetType = (EditText) findViewById(R.id.asset_type);
        btnAll = (Button) findViewById(R.id.btn_all_asset);
        assetId.setText(asset);
        // Progress dialog
        session.checkLogin();
        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    private void submitForm() {

        allocateAsset(
                Long.valueOf(empId.getText().toString().equals("") ? "0" : empId.getText().toString()),
                assetId.getText().toString(),
                assetType.getText().toString());
    }

    private void allocateAsset(final Long empId, final String assetId, final String assetType) {
        // Tag used to cancel the request
        String cancel_req_tag = "Allocate";

        progressDialog.setMessage("Allocating asset ...");
        showDialog();

        HashMap<String, Object> params = new HashMap<>();
        params.put("empId", empId);
        params.put("assetId", assetId);
        params.put("assetType", assetType);

        JsonObjectRequest request_json = new JsonObjectRequest(Request.Method.POST, URL_FOR_ALLOCATING_ASSET, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Add Response: " + response.toString());
                        hideDialog();

                        try {
                            boolean error = response.getBoolean("hasError");

                            if (!error) {
                                String user = response.getJSONObject("user").getString("firstName");
                                Toast.makeText(getApplicationContext(), assetType + " is successfully allocated to " + user + "!", Toast.LENGTH_SHORT).show();

                                // Launch login activity
                                Intent intent = new Intent(
                                        AllocateAssetActivity.this,
                                        HomeActivity.class);
                                startActivity(intent);
                                finish();
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
                Log.e(TAG, "Asset Allocation: " + error.getMessage());
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

    private void logout() {
        session.logoutUser();
    }

}
