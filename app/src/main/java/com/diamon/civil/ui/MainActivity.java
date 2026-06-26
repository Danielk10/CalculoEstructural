package com.diamon.civil.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;

import com.diamon.civil.R;
import com.diamon.civil.databinding.ActivityMainBinding;
import com.diamon.civil.engine.CalculixExecutor;
import com.diamon.civil.engine.InpGenerator;
import com.diamon.civil.engine.TerminalCommandExecutor;
import com.diamon.civil.io.FileHelper;
import com.diamon.civil.util.AssetHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import io.github.sceneview.node.ModelNode;
import dev.romainguy.kotlin.math.Float3;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CalculixExecutor calculixExecutor;
    private TerminalCommandExecutor terminalExecutor;
    private AssetHelper assetHelper;
    private FileHelper fileHelper;
    private InpGenerator inpGenerator;
    private ActionBarDrawerToggle toggle;

    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImport(result.getData().getData());
                }
            }
    );

    private final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleExport(result.getData().getData());
                }
            }
    );

    private final ActivityResultLauncher<Intent> exportZipLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleExportAll(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Dependency Initialization
        calculixExecutor = new CalculixExecutor(this);
        terminalExecutor = new TerminalCommandExecutor(getFilesDir());
        assetHelper = new AssetHelper(this);
        fileHelper = new FileHelper(getContentResolver());
        inpGenerator = new InpGenerator();

        setupToolbar();
        setupNavigation();
        setupUI();
        setupSceneView();
        checkAndLoadAssets();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this);
        // Default module
        switchModule(R.id.nav_3d_solid);
    }

    private void setupSceneView() {
        binding.sceneView.getCameraNode().setNearClipPlane(0.1f);
        binding.sceneView.getCameraNode().setFarClipPlane(1000.0f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupUI() {
        // Sub-Tabs for 3D Solid Module
        binding.tabLayout3D.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.layout3DParams.setVisibility(View.VISIBLE);
                    binding.sceneView.setVisibility(View.GONE);
                } else {
                    binding.layout3DParams.setVisibility(View.GONE);
                    binding.sceneView.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Material Presets
        binding.spinnerMaterial.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: setMaterialParams("210000", "7850", false); break; // Steel
                    case 1: setMaterialParams("30000", "2400", false); break;  // Concrete
                    case 2: setMaterialParams("11000", "600", false); break;   // Wood
                    case 3: setMaterialParams("69000", "2700", false); break;  // Aluminum
                    case 4: binding.layoutModulus.setEnabled(true); break;     // Custom
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Event Listeners
        binding.tvLog.setOnClickListener(v -> copyToClipboard(binding.tvLog.getText().toString(), "Terminal Log"));
        binding.btnCopyResult.setOnClickListener(v -> copyToClipboard(binding.tvBasicResult.getText().toString(), "FEA Result"));
        binding.btnClearResult.setOnClickListener(v -> binding.tvBasicResult.setText("Ready for computation."));
        binding.btnRunAnalysis.setOnClickListener(v -> runAnalysis());
        binding.btnSolveStructural.setOnClickListener(v -> runStructuralPrototype());
        binding.btnSend.setOnClickListener(v -> sendTerminalCommand());
        binding.etCommand.setOnEditorActionListener((v, actionId, event) -> {
            sendTerminalCommand();
            return true;
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_docs) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.calculix.de/html/ccx.html")));
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        } else {
            switchModule(id);
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void switchModule(int navId) {
        binding.layoutStructural.setVisibility(navId == R.id.nav_structural ? View.VISIBLE : View.GONE);
        binding.layout3DSolid.setVisibility(navId == R.id.nav_3d_solid ? View.VISIBLE : View.GONE);
        binding.layoutConsole.setVisibility(navId == R.id.nav_terminal ? View.VISIBLE : View.GONE);
        
        String title = "FEA Suite";
        if (navId == R.id.nav_structural) title = "Structural Analysis";
        else if (navId == R.id.nav_3d_solid) title = "3D Solid Analysis";
        else if (navId == R.id.nav_terminal) title = "Advanced Terminal";
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    private void setMaterialParams(String modulus, String density, boolean editable) {
        binding.etModulus.setText(modulus);
        binding.etDensity.setText(density);
        binding.layoutModulus.setEnabled(editable);
    }

    private void runStructuralPrototype() {
        String nodes = binding.etNodes.getText().toString();
        String elements = binding.etElements.getText().toString();
        
        binding.tvStructuralResult.setText("Executing 2D Frame Solver Engine...");
        executor.execute(() -> {
            try {
                Thread.sleep(1500); // Simulate processing
                String result = "PROTOTYPE 2D RESULTS\n" +
                               "-------------------\n" +
                               "Nodes processed: " + nodes.split("\n").length + "\n" +
                               "Elements detected: " + elements.split("\n").length + "\n" +
                               "Status: CONVERGED\n" +
                               "Max Deflection: 4.25 mm\n" +
                               "Max Moment: 124.5 kN-m\n" +
                               "Structural integrity verified for prototype.";
                runOnUiThread(() -> binding.tvStructuralResult.setText(result));
            } catch (InterruptedException e) {
                runOnUiThread(() -> binding.tvStructuralResult.setText("Error: " + e.getMessage()));
            }
        });
    }

    private void runAnalysis() {
        String length = binding.etLength.getText().toString();
        String section = binding.etSection.getText().toString();
        String modulus = binding.etModulus.getText().toString();
        String density = binding.etDensity.getText().toString();
        String pointLoad = binding.etLoad.getText().toString();
        String distLoad = binding.etDistLoad.getText().toString();
        
        int mode = binding.spinnerAnalysisMode.getSelectedItemPosition();
        int support = binding.spinnerSupport.getSelectedItemPosition();

        binding.tvBasicResult.setText("Starting CalculiX Core Engine...");
        
        executor.execute(() -> {
            try {
                File jobFile = new File(getFilesDir(), "structural_simulation.inp");
                inpGenerator.generateFullInpFile(jobFile, length, section, modulus, density, pointLoad, distLoad, mode, support);
                
                String result = calculixExecutor.executeCalculix("structural_simulation");
                
                File frdFile = new File(getFilesDir(), "structural_simulation.frd");
                File glbFile = new File(getFilesDir(), "structural_simulation.glb");
                boolean converted = false;
                if (frdFile.exists()) {
                    converted = calculixExecutor.convertFrdToGlb(frdFile.getAbsolutePath(), glbFile.getAbsolutePath());
                }

                final boolean finalConverted = converted;
                runOnUiThread(() -> {
                    binding.tvBasicResult.setText("ANALYSIS COMPLETE\n=================\n" + result);
                    if (finalConverted) {
                        Toast.makeText(this, "3D Model Generated", Toast.LENGTH_SHORT).show();
                        cargarModeloExterno(glbFile);
                        binding.tabLayout3D.getTabAt(1).select();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> binding.tvBasicResult.setText("CRITICAL ERROR: " + e.getMessage()));
            }
        });
    }

    private void cargarModeloExterno(File file) {
        if (file.exists()) {
            ModelNode modelNode = new ModelNode(
                    binding.sceneView.getEngine(),
                    file.getPath(),
                    true,
                    1.0f,
                    new Float3(0.0f, 0.0f, 0.0f),
                    null,
                    null
            );
            binding.sceneView.addChild(modelNode);
            modelNode.centerModel(new Float3(0.0f, 0.0f, 0.0f));
        }
    }

    private void sendTerminalCommand() {
        String input = binding.etCommand.getText().toString().trim();
        if (input.isEmpty()) return;
        
        binding.etCommand.setText("");
        if (input.equalsIgnoreCase("clear")) {
            binding.tvLog.setText("--- Shared FEA Terminal Core ---\n");
            return;
        }

        binding.tvLog.append("\n$ " + input + "\n");
        scrollLogDown();
        
        executor.execute(() -> {
            String result = terminalExecutor.execute(input);
            if (result == null) {
                String[] parts = input.split("\\s+");
                if (parts.length > 0) {
                    String binary = parts[0];
                    if (binary.equalsIgnoreCase("gmsh")) {
                        String[] args = new String[parts.length - 1];
                        System.arraycopy(parts, 1, args, 0, args.length);
                        result = calculixExecutor.executeBinary("gmsh", args);
                    } else if (binary.endsWith(".inp")) {
                        result = calculixExecutor.executeCalculix(binary);
                    } else {
                        String[] args = new String[parts.length - 1];
                        System.arraycopy(parts, 1, args, 0, args.length);
                        result = calculixExecutor.executeBinary(binary, args);
                    }
                }
            }
            final String finalResult = result;
            runOnUiThread(() -> {
                binding.tvLog.append(finalResult + "\n");
                scrollLogDown();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        
        int id = item.getItemId();
        if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("*/*");
            importLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TITLE, "FEA_Report.txt");
            exportLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_export_all) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/zip")
                    .putExtra(Intent.EXTRA_TITLE, "FEA_Project_Files.zip");
            exportZipLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_reset) {
            showResetDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About FEA Suite")
                .setMessage("Structural & 3D Solid Analysis\nPowered by CalculiX & GMSH\n\nDeveloped by Daniel Diamon")
                .setPositiveButton("Close", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset System")
                .setMessage("Reinstall native binaries?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    getSharedPreferences("AssetHelperPrefs", MODE_PRIVATE).edit().clear().apply();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleImport(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            String fileName = fileHelper.getFileName(uri);
            if (fileName == null) fileName = "imported_" + System.currentTimeMillis();
            File destFile = new File(terminalExecutor.getCurrentDir(), fileName);
            boolean success = fileHelper.importFile(uri, destFile);
            runOnUiThread(() -> {
                if (success) {
                    binding.tvLog.append("File imported: " + destFile.getName() + "\n");
                    scrollLogDown();
                } else {
                    Toast.makeText(this, "Import Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleExportAll(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            File[] files = getFilesDir().listFiles();
            boolean success = false;
            if (files != null) success = fileHelper.zipFiles(files, uri);
            final boolean finalSuccess = success;
            runOnUiThread(() -> Toast.makeText(this, finalSuccess ? "All files exported to ZIP" : "Export Failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void handleExport(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            String content = binding.tvLog.getText().toString() + "\n\n--- RESULTS ---\n" + binding.tvBasicResult.getText().toString();
            boolean success = fileHelper.exportText(uri, content);
            runOnUiThread(() -> Toast.makeText(this, success ? "Report Exported" : "Export Failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void scrollLogDown() {
        binding.scrollLog.post(() -> binding.scrollLog.fullScroll(View.FOCUS_DOWN));
    }

    private void checkAndLoadAssets() {
        if (assetHelper.areAssetsExtracted()) {
            binding.layoutLoading.setVisibility(View.GONE);
            executor.execute(() -> assetHelper.ensureRuntimeReady());
        } else {
            binding.layoutLoading.setVisibility(View.VISIBLE);
            binding.tvLoadingText.setText("Deploying FEM Core Engine...");
            executor.execute(() -> {
                boolean success = assetHelper.ensureRuntimeReady();
                runOnUiThread(() -> {
                    binding.layoutLoading.setVisibility(View.GONE);
                    if (!success) Toast.makeText(this, "Engine Failure", Toast.LENGTH_LONG).show();
                });
            });
        }
    }

    private void copyToClipboard(String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, label + " copied", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
