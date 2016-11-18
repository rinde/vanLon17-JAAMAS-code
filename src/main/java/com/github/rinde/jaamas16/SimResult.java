/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.jaamas16;

import java.io.Serializable;
import java.util.List;

import com.github.rinde.jaamas16.PerformExperiment.AuctionStats;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel.AuctionEvent;
import com.github.rinde.logistics.pdptw.mas.comm.Bidder;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlanner;
import com.github.rinde.rinsim.central.SolverTimeMeasurement;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

@AutoValue
abstract class SimResult implements Serializable {
  private static final long serialVersionUID = 6324066851233398736L;

  abstract List<LogEntry> getLog();

  abstract long getRtCount();

  abstract long getStCount();

  abstract StatisticsDTO getStats();

  abstract ImmutableList<RealtimeTickInfo> getTickInfoList();

  abstract Optional<AuctionStats> getAuctionStats();

  abstract ImmutableList<AuctionEvent> getAuctionEvents();

  abstract ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> getBidTimeMeasurements();

  abstract ImmutableListMultimap<RoutePlanner, SolverTimeMeasurement> getRpTimeMeasurements();

  static SimResult create(List<LogEntry> log, long rt, long st,
      StatisticsDTO stats, ImmutableList<RealtimeTickInfo> dev,
      Optional<AuctionStats> aStats,
      ImmutableList<AuctionEvent> auctionEvents,
      ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> timeMeasurements,
      ImmutableListMultimap<RoutePlanner, SolverTimeMeasurement> rpTimeMeasurements) {
    return new AutoValue_SimResult(log, rt, st, stats, dev, aStats,
      auctionEvents, timeMeasurements, rpTimeMeasurements);
  }
}
