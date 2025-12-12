package io.github.cactric.swalsh;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WifiUtilsValidationTests {
    @Test
    public void validationTest1() {
        String ssid = "switch_6CA04F0100J";
        String pass = "qvk22ssf";
        int res = WifiUtils.strictValidate(ssid, pass);
        assertEquals(0, res);
        int looseRes = WifiUtils.looseValidate(ssid, pass);
        assertEquals(0, looseRes);
    }

    @Test
    public void validationTest2() {
        String badSsid = "button_BEEFACECAFE";
        String pass = "qvk22ssf";
        int res = WifiUtils.strictValidate(badSsid, pass);
        assertEquals(R.string.bad_prefix, res);
        int looseRes = WifiUtils.looseValidate(badSsid, pass);
        assertEquals(0, looseRes);
    }

    @Test
    public void validationTest3() {
        String ssid = "switch_6CA04F0100J";
        String badPass = "octo expansion";
        int res = WifiUtils.strictValidate(ssid, badPass);
        assertEquals(R.string.bad_password, res);
        int looseRes = WifiUtils.looseValidate(ssid, badPass);
        assertEquals(0, looseRes);
    }

    @Test
    public void validationTest4() {
        String ssid = "";
        String pass = "qvk22ssf";
        int strictRes = WifiUtils.strictValidate(ssid, pass);
        assertEquals(R.string.bad_prefix, strictRes);
        int looseRes = WifiUtils.looseValidate(ssid, pass);
        assertEquals(R.string.ssid_required, looseRes);
    }

    @Test
    public void validationTest5() {
        String ssid = "switch_6CA04F0100J";
        String pass = "";
        int strictRes = WifiUtils.strictValidate(ssid, pass);
        assertEquals(R.string.bad_password, strictRes);
        int looseRes = WifiUtils.looseValidate(ssid, pass);
        assertEquals(R.string.password_required, looseRes);
    }
}
