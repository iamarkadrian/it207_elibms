package edu.bulsu.lms.domain;

import java.util.regex.Pattern;

/** A postal address. Immutable; every part is validated when the record is created. */
public record Address(String houseNo, String street, String barangay, String municipality, String province) {

    private static final Pattern HOUSE_NO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 /.-]*");

    public Address {
        houseNo = requireHouseNo(houseNo);
        street = requireStreet(street);
        barangay = requireBarangay(barangay);
        municipality = requireMunicipality(municipality);
        province = requireProvince(province);
    }

    // One validator per part, so a form can check each box as soon as it is filled in.

    public static String requireHouseNo(String raw) {
        return Check.matching(raw, "House number", 10, HOUSE_NO, "may only contain letters, digits, spaces, '/', '.' and '-'.");
    }

    public static String requireStreet(String raw) {
        return Check.text(raw, "Street", 60);
    }

    public static String requireBarangay(String raw) {
        return Check.text(raw, "Barangay", 40);
    }

    public static String requireMunicipality(String raw) {
        return Check.text(raw, "City or town", 40);
    }

    public static String requireProvince(String raw) {
        return Check.text(raw, "Province", 40);
    }

    /** "221 Paraluman St." */
    public String streetLine() {
        return houseNo + " " + street;
    }

    /** "San Mateo, Baliwag, Bulacan" */
    public String areaLine() {
        return barangay + ", " + municipality + ", " + province;
    }
}
