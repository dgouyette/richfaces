/**
 *
 */
package org.richfaces.theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.FacesException;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * @author leo
 * @author <a href="http://community.jboss.org/people/bleathem">Brian Leathem</a>
 *
 */
@ManagedBean
@ApplicationScoped
public class Pages {
    public static final String DEFAULT_TITLE_PATTERN = "<ui\\:param\\s+name=\"title\"\\s+value=\"([^\"]*)\"";
    private static final Pattern XHTML_PATTERN = Pattern.compile(".*\\.xhtml");
    private static final Pattern PARENT_FOLDER_PATTERN = Pattern.compile("(/.*/.*/).*\\.xhtml");
    private static final Pattern FOLDER_PATTERN = Pattern.compile(".*/$");
    private static final String EXAMPLE_PATH = "/examples";
    private Pattern titlePattern = compilePattern(DEFAULT_TITLE_PATTERN);
    private Map<String, List<PageDescriptionBean>> pageFolderMap;
    private List<PageDescriptionBean> indexPages;

    public Pattern compilePattern(String titlePattern) {
        return Pattern.compile(titlePattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }

    @PostConstruct
    public void init() {
        pageFolderMap = new HashMap<String, List<PageDescriptionBean>>();
        Set<String> resourcePaths = getExternalContext().getResourcePaths(EXAMPLE_PATH);
        indexPages = new ArrayList<PageDescriptionBean>(resourcePaths.size());
        for (Iterator<String> iterator = resourcePaths.iterator(); iterator.hasNext();) {
            String folderPath = iterator.next();
            File folder = new File(folderPath);
            if (FOLDER_PATTERN.matcher(folderPath).matches()) {
                pageFolderMap.put(folderPath, getPagesByPattern(XHTML_PATTERN, folderPath));
                indexPages.add(new PageDescriptionBean(folderPath + "index.jsf", folderPath));
            }
        }
        indexPages.addAll(getPagesByPattern(XHTML_PATTERN, EXAMPLE_PATH));
    }

    private ExternalContext getExternalContext() {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        ExternalContext externalContext = null;
        if (null != facesContext) {
            externalContext = facesContext.getExternalContext();
        }
        return externalContext;
    }

    public List<PageDescriptionBean> getXhtmlPages() {
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        Matcher m = PARENT_FOLDER_PATTERN.matcher(viewId);
        String path;
        if (m.find()) {
            path = m.group(1);
        } else {
            return indexPages;
        }
        return pageFolderMap.get(path);
    }

    /**
     *
     */
    private List<PageDescriptionBean> getPagesByPattern(Pattern pattern, String path) {
        List<PageDescriptionBean> pageList = new ArrayList<PageDescriptionBean>();
        Set<String> resourcePaths = getExternalContext().getResourcePaths(path);
        for (Iterator<String> iterator = resourcePaths.iterator(); iterator.hasNext();) {
            String page = iterator.next();
            if (pattern.matcher(page).matches() && ! page.endsWith("/index.xhtml")) {
                InputStream pageInputStream = getExternalContext().getResourceAsStream(page);
                String title = page;
                if (null != pageInputStream) {
                    byte[] head = new byte[1024];
                    try {
                        int readed = pageInputStream.read(head);
                        String headString = new String(head, 0, readed);
                        Matcher titleMatcher = titlePattern.matcher(headString);
                        if (titleMatcher.find() && titleMatcher.group(1).length() > 0) {
                            title = titleMatcher.group(1);
                        }
                    } catch (IOException e) {
                        throw new FacesException("can't read directory content", e);
                    } finally {
                        try {
                            pageInputStream.close();
                        } catch (IOException e) {
                            // ignore it.
                        }
                    }
                }
                pageList.add(new PageDescriptionBean(page, title));
            }
        }
        Collections.sort(pageList);
        return pageList;
    }

    /**
     * @param titlePattern the titlePattern to set
     */
    public void setTitlePattern(String titlePattern) {
        this.titlePattern = compilePattern(titlePattern);
    }

    /**
     * @return the titlePattern
     */
    public String getTitlePattern() {
        return titlePattern.toString();
    }
}
