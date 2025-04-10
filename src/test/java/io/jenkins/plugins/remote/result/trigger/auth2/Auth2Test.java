package io.jenkins.plugins.remote.result.trigger.auth2;

import hudson.util.Secret;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class Auth2Test {

    @Test
    void testCredentialsAuthCloneBehaviour(JenkinsRule jenkinsRule) throws Exception {
        CredentialsAuth original = new CredentialsAuth();
        original.setCredentialsId("original");
        CredentialsAuth clone = (CredentialsAuth) original.clone();
        verifyEqualsHashCode(original, clone);

        //Test changing clone
        clone.setCredentialsId("changed");
        verifyEqualsHashCode(original, clone, false);
        assertEquals("original", original.getCredentialsId());
        assertEquals("changed", clone.getCredentialsId());
    }

    @Test
    void testTokenAuthCloneBehaviour(JenkinsRule jenkinsRule) throws Exception {
        TokenAuth original = new TokenAuth();
        original.setApiToken(Secret.fromString("original"));
        original.setUserName("original");
        TokenAuth clone = (TokenAuth) original.clone();
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
    void testNoneAuthCloneBehaviour(JenkinsRule jenkinsRule) throws Exception {
        NoneAuth original = NoneAuth.INSTANCE;
        NoneAuth clone = (NoneAuth) original.clone();
        verifyEqualsHashCode(original, clone);
    }

    @Test
    void testNoneAuthEqualsWithNull(JenkinsRule jenkinsRule) {
        NoneAuth original = new NoneAuth();
        assertNotEquals(null, original);
    }

    private void verifyEqualsHashCode(Auth2 original, Auth2 clone) {
        verifyEqualsHashCode(original, clone, true);
    }

    private void verifyEqualsHashCode(Auth2 original, Auth2 clone, boolean expectToBeSame) {
        assertNotEquals(System.identityHashCode(original), System.identityHashCode(clone), "Still same object after clone");
        if (expectToBeSame) {
            assertEquals(clone, original, "clone not equals() original");
            assertEquals(original.hashCode(), clone.hashCode(), "clone has different hashCode() than original");
        } else {
            assertNotEquals(clone, original, "clone still equals() original");
            assertNotEquals(original.hashCode(), clone.hashCode(), "clone still has same hashCode() than original");
        }
    }
}
