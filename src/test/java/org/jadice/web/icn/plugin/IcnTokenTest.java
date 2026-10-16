package org.jadice.web.icn.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IcnTokenTest {

    /** Shared with IcnTokenCodecTest in jadice web toolkit (addons/icn-integration); both must stay identical. */
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String EXPECTED =
            "eyJ2IjoxLCJvcyI6IntPU30iLCJkb2MiOiJ7RE9DfSIsInciOnRydWUsImV4cCI6MTc2MDYxNjAwMH0.ypU2RByNGC37qXcmSPMXcA-NfZ8O0HPw-7TnlWAOZGs";

    @Test
    void producesTheSharedTestVector() {
        assertEquals(EXPECTED, IcnToken.create(SECRET, "{OS}", "{DOC}", true, 1760616000L));
    }

    @Test
    void isUrlSafeWithoutPadding() {
        String token = IcnToken.create(SECRET, "{OS}", "{DOC}", false, 1760616001L);

        assertTrue(token.matches("[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+"), token);
    }

    @Test
    void p8IdsAreTheLastTwoCommaSeparatedPartsOfTheIcnDocumentId() {
        assertEquals("{OS}|{DOC}", String.join("|", JWTViewerPluginService.p8IdsOf("Document,{OS},{DOC}")));
        assertEquals("{OS}|{DOC}", String.join("|", JWTViewerPluginService.p8IdsOf("Invoice,{OS},{DOC}")));
        assertEquals("{OS}|{DOC}", String.join("|", JWTViewerPluginService.p8IdsOf("{OS},{DOC}")));
        assertEquals(null, JWTViewerPluginService.p8IdsOf("{DOC}"));
        assertEquals(null, JWTViewerPluginService.p8IdsOf(null));
    }

    @Test
    void escapesQuotesAndBackslashesInIds() {
        String token = IcnToken.create(SECRET, "a\"b", "c\\d", true, 1L);
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]), java.nio.charset.StandardCharsets.UTF_8);

        assertEquals("{\"v\":1,\"os\":\"a\\\"b\",\"doc\":\"c\\\\d\",\"w\":true,\"exp\":1}", payload);
    }
}
