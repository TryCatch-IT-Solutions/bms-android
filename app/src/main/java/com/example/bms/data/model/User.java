package com.example.bms.data.model;

public class User {

    private String userId;
    private String role;
    private String displayName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String password;
    private String phone;
    private long groupId;
    private String address1;
    private String address2;
    private String barangay;
    private String municipality;
    private String province;
    private String birthDate;
    private String gender;
    private String zipCode;
    private String emergencyContactName;
    private String emergencyContactNo;
    private String status;
    private int isSynced;

    public User(String userId, String displayName, String firstName, String middleName, String lastName, String email, String phone, String password, long groupId, String role,
                String address1, String address2, String barangay, String municipality, String province, String birthDate,
                String gender, String zipCode, String emergencyContactName, String emergencyContactNo, String status, int isSynced) {
        this.userId = userId;
        this.displayName = displayName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.groupId = groupId;
        this.role = role;
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.barangay = barangay;
        this.municipality = municipality;
        this.province = province;
        this.birthDate = birthDate;
        this.gender = gender;
        this.zipCode = zipCode;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactNo = emergencyContactNo;
        this.status = status;
        this.isSynced = isSynced;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getMiddleName() {
        return middleName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getEmail() {
        return email;
    }

    public long getGroupId() {
        return groupId;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getBarangay() {
        return barangay;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getProvince() {
        return province;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactNo() {
        return emergencyContactNo;
    }

    public String getStatus() {
        return status;
    }

    public int getIsSynced() {
        return isSynced;
    }
}