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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.civil.R;
import com.diamon.civil.databinding.ActivityMainBinding;
import com.diamon.civil.engine.CalculixExecutor;
import com.diamon.civil.engine.InpGenerator;
import com.diamon.civil.engine.TerminalCommandExecutor;
import com.diamon.civil.io.FileHelper;
import com.diamon.civil.util.AssetHelper;
import com.google.android.material.tabs.TabLayout;

import io.github.sceneview.node.ModelNode;
import io.github.sceneview.math.Position;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CalculixExecutor calculixExecutor;
    private TerminalCommandExecutor terminalExecutor;
    private AssetHelper assetHelper;
    private FileHelper fileHelper;
    private InpGenerator inpGenerator;

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

        setupUI();
        setupSceneView();
        checkAndLoadAssets();
    }

    private void setupSceneView() {
        binding.sceneView.setZNear(0.1f);
        binding.sceneView.setZFar(1000.0f);
        
        // Aquí podrías cargar un modelo inicial si existiera
        // ModelNode modelNode = new ModelNode(binding.sceneView.getEngine(), "models/sample.glb", true, 1.0f);
        // binding.sceneView.addChild(modelNode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void setupUI() {
        // Tab Navigation
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        binding.layoutBasicUI.setVisibility(View.VISIBLE);
                        binding.layoutConsole.setVisibility(View.GONE);
                        binding.sceneView.setVisibility(View.GONE);
                        break;
                    case 1:
                        binding.layoutBasicUI.setVisibility(View.GONE);
                        binding.layoutConsole.setVisibility(View.VISIBLE);
                        binding.sceneView.setVisibility(View.GONE);
                        break;
                    case 2:
                        binding.layoutBasicUI.setVisibility(View.GONE);
                        binding.layoutConsole.setVisibility(View.GONE);
                        binding.sceneView.setVisibility(View.VISIBLE);
                        break;
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
        binding.btnSend.setOnClickListener(v -> sendTerminalCommand());
        binding.etCommand.setOnEditorActionListener((v, actionId, event) -> {
            sendTerminalCommand();
            return true;
        });
    }

    private void setMaterialParams(String modulus, String density, boolean editable) {
        binding.etModulus.setText(modulus);
        binding.etDensity.setText(density);
        binding.layoutModulus.setEnabled(editable);
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
                
                // Convert FRD to GLB for 3D visualization
                File frdFile = new File(getFilesDir(), "structural_simulation.frd");
                File glbFile = new File(getFilesDir(), "structural_simulation.glb");
                boolean converted = false;
                if (frdFile.exists()) {
                    converted = calculixExecutor.convertFrdToGlb(frdFile.getAbsolutePath(), glbFile.getAbsolutePath());
                }

                final boolean finalConverted = converted;
                runOnUiThread(() -> {
                    binding.tvBasicResult.setText("ANALYSIS COMPLETE\n=================\n" + result);
                    binding.layoutBasicUI.post(() -> binding.layoutBasicUI.fullScroll(View.FOCUS_DOWN));
                    
                    if (finalConverted) {
                        Toast.makeText(this, "3D Model Generated", Toast.LENGTH_SHORT).show();
                        // Optional: automatically switch to VIEWER tab
                        // binding.tabLayout.getTabAt(2).select();
                        cargarModeloExterno(glbFile);
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
                    file,
                    true,
                    1.0f
            );
            binding.sceneView.addChild(modelNode);
            modelNode.centerModel(new Position(0, 0, 0));
        }
    }

    private void sendTerminalCommand() {
        String input = binding.etCommand.getText().toString().trim();
        if (input.isEmpty()) return;
        
        binding.etCommand.setText("");
        if (input.equalsIgnoreCase("clear")) {
            binding.tvLog.setText("--- Structural FEA Advanced Terminal ---\n");
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
                        // Default to trying as a generic binary if it's not a known script
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
        } else if (id == R.id.action_docs) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.calculix.de/html/ccx.html")));
            return true;
        } else if (id == R.id.action_reset) {
            showResetDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset System")
                .setMessage("Are you sure you want to force reinstall the native binaries?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    getSharedPreferences("AssetHelperPrefs", MODE_PRIVATE).edit().clear().apply();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About Structural FEA")
                .setMessage("Structural Analysis FEA Advanced\nPowered by CalculiX 2.23\n\nDeveloped by Daniel Diamon\nVenezuela")
                .setPositiveButton("Close", null)
                .setIcon(android.R.drawable.ic_dialog_info)
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
            if (files != null) {
                success = fileHelper.zipFiles(files, uri);
            }
            final boolean finalSuccess = success;
            runOnUiThread(() -> Toast.makeText(this, finalSuccess ? "All files exported to ZIP" : "Export Failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void handleExport(Uri uri) {
        if (uri == null) return;
        executor.execute(() -> {
            String content = binding.tvLog.getText().toString() + 
                            "\n\n--- FEA ANALYSIS RESULTS ---\n" + 
                            binding.tvBasicResult.getText().toString();
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
            binding.layoutMainUI.setVisibility(View.VISIBLE);
            executor.execute(() -> assetHelper.ensureRuntimeReady());
        } else {
            binding.layoutLoading.setVisibility(View.VISIBLE);
            binding.layoutMainUI.setVisibility(View.GONE);
            binding.tvLoadingText.setText("Deploying High-Performance FEM Engine...");
            executor.execute(() -> {
                boolean success = assetHelper.ensureRuntimeReady();
                runOnUiThread(() -> {
                    binding.layoutLoading.setVisibility(View.GONE);
                    binding.layoutMainUI.setVisibility(View.VISIBLE);
                    if (!success) Toast.makeText(this, "Engine Initialization Failed", Toast.LENGTH_LONG).show();
                });
            });
        }
    }

    private void copyToClipboard(String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, label + " copied", Toast.LENGTH_SHORT).show();
    }
}
