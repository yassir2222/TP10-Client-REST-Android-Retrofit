package ma.projet;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ma.projet.restclient.adapter.*;
import ma.projet.restclient.entities.*;
import ma.projet.restclient.repository.*;
import ma.projet.restclient.api.*;
import ma.projet.restclient.config.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements CompteAdapter.OnDeleteClickListener, CompteAdapter.OnUpdateClickListener {
    private RecyclerView recyclerView;
    private CompteAdapter adapter;
    private RadioGroup formatGroup;
    private FloatingActionButton addbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupFormatSelection();
        setupAddButton();

        loadData("JSON");
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        formatGroup = findViewById(R.id.formatGroup);
        addbtn = findViewById(R.id.fabAdd);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompteAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupFormatSelection() {
        formatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String format = checkedId == R.id.radioJson ? "JSON" : "XML";
            loadData(format);
        });
    }

    private void setupAddButton() {
        addbtn.setOnClickListener(v -> showAddCompteDialog());
    }

    private void showAddCompteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);

        builder.setView(dialogView)
                .setTitle("Ajouter un compte")
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String solde = etSolde.getText().toString();
                    String type = typeGroup.getCheckedRadioButtonId() == R.id.radioCourant
                            ? "COURANT" : "EPARGNE";

                    String formattedDate = getCurrentDateFormatted();
                    Compte compte = new Compte(null, Double.parseDouble(solde), type, formattedDate);
                    addCompte(compte);
                })
                .setNegativeButton("Annuler", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getCurrentDateFormatted() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(calendar.getTime());
    }

    private void addCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository("JSON");
        compteRepository.addCompte(compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte ajouté");
                    loadData("JSON");
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur lors de l'ajout");
            }
        });
    }

 private void loadData(String format) {
    if ("XML".equalsIgnoreCase(format)) {
        CompteService service = RetrofitClient.getClient("XML").create(CompteService.class);
        service.getAllCompteXml().enqueue(new Callback<CompteList>() {
            @Override
            public void onResponse(Call<CompteList> call, Response<CompteList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Compte> list = response.body().getComptes();
                    runOnUiThread(() -> {
                        adapter.updateData(list);
                        if (list == null || list.isEmpty()) showToast("Aucun compte trouvé");
                    });
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "empty";
                        Log.e(TAG, "API error code=" + response.code() + " body=" + err);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading errorBody", e);
                    }
                    showToast("Erreur serveur: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CompteList> call, Throwable t) {
                Log.e(TAG, "Network failure", t);
                showToast("Erreur réseau: " + (t.getMessage() != null ? t.getMessage() : "connexion échouée"));
            }
        });
    } else {
        // existing JSON path using your repository
        CompteRepository compteRepository = new CompteRepository(format);
        compteRepository.getAllCompte(new Callback<List<Compte>>() {
            @Override
            public void onResponse(Call<List<Compte>> call, Response<List<Compte>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Compte> comptes = response.body();
                    runOnUiThread(() -> adapter.updateData(comptes));
                }
            }

            @Override
            public void onFailure(Call<List<Compte>> call, Throwable t) {
                showToast("Erreur: " + t.getMessage());
            }
        });
    }
}
    @Override
    public void onUpdateClick(Compte compte) {
        showUpdateCompteDialog(compte);
    }

    private void showUpdateCompteDialog(Compte compte) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);
        etSolde.setText(String.valueOf(compte.getSolde()));
        if (compte.getType().equalsIgnoreCase("COURANT")) {
            typeGroup.check(R.id.radioCourant);
        } else if (compte.getType().equalsIgnoreCase("EPARGNE")) {
            typeGroup.check(R.id.radioEpargne);
        }

        builder.setView(dialogView)
                .setTitle("Modifier un compte")
                .setPositiveButton("Modifier", (dialog, which) -> {
                    String solde = etSolde.getText().toString();
                    String type = typeGroup.getCheckedRadioButtonId() == R.id.radioCourant
                            ? "COURANT" : "EPARGNE";
                    compte.setSolde(Double.parseDouble(solde));
                    compte.setType(type);
                    updateCompte(compte);
                })
                .setNegativeButton("Annuler", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository("JSON");
        compteRepository.updateCompte(compte.getId(), compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte modifié");
                    loadData("JSON");
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur lors de la modification");
            }
        });
    }

    @Override
    public void onDeleteClick(Compte compte) {
        showDeleteConfirmationDialog(compte);
    }

    private void showDeleteConfirmationDialog(Compte compte) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Voulez-vous vraiment supprimer ce compte ?")
                .setPositiveButton("Oui", (dialog, which) -> deleteCompte(compte))
                .setNegativeButton("Non", null)
                .show();
    }

    private void deleteCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository("JSON");
        compteRepository.deleteCompte(compte.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("Compte supprimé");
                    loadData("JSON");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("Erreur lors de la suppression");
            }
        });
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}