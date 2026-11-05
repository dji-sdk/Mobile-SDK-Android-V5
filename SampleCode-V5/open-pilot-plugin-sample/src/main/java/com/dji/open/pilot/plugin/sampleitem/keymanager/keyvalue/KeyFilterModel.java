package com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** 主线程提交和发布，后台只读不可变请求并生成结果。 */
final class KeyFilterModel<T> {

    interface IdentifierProvider<T> {
        String getIdentifier(T item);
    }

    private final IdentifierProvider<T> identifierProvider;
    private List<T> visibleItems;
    private volatile Request<T> latestRequest;
    private long version;
    private String publishedToken;

    KeyFilterModel(List<T> source, IdentifierProvider<T> identifierProvider) {
        this.visibleItems = snapshotOf(source);
        this.identifierProvider = identifierProvider;
    }

    String submit(List<T> source, String query) {
        String token = Long.toString(++version);
        latestRequest = new Request<>(token, snapshotOf(source), query == null ? "" : query);
        return token;
    }

    Result<T> filter(String token) {
        Request<T> request = latestRequest;
        // 排队期间被替换的请求不能拿新快照计算旧查询。
        if (request == null || !request.token.equals(token)) {
            return new Result<>(token, Collections.emptyList());
        }
        if (request.query.isEmpty()) {
            return new Result<>(token, request.source);
        }
        List<T> filteredItems = new ArrayList<>();
        Pattern pattern = buildFuzzyPattern(request.query);
        for (T item : request.source) {
            if (pattern.matcher(identifierProvider.getIdentifier(item)).find()) {
                filteredItems.add(item);
            }
        }
        return new Result<>(token, Collections.unmodifiableList(filteredItems));
    }

    boolean publish(Result<T> filtered) {
        if (!isCurrent(filtered.token)) {
            return false;
        }
        visibleItems = filtered.items;
        publishedToken = filtered.token;
        return true;
    }

    boolean isPublished(String token) {
        return isCurrent(token) && token.equals(publishedToken);
    }

    private boolean isCurrent(String token) {
        Request<T> request = latestRequest;
        return request != null && request.token.equals(token);
    }

    List<T> getVisibleItems() {
        return visibleItems;
    }

    private static <T> List<T> snapshotOf(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static Pattern buildFuzzyPattern(String query) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < query.length(); index++) {
            if (builder.length() > 0) {
                builder.append(".*");
            }
            builder.append(Pattern.quote(String.valueOf(query.charAt(index))));
        }
        return Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static final class Request<T> {
        private final String token;
        private final List<T> source;
        private final String query;

        private Request(String token, List<T> source, String query) {
            this.token = token;
            this.source = source;
            this.query = query;
        }
    }

    static final class Result<T> {
        private final String token;
        private final List<T> items;

        private Result(String token, List<T> items) {
            this.token = token;
            this.items = items;
        }

        List<T> getItems() {
            return items;
        }

        int getCount() {
            return items.size();
        }
    }
}
