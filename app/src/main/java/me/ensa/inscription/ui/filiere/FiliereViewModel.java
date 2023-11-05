package me.ensa.inscription.ui.filiere;

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

public class FiliereViewModel extends ViewModel {
    private MutableLiveData<List<Filiere>> filiereList = new MutableLiveData<>();

    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchData(Context context) {
        String url = "http://192.168.0.131:8080/api/v1/filieres";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Process the JSON response from the server
                    List<Filiere> filieres = parseJsonResponse(response);
                    filiereList.setValue(filieres);
                    for(Filiere r : filieres){
                        Log.d("filiereCode: ", r.getCode());
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
    public LiveData<List<Filiere>> getFiliereList() {
        return filiereList;
    }


    private List<Filiere> parseJsonResponse(JSONArray response) {
        List<Filiere> filieres = new ArrayList<>();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject filiereObj = response.getJSONObject(i);
                Filiere filiere = new Filiere();

                filiere.setId(filiereObj.optInt("id"));
                filiere.setCode(filiereObj.optString("code"));
                filiere.setLibelle(filiereObj.optString("libelle"));

                filieres.add(filiere);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return filieres;
    }
}