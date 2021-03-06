/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch5x.query;

import java.io.IOException;
import java.util.Objects;
import org.apache.skywalking.oap.server.core.alarm.AlarmRecord;
import org.apache.skywalking.oap.server.core.query.entity.*;
import org.apache.skywalking.oap.server.core.source.Scope;
import org.apache.skywalking.oap.server.core.storage.query.IAlarmQueryDAO;
import org.apache.skywalking.oap.server.library.util.StringUtils;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch5x.base.*;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch5x.client.ElasticSearchClient5x;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * @author peng-yongsheng
 */
public class AlarmQueryEsDAO extends EsDAO implements IAlarmQueryDAO {

    public AlarmQueryEsDAO(ElasticSearchClient5x client) {
        super(client);
    }

    public Alarms getAlarm(final Scope scope, final String keyword, final int limit, final int from, final long startTB,
        final long endTB) throws IOException {
        SearchSourceBuilder sourceBuilder = SearchSourceBuilder.searchSource();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must().add(QueryBuilders.rangeQuery(AlarmRecord.TIME_BUCKET).gte(startTB).lte(endTB));

        if (Objects.nonNull(scope)) {
            boolQueryBuilder.must().add(QueryBuilders.termQuery(AlarmRecord.SCOPE, scope.ordinal()));
        }

        if (StringUtils.isNotEmpty(keyword)) {
            String matchCName = MatchCNameBuilder.INSTANCE.build(AlarmRecord.ALARM_MESSAGE);
            boolQueryBuilder.must().add(QueryBuilders.matchQuery(matchCName, keyword));
        }

        sourceBuilder.query(boolQueryBuilder);
        sourceBuilder.size(limit);
        sourceBuilder.from(from);

        SearchResponse response = getClient().search(AlarmRecord.INDEX_NAME, sourceBuilder);

        Alarms alarms = new Alarms();
        alarms.setTotal((int)response.getHits().totalHits);

        for (SearchHit searchHit : response.getHits().getHits()) {
            AlarmRecord.Builder builder = new AlarmRecord.Builder();
            AlarmRecord alarmRecord = builder.map2Data(searchHit.getSourceAsMap());

            AlarmMessage message = new AlarmMessage();
            message.setId(String.valueOf(alarmRecord.getId0()));
            message.setMessage(alarmRecord.getAlarmMessage());
            message.setStartTime(alarmRecord.getStartTime());
            message.setScope(Scope.valueOf(alarmRecord.getScope()));
            alarms.getMsgs().add(message);
        }
        return alarms;
    }
}
