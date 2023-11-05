package me.ensa.inscription.ui.student;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import me.ensa.inscription.R;
import me.ensa.inscription.adapters.StudentAdapter;
import me.ensa.inscription.classes.Filiere;
import me.ensa.inscription.classes.Role;
import me.ensa.inscription.classes.Student;
import me.ensa.inscription.databinding.FragmentStudentBinding;
import me.ensa.inscription.utlis.SwipeToDeleteCallback;

public class StudentFragment extends Fragment {
    private FragmentStudentBinding binding;
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<Filiere> list_filieres = new ArrayList<>();
    private List<Role> list_roles = new ArrayList<>();
    private LottieAnimationView loader;
    private TextView empty, error;
    private Button try_again;
    private LottieAnimationView error_icon;

    private final String URL = "http://192.168.0.131:8080/api/v1";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        StudentViewModel studentViewModel =
                new ViewModelProvider(this).get(StudentViewModel.class);

        binding = FragmentStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        loader = binding.loadData;
        empty = binding.empty;
        error_icon = binding.errorIcon;
        error =  binding.error;
        try_again = binding.tryAgain;

        recyclerView = binding.recycleViewStudents;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new StudentAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        studentViewModel = new ViewModelProvider(this).get(StudentViewModel.class);

        // fetch data
        loader.setVisibility(View.VISIBLE);
        studentViewModel.fetchData(requireContext());

        // Observe data from the ViewModel and update the adapter
        studentViewModel.getStudentList().observe(getViewLifecycleOwner(), students -> {
            adapter.setStudents(students);
            if(students.isEmpty()){
                empty.setVisibility(View.VISIBLE);
            } else{
                empty.setVisibility(View.GONE);
            }
            loader.setVisibility(View.GONE);
        });

        studentViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            loader.setVisibility(View.GONE);
            error_icon.setVisibility(View.VISIBLE);
            error.setVisibility(View.VISIBLE);
            try_again.setVisibility(View.VISIBLE);
        });

        // in case of a network error
        StudentViewModel finalStudentViewModel1 = studentViewModel;
        try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loader.setVisibility(View.VISIBLE);
                error_icon.setVisibility(View.GONE);
                error.setVisibility(View.GONE);
                try_again.setVisibility(View.GONE);
                finalStudentViewModel1.fetchData(requireContext());
            }
        });

        // update student
        StudentViewModel finalStudentViewModel = studentViewModel;
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && e.getAction() == MotionEvent.ACTION_UP) {
                    int position = rv.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        Student student = finalStudentViewModel.getStudentList().getValue().get(position);
                        showUpdateDialog(student);
                    }
                }
                return false;
            }
        });

        // enable swipe to delete
        enableSwipeToDeleteAndUndo();

        return root;
    }

    public void showAddStudentDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_student, null);
        dialogBuilder.setView(dialogView);

        TextInputEditText edit_username = dialogView.findViewById(R.id.add_username);
        TextInputEditText edit_password = dialogView.findViewById(R.id.add_password);
        TextInputEditText edit_name = dialogView.findViewById(R.id.add_name);
        TextInputEditText edit_email = dialogView.findViewById(R.id.add_email);
        TextInputEditText edit_phone = dialogView.findViewById(R.id.add_phone);
        AutoCompleteTextView edit_filiere = dialogView.findViewById(R.id.add_filiere);
        MultiAutoCompleteTextView edit_roles = dialogView.findViewById(R.id.add_roles);

        // fetch filieres form db
        String url = URL + "/filieres";
        List<Filiere> filieres = new ArrayList<>();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest( Request.Method.GET, url, null,
            response -> {
                try {
                    Log.d("response", response.toString());
                    for (int i = 0; i < response.length(); i++) {
                        Filiere filiere = new Filiere();
                        JSONObject filiereJson = response.getJSONObject(i);
                        filiere.setId(filiereJson.optInt("id"));
                        filiere.setCode(filiereJson.optString("code"));
                        filiere.setLibelle(filiereJson.optString("libelle"));
                        filieres.add(filiere);
                    }

                    List<String> codesFiliers = new ArrayList<>();
                    filieres.stream().forEach(f -> {
                        codesFiliers.add(f.getCode());
                    });

                    Log.d("codesFiliers:", codesFiliers.toString());

                    ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, codesFiliers);
                    edit_filiere.setAdapter(filiereAdapter);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("FiliereFetchError", error.toString());
            }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);

        // fetch roles form db
        String url_roles = URL + "/roles";
        List<Role> roles = new ArrayList<>();
        JsonArrayRequest jsonArrayRequestRoles = new JsonArrayRequest( Request.Method.GET, url_roles, null,
            response -> {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        Role role = new Role();
                        JSONObject roleJson = response.getJSONObject(i);
                        role.setId(roleJson.optInt("id"));
                        role.setName(roleJson.optString("name"));
                        roles.add(role);
                    }

                    List<String> rolesName = new ArrayList<>();
                    roles.stream().forEach(role -> {
                        rolesName.add(role.getName());
                    });

                    ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, rolesName);
                    edit_roles.setAdapter(roleAdapter);
                    edit_roles.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("RoleFetchError", error.toString());
            }
        );
        RequestQueue requestQueueRole = Volley.newRequestQueue(requireContext());
        requestQueueRole.add(jsonArrayRequestRoles);

        // show the dialog
        dialogBuilder.setTitle("Add Student");
        dialogBuilder.setPositiveButton("Add", (dialog, which) -> {
            String username = edit_username.getText().toString().trim();
            String password = edit_password.getText().toString().trim();
            String name = edit_name.getText().toString().trim();
            String email = edit_email.getText().toString().trim();
            String phone = edit_phone.getText().toString().trim();
            String filiere = edit_filiere.getText().toString().trim();
            String role = edit_roles.getText().toString().trim();

            Filiere chosenFiliere = filieres.stream().filter( f -> f.getCode().equals(filiere)).findFirst().get();
            List<Role> chosenRole = getChosenRoles(roles, role);

            Student newStudent = new Student(username, password, name, phone, email, chosenFiliere);
            newStudent.setRoles(chosenRole);
            saveStudentsToDB(newStudent);

            //Notify the adapter about changes in the data
//            adapter.addStudent(newStudent);
            adapter.notifyDataSetChanged();
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private List<Role> getChosenRoles(List<Role> roles, String text) {
        List<String> choosedRoles = Arrays.asList(text.split(","));

        choosedRoles = choosedRoles.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Set<Role> uniqueRoles = new HashSet<>();

        for (String roleName : choosedRoles) {
            roles.stream()
                    .filter(role -> role.getName().toLowerCase().equals(roleName))
                    .findFirst()
                    .ifPresent(uniqueRoles::add);
        }

        return new ArrayList<>(uniqueRoles);
    }

    private void saveStudentsToDB(Student student) {
        String url = URL + "/student";
        JSONObject  studentJson = createStudentObject(student);

        loader.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        JsonObjectRequest  jsonArrayRequest = new JsonObjectRequest (Request.Method.POST, url, studentJson,
            response -> {
                int studentId = 0;
                try {
                    studentId = response.getInt("id");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                student.setId(studentId);
                adapter.addStudent(student);

                recyclerView.setVisibility(View.VISIBLE);
                loader.setVisibility(View.GONE);

                Log.d("res", response.toString());
                Toast.makeText(requireContext(), "Created!", Toast.LENGTH_SHORT).show();
            },
            error -> {
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);
    }

    private JSONObject createStudentObject(Student student) {
        JSONObject studentJson = new JSONObject();
        try {
            studentJson.put("id", student.getId());
            studentJson.put("username", student.getUsername());
            studentJson.put("password", student.getPassword());
            studentJson.put("name", student.getName());
            studentJson.put("phone", student.getPhone());
            studentJson.put("email", student.getEmail());

            // Adding Filiere details if Student class includes a Filiere object
            Filiere filiere = student.getFiliere();
            if (filiere != null) {
                JSONObject filiereJson = new JSONObject();
                filiereJson.put("id", filiere.getId());
                filiereJson.put("code", filiere.getCode());
                filiereJson.put("libelle", filiere.getLibelle());

                studentJson.put("filiere", filiereJson);
            }

            List<Role> roles = student.getRoles();
            if (roles != null) {
                JSONArray rolesArray = new JSONArray();
                for (int j = 0; j < roles.size(); j++) {
                    JSONObject roleJson = new JSONObject();
                    roleJson.put("id", roles.get(j).getId());
                    roleJson.put("name", roles.get(j).getName());
                    rolesArray.put(roleJson);
                }
                studentJson.put("roles", rolesArray);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return studentJson;
    }

    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Student item = adapter.getData().get(position);
                Log.d("item", item.getId() + ", " + item.getName());
                adapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(recyclerView, "Student was removed.", Snackbar.LENGTH_LONG);

                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        adapter.restoreItem(item, position);
                        recyclerView.scrollToPosition(position);
                    }
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE) {
                            deleteEtudiant(item.getId(), position, item);
                        }
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();

            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    // delete a student
    private void deleteEtudiant(int id, int position, Student item) {
        String deleteUrl = URL + "/student/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());

        // set loader visible
        loader.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        StringRequest request = new StringRequest(Request.Method.DELETE, deleteUrl,
            response -> {
                loader.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), response.toString(), Toast.LENGTH_SHORT).show();
            }, error -> {
                Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show();
                Log.e("error", error.toString());
                adapter.restoreItem(item, position);
            }
        ){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("id", String.valueOf(id));
                return params;
            }
        };
        requestQueue.add(request);
    }

    // update student info
    // Create a method to show the update dialog
    private void showUpdateDialog(Student student) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_student, null);
        dialogBuilder.setView(dialogView);

        // Initialize UI elements in the dialog layout
        TextInputEditText edit_username = dialogView.findViewById(R.id.edit_username);
        TextInputEditText edit_password = dialogView.findViewById(R.id.edit_password);
        TextInputEditText edit_name = dialogView.findViewById(R.id.edit_name);
        TextInputEditText edit_email = dialogView.findViewById(R.id.edit_email);
        TextInputEditText edit_phone = dialogView.findViewById(R.id.edit_phone);
        AutoCompleteTextView edit_filiere = dialogView.findViewById(R.id.edit_filiere);
        MultiAutoCompleteTextView edit_roles = dialogView.findViewById(R.id.edit_roles);

        // Populate the dialog with the student's current information
        edit_username.setText(student.getUsername());
        edit_password.setText(student.getPassword());
        edit_name.setText(student.getName());
        edit_phone.setText(student.getPhone());
        edit_email.setText(student.getEmail());

        list_roles.clear();
        list_filieres.clear();

        // fetch filiere and roles
        fetchFiliereOptionsFromDatabase(dialogView, student.getFiliere().getCode());
        fetchRoleOptionsFromDatabase(dialogView, student.getRoles());

        dialogBuilder.setTitle("Update Student");
        dialogBuilder.setPositiveButton("Update", (dialog, which) -> {
            // Retrieve the updated information from the dialog
            String username = edit_username.getText().toString().trim();
            String password = edit_password.getText().toString().trim();
            String name = edit_name.getText().toString().trim();
            String email = edit_email.getText().toString().trim();
            String phone = edit_phone.getText().toString().trim();
            String filiere = edit_filiere.getText().toString().trim();
            String role = edit_roles.getText().toString().trim();



            Filiere chosenFiliere = list_filieres.stream().filter( f -> f.getCode().equals(filiere)).findFirst().get();
            List<Role> chosenRole = getChosenRoles(list_roles, role);

            // Update the student's information in the dataset
            student.setUsername(username);
            student.setPassword(password);
            student.setName(name);
            student.setEmail(email);
            student.setPhone(phone);
            student.setFiliere(chosenFiliere);

            Log.d("chosenRole", chosenRole.toString());

            student.setRoles(chosenRole);

            updateStudentsToDB(student);

            // Notify the adapter of the data change
            adapter.notifyDataSetChanged();
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void updateStudentsToDB(Student student) {
        String url = URL + "/student/" + student.getId();
        Log.d("url", url);
        JSONObject  studentJson = createStudentObject(student);

        recyclerView.setVisibility(View.GONE);
        loader.setVisibility(View.VISIBLE);

        JsonObjectRequest  jsonArrayRequest = new JsonObjectRequest(Request.Method.PUT, url, studentJson,
            response -> {
                try {
                    loader.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    String message = response.getString("message");
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("res", response.toString());
            },
            error -> {
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);
    }

    private void fetchRoleOptionsFromDatabase(View dialogView, List<Role> currentRole) {
        String url_roles = URL + "/roles";
        List<Role> roles = new ArrayList<>();
        JsonArrayRequest jsonArrayRequestRoles = new JsonArrayRequest( Request.Method.GET, url_roles, null,
            response -> {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        Role role = new Role();
                        JSONObject roleJson = response.getJSONObject(i);
                        role.setId(roleJson.optInt("id"));
                        role.setName(roleJson.optString("name"));
                        roles.add(role);
                    }
                    list_roles.addAll(roles);
                    populateRoleDropdown(roles, dialogView, currentRole);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("RoleFetchError", error.toString());
            }
        );
        RequestQueue requestQueueRole = Volley.newRequestQueue(requireContext());
        requestQueueRole.add(jsonArrayRequestRoles);
    }

    private void fetchFiliereOptionsFromDatabase(View dialogView, String currentFilierCode) {
        String url = URL + "/filieres";
        List<Filiere> filieres = new ArrayList<>();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest( Request.Method.GET, url, null,
            response -> {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        Filiere filiere = new Filiere();
                        JSONObject filiereJson = response.getJSONObject(i);
                        filiere.setId(filiereJson.optInt("id"));
                        filiere.setCode(filiereJson.optString("code"));
                        filiere.setLibelle(filiereJson.optString("libelle"));
                        filieres.add(filiere);
                    }
                    list_filieres.addAll(filieres);
                    Filiere filiere = filieres.stream().filter(f -> f.getCode().equals(currentFilierCode)).findFirst().get();
                    populateFiliereDropdown(filieres, dialogView, filiere);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Log.e("FiliereFetchError", error.toString());
            }
        );
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonArrayRequest);
    }

    private void populateFiliereDropdown(List<Filiere> filiereList, View dialogView, Filiere currentFiliere) {
        AutoCompleteTextView editFiliere = dialogView.findViewById(R.id.edit_filiere);
        List<String> filiereNames = new ArrayList<>();

        for (Filiere filiere : filiereList) {
            filiereNames.add(filiere.getCode()); // Change to filiere.getCode() or the specific attribute you want to display
        }

        ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, filiereNames);
        editFiliere.setAdapter(filiereAdapter);

        if (currentFiliere != null) {
            int position = filiereNames.indexOf(currentFiliere.getCode()); // Change to match the attribute used in the adapter
            if (position != -1) {
                editFiliere.setText(filiereAdapter.getItem(position), false);
            }
        }
    }

    private void populateRoleDropdown(List<Role> roleList, View dialogView, List<Role> currentRoles) {
        MultiAutoCompleteTextView editRole = dialogView.findViewById(R.id.edit_roles);
        List<String> roleNames = new ArrayList<>();

        for (Role role : roleList) {
            roleNames.add(role.getName());
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, roleNames);
        editRole.setAdapter(roleAdapter);
        editRole.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        if (currentRoles != null && !currentRoles.isEmpty()) {
            StringBuilder selectedRoles = new StringBuilder();
            for (Role role : currentRoles) {
                int position = roleNames.indexOf(role.getName());
                if (position != -1) {
                    selectedRoles.append(roleAdapter.getItem(position)).append(",");
                }
            }
            editRole.setText(selectedRoles.toString(), false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}