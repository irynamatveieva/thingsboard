/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.stats;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultTbPrintStatsExecutorService implements TbPrintStatsExecutorService {

    private ScheduledExecutorService service;

    @PostConstruct
    public void init() {
        this.service = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-print-stats-scheduler"));
    }

    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    public void scheduleWithFixedDelay(Runnable command, long initialDelay, long period, TimeUnit unit) {
        service.scheduleWithFixedDelay(command, initialDelay, period, unit);
    }

    @Override
    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        service.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
