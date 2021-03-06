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

public class AddUserActivity extends AppCompatActivity {


    private static final String TAG = "AddUserActivity";

    private static final String URL_FOR_ADD_USER = AssetManagerConstant.DNS_URL + "ITAssetManager/addUser";
    ProgressDialog progressDialog;
    private EditText empId, firstName, lastName, email;

    SessionManager session;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        session = new SessionManager(getApplicationContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        empId = (EditText) findViewById(R.id.add_emp_id);
        firstName = (EditText) findViewById(R.id.add_first_name);
        lastName = (EditText) findViewById(R.id.add_last_name);
        email = (EditText) findViewById(R.id.add_email);
        Button btnAdd = (Button) findViewById(R.id.btn_add_user);
        session.checkLogin();
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    private void submitForm() {

        addUser(
                Long.valueOf(empId.getText().toString().equals("") ? "0" : empId.getText().toString()),
                firstName.getText().toString(),
                lastName.getText().toString(),
                email.getText().toString());
    }

    private void addUser(final Long empId, final String firstName, final String lastName, final String email) {
        String cancel_req_tag = "Add";

        progressDialog.setMessage("Adding user ...");
        showDialog();

        HashMap<String, Object> params = new HashMap<>();
        params.put("empId", empId);
        params.put("firstName", firstName);
        params.put("email", email);
        params.put("lastName", lastName);

        JsonObjectRequest request_json = new JsonObjectRequest(Request.Method.POST, URL_FOR_ADD_USER, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Add Response: " + response.toString());
                        hideDialog();

                        try {
                            boolean error = response.getBoolean("hasError");

                            if (!error) {
                                String user = response.getString("firstName");
                                Toast.makeText(getApplicationContext(), user + " is successfully Added!", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(
                                        AddUserActivity.this,
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
                Log.e(TAG, "User Add Error: " + error.getMessage());
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
