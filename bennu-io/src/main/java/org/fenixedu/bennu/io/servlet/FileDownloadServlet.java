package org.fenixedu.bennu.io.servlet;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.bennu.io.util.DownloadUtil;

import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Charsets;

@WebServlet(urlPatterns = FileDownloadServlet.SERVLET_PATH + "*")
public class FileDownloadServlet extends HttpServlet {
    private static final long serialVersionUID = -6697201298218837492L;

    static final String SERVLET_PATH = "/downloadFile/";

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final GenericFile file = getFileFromURL(request.getRequestURI());
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
        } else {
            if (request.getPathInfo().equals("/" + file.getExternalId())) {
                response.sendRedirect(getDownloadUrl(file));
                return;
            }
            if (file.isAccessible(Authenticate.getUser())) {
                DownloadUtil.downloadFile(file, request, response, "max-age=31536000");
            } else if (file.isPrivate() && !Authenticate.isLogged() && !"true".equals(request.getParameter("login_failed"))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.sendRedirect(sendLoginRedirect(file));
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "File not accessible");
            }
        }
    }

    public static String getDownloadUrl(GenericFile file) {
        return CoreConfiguration.getConfiguration().applicationUrl() + SERVLET_PATH + file.getExternalId() + "/"
                + file.getFilename();
    }

    private String sendLoginRedirect(final GenericFile file) throws IOException {
        return CoreConfiguration.getConfiguration().applicationUrl() + "/login?callback="
                + URLEncoder.encode(getDownloadUrl(file), Charsets.UTF_8.name());
    }

    public static GenericFile getFileFromURL(String url) {
        try {
            // Remove trailing path, and split the tokens
            String[] parts = url.substring(url.indexOf(SERVLET_PATH)).replace(SERVLET_PATH, "").split("\\/");
            if (parts.length == 0) {
                return null;
            }
            DomainObject object = FenixFramework.getDomainObject(parts[0]);
            if (object instanceof GenericFile && FenixFramework.isDomainObjectValid(object)) {
                return (GenericFile) object;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
