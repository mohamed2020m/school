package me.ensa.inscription.ui.student;

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

public class StudentViewModel extends ViewModel {
    private MutableLiveData<List<Student>> studentList = new MutableLiveData<>();

    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void fetchData(Context context) {
        String url = "http://192.168.0.131:8080/api/v1/student";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                Log.d("useranme: ", response.toString());
                // Process the JSON response from the server
                List<Student> students = parseJsonResponse(response);
                studentList.setValue(students);
                for(Student s : students){
                    Log.d("useranme: ", s.getUsername());
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
    public LiveData<List<Student>> getStudentList() {
        return studentList;
    }


    private List<Student> parseJsonResponse(JSONArray response) {
        List<Student> students = new ArrayList<>();

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject studentObj = response.getJSONObject(i);
                Student student = new Student();

                student.setId(studentObj.optInt("id"));
                student.setUsername(studentObj.optString("username"));
                student.setPassword(studentObj.optString("password"));
                student.setName(studentObj.optString("name"));
                student.setPhone(studentObj.optString("phone"));
                student.setEmail(studentObj.optString("email"));

                // Parse roles array
                JSONArray rolesArray = studentObj.optJSONArray("roles");
                List<Role> roles = new ArrayList<>();
                if (rolesArray != null) {
                    for (int j = 0; j < rolesArray.length(); j++) {
                        JSONObject roleObj = rolesArray.optJSONObject(j);
                        if (roleObj != null) {
                            Role role = new Role();
                            role.setId(roleObj.optInt("id"));
                            role.setName(roleObj.optString("name"));
                            roles.add(role);
                        }
                    }
                }
                student.setRoles(roles);

                // Parse filiere object
                JSONObject filiereObj = studentObj.optJSONObject("filiere");
                if (filiereObj != null) {
                    Filiere filiere = new Filiere();
                    filiere.setId(filiereObj.optInt("id"));
                    filiere.setCode(filiereObj.optString("code"));
                    filiere.setLibelle(filiereObj.optString("libelle"));
                    student.setFiliere(filiere);
                }

                students.add(student);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return students;
    }
}
