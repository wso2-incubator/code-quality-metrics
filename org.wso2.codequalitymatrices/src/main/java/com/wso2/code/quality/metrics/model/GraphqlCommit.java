/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wso2.code.quality.metrics.model;

import com.google.gson.annotations.SerializedName;

/**
 * Pojo class used for parsing JSON response received from github graphql API.
 *
 * @since 1.0.0
 */
public class GraphqlCommit {
    @SerializedName("author")
    private GraphqlAuthor author;
    @SerializedName("history")
    private GraphqlHistory history;
    @SerializedName("url")
    private String url;

    public GraphqlAuthor getAuthor() {
        return author;
    }

    public void setAuthor(GraphqlAuthor author) {
        this.author = author;
    }

    public GraphqlHistory getHistory() {
        return history;
    }

    public void setHistory(GraphqlHistory history) {
        this.history = history;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
