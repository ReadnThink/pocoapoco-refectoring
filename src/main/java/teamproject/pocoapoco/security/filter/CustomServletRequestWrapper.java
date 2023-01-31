package teamproject.pocoapoco.security.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class CustomServletRequestWrapper extends HttpServletRequestWrapper {

    private Map headerMap;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The request to wrap
     * @throws IllegalArgumentException if the request is null
     */
    public CustomServletRequestWrapper(HttpServletRequest request) {
        super(request);
        headerMap = new HashMap();
    }

    public void addHeader(String name, String value){
        headerMap.put(name, new String(value));
    }


    public Enumeration getHeaderNames(){

        HttpServletRequest request = (HttpServletRequest)getRequest();

        List list = new ArrayList();

        for( Enumeration e = request.getHeaderNames() ; e.hasMoreElements() ;)
            list.add(e.nextElement().toString());

        for( Iterator i = headerMap.keySet().iterator() ; i.hasNext() ;){
            list.add(i.next());
        }

        return Collections.enumeration(list);
    }

    public String getHeader(String name){
        Object value;
        if((value = headerMap.get(""+name)) != null)
            return value.toString();
        else
            return ((HttpServletRequest)getRequest()).getHeader(name);

    }
}
