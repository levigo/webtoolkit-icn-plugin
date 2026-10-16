package org.jadice.web.icn.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.filenet.api.constants.AccessRight;

class IcnAccessTest {

    private static final String DOC_URL = "/navigator/jaxrs/p8/getDocument?docid=Document%2C%7BOS%7D%2C%7BDOC%7D"
            + "&template_name=Document&repositoryId=P8Repository&vsId=%7BVS%7D&objectStoreName=P8ObjectStore"
            + "&security_token=-123";

    @Test
    void readsDecodedQueryParametersFromTheDocUrl() {
        assertEquals("P8Repository", IcnAccess.queryParam(DOC_URL, "repositoryId"));
        assertEquals("{VS}", IcnAccess.queryParam(DOC_URL, "vsId"));
        assertEquals("Document,{OS},{DOC}", IcnAccess.queryParam(DOC_URL, "docid"));
        assertNull(IcnAccess.queryParam(DOC_URL, "missing"));
        assertNull(IcnAccess.queryParam(null, "repositoryId"));
        assertNull(IcnAccess.queryParam("/no/query", "repositoryId"));
    }

    @Test
    void annotatingNeedsTheLinkRightOnTheDocument() {
        assertTrue(IcnAccess.mayAnnotate(AccessRight.LINK_AS_INT | AccessRight.READ_AS_INT));
        assertFalse(IcnAccess.mayAnnotate(AccessRight.READ_AS_INT | AccessRight.VIEW_CONTENT_AS_INT));
        assertFalse(IcnAccess.mayAnnotate(0));
    }
}
