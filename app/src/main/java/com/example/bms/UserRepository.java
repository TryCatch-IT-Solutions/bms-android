package com.example.bms;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.bms.data.LoginDataSource;
import com.example.bms.data.model.LoggedInUser;

public class UserRepository {

    private final DatabaseHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void updateUserGroupByEmail(String email, long groupId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_GROUP_ID, groupId);
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, 0);
        db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_EMAIL + " = ?", new String[]{email});
        db.close();
    }

    public void resetUsersTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //delete all users
        db.delete(DatabaseHelper.TABLE_USERS, null, null);
        db.delete(DatabaseHelper.TABLE_BIOMETRICS, null, null);
        db.delete(DatabaseHelper.TABLE_FINGERPRINTS, null, null);
        //reset autoincrement
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_USERS + "'");
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_BIOMETRICS + "'");
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + DatabaseHelper.TABLE_FINGERPRINTS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_USERS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_BIOMETRICS + "'");
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='" + DatabaseHelper.TABLE_FINGERPRINTS + "'");
        db.close();
    }

    public void deleteUser(long userId,Context context) {
        LoginDataSource loginDataSource = new LoginDataSource(context);
        LoggedInUser data = loginDataSource.getUserData(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //update deleted at
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DELETED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_DELETED_BY, data.getEmail());

        values.put(DatabaseHelper.COLUMN_IS_SYNCED, 0);
        db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(userId)});
        db.close();
    }

    public long updateUser(long userId, long groupId, String firstName, String middleName, String lastName, String address1, String address2, String barangay,
                           String municipality, String province, String birthDate, String gender, int zipCode, double lon, double lat,
                           String email, String phone, String emergencyContactNo, String emergencyContactName, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_GROUP_ID, groupId);
        values.put(DatabaseHelper.COLUMN_FIRST_NAME, firstName);
        values.put(DatabaseHelper.COLUMN_MIDDLE_NAME, middleName);
        values.put(DatabaseHelper.COLUMN_LAST_NAME, lastName);
        values.put(DatabaseHelper.COLUMN_ADDRESS1, address1);
        values.put(DatabaseHelper.COLUMN_ADDRESS2, address2);
        values.put(DatabaseHelper.COLUMN_BARANGAY, barangay);
        values.put(DatabaseHelper.COLUMN_MUNICIPALITY, municipality);
        values.put(DatabaseHelper.COLUMN_PROVINCE, province);
        values.put(DatabaseHelper.COLUMN_BIRTH_DATE, birthDate);
        values.put(DatabaseHelper.COLUMN_GENDER, gender.toLowerCase());
        values.put(DatabaseHelper.COLUMN_ZIP_CODE, zipCode);
        values.put(DatabaseHelper.COLUMN_LON, lon);
        values.put(DatabaseHelper.COLUMN_LAT, lat);
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, phone);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME, emergencyContactName);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO, emergencyContactNo);
        values.put(DatabaseHelper.COLUMN_ROLE, role);
        values.put(DatabaseHelper.COLUMN_STATUS, "active");
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, false);
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());

        long rowsAffected = db.update(DatabaseHelper.TABLE_USERS, values, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(userId)});
        db.close();
        return userId;
    }

    public long insertSyncUser(long groupId, String firstName, String middleName, String lastName, String address1, String address2, String barangay, String municipality, String province, String birthDate, String gender, int zipCode, double lon, double lat, String email, String phone, String emergencyContactNo,String emergencyContactName, String role, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_GROUP_ID, groupId);
        values.put(DatabaseHelper.COLUMN_FIRST_NAME, firstName);
        values.put(DatabaseHelper.COLUMN_MIDDLE_NAME, middleName == null ? "" : middleName);
        values.put(DatabaseHelper.COLUMN_LAST_NAME, lastName);
        values.put(DatabaseHelper.COLUMN_ADDRESS1, address1);
        values.put(DatabaseHelper.COLUMN_ADDRESS2, address2);
        values.put(DatabaseHelper.COLUMN_BARANGAY, barangay);
        values.put(DatabaseHelper.COLUMN_MUNICIPALITY, municipality);
        values.put(DatabaseHelper.COLUMN_PROVINCE, province);
        values.put(DatabaseHelper.COLUMN_BIRTH_DATE, birthDate);
        values.put(DatabaseHelper.COLUMN_GENDER, gender.toLowerCase());
        values.put(DatabaseHelper.COLUMN_ZIP_CODE, zipCode);
        values.put(DatabaseHelper.COLUMN_LON, lon);
        values.put(DatabaseHelper.COLUMN_LAT, lat);
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, phone);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME, emergencyContactName);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO, emergencyContactNo);
        values.put(DatabaseHelper.COLUMN_ROLE, role);
        values.put(DatabaseHelper.COLUMN_STATUS, "active");
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, false);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);
        values.put((DatabaseHelper.COLUMN_IS_SYNCED), true);

        long id = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public long insertUser(long groupId, String firstName, String middleName, String lastName, String address1, String address2, String barangay, String municipality, String province, String birthDate, String gender, int zipCode, double lon, double lat, String email, String phone, String emergencyContactNo,String emergencyContactName, String role, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_GROUP_ID, groupId);
        values.put(DatabaseHelper.COLUMN_FIRST_NAME, firstName);
        values.put(DatabaseHelper.COLUMN_MIDDLE_NAME, middleName);
        values.put(DatabaseHelper.COLUMN_LAST_NAME, lastName);
        values.put(DatabaseHelper.COLUMN_ADDRESS1, address1);
        values.put(DatabaseHelper.COLUMN_ADDRESS2, address2);
        values.put(DatabaseHelper.COLUMN_BARANGAY, barangay);
        values.put(DatabaseHelper.COLUMN_MUNICIPALITY, municipality);
        values.put(DatabaseHelper.COLUMN_PROVINCE, province);
        values.put(DatabaseHelper.COLUMN_BIRTH_DATE, birthDate);
        values.put(DatabaseHelper.COLUMN_GENDER, gender.toLowerCase());
        values.put(DatabaseHelper.COLUMN_ZIP_CODE, zipCode);
        values.put(DatabaseHelper.COLUMN_LON, lon);
        values.put(DatabaseHelper.COLUMN_LAT, lat);
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        values.put(DatabaseHelper.COLUMN_PHONE_NUMBER, phone);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NAME, emergencyContactName);
        values.put(DatabaseHelper.COLUMN_EMERGENCY_CONTACT_NO, emergencyContactNo);
        values.put(DatabaseHelper.COLUMN_ROLE, role);
        values.put(DatabaseHelper.COLUMN_STATUS, "active");
        values.put(DatabaseHelper.COLUMN_IS_SYNCED, false);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_UPDATED_AT, dbHelper.getCurrentDateTime());
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);

        long id = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public long updateUser(long userId, long groupId, String firstName, String middleName, String lastName, String address1, String address2, String barangay, String municipality, String province, String birthDate, String gender, int zipCode, double lon, double lat, String email, String phone, String emergencyContactNo, String emergencyContactName) {
        return updateUser(userId,groupId, firstName, middleName, lastName, address1, address2, barangay, municipality, province, birthDate, gender, zipCode, lon, lat, email, phone, emergencyContactNo, emergencyContactName,"employee"); // Call the overloaded method with the default value
    }


    public long insertUser(long groupId, String firstName, String middleName, String lastName, String address1, String address2, String barangay, String municipality, String province, String birthDate, String gender, int zipCode, double lon, double lat, String email, String phone, String emergencyContactNo, String emergencyContactName) {
        return insertUser(groupId, firstName, middleName, lastName, address1, address2, barangay, municipality, province, birthDate, gender, zipCode, lon, lat, email, phone, emergencyContactNo, emergencyContactName,"employee",null); // Call the overloaded method with the default value
    }

}