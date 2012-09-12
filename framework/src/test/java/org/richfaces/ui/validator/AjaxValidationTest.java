package org.richfaces.ui.validator;

import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;

/**
 * Test for dynamic add/remove {@link org.richfaces.ui.core.UIScripts} as view resource.
 *
 * @author asmirnov
 *
 */
public class AjaxValidationTest extends ValidatorIntegrationTestBase {

    protected String getFacesConfig() {
        return "faces-config.xml";
    }

    protected String getPageName() {
        return "test";
    }

    @Override
    protected String getResourcePath() {
        return "org/richfaces/ui/ajax/";
    }

    @Test
    public void testRequest() throws Exception {
        HtmlPage page = requestPage();
        HtmlInput input = getInput(page);
        assertNotNull(input);
    }

    @Test
    public void testSubmitTooShortValue() throws Exception {
        submitValueAndCheckMessage("", not(equalTo("")));
    }

    @Test
    public void testSubmitTooLongValue() throws Exception {
        submitValueAndCheckMessage("123456", not(equalTo("")));
    }

    @Test
    public void testSubmitProperValue() throws Exception {
        submitValueAndCheckMessage("ab", equalTo(""));
    }

}
