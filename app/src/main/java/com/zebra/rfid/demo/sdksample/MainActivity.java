package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.HashSet;


/**
 * Main Activity for the RFID Sample application.
 */
public class MainActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    /**
     * List of tag IDs detected by the RFID reader.
     */
    private final ArrayList<String> tagList = new ArrayList<>();

    /**
     * Set of unique tag IDs detected by the RFID reader.
     */
    private final HashSet<String> tagSet = new HashSet<>();

    /**
     * Handler for RFID operations and responses.
     */
    private RFIDHandler rfidHandler;

    /**
     * Request code for Bluetooth permission.
     */
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private TextView statusTextViewRFID;
    private ListView tagListView;
    private ArrayAdapter<String> tagAdapter;
    private Button btnStart;
    private Button btnStop;
    private Button btnScan;
    private TextView scanResultText;
    private ProgressBar progressBar;
    private View rootLayout;

    private boolean bTestTriggerConfig = false;

    /**
     * Reference to the currently displayed snackbar for programmatic dismissal.
     */
    private Snackbar activeSnackbar;
    private final Handler autoDismissHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Called when the activity is starting. Initializes UI and RFID handler.
         * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();

        rfidHandler = new RFIDHandler();
        checkPermissionsAndInit();
    }

    public boolean getTestStatus(){
        return bTestTriggerConfig;
    }

    /**
     * Consolidates UI initialization and setup.
     */
    private void setupUI() {
        bTestTriggerConfig = false;
        String appName = getString(R.string.app_name);
        try {
            setTitle(appName + " (" + com.zebra.rfid.api3.BuildConfig.VERSION_NAME + ")");
        } catch (Exception e) {
            /* Exception intentionally ignored for compatibility with Java 8. */
            setTitle(appName);
        }

        rootLayout = findViewById(R.id.coordinatorLayout);
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        if (statusTextViewRFID != null) {
            statusTextViewRFID.setOnClickListener(v -> {
                if (rfidHandler != null) {
                    rfidHandler.toggleConnection();
                }
            });
        }

        tagListView = findViewById(R.id.tag_list);
        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagList);
        if (tagListView != null) {
            tagListView.setAdapter(tagAdapter);
        }

        btnStart = findViewById(R.id.StartButton);
        btnStop = findViewById(R.id.btnStop);
        btnScan = findViewById(R.id.scan);
        scanResultText = findViewById(R.id.scanResult);
        progressBar = findViewById(R.id.progressBar);

        if (btnStart != null) btnStart.setEnabled(false);
        if (btnStop != null) btnStop.setEnabled(false);
        if (btnScan != null) {
            btnScan.setEnabled(false);
            btnScan.setOnClickListener(v -> {
                if (!checkReaderHealthy()) {
                    showSnackbar("SKIP!!!\nRFID Busy", true);
                }
                else {
                    Context context = v.getContext();
                    scanCode(context);
                }
            });
        }
    }

    public void updateReaderStatus(String status, boolean isConnected) {
        /**
         * Updates the reader status on the UI.
         * @param status The status message to display.
         * @param isConnected Whether the reader is connected.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || status == null) return;
            if (statusTextViewRFID != null) {
                statusTextViewRFID.setText(status);
                int color = isConnected ? R.color.status_connected : R.color.status_disconnected;
                statusTextViewRFID.setTextColor(ContextCompat.getColor(this, color));
            }
            if (btnStart != null) {
                btnStart.setEnabled(isConnected);
            }
            if (status.contains(getString(R.string.connecting))) {
                showProgress(true);
            } else {
                showProgress(false);
            }
        });
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                rfidHandler.onCreate(this);
            }
        } else {
            rfidHandler.onCreate(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (rfidHandler != null) rfidHandler.onCreate(this);
            } else {
                showSnackbar(getString(R.string.bluetooth_permissions_not_granted), true);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    private boolean checkReaderHealthy(){
        if(rfidHandler!=null && rfidHandler.isReaderConnected()){
           if(!rfidHandler.isbRfidBusy())
               return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (rfidHandler == null) return super.onOptionsItemSelected(item);

        if (!checkReaderHealthy()) {
                showSnackbar("SKIP!!!\nRFID Busy", true);
                return false;
        } 
        /////////////////////////////////////////
        if (id == R.id.trigger_rfid_rfid) {
            if (checkReaderHealthy()) {
                rfidHandler.setTriggerEnabled(true);
                showSnackbar("RFID Triggers Enabled", true);
                return true;
            } 
        } else if (id == R.id.trigger_barcode_barcode) {
            if (checkReaderHealthy()) {
                rfidHandler.setTriggerEnabled(false);
                showSnackbar("Barcode Triggers Enabled", true);
                return true;
            } 
        } else if (id == R.id.Default) {
            if (checkReaderHealthy()) {
                rfidHandler.restoreDefaultTriggerConfig();
                showSnackbar("Default Trigger Settings", true);
                return true;
            } 
        } else if (id == R.id.auto) {
            if (checkReaderHealthy()) {
                bTestTriggerConfig = true;
                showSnackbar("Pull Trigger:\r\nRFID Operation\r\nBarcode Trigger Disabled", false);
                return true;
            } 
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rfidHandler != null) {
            rfidHandler.onPause();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (rfidHandler != null) rfidHandler.onResume();
    }

    @Override
    protected void onDestroy() {
        showProgress(false);
        if (rfidHandler != null) {
            rfidHandler.onDestroy();
        }
        super.onDestroy();
    }

    private void toggleInventoryButtons(boolean isRunning) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (btnStart != null) btnStart.setEnabled(!isRunning);
            if (btnStop != null) btnStop.setEnabled(isRunning);
        });
    }

    public void setScanButtonEnabled(boolean enabled) {
        /**
         * Enables or disables the scan button.
         * @param enabled True to enable, false to disable.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (btnScan != null) {
                btnScan.setEnabled(enabled);
            }
        });
    }

    public void StartInventory(View view) {
        /**
         * Starts RFID inventory when the start button is pressed.
         * @param view The view that triggered this method.
         */
        toggleInventoryButtons(true);
        clearTagData();
        if (rfidHandler != null) rfidHandler.performInventory();
    }

    private void clearTagData() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            tagSet.clear();
            tagList.clear();
            if (tagAdapter != null) {
                tagAdapter.notifyDataSetChanged();
            }
        });
    }

    public void scanCode(Context view) {
        /**
         * Initiates barcode scanning when the scan button is pressed.
         * @param view The view that triggered this method.
         */
        if (rfidHandler != null) rfidHandler.scanCode();
    }

    public void StopInventory(View view) {
        /**
         * Stops RFID inventory when the stop button is pressed.
         * @param view The view that triggered this method.
         */
        toggleInventoryButtons(false);
        if (rfidHandler != null) rfidHandler.stopInventory();
    }

    @SuppressLint("SetTextI18n")
    public void handleTagdata(TagData[] tagData) {
        /**
         * Handles new tag data received from the RFID reader.
         * @param tagData Array of TagData objects.
         */
        if (tagData == null || tagData.length == 0) return;

        final ArrayList<String> newTags = collectNewTags(tagData);
        if (!newTags.isEmpty()) {
            final int totalUniqueTags = tagSet.size();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                updateTagListUI(newTags);
                updateStatusTextWithUniqueTags(totalUniqueTags);
            });
        }
    }

    private ArrayList<String> collectNewTags(TagData[] tagData) {
        ArrayList<String> newTags = new ArrayList<>();
        for (TagData tag : tagData) {
            if (tag == null) continue;
            String tagId = tag.getTagID();
            if (tagId != null && !tagSet.contains(tagId)) {
                tagSet.add(tagId);
                newTags.add(tagId + " (RSSI: " + tag.getPeakRSSI() + ")");
            }
        }
        return newTags;
    }

    private void updateTagListUI(ArrayList<String> newTags) {
        tagList.addAll(0, newTags);
        if (tagAdapter != null) {
            tagAdapter.notifyDataSetChanged();
        }
    }

    private void updateStatusTextWithUniqueTags(int totalUniqueTags) {
        if (statusTextViewRFID != null && statusTextViewRFID.getText() != null) {
            String statusStr = statusTextViewRFID.getText().toString();
            if (statusStr.contains(getString(R.string.connected))) {
                String[] parts = statusStr.split("\n");
                String currentStatus = parts.length > 0 ? parts[0] : statusStr;
                statusTextViewRFID.setText(currentStatus + "\n" + getString(R.string.unique_tags, totalUniqueTags));
            }
        }
    }

    public void handleTriggerPress(boolean pressed) {
        /**
         * Handles trigger press events from the RFID reader.
         * @param pressed True if trigger is pressed, false otherwise.
         */
        toggleInventoryButtons(pressed);
        if (pressed) {
            clearTagData();
            if (rfidHandler != null) rfidHandler.performInventory();
        } else {
            if (rfidHandler != null) rfidHandler.stopInventory();
        }
    }

    @Override
    public void barcodeData(String val) {
        /**
         * Displays barcode data received from the scanner.
         * @param val The barcode value.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (scanResultText != null) {
                scanResultText.setText(getString(R.string.scan_result_label, val != null ? val : ""));

                if(bTestTriggerConfig) {
                    sendToast("Restore to RFID");
                    rfidHandler.subsribeRfidTriggerEvents(true);
                    rfidHandler.setTriggerEnabled(true);
                    bTestTriggerConfig = false;
                }
            }
        });
    }

    @Override
    public void sendToast(String val) {
        /**
         * Shows a modern snackbar message on the UI.
         * @param val The message to display.
         */
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            showSnackbar(val, true);
        });
    }

    @Override
    public void dismissToast() {
        /**
         * Dismisses the current snackbar programmatically.
         */
        runOnUiThread(() -> {
            if (activeSnackbar != null && activeSnackbar.isShown()) {
                activeSnackbar.dismiss();
            }
        });
    }

    /**
     * Displays a modern, custom Snackbar pop-up UI with improved alignment and styling.
     * <ul>
     *   <li>Very large, rounded, shadowed Snackbar</li>
     *   <li>Custom close (X) icon aligned to the far left for easy dismissal</li>
     *   <li>Bold, smaller text for message</li>
     *   <li>Optional hourglass progress indicator for auto-disappear</li>
     *   <li>Centered content and responsive width</li>
     * </ul>
     * @param message The message to display in the Snackbar.
     * @param bAutoDisappear If true, the Snackbar auto-dismisses after 3 seconds; otherwise, user must tap X to dismiss.
     */
    @Override
    public void showSnackbar(String message, boolean bAutoDisappear) {
        runOnUiThread(() -> {
            if (rootLayout == null || message == null) return;
            
            if (activeSnackbar != null && activeSnackbar.isShown()) {
                activeSnackbar.dismiss();
            }
            autoDismissHandler.removeCallbacksAndMessages(null);

            activeSnackbar = Snackbar.make(rootLayout, message, Snackbar.LENGTH_INDEFINITE);
            // Remove default action button, add custom close icon for better alignment
            activeSnackbar.setAction(null, null);
            
            View snackbarView = activeSnackbar.getView();
            
            // Modern appearance: very big, rounded, shadow, icon, bold text
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(80f);
            shape.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            snackbarView.setBackground(shape);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                snackbarView.setElevation(40f);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                snackbarView.setOutlineSpotShadowColor(ContextCompat.getColor(this, R.color.black));
            }

            @SuppressLint("RestrictedApi") Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbarView;
            
            // Adjust original message text view
            TextView textView = layout.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setMaxLines(6);
            textView.setGravity(Gravity.CENTER);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(16); // Smaller text
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
            textView.setTextColor(ContextCompat.getColor(this, R.color.white));

                LinearLayout customLayout = new LinearLayout(this);
                customLayout.setOrientation(LinearLayout.HORIZONTAL);
                customLayout.setGravity(Gravity.CENTER);
                customLayout.setPadding(64, 32, 64, 32); // Very big padding

                // Removed info icon for cleaner UI

                // Add custom close (X) icon aligned left
                ImageView closeIcon = new ImageView(this);
                closeIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                closeIcon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                closeParams.setMargins(0, 0, 40, 0); // margin to right
                closeParams.gravity = Gravity.CENTER_VERTICAL;
                closeIcon.setOnClickListener(v -> dismissToast());
                customLayout.addView(closeIcon, closeParams);

            if (bAutoDisappear) {
                // Add Hourglass style ProgressBar only for auto-disappearing toasts
                ProgressBar hourglass = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
                hourglass.setIndeterminate(true);
                if (hourglass.getIndeterminateDrawable() != null) {
                    hourglass.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                }
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                progressParams.setMargins(0, 0, 40, 0);
                customLayout.addView(hourglass, progressParams);
            }
            
            // Re-arrange views for better centered layout
            ViewGroup textParent = (ViewGroup) textView.getParent();
            if (textParent != null) {
                textParent.removeView(textView);
            }
            customLayout.addView(textView);
            layout.addView(customLayout, 0);

            // Very big resize and centering logic
            ViewGroup.LayoutParams baseParams = snackbarView.getLayoutParams();
            int targetWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.95); // 95% width
            if (baseParams instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) baseParams;
                params.gravity = Gravity.CENTER;
                params.width = targetWidth;
                snackbarView.setLayoutParams(params);
            } else if (baseParams instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) baseParams;
                params.gravity = Gravity.CENTER;
                params.width = targetWidth;
                snackbarView.setLayoutParams(params);
            } else {
                snackbarView.setLayoutParams(new ViewGroup.LayoutParams(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            snackbarView.setMinimumWidth(targetWidth);

            activeSnackbar.show();

            // Auto-dismiss after 3 seconds
            if(bAutoDisappear) {
                autoDismissHandler.postDelayed(this::dismissToast, 3000);
            }
        });
    }
}
