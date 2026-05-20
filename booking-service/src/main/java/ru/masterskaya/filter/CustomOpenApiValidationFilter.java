package ru.masterskaya.filter;

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

public class CustomOpenApiValidationFilter extends OpenApiValidationFilter {

    private final long maxBodySizeInBytes;

    public CustomOpenApiValidationFilter (String maxBodySize) {
        this.maxBodySizeInBytes = DataSize.parse(maxBodySize).toBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String contentType = request.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            long contentLength = request.getContentLengthLong();

            if (contentLength > maxBodySizeInBytes) {
                throw new MaxUploadSizeExceededException(maxBodySizeInBytes);
            }
        }

        // Если все хорошо, то передаем обернутый запрос дальше
        super.doFilterInternal(request, response, filterChain);
    }
}
