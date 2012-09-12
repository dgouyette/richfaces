package org.richfaces.ui.iteration;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@SessionScoped
public class DataScrollerBean {
    private List<String> content;
    private int pageNumber = 1;

    public DataScrollerBean() {
        content = new ArrayList<String>();
        content.add("1 page content");
        content.add("2 page content");
        content.add("3 page content");
        content.add("4 page content");
        content.add("5 page content");
    }

    public List<String> getContent() {
        return content;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}
