package com.itfsw.remote.result.trigger.auth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.util.Secret;

public class Auth2Test {
	
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testCredentialsAuthCloneBehaviour() throws CloneNotSupportedException {
        CredentialsAuth original = new CredentialsAuth();
        original.setCredentials("original");
        CredentialsAuth clone = (CredentialsAuth)original.clone();
        verifyEqualsHashCode(original, clone);

        //Test changing clone
        clone.setCredentials("changed");
        verifyEqualsHashCode(original, clone, false);
        assertEquals("original", original.getCredentials());
        assertEquals("changed", clone.getCredentials());
    }

    @Test
    public void testTokenAuthCloneBehaviour() throws CloneNotSupportedException {
        TokenAuth original = new TokenAuth();
        original.setApiToken(Secret.fromString("original"));
        original.setUserName("original");
        TokenAuth clone = (TokenAuth)original.clone();
        verifyEqualsHashCode(original, clone);

        //Test changing clone
        clone.setApiToken(Secret.fromString("changed"));
        clone.setUserName("changed");
        verifyEqualsHashCode(original, clone, false);
        assertEquals("original", original.getApiToken().getPlainText());
        assertEquals("original", original.getUserName());
        assertEquals("changed", clone.getApiToken().getPlainText());
        assertEquals("changed", clone.getUserName());
    }

    @Test
    public void testNoneAuthCloneBehaviour() throws CloneNotSupportedException {
        NoneAuth original = NoneAuth.INSTANCE;
        NoneAuth clone = (NoneAuth)original.clone();
        verifyEqualsHashCode(original, clone);
    }

    @Test
    public void testNoneAuthEqualsWithNull() throws CloneNotSupportedException {
        NoneAuth original = new NoneAuth();
        assertFalse(original.equals(null));
    }

    private void verifyEqualsHashCode(Auth2 original, Auth2 clone) throws CloneNotSupportedException {
        verifyEqualsHashCode(original, clone, true);
    }

    private void verifyEqualsHashCode(Auth2 original, Auth2 clone, boolean expectToBeSame) throws CloneNotSupportedException {
        assertNotEquals("Still same object after clone", System.identityHashCode(original), System.identityHashCode(clone));
        if(expectToBeSame) {
            assertTrue("clone not equals() original", clone.equals(original));
            assertEquals("clone has different hashCode() than original", original.hashCode(), clone.hashCode());
        } else {
            assertFalse("clone still equals() original", clone.equals(original));
            assertNotEquals("clone still has same hashCode() than original", original.hashCode(), clone.hashCode());
        }
    }
}
