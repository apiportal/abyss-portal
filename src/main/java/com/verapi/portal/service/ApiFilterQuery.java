/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 6 2018
 *
 */

package com.verapi.portal.service;

import com.verapi.abyss.common.Constants;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApiFilterQuery {
    private String filterQuery = "";
    private JsonArray filterQueryParams;

    public ApiFilterQuery() {
    }

    public ApiFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public ApiFilterQuery setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
        return this;
    }

    public ApiFilterQuery addFilterQuery(String filterQuery) {
        if (!this.filterQuery.toLowerCase().endsWith("\n"))
            this.filterQuery = this.filterQuery + "\n";
        if (!this.filterQuery.toLowerCase().contains("where"))
            filterQuery = "where\n" + filterQuery;
        else
            filterQuery = "and\n" + filterQuery;
        if (!filterQuery.toLowerCase().endsWith("\n"))
            filterQuery = filterQuery + "\n";
        this.filterQuery = this.filterQuery + filterQuery;
        return this;
    }

    public JsonArray getFilterQueryParams() {
        return filterQueryParams;
    }

    public ApiFilterQuery setFilterQueryParams(JsonArray filterQueryParams) {
        this.filterQueryParams = filterQueryParams;
        return this;
    }

    public ApiFilterQuery addFilterQueryParams(JsonArray filterQueryParams) {
        if (this.filterQueryParams == null)
            this.filterQueryParams = new JsonArray();
        filterQueryParams.forEach(o -> this.filterQueryParams.add(o));
        return this;
    }

    public static final class APIFilter {
        JsonObject apiFilter = new JsonObject();

        public APIFilter(String apiFilterByNameQuery, String apiFilterLikeNameQuery) {
            apiFilter.put(Constants.RESTAPI_FILTERING_BY_NAME, apiFilterByNameQuery);
            apiFilter.put(Constants.RESTAPI_FILTERING_LIKE_NAME, apiFilterLikeNameQuery);
        }

        public APIFilter setApiFilterByNameQuery(String apiFilterByNameQuery) {
            apiFilter.put(Constants.RESTAPI_FILTERING_BY_NAME, "");
            return this;
        }

        public String getApiFilterByNameQuery() {
            return apiFilter.getString(Constants.RESTAPI_FILTERING_BY_NAME);
        }

        public APIFilter setApiFilterLikeNameQuery(String apiFilterLikeNameQuery) {
            apiFilter.put(Constants.RESTAPI_FILTERING_LIKE_NAME, "");
            return this;
        }

        public String getApiFilterLikeNameQuery() {
            return apiFilter.getString(Constants.RESTAPI_FILTERING_LIKE_NAME);
        }

    }

}
