package me.ensa.inscription.ui.role;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import me.ensa.inscription.classes.Filiere;
import me.ensa.inscription.classes.Role;
import me.ensa.inscription.classes.Student;

public class RoleViewModel extends ViewModel {
    private MutableLiveData<List<Role>> roleList = new MutableLiveData<>();

    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchData(Context context) {
        String url = "http://192.168.0.131:8080/api/v1/roles";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Process the JSON response from the server
                    List<Role> roles = parseJsonResponse(response);
                    roleList.setValue(roles);
                    for(Role r : roles){
                        Log.d("roleName: ", r.getName());
                    }
                },
                error -> {
                    errorLiveData.setValue(error.toString());
                    Log.e("Error", error.toString());
                }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonArrayRequest);
    }

    // Method to get LiveData of student list
    public LiveData<List<Role>> getRoleList() {
        return roleList;
    }


    private List<Role> parseJsonResponse(JSONArray response) {
        List<Role> roles = new ArrayList<>();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject roleObj = response.getJSONObject(i);
                Role role = new Role();

                role.setId(roleObj.optInt("id"));
                role.setName(roleObj.optString("name"));

                roles.add(role);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return roles;
    }
}