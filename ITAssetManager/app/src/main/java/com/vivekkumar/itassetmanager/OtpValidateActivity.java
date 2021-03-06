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
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.vivekkumar.itassetmanager.constant.AssetManagerConstant;
import com.vivekkumar.itassetmanager.sessionutil.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class OtpValidateActivity extends AppCompatActivity {


    private static final String TAG = "OtpValidateActivity";

    private static final String URL_FOR_ACTIVATE_USER = AssetManagerConstant.DNS_URL + "ITAssetManager/Activate_User";
    ProgressDialog progressDialog;
    private EditText otp;
    private String userEmail;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_validation);
        session = new SessionManager(getApplicationContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        otp = (EditText) findViewById(R.id.otp_text);
        userEmail = session.getUserDetails().get("email");
        Button btnActivate = (Button) findViewById(R.id.btn_avtivate);
        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    private void submitForm() {

        activateUser(
                Long.valueOf(otp.getText().toString().equals("") ? "0" : otp.getText().toString()),
                userEmail);
    }

    private void activateUser(final Long otp, final String email) {
        String cancel_req_tag = "Activate";

        progressDialog.setMessage("Activating user ...");
        showDialog();

        HashMap<String, Object> params = new HashMap<>();
        params.put("otp", otp);
        params.put("email", email);

        JsonObjectRequest request_json = new JsonObjectRequest(Request.Method.PUT, URL_FOR_ACTIVATE_USER, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Add Response: " + response.toString());
                        hideDialog();

                        try {
                            boolean error = response.getBoolean("hasError");

                            if (!error) {
                                String user = response.getJSONObject("user").getString("firstName");
                                Toast.makeText(getApplicationContext(), "Your account is successfully activated!", Toast.LENGTH_SHORT).show();

                                // Launch login activity
                                Intent intent = new Intent(
                                        OtpValidateActivity.this,
                                        LoginActivity.class);
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
                Log.e(TAG, "User Activate Error: " + error.getMessage());
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


}
