package com.example.bms;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.bms.data.model.LoggedInUser;
import com.example.bms.data.model.User;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bms.db";
    private static final int DATABASE_VERSION = 32;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_GROUP_ID = "group_id";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_FIRST_NAME = "first_name";
    public static final String COLUMN_MIDDLE_NAME = "middle_name";
    public static final String COLUMN_LAST_NAME = "last_name";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_ADDRESS1 = "address1";
    public static final String COLUMN_ADDRESS2 = "address2";
    public static final String COLUMN_BARANGAY = "barangay";
    public static final String COLUMN_MUNICIPALITY = "municipality";
    public static final String COLUMN_PROVINCE = "province";
    public static final String COLUMN_BIRTH_DATE = "birth_date";
    public static final String COLUMN_GENDER = "gender";
    public static final String COLUMN_ZIP_CODE = "zip_code";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_EMERGENCY_CONTACT_NAME = "emergency_contact_name";
    public static final String COLUMN_EMERGENCY_CONTACT_NO = "emergency_contact_no";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_IS_SYNCED = "is_synced";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_DELETED_AT = "deleted_at";
    public static final String COLUMN_DELETED_BY = "deleted_by";

    public static final String COLUMN_SOURCE = "source";
   private static final String TABLE_CREATE_USERS = String.format(
           "CREATE TABLE %s (" +
                   "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   "%s INTEGER, " +
                   "%s TEXT CHECK(%s IN ('superadmin', 'groupadmin', 'employee')), " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s DOUBLE, " +
                   "%s DOUBLE, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s DATE, " +
                   "%s TEXT, " +
                   "%s INTEGER, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " +
                   "%s TEXT, " + // Added source column
                   "%s BOOLEAN, " +
                   "%s DATETIME, " +
                   "%s DATETIME, " +
                   "%s DATETIME, " +
                   "%s INTEGER);",
           TABLE_USERS, COLUMN_ID, COLUMN_GROUP_ID, COLUMN_ROLE, COLUMN_ROLE, COLUMN_FIRST_NAME, COLUMN_MIDDLE_NAME, COLUMN_LAST_NAME, COLUMN_LAT, COLUMN_LON,
           COLUMN_ADDRESS1, COLUMN_ADDRESS2, COLUMN_BARANGAY, COLUMN_MUNICIPALITY, COLUMN_PROVINCE, COLUMN_BIRTH_DATE, COLUMN_GENDER, COLUMN_ZIP_CODE,
           COLUMN_EMAIL, COLUMN_PHONE_NUMBER, COLUMN_EMERGENCY_CONTACT_NAME, COLUMN_EMERGENCY_CONTACT_NO, COLUMN_PASSWORD, COLUMN_STATUS, COLUMN_SOURCE, COLUMN_IS_SYNCED,
           COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY
   );

    public static final String TABLE_FINGERPRINTS = "fingerprints";
    public static final String COLUMN_BIOMETRIC_ID = "biometric_id";
    public static final String COLUMN_KEY = "key";

    public static final String TABLE_BIOMETRICS = "biometrics";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    public static final String TABLE_GROUPS = "groups";
    public static final String COLUMN_NAME = "name";

    public static final String TABLE_DEVICES = "devices";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_SERIAL_NO = "serial_no";
    public static final String COLUMN_REGISTERED_AT = "registered_at";

    private static final String TABLE_CREATE_BIOMETRICS = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s TEXT, " +
                    "%s INTEGER, " +
                    "%s TEXT CHECK(%s IN ('fingerprint', 'rfid', 'face')), " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER, " +
                    "%s INTEGER);",
            TABLE_BIOMETRICS, COLUMN_ID, COLUMN_KEY, COLUMN_USER_ID, COLUMN_TYPE, COLUMN_TYPE, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_IS_SYNCED, COLUMN_DELETED_BY
    );

    private static final String TABLE_CREATE_FINGERPRINTS = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s INTEGER, " +
                    "%s TEXT, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER);",
            TABLE_FINGERPRINTS, COLUMN_ID, COLUMN_BIOMETRIC_ID, COLUMN_KEY, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY
    );

    private static final String TABLE_CREATE_GROUPS = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s TEXT, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER);",
            TABLE_GROUPS, COLUMN_ID, COLUMN_NAME, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY
    );

    private static final String TABLE_CREATE_DEVICES = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s INTEGER, " +
                    "%s TEXT, " +
                    "%s TEXT, " +
                    "%s DOUBLE, " +
                    "%s DOUBLE, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s TEXT, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s BOOLEAN DEFAULT 0, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER, " +
                    "%s BOOLEAN DEFAULT 0);",
            TABLE_DEVICES, COLUMN_ID, COLUMN_GROUP_ID, COLUMN_MODEL, COLUMN_SERIAL_NO, COLUMN_LAT, COLUMN_LON,
            "is_online", "last_sync", "last_activity", "logo_url", "manual_time_entry", "check_in", "check_out",
            "break_in", "break_out", "overtime_in", "overtime_out", COLUMN_REGISTERED_AT, COLUMN_CREATED_AT,
            COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY, "is_synced"
    );

    public static final String TABLE_TIME_ENTRIES = "time_entries";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_METADATA = "metadata";
    public static final String COLUMN_SNAPSHOT = "snapshot";

    private static final String TABLE_CREATE_TIME_ENTRIES = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s INTEGER, " +
                    "%s TEXT, " + // Type can be nullable
                    "%s DATETIME, " +
                    "%s TEXT, " +
                    "%s BOOLEAN, " +
                    "%s DOUBLE, " + // Latitude
                    "%s DOUBLE, " + // Longitude
                    "%s TEXT, " + // Snapshot
                    "%s TEXT, " + // Serial number
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER);",
            TABLE_TIME_ENTRIES, COLUMN_ID, COLUMN_USER_ID, COLUMN_TYPE, COLUMN_DATETIME, COLUMN_METADATA, COLUMN_IS_SYNCED,
            COLUMN_LATITUDE, COLUMN_LONGITUDE,
            COLUMN_SNAPSHOT,
            COLUMN_SERIAL_NO,
            COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY
    );

    public static final String TABLE_ANNOUNCEMENTS = "announcements";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_EXPIRATION = "expiration";

    private static final String TABLE_CREATE_ANNOUNCEMENTS = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "%s INTEGER, " +
                    "%s TEXT, " +
                    "%s TEXT, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s DATETIME, " +
                    "%s INTEGER);",
            TABLE_ANNOUNCEMENTS, COLUMN_ID, COLUMN_USER_ID, COLUMN_TITLE, COLUMN_MESSAGE, COLUMN_EXPIRATION, COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT, COLUMN_DELETED_BY
    );



    private static final String INSERT_SAMPLE_GROUPS = String.format(
            "INSERT INTO %s (%s) VALUES ('Group A'), ('Group B'), ('Group C'), ('Group D'), ('Group E'), ('Group F'), ('Group G'), ('Group H'), ('Group I'), ('Group J');",
            TABLE_GROUPS, COLUMN_NAME
    );

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GROUPS, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                groups.add(new Group(name, id));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return groups;
    }

    public void updateFingerprint(long id, long biometricId, String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BIOMETRIC_ID, biometricId);
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_UPDATED_AT, getCurrentDateTime());

        db.update(TABLE_FINGERPRINTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Fingerprint> getAllFingerprints() {
        List<Fingerprint> fingerprints = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get all fingerprints of users with deleted_at as null
        String query = "SELECT f.* FROM " + TABLE_FINGERPRINTS + " f " +
                "JOIN " + TABLE_BIOMETRICS + " b ON f." + COLUMN_BIOMETRIC_ID + " = b." + COLUMN_ID + " " +
                "JOIN " + TABLE_USERS + " u ON b." + COLUMN_USER_ID + " = u." + COLUMN_ID + " " +
                "WHERE u." + COLUMN_DELETED_AT + " IS NULL";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                long biometricId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BIOMETRIC_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                String deletedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELETED_AT));
                long deletedBy = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DELETED_BY));

                fingerprints.add(new Fingerprint(id, biometricId, key, createdAt, updatedAt, deletedAt, deletedBy));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return fingerprints;
    }

    public List<Fingerprint> getFingerprintsByGroupId(long groupId) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get all biometrics with group = groupId and type = fingerprint and join with fingerprints
        String query = "SELECT f.* FROM " + TABLE_FINGERPRINTS + " f " +
                "JOIN " + TABLE_BIOMETRICS + " b ON f." + COLUMN_BIOMETRIC_ID + " = b." + COLUMN_ID + " " +
                "JOIN " + TABLE_USERS + " u ON b." + COLUMN_USER_ID + " = u." + COLUMN_ID + " " +
                "WHERE u." + COLUMN_GROUP_ID + " = ? AND b." + COLUMN_TYPE + " = 'fingerprint' AND u." + COLUMN_DELETED_AT + " IS NULL";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(groupId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
                long biometricId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BIOMETRIC_ID));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                String deletedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELETED_AT));
                long deletedBy = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DELETED_BY));

                fingerprints.add(new Fingerprint(id, biometricId, key, createdAt, updatedAt, deletedAt, deletedBy));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return fingerprints;
    }

    public List<Fingerprint> getFingerprintsByBiometricId(long biometricId) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FINGERPRINTS + " WHERE " + COLUMN_BIOMETRIC_ID + " = ?", new String[]{String.valueOf(biometricId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                String deletedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELETED_AT));
                long deletedBy = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DELETED_BY));

                fingerprints.add(new Fingerprint(id, biometricId, key, createdAt, updatedAt, deletedAt, deletedBy));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return fingerprints;
    }

    public List<Group> getGroup(long groupId) {
        List<Group> groups = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GROUPS + " WHERE " + COLUMN_ID + " = ?", new String[]{String.valueOf(groupId)});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                groups.add(new Group(name, id));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return groups;
    }


    public LoggedInUser getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});

        if (cursor.moveToFirst()) {
            String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
            String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
            long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));

            cursor.close();
            db.close();
            return new LoggedInUser(userId, firstName + " " + lastName, email, storedHashedPassword, groupId, role);
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }


    public String getCurrentDateTime() {
        Log.d("Datetime", "Test");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d("Datetime", "Test Done" + sdf.format(new Date()));

        return sdf.format(new Date());
    }


    public void insertOrUpdateDevice(long groupId, String model, String serialNo, double lat, double lon, String registeredAt, boolean isOnline, String lastSync, String lastActivity, String logoUrl, boolean manualTimeEntry, boolean checkIn, boolean checkOut, boolean breakIn, boolean breakOut, boolean overtimeIn, boolean overtimeOut) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Check if serialNo already exists
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DEVICES + " WHERE " + COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});

        values.put(COLUMN_GROUP_ID, groupId);
        values.put(COLUMN_MODEL, model);
        values.put(COLUMN_SERIAL_NO, serialNo);
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_LON, lon);
        values.put(COLUMN_REGISTERED_AT, registeredAt);
        values.put(COLUMN_UPDATED_AT, getCurrentDateTime());
        values.put("is_online", isOnline);
        values.put("last_sync", lastSync);
        values.put("last_activity", lastActivity);
        values.put("logo_url", logoUrl);
        values.put("manual_time_entry", manualTimeEntry);
        values.put("check_in", checkIn);
        values.put("check_out", checkOut);
        values.put("break_in", breakIn);
        values.put("break_out", breakOut);
        values.put("overtime_in", overtimeIn);
        values.put("overtime_out", overtimeOut);

        if (cursor.moveToFirst()) {
            db.update(TABLE_DEVICES, values, COLUMN_SERIAL_NO + " = ?", new String[]{serialNo});
        } else {
            values.put(COLUMN_CREATED_AT, getCurrentDateTime());
            db.insertWithOnConflict(TABLE_DEVICES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        cursor.close();
        db.close();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_USERS);
        db.execSQL(TABLE_CREATE_FINGERPRINTS);
        db.execSQL(TABLE_CREATE_BIOMETRICS);
        db.execSQL(TABLE_CREATE_GROUPS);
        db.execSQL(TABLE_CREATE_DEVICES);
//        db.execSQL(INSERT_SAMPLE_GROUPS);
        db.execSQL(TABLE_CREATE_TIME_ENTRIES);
        db.execSQL(TABLE_CREATE_ANNOUNCEMENTS);
//        insertSuperAdmin(db);
//        insertGroupAdmin(db);
    }

    public void resetUsersTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        //delete all users
        db.delete(DatabaseHelper.TABLE_USERS, null, null);
        db.delete(DatabaseHelper.TABLE_BIOMETRICS, null, null);
        db.delete(DatabaseHelper.TABLE_FINGERPRINTS, null, null);
        db.delete(DatabaseHelper.TABLE_TIME_ENTRIES, null, null);
        //reset autoincrement
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_USERS + "'");
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_BIOMETRICS + "'");
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_FINGERPRINTS + "'");
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_TIME_ENTRIES + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_USERS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_BIOMETRICS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_FINGERPRINTS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_TIME_ENTRIES + "'");
        db.close();
    }

    public void insertSuperAdmin() {
        SQLiteDatabase db = this.getReadableDatabase();

        //insert admin user
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP_ID, 1);
        values.put(COLUMN_ROLE, "superadmin");
        values.put(COLUMN_FIRST_NAME, "Admin");
        values.put(COLUMN_LAST_NAME, "BMS");
        values.put(COLUMN_LAT, 0.0);
        values.put(COLUMN_LON, 0.0);
        values.put(COLUMN_ADDRESS1, "Bataan 2113");
        values.put(COLUMN_ADDRESS2, "Philippines");
        values.put(COLUMN_BARANGAY, "Imelda");
        values.put(COLUMN_MUNICIPALITY, "Samal");
        values.put(COLUMN_PROVINCE, "Bataan");
        values.put(COLUMN_BIRTH_DATE, "2024-01-01");
        values.put(COLUMN_GENDER, "male");
        values.put(COLUMN_ZIP_CODE, "2113");
        values.put(COLUMN_EMAIL, "superadmin@bms.com");
        values.put(COLUMN_PHONE_NUMBER, "09123456782");
        values.put(COLUMN_EMERGENCY_CONTACT_NAME, "Admin");
        values.put(COLUMN_EMERGENCY_CONTACT_NO, "09123456789");
        String hashedPassword = BCrypt.hashpw("superadmin", BCrypt.gensalt());
        values.put(COLUMN_PASSWORD, hashedPassword);
        values.put(COLUMN_STATUS, "active");
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, getCurrentDateTime());
        values.put(COLUMN_UPDATED_AT, getCurrentDateTime());
        db.insert(TABLE_USERS, null, values);
    }

    public List<User> searchUsersByGroupId(String query, String _groupId, int limit, int offset) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String searchQuery = "%" + query + "%";

        String queryStr;
        String[] queryArgs;

      queryStr = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_STATUS + " = 'active' AND " + COLUMN_GROUP_ID + " = ? AND " + COLUMN_ROLE + " != 'superadmin' AND (" +
        COLUMN_FIRST_NAME + " LIKE ? COLLATE NOCASE OR " +
        COLUMN_LAST_NAME + " LIKE ? COLLATE NOCASE OR " +
        COLUMN_EMAIL + " LIKE ? COLLATE NOCASE OR " +
        COLUMN_PHONE_NUMBER + " LIKE ? COLLATE NOCASE) LIMIT ? OFFSET ?";

        queryArgs = new String[]{_groupId, searchQuery, searchQuery, searchQuery, searchQuery, String.valueOf(limit), String.valueOf(offset)};
        Cursor cursor = db.rawQuery(queryStr, queryArgs);

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupId, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }


    public List<User> searchUsers(String query, int limit, int offset, String groupId) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String searchQuery = "%" + query + "%";
        String queryStr;
        String[] queryArgs;

        if (groupId != null) {
            queryStr = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_STATUS + " = 'active' AND " + COLUMN_GROUP_ID + " = ? AND (" +
                    COLUMN_FIRST_NAME + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_LAST_NAME + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_EMAIL + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_PHONE_NUMBER + " LIKE ? COLLATE NOCASE) LIMIT ? OFFSET ?";
            queryArgs = new String[]{groupId, searchQuery, searchQuery, searchQuery, searchQuery, String.valueOf(limit), String.valueOf(offset)};
        } else {
            queryStr = "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_STATUS + " = 'active' AND (" +
                    COLUMN_FIRST_NAME + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_LAST_NAME + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_EMAIL + " LIKE ? COLLATE NOCASE OR " +
                    COLUMN_PHONE_NUMBER + " LIKE ? COLLATE NOCASE) LIMIT ? OFFSET ?";
            queryArgs = new String[]{searchQuery, searchQuery, searchQuery, searchQuery, String.valueOf(limit), String.valueOf(offset)};
        }

        Cursor cursor = db.rawQuery(queryStr, queryArgs);

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupIdLong = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupIdLong, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public List<User> getPaginatedUsersByGroupId(String groupId, int limit, int offset) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_GROUP_ID + " = ? AND " + COLUMN_STATUS + " = 'active' AND " + COLUMN_ROLE + " != 'superadmin' ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT ? OFFSET ?", new String[]{groupId, String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupIdLong = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupIdLong, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public List<User> getUsersByGroupId(String groupId) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_GROUP_ID + " = ? AND " + COLUMN_STATUS + " = 'active'", new String[]{groupId});

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupIdLong = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupIdLong, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public List<User> getPaginatedUsers(int limit, int offset) {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_STATUS + " IS 'active' ORDER BY " + COLUMN_CREATED_AT + " DESC LIMIT ? OFFSET ?", new String[]{String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupId, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }


    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_STATUS + " IS 'active'", null);

        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
                String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
                String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
                long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
                String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
                String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
                String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
                String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
                String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
                String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                users.add(new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupId, role,
                        address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public Biometric getRfidByKey(String key) {
        return getRfidByKey(key, null);
    }

    public Biometric getRfidByKey(String key, String user_id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        if (user_id != null) {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BIOMETRICS + " WHERE " + COLUMN_KEY + " = ? AND " + COLUMN_USER_ID + " != ?", new String[]{key, user_id});
        } else {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BIOMETRICS + " WHERE " + COLUMN_KEY + " = ?", new String[]{key});
        }

        if (cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            String rfidKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
            String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
            String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
            Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

            cursor.close();
            db.close();
            return new Biometric(id, userId, rfidKey, type, createdAt, updatedAt, isSynced);
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    public List<Biometric> getFaceBiometricsByUserId(long userId) {
        List<Biometric> biometrics = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BIOMETRICS + " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_TYPE + " = 'face'", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                biometrics.add(new Biometric(id, userId, key, type, createdAt, updatedAt, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return biometrics;
    }

    public List<Biometric> getBiometricsByUserId(long userId) {
        List<Biometric> biometrics = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BIOMETRICS + " WHERE " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                String updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT));
                Integer isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

                biometrics.add(new Biometric(id, userId, key, type, createdAt, updatedAt, isSynced));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return biometrics;
    }

    public User getUserByEmailOrPhone(String emailOrPhone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ? OR " + COLUMN_PHONE_NUMBER + " = ?", new String[]{emailOrPhone, emailOrPhone});

        if (cursor.moveToFirst()) {
            String userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
            String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
            String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
            long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
            String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
            String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
            String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
            String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
            String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
            String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
            String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
            String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
            String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));

            cursor.close();
            db.close();
            return new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupId, role,
                    address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced);
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    public User getUserById(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_ID + " = ?", new String[]{userId});

        if (cursor.moveToFirst()) {
            String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRST_NAME));
            String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_NAME));
            String middleName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MIDDLE_NAME));
            String storedHashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
            long groupId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID));
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
            String address1 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS1));
            String address2 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS2));
            String barangay = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARANGAY));
            String municipality = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MUNICIPALITY));
            String province = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROVINCE));
            String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTH_DATE));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
            String zipCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZIP_CODE));
            String emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NAME));
            String emergencyContactNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT_NO));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            int isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED));


            cursor.close();
            db.close();
            return new User(userId, firstName + " " + lastName, firstName, middleName, lastName, email, phone, storedHashedPassword, groupId, role,
                    address1, address2, barangay, municipality, province, birthDate, gender, zipCode, emergencyContactName, emergencyContactNo, status, isSynced);
        } else {
            cursor.close();
            db.close();
            return null;
        }
    }

    public boolean isEmailExists(String email) {
        return isEmailExists(email, null);
    }

    public boolean isEmailExists(String email, String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        boolean exists;
        if (userId != null) {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ? AND " + COLUMN_ID + " != ?", new String[]{email, userId});
            exists = cursor.moveToFirst();
        } else {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
            exists = cursor.moveToFirst();
        }

        cursor.close();
        db.close();
        return exists;
    }

    public boolean isPhoneExists(String phone, String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;
        boolean exists;
        if (userId != null) {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE " + COLUMN_PHONE_NUMBER + " = ? AND " + COLUMN_ID + " != ?", new String[]{phone, userId});
            exists = cursor.moveToFirst();
        } else {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USERS + " WHERE " + COLUMN_PHONE_NUMBER + " = ?", new String[]{phone});
            exists = cursor.moveToFirst();
        }

        cursor.close();
        db.close();
        return exists;
    }

    public boolean isPhoneExists(String phone) {
        return isPhoneExists(phone, null);
    }

    private void insertGroupAdmin(SQLiteDatabase db) {
        //insert admin user
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP_ID, 1);
        values.put(COLUMN_ROLE, "groupadmin");
        values.put(COLUMN_FIRST_NAME, "Group Admin");
        values.put(COLUMN_LAST_NAME, "BMS");
        values.put(COLUMN_LAT, 0.0);
        values.put(COLUMN_LON, 0.0);
        values.put(COLUMN_ADDRESS1, "Bataan 2113");
        values.put(COLUMN_ADDRESS2, "Philippines");
        values.put(COLUMN_BARANGAY, "Imelda");
        values.put(COLUMN_MUNICIPALITY, "Samal");
        values.put(COLUMN_PROVINCE, "Bataan");
        values.put(COLUMN_BIRTH_DATE, "2024-01-01");
        values.put(COLUMN_GENDER, "male");
        values.put(COLUMN_ZIP_CODE, "2113");
        values.put(COLUMN_EMAIL, "groupadmin@bms.com");
        values.put(COLUMN_PHONE_NUMBER, "09123456781");
        values.put(COLUMN_EMERGENCY_CONTACT_NAME, "Admin");
        values.put(COLUMN_EMERGENCY_CONTACT_NO, "09123456789");
        String hashedPassword = BCrypt.hashpw("groupadmin", BCrypt.gensalt());
        values.put(COLUMN_PASSWORD, hashedPassword);
        values.put(COLUMN_STATUS, "active");
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, getCurrentDateTime());
        values.put(COLUMN_UPDATED_AT, getCurrentDateTime());
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FINGERPRINTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BIOMETRICS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIME_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANNOUNCEMENTS);
        onCreate(db);
    }
}