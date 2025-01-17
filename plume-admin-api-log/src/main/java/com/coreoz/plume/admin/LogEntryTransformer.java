package com.coreoz.plume.admin;

import com.coreoz.plume.admin.services.logapi.LogInterceptApiBean;
import okhttp3.Request;
import okhttp3.Response;

import java.util.function.Predicate;

/**
 * Represent an operation that transforms the trace generated by {@link OkHttpLoggerInterceptor}
 * <p>
 * This is a functional interface</a>
 * whose functional method is {@link #transform(Request, Response, LogInterceptApiBean)}.
 * <p>
 * The static method {@link #limitBodySizeTransformer(int)} transforms the body of either the request or the response
 * to be truncated at a given limit
 */
@FunctionalInterface
public interface LogEntryTransformer {
    LogInterceptApiBean transform(Request request, Response response, LogInterceptApiBean trace);

    default LogEntryTransformer andApply(LogEntryTransformer otherTransformerToApplyAfter) {
        return (request, response, apiTrace) -> otherTransformerToApplyAfter
            .transform(request, response, transform(request, response, apiTrace));
    }

    default LogEntryTransformer applyOnlyToRequests(Predicate<Request> allowRequestPredicate) {
        return (request, response, apiTrace) ->
            allowRequestPredicate.test(request) ?
                transform(request, response, apiTrace)
                : apiTrace;
    }

    default LogEntryTransformer applyOnlyToResponses(Predicate<Response> allowResponsePredicate) {
        return (request, response, apiTrace) ->
            allowResponsePredicate.test(response) ?
                transform(request, response, apiTrace)
                : apiTrace;
    }

    default LogEntryTransformer applyOnlyToResponsesWithHeader(String headerName, String headerValue) {
        return applyOnlyToResponses(
            response -> OkHttpMatchers.matchResponseHeaders(response.headers(), headerName, headerValue)
        );
    }

    static LogEntryTransformer limitBodySizeTransformer(int bodyCharLengthLimit) {
        return (request, response, apiTrace) -> {
            if (bodyCharLengthLimit < 0) {
                return apiTrace;
            }
            if (apiTrace.getBodyRequest().length() > bodyCharLengthLimit) {
                apiTrace.setBodyRequest(apiTrace.getBodyRequest().substring(0, bodyCharLengthLimit));
            }
            if (apiTrace.getBodyResponse().length() > bodyCharLengthLimit) {
                apiTrace.setBodyResponse(apiTrace.getBodyResponse().substring(0, bodyCharLengthLimit));
            }
            return apiTrace;
        };
    }
}
