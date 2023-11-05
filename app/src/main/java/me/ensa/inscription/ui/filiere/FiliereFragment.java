package me.ensa.inscription.ui.filiere;

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
import me.ensa.inscription.adapters.FiliereAdapter;
import me.ensa.inscription.classes.Filiere;
import me.ensa.inscription.databinding.FragmentFiliereBinding;
import me.ensa.inscription.ui.role.RoleViewModel;
import me.ensa.inscription.utlis.SwipeToDeleteCallback;

public class FiliereFragment extends Fragment {
    private FragmentFiliereBinding binding;
    private RecyclerView recyclerView;
    private FiliereAdapter adapter;
    private LottieAnimationView loader;
    private TextView empty, error;
    private Button try_again;
    private LottieAnimationView error_icon;
    private final String URL = "http://192.168.0.131:8080/api/v1";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        FiliereViewModel filiereViewModel = new ViewModelProvider(this).get(FiliereViewModel.class);

        binding = FragmentFiliereBinding.inflate(inflater, container, false);

        loader = binding.loadData;
        empty = binding.empty;
        error_icon = binding.errorIcon;
        error =  binding.error;
        try_again = binding.tryAgain;

        View root = binding.getRoot();
        recyclerView = binding.recycleViewFiliere;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new FiliereAdapter(requireContext());
        recyclerView.setAdapter(adapter);

        filiereViewModel = new ViewModelProvider(this).get(FiliereViewModel.class);

        // fetch data
        loader.setVisibility(View.VISIBLE);
        filiereViewModel.fetchData(requireContext());

        // Observe data from the ViewModel and update the adapter
        filiereViewModel.getFiliereList().observe(getViewLifecycleOwner(), filieres -> {
            adapter.setFilieres(filieres);
            if(filieres.isEmpty()){
                empty.setVisibility(View.VISIBLE);
            } else{
                empty.setVisibility(View.GONE);
            }
            loader.setVisibility(View.GONE);
        });

        filiereViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            loader.setVisibility(View.GONE);
            error_icon.setVisibility(View.VISIBLE);
            error.setVisibility(View.VISIBLE);
            try_again.setVisibility(View.VISIBLE);
        });

        FiliereViewModel finalFiliereViewModel1 = filiereViewModel;
        try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loader.setVisibility(View.VISIBLE);
                error_icon.setVisibility(View.GONE);
                error.setVisibility(View.GONE);
                try_again.setVisibility(View.GONE);
                finalFiliereViewModel1.fetchData(requireContext());
            }
        });

        // update Filiere
        FiliereViewModel finalFiliereViewModel = filiereViewModel;
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && e.getAction() == MotionEvent.ACTION_UP) {
                    int position = rv.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        Filiere Filiere = finalFiliereViewModel.getFiliereList().getValue().get(position);
                        showUpdateDialog(Filiere);
                    }
                }
                return false;
            }
        });

        // enable swipe to delete
        enableSwipeToDeleteAndUndo();

        return root;
    }

    public void showAddFilierDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_filiere, null);
        dialogBuilder.setView(dialogView);

        TextInputEditText add_code = dialogView.findViewById(R.id.add_code);
        TextInputEditText add_libelle = dialogView.findViewById(R.id.add_libelle);

        // show the dialog
        dialogBuilder.setTitle("Add Filiere");
        dialogBuilder.setPositiveButton("Add", (dialog, which) -> {
            String code = add_code.getText().toString().trim();
            String libelle = add_libelle.getText().toString().trim();

            Filiere newFiliere = new Filiere(code, libelle);
            try {
                saveFiliereToDB(newFiliere);
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

    private void saveFiliereToDB(Filiere filiere) throws JSONException {
        String url = URL + "/filieres";
        JSONObject filiereJSON = new JSONObject();
        filiereJSON.put("code", filiere.getCode());
        filiereJSON.put("libelle", filiere.getLibelle());

        loader.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest (Request.Method.POST, url, filiereJSON,
                response -> {

                    int filiereId = 0;
                    try {
                        filiereId = response.getInt("id");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    filiere.setId(filiereId);
                    adapter.addFiliere(filiere);

                    recyclerView.setVisibility(View.VISIBLE);
                    loader.setVisibility(View.GONE);

                    Log.d("res", response.toString());
                    Toast.makeText(requireContext(), "Filiere Created!", Toast.LENGTH_SHORT).show();
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
                final Filiere item = adapter.getData().get(position);
                Log.d("item", item.getId() + ", " + item.getCode());
                adapter.removeItem(position);

                Snackbar snackbar = Snackbar
                        .make(recyclerView, "Filiere was removed.", Snackbar.LENGTH_LONG);

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
                            deleteFiliere(item.getId(), position, item);
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

    // delete a Filiere
    private void deleteFiliere(int id, int position, Filiere item) {
        String deleteUrl = URL + "/filieres/" + id;

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

    // update Filiere
    private void showUpdateDialog(Filiere filiere) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_filiere, null);
        dialogBuilder.setView(dialogView);

        // Initialize UI elements in the dialog layout
        TextInputEditText edit_filiere_code = dialogView.findViewById(R.id.edit_filiere_code);
        TextInputEditText edit_filiere_libelle = dialogView.findViewById(R.id.edit_filiere_libelle);

        // Populate the dialog with the student's current information
        edit_filiere_code.setText(filiere.getCode());
        edit_filiere_libelle.setText(filiere.getLibelle());

        dialogBuilder.setTitle("Update Filiere");
        dialogBuilder.setPositiveButton("Update", (dialog, which) -> {
            // Retrieve the updated information from the dialog
            String code = edit_filiere_code.getText().toString().trim();
            String libelle = edit_filiere_libelle.getText().toString().trim();

            filiere.setCode(code);
            filiere.setLibelle(libelle);

            try {
                updateFiliereToDB(filiere);
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

    private void updateFiliereToDB(Filiere role) throws JSONException {
        String url = URL + "/filieres/" + role.getId();
        JSONObject roleJSON = new JSONObject();
        roleJSON.put("id", role.getId());
        roleJSON.put("code", role.getCode());
        roleJSON.put("libelle", role.getLibelle());

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