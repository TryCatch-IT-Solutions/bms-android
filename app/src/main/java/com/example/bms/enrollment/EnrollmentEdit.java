package com.example.bms.enrollment;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bms.App;
import com.example.bms.Biometric;
import com.example.bms.BiometricRepository;
import com.example.bms.CapitalizeFirstLetterInputFilter;
import com.example.bms.DatabaseHelper;
import com.example.bms.EncryptionUtil;
import com.example.bms.FaceScanner;
import com.example.bms.FingerPrintScanActivity;
import com.example.bms.Fingerprint;
import com.example.bms.FingerprintRepository;
import com.example.bms.GroupActivity;
import com.example.bms.R;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hibory.Conversion;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

import com.example.bms.SplashScreen;
import com.example.bms.UserRepository;
import com.example.bms.data.model.User;
import com.example.bms.databinding.ActivityEnrollmentEditBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.card.MaterialCardView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
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

public class EnrollmentEdit extends AppCompatActivity {

    private ActivityEnrollmentEditBinding binding;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private MaterialCardView cardRfidRegistered;
    private MaterialCardView scanRfidView;
    private MaterialCardView scanFingerprintView;
    private MaterialCardView fingerPrintRegistered;
    private NestedScrollView nestedScrollView;

    private Spinner genderSpinner;


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
    private MaterialCardView faceViewRegistered;
    private MaterialCardView scanFaceCard;

    public long rfidDbId = 0;

    private ActivityResultLauncher<Intent> fingerprintScanLauncher;
    private HashMap<Integer, String> fingerDataMap;
    private HashMap<Integer, String> faceDataMap;

    private DatabaseHelper dbHelper;

    private final Integer GROUP_ID = 1;

    private User employee;

