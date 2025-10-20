package gov.nist.oscal.tools.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SwaggerUiCssInjectionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only inject CSS for Swagger UI HTML pages
        if (httpRequest.getRequestURI().contains("/swagger-ui") &&
            httpRequest.getRequestURI().endsWith("html")) {

            // Use a response wrapper to capture and modify the output
            ResponseWrapper responseWrapper = new ResponseWrapper(httpResponse);
            chain.doFilter(request, responseWrapper);

            String html = responseWrapper.toString();

            // Inject custom CSS link before </head>
            if (html.contains("</head>")) {
                String cssLink = "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/swagger-custom.css\" />\n";
                html = html.replace("</head>", cssLink + "  </head>");
            }

            byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
            httpResponse.setContentLength(htmlBytes.length);
            httpResponse.getOutputStream().write(htmlBytes);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class ResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final java.io.ByteArrayOutputStream buffer;
        private final jakarta.servlet.ServletOutputStream outputStream;
        private final java.io.PrintWriter writer;

        public ResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
            buffer = new java.io.ByteArrayOutputStream();
            outputStream = new jakarta.servlet.ServletOutputStream() {
                @Override
                public void write(int b) {
                    buffer.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(jakarta.servlet.WriteListener listener) {
                }
            };
            writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public java.io.PrintWriter getWriter() {
            return writer;
        }

        @Override
        public String toString() {
            writer.flush();
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
