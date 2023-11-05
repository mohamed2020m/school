package me.ensa.inscription.ui.role;

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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import me.ensa.inscription.R;
import me.ensa.inscription.adapters.RoleAdapter;
import me.ensa.inscription.classes.Role;
import me.ensa.inscription.databinding.FragmentRoleBinding;
import me.ensa.inscription.utlis.SwipeToDeleteCallback;

public class RoleFragment extends Fragment {
    private FragmentRoleBinding binding;
    private RecyclerView recyclerView;
    private RoleAdapter adapter;
    private LottieAnimationView loader;
    private TextView empty, error;
    private Button try_again;
    private LottieAnimationView error_icon;
    private final String URL = "http://192.168.0.131:8080/api/v1";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        RoleViewModel roleViewModel = new ViewModelProvider(this).get(RoleViewModel.class);

        binding = FragmentRoleBinding.inflate(inflater, container, false);

        loader = binding.loadData;
        empty = binding.empty;
        error_icon = binding.errorIcon;
        error =  binding.error;
        try_again = binding.tryAgain;

        View root = binding.getRoot();
        recyclerView = binding.recycleViewRole;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new RoleAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        roleViewModel = new ViewModelProvider(this).get(RoleViewModel.class);

        // fetch data
        loader.setVisibility(View.VISIBLE);
        roleViewModel.fetchData(requireContext());

        // Observe data from the ViewModel and update the adapter
        roleViewModel.getRoleList().observe(getViewLifecycleOwner(), roles -> {
            adapter.setRoles(roles);
            if(roles.isEmpty()){
                empty.setVisibility(View.VISIBLE);
            } else{
                empty.setVisibility(View.GONE);
            }
            loader.setVisibility(View.GONE);
        });

        roleViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            loader.setVisibility(View.GONE);
            error_icon.setVisibility(View.VISIBLE);
            error.setVisibility(View.VISIBLE);
            try_again.setVisibility(View.VISIBLE);
        });

        RoleViewModel finalRoleViewModel1 = roleViewModel;
        try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loader.setVisibility(View.VISIBLE);
                error_icon.setVisibility(View.GONE);
                error.setVisibility(View.GONE);
                try_again.setVisibility(View.GONE);
                finalRoleViewModel1.fetchData(requireContext());
            }
        });

        // update Role
        RoleViewModel finalRoleViewModel = roleViewModel;
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && e.getAction() == MotionEvent.ACTION_UP) {
                    int position = rv.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        Role Role = finalRoleViewModel.getRoleList().getValue().get(position);
                        showUpdateDialog(Role);
                    }
                }
                return false;
            }
        });

        // enable swipe to delete
        enableSwipeToDeleteAndUndo();

        return root;
    }

    public void showAddRoleDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_role, null);
        dialogBuilder.setView(dialogView);

        TextInputEditText add_name = dialogView.findViewById(R.id.add_name);

        // show the dialog
        dialogBuilder.setTitle("Add Role");
        dialogBuilder.setPositiveButton("Add", (dialog, which) -> {
            String name = add_name.getText().toString().trim();

            Role newRole = new Role(name);
            try {
                saveRoleToDB(newRole);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            //Notify the adapter about changes in the data
            adapter.notifyDataSetChanged();
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void saveRoleToDB(Role role) throws JSONException {
        String url = URL + "/roles";
        JSONObject roleJSON = new JSONObject();
//        roleJSON.put("id", role.getId());
        roleJSON.put("name", role.getName());

        loader.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.POST, url, roleJSON,
            response -> {

                int roleId = 0;
                try {
                    roleId = response.getInt("id");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                role.setId(roleId);
                adapter.addRole(role);

                recyclerView.setVisibility(View.VISIBLE);
                loader.setVisibility(View.GONE);

                Log.d("res", response.toString());
                Toast.makeText(requireContext(), "Role Created!", Toast.LENGTH_SHORT).show();
            },
            error -> {
                Log.e("Error", error.toString());
            }
        );

        // Instantiate Volley RequestQueue and add the request
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(jsonObjectRequest);
    }
    
    private void enableSwipeToDeleteAndUndo() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(requireContext()) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                final Role item = adapter.getData().get(position);
                Log.d("item", item.getId() + ", " + item.getName());
                adapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(recyclerView, "Role was removed.", Snackbar.LENGTH_LONG);

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
                            deleteRole(item.getId(), position, item);
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

    // delete a Role
    private void deleteRole(int id, int position, Role item) {
        String deleteUrl = URL + "/roles/" + id;

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

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(request);
    }

    // update Role
    private void showUpdateDialog(Role role) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_role, null);
        dialogBuilder.setView(dialogView);

        // Initialize UI elements in the dialog layout
        TextInputEditText edit_role_name = dialogView.findViewById(R.id.edit_role_name);

        // Populate the dialog with the student's current information
        edit_role_name.setText(role.getName());

        dialogBuilder.setTitle("Update Role");
        dialogBuilder.setPositiveButton("Update", (dialog, which) -> {
            // Retrieve the updated information from the dialog
            String name = edit_role_name.getText().toString().trim();

            // Update the student's information in the dataset
            role.setName(name);

            try {
                updateRoleToDB(role);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            // Notify the adapter of the data change
            adapter.notifyDataSetChanged();
        });

        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void updateRoleToDB(Role role) throws JSONException {
        String url = URL + "/roles/" + role.getId();
        JSONObject roleJSON = new JSONObject();
        roleJSON.put("id", role.getId());
        roleJSON.put("name", role.getName());

        recyclerView.setVisibility(View.GONE);
        loader.setVisibility(View.VISIBLE);

        JsonObjectRequest  jsonArrayRequest = new JsonObjectRequest(Request.Method.PUT, url, roleJSON,
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}