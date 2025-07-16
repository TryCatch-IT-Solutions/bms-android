package com.example.bms;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hibory.Conversion;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bms.databinding.ActivityEnrollmentBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class EnrollmentActivity extends AppCompatActivity {

    private ActivityEnrollmentBinding binding;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private MaterialCardView cardRfidRegistered;
    private MaterialCardView scanRfidView;
    private MaterialCardView scanFingerprintView;
    private MaterialCardView fingerPrintRegistered;
    private NestedScrollView nestedScrollView;
    private MaterialCardView faceViewRegistered;
    private MaterialCardView scanFaceCard;

    private Spinner genderSpinner;

    SweetAlertDialog sweetAlertDialog;


    private static final int FINGERPRINT_SCAN_REQUEST_CODE = 1;

    private TextInputEditText firstNameInput;
    private TextInputEditText middleNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText address1Input;
    private TextInputEditText address2Input;
    private TextInputEditText barangayInput;
    private TextInputEditText municipalityInput;
    private TextInputEditText provinceInput;
    private TextInputEditText birthDateInput;
    private TextInputEditText zipCodeInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText emergencyContactInput;
    private TextInputEditText emergencyContactInputName;

    private String rfidId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String address1;
    private String address2;
    private String barangay;
    private String municipality;
    private String province;
    private String birthDate;
    private String gender;
    private int zipCode;
    private double lon;
    private double lat;
    private String email;
    private String phone;
    private String emergencyContact;
    private String emergencyContactName;

    private ActivityResultLauncher<Intent> fingerprintScanLauncher;
    private HashMap<Integer, String> fingerDataMap;
    private HashMap<Integer, String> faceDataMap;

    private DatabaseHelper dbHelper;

    private final Integer GROUP_ID = 1;
    private ProgressDialog mProgressDialog;

    private ActivityResultLauncher<Intent> faceScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getSerializableExtra("dataMap") != null) {
                        faceDataMap = (HashMap<Integer, String>) data.getSerializableExtra("dataMap");
                        if (faceDataMap != null) {
                            faceViewRegistered.setVisibility(View.VISIBLE);
                            scanFaceCard.setVisibility(View.GONE);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEnrollmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mProgressDialog = new ProgressDialog(this);

        // Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsetsCompat.Type.systemBars());

        dbHelper = new DatabaseHelper(this);

        nestedScrollView = findViewById(R.id.nested_scroll_view);

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        cardRfidRegistered = findViewById(R.id.card_rfid_registered);
        scanRfidView = findViewById(R.id.scan_rfid_box);
        scanFingerprintView = findViewById(R.id.scan_fingerprint_box);
        fingerPrintRegistered = findViewById(R.id.card_fingerprint_registered);
        faceViewRegistered = findViewById(R.id.card_face_verified);

        firstNameInput = findViewById(R.id.first_name);
        middleNameInput = findViewById(R.id.middle_name);
        lastNameInput = findViewById(R.id.last_name);
        address1Input = findViewById(R.id.address1);
        address2Input = findViewById(R.id.address2);
        barangayInput = findViewById(R.id.barangay);
        municipalityInput = findViewById(R.id.municipality);
        provinceInput = findViewById(R.id.province);
        birthDateInput = findViewById(R.id.birth_date);
        genderSpinner = findViewById(R.id.gender_spinner);
        zipCodeInput = findViewById(R.id.zip_code);
        emailInput = findViewById(R.id.email);
        phoneInput = findViewById(R.id.phone);
        emergencyContactInput = findViewById(R.id.emergency_contact);
        emergencyContactInputName = findViewById(R.id.emergency_contact_name);

        firstNameInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        middleNameInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        lastNameInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        address1Input.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        address2Input.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        barangayInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        municipalityInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        provinceInput.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});
        emergencyContactInputName.setFilters(new InputFilter[]{new CapitalizeFirstLetterInputFilter()});

        phoneInput.setText("+63");
        phoneInput.setSelection(phoneInput.getText().length());

        emergencyContactInput.setText("+63");
        emergencyContactInput.setSelection(emergencyContactInput.getText().length());

        scanFaceCard = findViewById(R.id.scan_face_box);
        scanFaceCard.setOnClickListener(v -> {
            faceScannerLauncher.launch(new Intent(this, FaceScanner.class));
        });

        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().startsWith("+63")) {
                    phoneInput.setText("+63");
                    phoneInput.setSelection(phoneInput.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        emergencyContactInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().startsWith("+63")) {
                    emergencyContactInput.setText("+63");
                    emergencyContactInput.setSelection(emergencyContactInput.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });


        birthDateInput.setOnClickListener(v -> showDatePickerDialog());

        // Set up the gender spinner with a hint
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        List<String> genderList = new ArrayList<>();
        genderList.add(getString(R.string.gender_hint));
        genderList.addAll(Arrays.asList(getResources().getStringArray(R.array.gender_array)));

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, genderList) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Disable the hint item
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(Color.GRAY); // Set hint color
                } else {
                    tv.setTextColor(Color.BLACK); // Set normal item color
                }
                return view;
            }
        };
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner.setSelection(0); // Set the hi

        fingerprintScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getSerializableExtra("fingerDataMap") != null) {
                            fingerDataMap = (HashMap<Integer, String>) data.getSerializableExtra("fingerDataMap");
                            if (fingerDataMap != null) {
                                fingerPrintRegistered.setVisibility(View.VISIBLE);
                                scanFingerprintView.setVisibility(View.GONE);
                            }
                        }
                    }
                }
        );

        Button buttonResetRfid = findViewById(R.id.button_reset_rfid);
        Button buttonResetFingerprint = findViewById(R.id.button_reset_fingerprint);
        Button buttonSubmit = findViewById(R.id.btn_submit);
        Button buttonResetFace = findViewById(R.id.button_reset_face);

        Toolbar toolbarHead = findViewById(R.id.toolbar_header);
        toolbarHead.setNavigationOnClickListener(view -> finish());

        buttonSubmit.setOnClickListener(v -> {
            firstName = firstNameInput.getText().toString();
            middleName = middleNameInput.getText().toString();
            lastName = lastNameInput.getText().toString();
            address1 = address1Input.getText().toString();
            address2 = address2Input.getText().toString();
            barangay = barangayInput.getText().toString();
            municipality = municipalityInput.getText().toString();
            province = provinceInput.getText().toString();
            birthDate = birthDateInput.getText().toString();
            gender = genderSpinner.getSelectedItem().toString();
            email = emailInput.getText().toString();
            phone = phoneInput.getText().toString();
            emergencyContact = emergencyContactInput.getText().toString();
            emergencyContactName = emergencyContactInputName.getText().toString();


            if (firstName.isEmpty()) {
                Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
                firstNameInput.requestFocus();
                return;
            }

            if (lastName.isEmpty()) {
                Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show();
                lastNameInput.requestFocus();
                return;
            }

            if (address1.isEmpty()) {
                Toast.makeText(this, "Please enter your address line 1", Toast.LENGTH_SHORT).show();
                address1Input.requestFocus();
                return;
            }

            if (address2.isEmpty()) {
                Toast.makeText(this, "Please enter your address line 2", Toast.LENGTH_SHORT).show();
                address2Input.requestFocus();
                return;
            }

            if (barangay.isEmpty()) {
                Toast.makeText(this, "Please enter your barangay", Toast.LENGTH_SHORT).show();
                barangayInput.requestFocus();
                return;
            }

            if (municipality.isEmpty()) {
                Toast.makeText(this, "Please enter your municipality", Toast.LENGTH_SHORT).show();
                municipalityInput.requestFocus();
                return;
            }

            if (province.isEmpty()) {
                Toast.makeText(this, "Please enter your province", Toast.LENGTH_SHORT).show();
                provinceInput.requestFocus();
                return;
            }

            if (birthDate.isEmpty()) {
                Toast.makeText(this, "Please enter your birth date", Toast.LENGTH_SHORT).show();
                birthDateInput.requestFocus();
                return;
            }

            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
                genderSpinner.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                emailInput.requestFocus();
                return;
            }

            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                phoneInput.requestFocus();
                return;
            }

            if (emergencyContactName.isEmpty()) {
                Toast.makeText(this, "Please enter your emergency contact name", Toast.LENGTH_SHORT).show();
                emergencyContactInputName.requestFocus();
                return;
            }

            if (emergencyContact.isEmpty()) {
                Toast.makeText(this, "Please enter your emergency contact number", Toast.LENGTH_SHORT).show();
                emergencyContactInput.requestFocus();
                return;
            }

            if (zipCodeInput.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter your zip code", Toast.LENGTH_SHORT).show();
                zipCodeInput.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                emailInput.requestFocus();
                return;
            }

            if (!phone.matches("\\+\\d{12}")) {
                Toast.makeText(this, "Phone number must be 12 digits", Toast.LENGTH_SHORT).show();
                phoneInput.requestFocus();
                return;
            }

            if (!emergencyContact.matches("\\+\\d{12}")) {
                Toast.makeText(this, "Emergency contact number must be 12 digits", Toast.LENGTH_SHORT).show();
                emergencyContactInput.requestFocus();
                return;
            }


            if (dbHelper.isEmailExists(email)) {
                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.isPhoneExists(phone)) {
                Toast.makeText(this, "Phone number already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            if(phone.equals(emergencyContact)){
                Toast.makeText(this, "Phone number and emergency contact number cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    sweetAlertDialog =  new SweetAlertDialog(EnrollmentActivity.this, SweetAlertDialog.PROGRESS_TYPE);
                    sweetAlertDialog.setTitleText("Saving Enrollment");
                    sweetAlertDialog.show();
                }
            }, 100);

            zipCode = Integer.parseInt(zipCodeInput.getText().toString());

            UserRepository userRepository = new UserRepository(this);

            SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
            String savedGroupId = sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);

            long user_id = userRepository.insertUser(Integer.parseInt(savedGroupId), firstName, middleName,
                    lastName, address1, address2, barangay, municipality, province, birthDate,
                    gender, zipCode, lon, lat, email, phone, emergencyContact, emergencyContactName);

            BiometricRepository biometricRepository = new BiometricRepository(this);

            if (rfidId != null) {
                long bio_id = biometricRepository.insertBiometric(rfidId, user_id, "rfid");
                Log.d("bio_id", "bio_id: " + bio_id);
            }

            if (fingerDataMap != null) {
                long biometric_id = biometricRepository.insertBiometric(null, user_id, "fingerprint");
                FingerprintRepository fingerprintRepository = new FingerprintRepository(this);
                for (int i = 0; i < fingerDataMap.size(); i++) {
                    fingerprintRepository.insertFingerprint(biometric_id, fingerDataMap.get(i));
                }
            }

            //reuse the the fingerprint code
            if (faceDataMap != null) {
                for (int i = 0; i < faceDataMap.size(); i++) {
                    long biometric_id = biometricRepository.insertBiometric(faceDataMap.get(i), user_id, "face");
                }
            }

            ((App)getApplication()).syncUsersOnLogout(EnrollmentActivity.this, new App.SyncCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EnrollmentActivity.this, "Enrollment saved successfully", Toast.LENGTH_SHORT).show();
                    if(sweetAlertDialog != null){
                        sweetAlertDialog.dismiss();
                    }
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Handle failure
                    Toast.makeText(EnrollmentActivity.this, "Sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.d("Errorrrrr", errorMessage);
                    if(sweetAlertDialog != null){
                        sweetAlertDialog.dismiss();
                    }
                    finish();
                }
            });
        });

        buttonResetRfid.setOnClickListener(v -> {
            cardRfidRegistered.setVisibility(View.GONE);
            scanRfidView.setVisibility(View.VISIBLE);
        });

        buttonResetFingerprint.setOnClickListener(v -> {
            fingerPrintRegistered.setVisibility(View.GONE);
            scanFingerprintView.setVisibility(View.VISIBLE);
            fingerDataMap = null;
        });

        buttonResetFace.setOnClickListener(v -> {
            faceViewRegistered.setVisibility(View.GONE);
            scanFaceCard.setVisibility(View.VISIBLE);
            faceDataMap = null;
        });


        scanFingerprintView.setOnClickListener(v -> {
            if (!checkPermission()) {
                Toast.makeText(this, "Please grant permission in settings to use fingerprint", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, FingerPrintScanActivity.class);
            fingerprintScanLauncher.launch(intent);
        });
    }

    private boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }
        boolean manage = Environment.isExternalStorageManager();
        if (!manage) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
            return false;
        }
        return true;
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = year1 + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                    birthDateInput.setText(selectedDate);
                }, year, month, day);

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        mNfcAdapter = manager.getDefaultAdapter();

        if (mNfcAdapter == null) {
            // Device does not support NFC
            new SweetAlertDialog(EnrollmentActivity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("NFC Not Supported")
                    .setContentText("This device does not support NFC")
                    .show();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            // NFC is not enabled
            new SweetAlertDialog(EnrollmentActivity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("NFC Not Enabled")
                    .setContentText("Please enable NFC in settings")
                    .show();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("hello", "nfc:" + intent.getAction());

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String id = Conversion.Bytes2HexString(tag.getId());
        Log.d("SCAN NFC", "ID: " + id);

        if (cardRfidRegistered != null) {

            //check for rfid
            if (dbHelper.getRfidByKey(id) != null) {
                Toast.makeText(this, "RFID already exists", Toast.LENGTH_SHORT).show();
                rfidId = null;
                cardRfidRegistered.setVisibility(View.GONE);
                scanRfidView.setVisibility(View.VISIBLE);
                scanRfidView.requestFocus();

                // Scroll to the scanRfidView with an offset
                nestedScrollView.post(() -> {
                    int y = scanRfidView.getTop() - 50; // Adjust the offset as needed
                    nestedScrollView.scrollTo(0, y);
                });
                return;
            }

            rfidId = id;
            cardRfidRegistered.setVisibility(View.VISIBLE);
            scanRfidView.setVisibility(View.GONE);
        }
    }
}