    private ActivityResultLauncher<Intent> faceScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getSerializableExtra("dataMap") != null) {
                        faceDataMap = (HashMap<Integer, String>) data.getSerializableExtra("dataMap");
                        Log.d("faceDataMap", "faceDataMap: " + faceDataMap);
                        if (faceDataMap != null) {
                            faceViewRegistered.setVisibility(View.VISIBLE);
                            scanFaceCard.setVisibility(View.GONE);
                        }
                    }
                }
            });

    private void fetch_employee() {
        String employeeId = getIntent().getStringExtra("employeeId");

        if (employeeId != null) {
            try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
                employee = dbHelper.getUserById(employeeId);
            }

            if (employee != null) {
                firstNameInput.setText(employee.getFirstName());
                middleNameInput.setText(employee.getMiddleName());
                lastNameInput.setText(employee.getLastName());
                address1Input.setText(employee.getAddress1());
                address2Input.setText(employee.getAddress2());
                barangayInput.setText(employee.getBarangay());
                municipalityInput.setText(employee.getMunicipality());
                provinceInput.setText(employee.getProvince());
                birthDateInput.setText(employee.getBirthDate());
                emailInput.setText(employee.getEmail());
                phoneInput.setText(employee.getPhone());
                emergencyContactInput.setText(employee.getEmergencyContactNo());
                emergencyContactInputName.setText(employee.getEmergencyContactName());
                zipCodeInput.setText(String.valueOf(employee.getZipCode()));

                if (employee.getGender() != null) {
                    int spinnerPosition = ArrayAdapter.createFromResource(this,
                                    R.array.gender_array, android.R.layout.simple_spinner_item)
                            .getPosition(employee.getGender());
                    genderSpinner.setSelection(1);
                    Log.d("Enrollment", "Spinner position: " + spinnerPosition);
                }

                List<Biometric> biometrics = dbHelper.getBiometricsByUserId(Long.parseLong(employeeId));
                for (Biometric biometric : biometrics) {
                    if (biometric.getType().equals("fingerprint")) {
                        fingerPrintRegistered.setVisibility(View.VISIBLE);
                        scanFingerprintView.setVisibility(View.GONE);
                    }
                    if (biometric.getType().equals("face")) {
                        faceViewRegistered.setVisibility(View.VISIBLE);
                        scanFaceCard.setVisibility(View.GONE);
                    }
                    if (biometric.getType().equals("rfid")) {
                        cardRfidRegistered.setVisibility(View.VISIBLE);
                        scanRfidView.setVisibility(View.GONE);
                        rfidId = biometric.getKey();
                        rfidDbId = biometric.getId();
                    }
                }


            } else {
                Log.e("EnrollmentEdit", "Employee not found with ID: " + employeeId);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEnrollmentEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        faceViewRegistered = findViewById(R.id.card_face_verified);

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

        fetch_employee();

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
        Button buttonResetFace = findViewById(R.id.button_reset_face);
        Button buttonSubmit = findViewById(R.id.btn_submit);

        buttonSubmit.setText("Update");

        Toolbar toolbarHead = findViewById(R.id.toolbar_header_edit);
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
            if (dbHelper.isEmailExists(email,employee.getUserId())) {
                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.isPhoneExists(phone,employee.getUserId())) {
                Toast.makeText(this, "Phone number already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            zipCode = Integer.parseInt(zipCodeInput.getText().toString());

            UserRepository userRepository = new UserRepository(this);

            SharedPreferences sharedPreferences = getSharedPreferences(GroupActivity.PREFS_NAME, Context.MODE_PRIVATE);
            String savedGroupId = sharedPreferences.getString(GroupActivity.KEY_SELECTED_GROUP, null);

            Log.d("savedGroupId", "UserId: " + Long.parseLong(employee.getUserId()));
            long user_id = userRepository.updateUser(Long.parseLong(employee.getUserId()),Integer.parseInt(savedGroupId), firstName, middleName,
                    lastName, address1, address2, barangay, municipality, province, birthDate,
                    gender, zipCode, lon, lat, email, phone, emergencyContact, emergencyContactName, employee.getRole());

            BiometricRepository biometricRepository = new BiometricRepository(this);
            Biometric fingerPrintBiometric = biometricRepository.getFingerPrintBiometricByUserId(user_id);

            if (cardRfidRegistered.getVisibility() != View.GONE) {
                long bio_id = biometricRepository.updateBiometric(rfidDbId,rfidId, user_id, "rfid");
                Log.d("bio_id", "updated_bio: " + bio_id);
            }else{
                Log.d("bio_id", "deleted: " + rfidDbId);
                biometricRepository.deleteBiometric(rfidDbId);
            }


            if (fingerPrintRegistered.getVisibility() != View.GONE && fingerDataMap != null) {
                long biometric_id = biometricRepository.insertBiometric(null, user_id, "fingerprint");
                FingerprintRepository fingerprintRepository = new FingerprintRepository(this);
                List<Fingerprint> fingerprints = fingerprintRepository.getFingerprintsByBiometricId(biometric_id);

                for (int i = 0; i < fingerDataMap.size(); i++) {
                    String key = fingerDataMap.get(i);
                    if (fingerprints.size() > i) {
                        fingerprintRepository.updateFingerprint(fingerprints.get(i).getId(), biometric_id, key);
                    } else {
                        fingerprintRepository.insertFingerprint(biometric_id, key);
                    }
                }
            }else{
                if(fingerPrintBiometric != null && fingerPrintRegistered.getVisibility() == View.GONE){
                    FingerprintRepository fingerprintRepository = new FingerprintRepository(this);
                    fingerprintRepository.deleteFingerprintsByBiometricId(fingerPrintBiometric.getId());
                    biometricRepository.deleteBiometric(fingerPrintBiometric.getId());
                }
            }

            if(faceViewRegistered.getVisibility() != View.GONE && faceDataMap != null) {
                List<Biometric> biometrics = dbHelper.getFaceBiometricsByUserId(user_id);
                for (int i = 0; i < faceDataMap.size(); i++) {
                    String key = faceDataMap.get(i);
                    Log.d("faceDataMap", "size: " + faceDataMap.size() + " i: " + i );
                    if (biometrics.size() > i) {
                        biometricRepository.updateFaceBiometric(biometrics.get(i).getId(), key, user_id, "face");
                    } else {
                       biometricRepository.insertBiometric(key, user_id, "face");
                    }
                }
            }else{
                if(faceViewRegistered.getVisibility() == View.GONE) {
                    List<Biometric> biometrics = dbHelper.getFaceBiometricsByUserId(user_id);
                    for (Biometric biometric : biometrics) {
                        biometricRepository.deleteBiometric(biometric.getId());
                    }
                }

            }

            ((App)getApplication()).syncUsers(EnrollmentEdit.this, new App.SyncCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(EnrollmentEdit.this, "Enrollment saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Handle failure
                    Toast.makeText(EnrollmentEdit.this, "Sync failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                }
            },true);

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
        datePickerDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Device does not support NFC
        }
        if (!mNfcAdapter.isEnabled()) {
            // NFC is not enabled
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
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
            if (dbHelper.getRfidByKey(id, employee.getUserId()) != null) {
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