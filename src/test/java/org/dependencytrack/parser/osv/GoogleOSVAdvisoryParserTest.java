package org.dependencytrack.parser.osv;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.dependencytrack.parser.osv.model.OSVAdvisory;
import org.dependencytrack.parser.osv.model.OSVVulnerability;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GoogleOSVAdvisoryParserTest {

    GoogleOSVAdvisoryParser parser = new GoogleOSVAdvisoryParser();

    @Test
    public void testTrimSummary() {

        String osvLongSummary = "In uvc_scan_chain_forward of uvc_driver.c, there is a possible linked list corruption due to an unusual root cause. This could lead to local escalation of privilege in the kernel with no additional execution privileges needed. User interaction is not needed for exploitation.";
        String trimmedSummary = parser.trimSummary(osvLongSummary);
        Assert.assertNotNull(trimmedSummary);
        Assert.assertEquals(255, trimmedSummary.length());
        Assert.assertEquals("In uvc_scan_chain_forward of uvc_driver.c, there is a possible linked list corruption due to an unusual root cause. This could lead to local escalation of privilege in the kernel with no additional execution privileges needed. User interaction is not ne..", trimmedSummary);

        osvLongSummary = "I'm a short Summary";
        trimmedSummary = parser.trimSummary(osvLongSummary);
        Assert.assertNotNull(trimmedSummary);
        Assert.assertEquals("I'm a short Summary", trimmedSummary);

        osvLongSummary = null;
        trimmedSummary = parser.trimSummary(osvLongSummary);
        Assert.assertNull(trimmedSummary);
    }

    @Test
    public void testVulnerabilityRangeEmpty() throws IOException {

        String jsonFile = "src/test/resources/unit/osv.jsons/osv-vulnerability-no-range.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonObject = new JSONObject(jsonString);
        final JSONArray vulnerabilities = jsonObject.optJSONArray("affected");
        List<OSVVulnerability> osvVulnerabilityList = parser.parseVulnerabilityRange(vulnerabilities.getJSONObject(0));
        Assert.assertNotNull(osvVulnerabilityList);
        Assert.assertEquals(1, osvVulnerabilityList.size());
    }

    @Test
    public void testVulnerabilityRangeSingle() throws IOException {

        String jsonFile = "src/test/resources/unit/osv.jsons/osv-vulnerability-with-ranges.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonObject = new JSONObject(jsonString);
        final JSONArray vulnerabilities = jsonObject.optJSONArray("affected");
        List<OSVVulnerability> osvVulnerabilityList = parser.parseVulnerabilityRange(vulnerabilities.getJSONObject(1));
        Assert.assertNotNull(osvVulnerabilityList);
        Assert.assertEquals(1, osvVulnerabilityList.size());
        OSVVulnerability vuln = osvVulnerabilityList.get(0);
        Assert.assertEquals("pkg:maven/org.springframework.security.oauth/spring-security-oauth", vuln.getPurl());
        Assert.assertEquals("0", vuln.getLowerVersionRange());
        Assert.assertEquals("2.0.17", vuln.getUpperVersionRange());
        Assert.assertEquals("Maven", vuln.getPackageEcosystem());

    }

    @Test
    public void testVulnerabilityRangeMultiple() throws IOException {

        String jsonFile = "src/test/resources/unit/osv.jsons/osv-vulnerability-with-ranges.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonObject = new JSONObject(jsonString);
        final JSONArray vulnerabilities = jsonObject.optJSONArray("affected");

        // range test full pairs
        List<OSVVulnerability> osvVulnerabilityList = parser.parseVulnerabilityRange(vulnerabilities.getJSONObject(2));
        Assert.assertNotNull(osvVulnerabilityList);
        Assert.assertEquals(3, osvVulnerabilityList.size());
        Assert.assertEquals("1", osvVulnerabilityList.get(0).getLowerVersionRange());
        Assert.assertEquals("2", osvVulnerabilityList.get(0).getUpperVersionRange());
        Assert.assertEquals("3", osvVulnerabilityList.get(1).getLowerVersionRange());
        Assert.assertEquals("4", osvVulnerabilityList.get(1).getUpperVersionRange());

        // range test half pairs
        osvVulnerabilityList = parser.parseVulnerabilityRange(vulnerabilities.getJSONObject(3));
        Assert.assertNotNull(osvVulnerabilityList);
        Assert.assertEquals(osvVulnerabilityList.size(), 3);
        Assert.assertEquals(null, osvVulnerabilityList.get(0).getLowerVersionRange());
        Assert.assertEquals("2", osvVulnerabilityList.get(0).getUpperVersionRange());
        Assert.assertEquals("3", osvVulnerabilityList.get(1).getLowerVersionRange());
        Assert.assertEquals(null, osvVulnerabilityList.get(1).getUpperVersionRange());
        Assert.assertEquals("4", osvVulnerabilityList.get(2).getLowerVersionRange());
        Assert.assertEquals("5", osvVulnerabilityList.get(2).getUpperVersionRange());
    }

    @Test
    public void testParseOSVJson() throws IOException {

        String jsonFile = "src/test/resources/unit/osv.jsons/osv-GHSA-77rv-6vfw-x4gc.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonObject = new JSONObject(jsonString);
        OSVAdvisory advisory = parser.parse(jsonObject);
        Assert.assertNotNull(advisory);
        Assert.assertEquals("GHSA-77rv-6vfw-x4gc", advisory.getId());
        Assert.assertEquals("LOW", advisory.getSeverity());
        Assert.assertEquals(1, advisory.getCweIds().size());
        Assert.assertEquals(6, advisory.getReferences().size());
        Assert.assertEquals(2, advisory.getCredits().size());
        Assert.assertEquals(8, advisory.getVulnerabilities().size());
        Assert.assertEquals("CVSS:3.1/AV:N/AC:L/PR:L/UI:R/S:C/C:H/I:H/A:H", advisory.getCvssV3Vector());
        Assert.assertEquals("CVE-2019-3778", advisory.getAliases().get(0));
        Assert.assertEquals("2022-06-09T07:01:32.587163Z", advisory.getModified().toString());
    }

    @Test
    public void testCommitHashRanges() throws IOException {

        String jsonFile = "src/test/resources/unit/osv.jsons/osv-git-commit-hash-ranges.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonFile)));
        JSONObject jsonObject = new JSONObject(jsonString);
        OSVAdvisory advisory = parser.parse(jsonObject);
        Assert.assertNotNull(advisory);
        Assert.assertEquals("OSV-2021-1820", advisory.getId());
        Assert.assertEquals(22, advisory.getVulnerabilities().size());
        Assert.assertEquals("4.4.0", advisory.getVulnerabilities().get(0).getVersion());
    }
}
