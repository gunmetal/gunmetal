package io.gunmetal.integration;

import io.gunmetal.BlackList;
import io.gunmetal.Module;
import io.gunmetal.Supplies;
import io.gunmetal.WhiteList;
import io.gunmetal.internal.ComponentTemplate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author rees.byars
 */
public class AccessibilityIntegrationTest {

    static String testValue = "test";

    @BlackList(BlackListedComponent.class) class Deny { }
    @WhiteList(WhiteListedComponent.class) class Grant { }

    @Module(notAccessibleFrom = Deny.class)
    static class BlackListModule { @Supplies String string = testValue; }

    @Module(onlyAccessibleFrom = Grant.class)
    static class WhiteListModule { @Supplies String string = testValue; }
    
    @Module
    static class UnrestrictedModule { @Supplies String string = testValue; }

    @Module(type = Module.Type.COMPONENT)
    interface BlackListedComponent { String string(); }

    @Module(type = Module.Type.COMPONENT)
    interface WhiteListedComponent { String string(); }

    interface BlackListedComponentFactory1 {
        BlackListedComponent create(BlackListModule testModule);
    }

    interface BlackListedComponentFactory2 {
        BlackListedComponent create(WhiteListModule testModule);
    }

    interface BlackListedComponentFactory3 {
        BlackListedComponent create(UnrestrictedModule testModule);
    }

    interface WhiteListedComponentFactory1 {
        WhiteListedComponent create(BlackListModule testModule);
    }

    interface WhiteListedComponentFactory2 {
        WhiteListedComponent create(WhiteListModule testModule);
    }

    @Test(expected = Exception.class)
    public void testBlackListDeny() {
        ComponentTemplate
                .build(BlackListedComponentFactory1.class)
                .create(new BlackListModule())
                .string();
    }

    @Test(expected = Exception.class)
    public void testWhiteListDeny() {
        ComponentTemplate
                .build(BlackListedComponentFactory2.class)
                .create(new WhiteListModule())
                .string();
    }

    @Test
    public void testBlackListGrant() {
        assertEquals(testValue,
                ComponentTemplate
                        .build(WhiteListedComponentFactory1.class)
                        .create(new BlackListModule())
                        .string());
    }

    @Test
    public void testWhiteListGrant() {
        assertEquals(testValue,
                ComponentTemplate
                        .build(WhiteListedComponentFactory2.class)
                        .create(new WhiteListModule())
                        .string());
    }

    @Test
    public void testUnrestrictedGrant() {
        assertEquals(testValue,
                ComponentTemplate
                        .build(BlackListedComponentFactory3.class)
                        .create(new UnrestrictedModule())
                        .string());
    }

}
